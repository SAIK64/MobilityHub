import { apiRequest, announce, announceRequestError, downloadFile } from './common.js';

function profileFields() {
  // Liste les champs autorisés à être chargés et enregistrés depuis le profil.
  return ['firstName', 'lastName', 'birthDate', 'nationality', 'email', 'phone', 'passport'];
}

function parseBirthDate(value) {
  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  const french = /^(\d{1,2})\s+([a-zéû]+)\s+(\d{4})$/i.exec(value.trim());
  const months = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
  let year;
  let month;
  let day;
  if (iso) {
    [, year, month, day] = iso.map(Number);
  } else if (french) {
    day = Number(french[1]);
    month = months.indexOf(french[2].toLocaleLowerCase('fr-FR')) + 1;
    year = Number(french[3]);
  } else {
    return null;
  }
  if (!month || year < 1) return null;
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null;
  return date;
}

function updateProfileSummary(profile) {
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(' ');
  document.querySelectorAll('[data-profile-name]').forEach((element) => { element.textContent = name || 'Mon profil'; });
  const date = profile.birthDate ? parseBirthDate(profile.birthDate) : null;
  const today = new Date();
  let age = date ? today.getFullYear() - date.getFullYear() : null;
  if (date && (today.getMonth() < date.getMonth() || (today.getMonth() === date.getMonth() && today.getDate() < date.getDate()))) age--;
  const ageText = age !== null && age >= 0 ? `${age} ans` : null;
  document.querySelectorAll('[data-profile-age]').forEach((element) => {
    element.textContent = ageText ? `Âge : ${ageText}` : profile.birthDate ? 'Âge indisponible : vérifiez la date.' : 'Âge non renseigné.';
  });
  document.querySelectorAll('[data-profile-summary]').forEach((element) => {
    element.textContent = [ageText, profile.nationality].filter(Boolean).join(' · ') || 'Complétez vos informations personnelles.';
  });
}

// Recopie les informations renvoyées par le serveur dans le formulaire du profil.
function displayProfile(profile) {
  // Traite chaque champ avec le même mécanisme pour éviter de répéter le code.
  profileFields().forEach((id) => {
    // Cherche le champ HTML correspondant au nom de la propriété.
    const field = document.getElementById(id);
    // N’écrase le champ que si le serveur a bien renvoyé cette propriété.
    if (field && profile[id] !== undefined) field.value = profile[id] ?? '';
  });
  updateProfileSummary(profile);
}

window.showSavedToast = async () => {
  if (!document.getElementById('profile-form').reportValidity()) return;
  // Prépare un objet contenant les valeurs actuellement saisies dans le formulaire.
  const profile = {};
  profileFields().forEach((id) => {
    // Ignore les champs absents de la page et retire les espaces périphériques.
    const field = document.getElementById(id);
    if (field) profile[id] = field.value.trim();
  });
  if (profile.birthDate && (!parseBirthDate(profile.birthDate) || parseBirthDate(profile.birthDate) > new Date())) {
    announce('Vérifiez la date de naissance : utilisez AAAA-MM-JJ ou une date en français, sans date future.');
    return;
  }
  try {
    // Envoie les nouvelles coordonnées au serveur pour les enregistrer.
    const savedProfile = await apiRequest('/api/profile', {
      method: 'PUT',
      body: JSON.stringify(profile)
    });
    // Remplace le formulaire par les valeurs réellement confirmées par le serveur.
    displayProfile(savedProfile);
    // Rend visible le statut de sauvegarde s’il existe dans la page.
    document.getElementById('save-status')?.classList.remove('opacity-0');
    // Confirme la mise à jour du profil.
    announce('Profil enregistré dans la base H2.');
  } catch (error) {
    // Présente l’erreur renvoyée si la sauvegarde échoue.
    announceRequestError('Impossible d’enregistrer le profil', error);
  }
};

// Vérifie les champs du formulaire, sans prétendre modifier un mot de passe serveur.
window.showPasswordToast = () => {
  const current = document.getElementById('current-pwd')?.value || '';
  const password = document.getElementById('new-pwd')?.value || '';
  const confirmation = document.getElementById('confirm-pwd')?.value || '';
  if (!current || !password || password !== confirmation) {
    announce('Vérifiez le mot de passe actuel et la confirmation.');
    return;
  }
  announce('Aucun compte serveur n’est configuré : le mot de passe n’a pas été modifié.');
};

window.exportTravelData = async () => {
  try {
    // Charge tous les voyages appartenant à la personne connectée.
    const trips = await apiRequest('/api/trips');
    // Récupère le profil et les détails de chaque voyage en parallèle.
    const [profile, tripData] = await Promise.all([
      apiRequest('/api/profile'),
      // Ajoute à chaque voyage ses titres et ses dépenses enregistrés.
      Promise.all(trips.map(async (trip) => ({
        ...trip,
        tickets: await apiRequest(`/api/trips/${trip.id}/tickets`),
        expenses: await apiRequest(`/api/trips/${trip.id}/expenses`)
      })))
    ]);
    // Crée un fichier JSON lisible contenant le profil et les données de voyage.
    downloadFile('donnees-voyage-saik.json', JSON.stringify({ profile, trips: tripData }, null, 2), 'application/json');
    // Confirme le téléchargement.
    announce('Données de voyage exportées depuis H2.');
  } catch (error) {
    // Explique l’échec si les données ne peuvent pas être récupérées ou exportées.
    announceRequestError('Impossible d’exporter les données', error);
  }
};

// Précise que l’application ne propose pas encore de révocation de sessions distantes.
window.revokeSessions = () => announce('Aucun service de compte distant n’est configuré : aucune session n’a été révoquée.');

// Relie le choix d’une photo à un aperçu local dans la page profil.
function initializeProfile() {
  const form = document.getElementById('profile-form');
  form?.addEventListener('input', () => {
    const profile = Object.fromEntries(profileFields().map((id) => [id, document.getElementById(id)?.value.trim() || '']));
    updateProfileSummary(profile);
    document.getElementById('save-status')?.classList.add('opacity-0');
  });
  document.querySelectorAll('button').forEach((button) => {
    if (`${button.textContent} ${button.title}`.toLowerCase().includes('photo de profil')) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.className = 'hidden';
      input.addEventListener('change', () => {
        const file = input.files[0];
        if (!file) return;
        const avatar = document.querySelector('img[alt="Photo de profil"], img[alt="Profile"], img[alt="Iris Geyer"]');
        if (avatar) avatar.src = URL.createObjectURL(file);
        announce('Aperçu de la photo mis à jour pour cette session.');
      });
      button.addEventListener('click', () => input.click());
      button.dataset.appHandled = 'true';
      button.after(input);
    }
  });
}


export { initializeProfile, displayProfile };
