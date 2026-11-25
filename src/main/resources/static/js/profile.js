import { apiRequest, announce, announceRequestError, downloadFile } from './common.js';

// Comportements d’interface de la page profil.
// Le formulaire est pré-rempli et enregistré par ProfilController (POST /profil).

function parseBirthDate(value) {
  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  if (!iso) return null;
  const [, year, month, day] = iso.map(Number);
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null;
  return date;
}

// Met à jour l’âge affiché pendant la saisie (aperçu ; la valeur de référence est calculée par le serveur).
function updateAgePreview() {
  const value = document.getElementById('birthDate')?.value || '';
  const date = value ? parseBirthDate(value) : null;
  const today = new Date();
  let age = date ? today.getFullYear() - date.getFullYear() : null;
  if (date && (today.getMonth() < date.getMonth() || (today.getMonth() === date.getMonth() && today.getDate() < date.getDate()))) age--;
  document.querySelectorAll('[data-profile-age]').forEach((element) => {
    element.textContent = age !== null && age >= 0 ? `Âge : ${age} ans`
      : value ? 'Âge indisponible : vérifiez la date (AAAA-MM-JJ).' : 'Âge non renseigné.';
  });
}

// Exporte le profil et les voyages au format JSON (via l’API REST).
window.exportTravelData = async () => {
  try {
    const trips = await apiRequest('/api/trips');
    const [profile, tripData] = await Promise.all([
      apiRequest('/api/profile'),
      Promise.all(trips.map(async (trip) => ({
        ...trip,
        tickets: await apiRequest(`/api/trips/${trip.id}/tickets`),
        expenses: await apiRequest(`/api/trips/${trip.id}/expenses`)
      })))
    ]);
    downloadFile('donnees-voyage-saik.json', JSON.stringify({ profile, trips: tripData }, null, 2), 'application/json');
    announce('Données de voyage exportées depuis H2.');
  } catch (error) {
    announceRequestError('Impossible d’exporter les données', error);
  }
};


function initializeProfile() {
  document.getElementById('birthDate')?.addEventListener('input', updateAgePreview);
}

export { initializeProfile };
