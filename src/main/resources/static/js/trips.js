import { apiRequest, announce, announceRequestError, createTextElement, downloadFile } from './common.js';

const selectedTripStorageKey = 'saik.selected-trip-id';
let databaseTrips = [];
let currentTripId = null;

function setDatabaseTrips(trips) {
databaseTrips = trips;
}

// Vérifie que le voyage mémorisé existe toujours dans la liste chargée depuis le serveur.
function selectedDatabaseTripId() {
  // Privilégie la sélection en mémoire, puis relit celle conservée pendant la session.
  const selected = currentTripId || sessionStorage.getItem(selectedTripStorageKey);
  // Évite d’utiliser un identifiant périmé après une mise à jour des voyages.
  return databaseTrips.some((trip) => String(trip.id) === String(selected)) ? selected : null;
}

// Efface à la fois la sélection en mémoire et celle conservée dans l’onglet.
function clearDatabaseTripSelection() {
  currentTripId = null;
  sessionStorage.removeItem(selectedTripStorageKey);
}

// Rend le voyage actif et synchronise la sélection avec les autres éléments de l’interface.
function selectDatabaseTrip(trip, announceSelection = true) {
  const countries = trip.countries?.length ? trip.countries : [{ country: trip.country, iso: trip.iso }];
  const countryNames = countries.map((country) => country.country).join(', ');
  const countryCodes = countries.map((country) => country.iso).join(', ');
  // Garde l’identifiant sous forme de texte pour comparer uniformément les valeurs du DOM.
  currentTripId = String(trip.id);
  // Conserve la sélection pendant la session du navigateur.
  sessionStorage.setItem(selectedTripStorageKey, currentTripId);
  document.querySelectorAll('[data-selected-trip]').forEach((element) => {
    element.textContent = `Voyage sélectionné : ${trip.name} · ${countryNames}`;
  });
  // Met à jour le bandeau du voyage si le tableau de bord expose cette fonction.
  window.switchTrip?.(trip.name, countryCodes, trip.start, trip.end);
  // Confirme la sélection sauf si l’appelant demande une mise à jour silencieuse.
  if (announceSelection) announce(`Voyage sélectionné : ${trip.name}.`);
}

// Ajoute un voyage enregistré dans le menu déroulant.
function addTripToMenu(trip) {
  // Cherche le menu qui contient les voyages disponibles.
  const menu = document.getElementById('tripDropdownMenu');
  // Ne fait rien si la page courante ne possède pas ce menu.
  if (!menu) return;

  // Crée le bouton qui représentera le voyage dans le menu.
  const button = document.createElement('button');
  // Empêche ce bouton d’envoyer un formulaire par défaut.
  button.type = 'button';
  // Stocke l’identifiant pour reconnaître les voyages déjà ajoutés.
  button.dataset.savedTrip = String(trip.id);
  // Signale qu’un gestionnaire spécifique est déjà associé à ce bouton.
  button.dataset.appHandled = 'true';
  // Applique les classes de mise en forme du menu.
  button.className = 'w-full text-left px-space-md py-2.5 hover:bg-surface-container-low flex items-center justify-between group transition-colors';
  // Affiche le nom du voyage dans le bouton.
  button.append(createTextElement('span', 'font-label-lg text-label-lg text-on-surface', trip.name));
  // Affiche aussi le pays correspondant.
  const countries = trip.countries?.length ? trip.countries : [{ country: trip.country, iso: trip.iso }];
  button.append(createTextElement('span', 'font-label-md text-label-md text-on-surface-variant', countries.map((country) => country.country).join(', ')));
  // Sélectionne le voyage au clic.
  button.addEventListener('click', () => selectDatabaseTrip(trip));
  // Insère le nouveau bouton dans le menu.
  menu.append(button);
}

// Relie le menu des voyages et les cartes déjà présentes sur le tableau de bord.
function initializeTrips() {
  const dropdown = document.getElementById('tripDropdownBtn');
  if (dropdown) dropdown.dataset.appHandled = 'true';
  const menu = document.getElementById('tripDropdownMenu');
  menu?.querySelectorAll('button:not([data-saved-trip])').forEach((button) => {
    button.dataset.appHandled = 'true';
    button.addEventListener('click', () => {
      const title = button.querySelector('.font-label-lg')?.textContent.trim();
      if (!title) return;
      const iso = button.textContent.match(/\(([A-Z]{2})\)/)?.[1] || '';
      const status = button.textContent.includes('Archivé') ? 'Archivé'
        : button.textContent.includes('Prévu') ? 'À venir' : 'En cours';
      const countdown = button.textContent.match(/Départ dans (\d+)/)?.[1];
      clearDatabaseTripSelection();
      document.getElementById('selectedTripName').textContent = title;
      document.getElementById('heroTripTitle').textContent = title;
      document.getElementById('heroTripIso').textContent = iso;
      document.getElementById('heroTripStatusBadge').textContent = status;
      const dates = button.querySelector('.text-on-surface-variant')?.textContent.trim();
      if (dates) document.getElementById('heroTripDates').textContent = dates;
      if (countdown) document.getElementById('countdownDays').textContent = countdown;
      menu.classList.add('hidden');
      document.getElementById('tripDropdownChevron')?.classList.remove('rotate-180');
      announce(`Voyage sélectionné : ${title}.`);
    });
  });
  bindTripModalMode();
  if (new URLSearchParams(window.location.search).get('action') === 'new-trip') {
    window.openModal?.();
  }
}

// Retrouve la zone HTML où les cartes des voyages enregistrés doivent apparaître.
function savedTripsContainer() {
  const heading = [...document.querySelectorAll('h3')]
    .find((element) => element.textContent.includes('Voyages & Escapades Enregistrés'));
  const section = heading?.closest('div.bg-surface-container-lowest');
  return section?.children[1] || null;
}

function updateTripCounts() {
  const container = savedTripsContainer();
  if (!container) return;
  container.parentElement.querySelectorAll('button').forEach((button) => {
    const match = button.textContent.trim().match(/^(Tous|Actif|Futur|Archivé)/);
    if (!match) return;
    const cards = [...container.querySelectorAll('[data-saved-trip-card]')];
    button.textContent = `${match[1]} (${match[1] === 'Tous' ? cards.length : cards.filter((card) => card.dataset.tripStatus === match[1]).length})`;
  });
}

// Sélectionne un voyage enregistré ou une carte de démonstration.
function selectTripCard(card) {
  // Essaie d’abord de retrouver ce voyage parmi ceux qui viennent de la base.
  const databaseTrip = databaseTrips.find((trip) => String(trip.id) === card.dataset.databaseTripId);
  // Si le voyage vient de la base, sa sélection pilote aussi les formulaires.
  if (databaseTrip) {
    selectDatabaseTrip(databaseTrip);
    return;
  }
  // Les cartes de démonstration ne correspondent pas à un voyage persistant.
  clearDatabaseTripSelection();
  // Récupère le titre affiché sur la carte.
  const name = card.querySelector('h4')?.textContent.trim();
  // Ignore la carte si elle ne contient pas de nom.
  if (!name) return;
  // Met à jour le nom du voyage sélectionné dans les différentes zones du tableau de bord.
  document.getElementById('selectedTripName').textContent = name;
  document.getElementById('heroTripTitle').textContent = name;
  // Récupère le code pays de la carte, ou laisse un texte vide s’il manque.
  document.getElementById('heroTripIso').textContent = card.querySelector('.w-12')?.textContent.trim() || '';
  // Affiche le statut stocké sur la carte, avec « En cours » comme valeur de secours.
  document.getElementById('heroTripStatusBadge').textContent = card.dataset.tripStatus || 'En cours';
  // Lit les dates de la carte et les affiche lorsqu’elles sont présentes.
  const dates = card.querySelector('p')?.textContent.trim();
  if (dates) document.getElementById('heroTripDates').textContent = dates;
  // Confirme la sélection à la personne.
  announce(`Voyage sélectionné : ${name}.`);
}

// Construit une carte visuelle pour un voyage provenant de la base.
function appendSavedTripCard(trip) {
  // Trouve la zone qui contient les voyages enregistrés.
  const container = savedTripsContainer();
  // Évite les doublons et ignore les pages sans cette zone.
  if (!container || container.querySelector(`[data-saved-trip-card="${trip.id}"]`)) return;
  container.querySelector('[data-empty-trips]')?.remove();
  // Compare les dates au jour courant pour déterminer le statut du séjour.
  const today = new Date().toISOString().slice(0, 10);
  const status = trip.end < today ? 'Archivé' : trip.start > today ? 'Futur' : 'Actif';
  // Crée l’enveloppe de la carte.
  const card = document.createElement('div');
  // Stocke des attributs de données pour pouvoir retrouver et sélectionner la carte.
  card.dataset.savedTripCard = trip.id;
  card.dataset.databaseTripId = String(trip.id);
  card.dataset.tripStatus = status;
  // Applique la mise en page de la carte.
  card.className = 'bg-surface-container-lowest p-space-md rounded-xl flex flex-col md:flex-row md:items-center justify-between gap-space-md shadow-sm';
  const details = document.createElement('div');
  details.className = 'flex items-center gap-space-md';
  const countries = trip.countries?.length ? trip.countries : [{ country: trip.country, iso: trip.iso }];
  details.append(createTextElement('div', 'w-12 h-12 rounded-lg bg-surface-container flex items-center justify-center font-bold text-headline-sm text-primary shrink-0', countries.map((country) => country.iso).join(', ')));
  const copy = document.createElement('div');
  const heading = document.createElement('h4');
  heading.className = 'font-label-lg text-label-lg text-primary font-bold';
  heading.textContent = trip.name;
  const badge = createTextElement('span', 'ml-2 rounded-full bg-surface-container-high px-2 py-0.5 text-[10px] font-semibold text-on-surface-variant', status);
  const title = document.createElement('div');
  title.append(heading, badge);
  const countrySummary = createTextElement('p', 'mt-0.5 text-body-sm text-on-surface-variant', `${countries.map((country) => `${country.country} (${country.iso})`).join(', ')} · ${trip.start} - ${trip.end}`);
  countrySummary.dataset.tripCountrySummary = 'true';
  const countryList = document.createElement('div');
  countryList.className = 'mt-2 flex flex-wrap gap-2';
  countryList.dataset.tripCountryList = 'true';
  renderTripCountryList(trip, countryList);
  copy.append(title, countrySummary, countryList);
  details.append(copy);
  const select = createTextElement('button', 'rounded-lg bg-surface-container-high px-space-md py-2 font-label-md text-label-md text-on-surface hover:bg-surface-container transition-colors', 'Sélectionner');
  select.type = 'button';
  select.dataset.appHandled = 'true';
  select.addEventListener('click', () => selectTripCard(card));
  const edit = createTextElement('button', 'rounded-lg bg-surface-container px-space-md py-2 font-label-md text-label-md text-primary hover:bg-surface-container-high transition-colors', 'Modifier');
  edit.type = 'button';
  edit.dataset.appHandled = 'true';
  edit.addEventListener('click', () => openTripEditor(trip));
  const remove = createTextElement('button', 'rounded-lg px-space-md py-2 font-label-md text-label-md text-error hover:bg-error-container transition-colors', 'Supprimer');
  remove.type = 'button';
  remove.dataset.appHandled = 'true';
  remove.addEventListener('click', () => deleteTrip(trip));
  const actions = document.createElement('div');
  actions.className = 'flex flex-wrap items-center gap-space-sm';
  actions.append(select, edit, remove);
  const countryManager = document.createElement('form');
  countryManager.className = 'flex flex-wrap items-end gap-2';
  countryManager.dataset.tripCountryManager = String(trip.id);
  const nameInput = createTextElement('label', 'flex flex-col gap-1 text-xs text-on-surface-variant', 'Ajouter un pays');
  const countryInput = document.createElement('input');
  countryInput.type = 'text';
  countryInput.required = true;
  countryInput.placeholder = 'Pays';
  countryInput.dataset.countryName = 'true';
  countryInput.className = 'rounded-lg bg-surface-container-low px-3 py-2 text-sm text-on-surface';
  nameInput.append(countryInput);
  const isoInput = document.createElement('input');
  isoInput.type = 'text';
  isoInput.required = true;
  isoInput.maxLength = 2;
  isoInput.pattern = '[A-Za-z]{2}';
  isoInput.placeholder = 'ISO';
  isoInput.setAttribute('aria-label', 'Code ISO du pays');
  isoInput.className = 'w-20 rounded-lg bg-surface-container-low px-3 py-2 text-sm uppercase text-on-surface';
  const addCountryButton = createTextElement('button', 'rounded-lg bg-surface-container px-space-md py-2 font-label-md text-label-md text-primary', 'Ajouter');
  addCountryButton.type = 'submit';
  addCountryButton.dataset.appHandled = 'true';
  countryManager.append(nameInput, isoInput, addCountryButton);
  countryManager.addEventListener('submit', (event) => addTripCountry(event, trip, countryManager));
  card.append(details, countryManager, actions);
  container.append(card);
  updateTripCounts();
}

function renderTripCountryList(trip, container) {
  container.replaceChildren();
  (trip.countries || []).forEach((country) => {
    const chip = document.createElement('span');
    chip.className = 'inline-flex items-center gap-1 rounded-full bg-surface-container-low px-2 py-1 text-xs text-on-surface';
    chip.append(document.createTextNode(`${country.country} (${country.iso})`));
    const remove = createTextElement('button', 'rounded-full px-1 text-on-surface-variant hover:text-error', '×');
    remove.type = 'button';
    remove.setAttribute('aria-label', `Retirer ${country.country} du voyage`);
    remove.dataset.appHandled = 'true';
    remove.addEventListener('click', () => removeTripCountry(trip, country.iso));
    chip.append(remove);
    container.append(chip);
  });
}

async function addTripCountry(event, trip, form) {
  event.preventDefault();
  const country = form.querySelector('[data-country-name="true"]').value.trim();
  const iso = form.querySelector('input[aria-label="Code ISO du pays"]').value.trim().toUpperCase();
  if (!/^[A-Z]{2}$/.test(iso)) {
    announce('Le code pays doit contenir exactement deux lettres.');
    return;
  }
  try {
    await updateTripCountries(trip, await apiRequest(`/api/trips/${trip.id}/countries`, {
      method: 'POST',
      body: JSON.stringify({ country, iso })
    }));
    form.reset();
    announce(`${country} ajouté au voyage.`);
  } catch (error) {
    announceRequestError('Impossible d’ajouter le pays', error);
  }
}

async function removeTripCountry(trip, iso) {
  try {
    await updateTripCountries(trip, await apiRequest(`/api/trips/${trip.id}/countries/${encodeURIComponent(iso)}`, {
      method: 'DELETE'
    }));
    announce('Pays retiré du voyage.');
  } catch (error) {
    announceRequestError('Impossible de retirer le pays', error);
  }
}

function updateTripCountries(trip, updatedTrip) {
  Object.assign(trip, updatedTrip);
  const card = document.querySelector(`[data-saved-trip-card="${trip.id}"]`);
  if (card) {
    const countries = trip.countries || [];
    const today = new Date().toISOString().slice(0, 10);
    const status = trip.end < today ? 'Archivé' : trip.start > today ? 'Futur' : 'Actif';
    card.dataset.tripStatus = status;
    const heading = card.querySelector('h4');
    if (heading) heading.textContent = trip.name;
    const badge = heading?.parentElement?.querySelector('span');
    if (badge) badge.textContent = status;
    const summary = card.querySelector('[data-trip-country-summary]');
    const dates = `${trip.start} - ${trip.end}`;
    summary.textContent = `${countries.map((country) => `${country.country} (${country.iso})`).join(', ')} · ${dates}`;
    const list = card.querySelector('[data-trip-country-list]');
    renderTripCountryList(trip, list);
    const isoBadge = card.querySelector('.w-12');
    if (isoBadge) isoBadge.textContent = countries.map((country) => country.iso).join(', ');
    updateTripCounts();
  }
  const menuButton = document.querySelector(`[data-saved-trip="${trip.id}"]`);
  if (menuButton) {
    const nameLabel = menuButton.querySelector('.font-label-lg');
    if (nameLabel) nameLabel.textContent = trip.name;
    const label = menuButton.querySelector('.text-on-surface-variant');
    if (label) label.textContent = (trip.countries || []).map((country) => country.country).join(', ');
  }
  if (String(currentTripId) === String(trip.id)) selectDatabaseTrip(trip, false);
}

async function updateTrip(trip, payload) {
  const updatedTrip = await apiRequest(`/api/trips/${trip.id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
  updateTripCountries(trip, updatedTrip);
  return trip;
}

async function deleteTrip(trip) {
  if (!window.confirm(`Supprimer le voyage « ${trip.name} » ? Les billets et dépenses associés seront également retirés.`)) {
    return;
  }
  try {
    await apiRequest(`/api/trips/${trip.id}`, { method: 'DELETE' });
    setDatabaseTrips(databaseTrips.filter((item) => String(item.id) !== String(trip.id)));
    document.querySelector(`[data-saved-trip-card="${trip.id}"]`)?.remove();
    document.querySelector(`[data-saved-trip="${trip.id}"]`)?.remove();
    const container = savedTripsContainer();
    if (container && !container.querySelector('[data-saved-trip-card]')) {
      const empty = createTextElement('p', 'text-on-surface-variant', 'Aucun voyage enregistré. Créez votre premier voyage.');
      empty.dataset.emptyTrips = 'true';
      container.append(empty);
    }
    updateTripCounts();
    if (String(currentTripId) === String(trip.id)) {
      const fallback = databaseTrips[0];
      if (fallback) selectDatabaseTrip(fallback, false);
      else {
        clearDatabaseTripSelection();
        document.querySelectorAll('[data-selected-trip]').forEach((element) => {
          element.textContent = 'Aucun voyage sélectionné. Créez un voyage depuis le tableau de bord.';
        });
      }
    }
    announce(`Voyage « ${trip.name} » supprimé.`);
  } catch (error) {
    announceRequestError('Impossible de supprimer le voyage', error);
  }
}

function resetTripFormMode() {
  const form = document.getElementById('newTripForm');
  if (!form) return;
  delete form.dataset.editingTripId;
  const title = document.getElementById('tripModalTitle');
  if (title) title.textContent = 'Nouveau Voyage & Destination';
  const submit = document.getElementById('tripSubmitButton');
  if (submit) submit.textContent = 'Créer et Enregistrer';
}

function bindTripModalMode() {
  const originalOpen = window.openModal;
  window.openModal = () => {
    if (!document.getElementById('newTripForm')?.dataset.editingTripId) {
      resetTripFormMode();
      initializeTripCountryRows();
    }
    originalOpen?.();
  };
  const originalClose = window.closeModal;
  window.closeModal = () => {
    originalClose?.();
    resetTripFormMode();
    document.getElementById('newTripForm')?.reset();
    initializeTripCountryRows();
  };
}

function openTripEditor(trip) {
  const form = document.getElementById('newTripForm');
  const container = document.getElementById('tripCountriesContainer');
  if (!form || !container) return;
  const template = container.querySelector('[data-trip-country-row]');
  form.dataset.editingTripId = String(trip.id);
  document.getElementById('tripModalTitle').textContent = 'Modifier le voyage';
  document.getElementById('tripSubmitButton').textContent = 'Enregistrer les modifications';
  document.getElementById('tripNameInput').value = trip.name;
  document.getElementById('tripStartInput').value = trip.start;
  document.getElementById('tripEndInput').value = trip.end;
  const countries = trip.countries?.length ? trip.countries : [{ country: trip.country, iso: trip.iso }];
  container.replaceChildren();
  countries.forEach((country) => {
    const row = template.cloneNode(true);
    row.querySelector('[data-country-name]').value = country.country;
    row.querySelector('[data-country-iso]').value = country.iso;
    container.append(row);
  });
  const rows = [...container.querySelectorAll('[data-trip-country-row]')];
  rows.forEach((row) => {
    const remove = row.querySelector('[data-remove-trip-country]');
    remove.disabled = rows.length === 1;
    remove.classList.toggle('opacity-40', rows.length === 1);
  });
  window.openModal?.();
}

function readTripForm() {
  const name = document.getElementById('tripNameInput').value.trim();
  const countries = [...document.querySelectorAll('#tripCountriesContainer [data-trip-country-row]')].map((row) => ({
    country: row.querySelector('[data-country-name]').value.trim(),
    iso: row.querySelector('[data-country-iso]').value.trim().toUpperCase()
  }));
  const start = document.getElementById('tripStartInput').value;
  const end = document.getElementById('tripEndInput').value;
  return { name, countries, start, end };
}

// Active les boutons et interactions propres au tableau de bord.
function initializeDashboardControls() {
  const container = savedTripsContainer();
  if (container) {
    container.querySelectorAll(':scope > div').forEach((card) => {
      const text = card.textContent;
      card.dataset.tripStatus = text.includes('Archivé') ? 'Archivé'
        : text.includes('À venir') || text.includes('Prévu') ? 'Futur' : 'Actif';
    });
    container.parentElement.querySelectorAll('button').forEach((button) => {
      const label = button.textContent.trim();
      const filter = label.startsWith('Tous') ? 'Tous'
        : label.startsWith('Actif') ? 'Actif'
          : label.startsWith('Futur') ? 'Futur'
            : label.startsWith('Archivé') ? 'Archivé' : null;
      if (filter) {
        button.dataset.appHandled = 'true';
        button.addEventListener('click', () => {
          container.querySelectorAll(':scope > div').forEach((card) => {
            card.hidden = filter !== 'Tous' && card.dataset.tripStatus !== filter;
          });
          container.parentElement.querySelectorAll('button').forEach((filterButton) => {
            const label = filterButton.textContent.trim();
            if (!/^(Tous|Actif|Futur|Archivé)/.test(label)) return;
            const status = label.match(/^(Tous|Actif|Futur|Archivé)/)[1];
            const count = status === 'Tous'
              ? container.querySelectorAll(':scope > div').length
              : [...container.querySelectorAll(':scope > div')].filter((card) => card.dataset.tripStatus === status).length;
            filterButton.textContent = `${status} (${count})`;
          });
        });
      } else if (label === 'Gérer') {
        button.dataset.appHandled = 'true';
        button.addEventListener('click', () => {
          window.location.href = '/transports';
        });
      } else if (label === 'Sélectionner') {
        button.dataset.appHandled = 'true';
        button.addEventListener('click', () => selectTripCard(button.closest('[data-trip-status]')));
      } else if (label === 'Détails') {
        button.dataset.appHandled = 'true';
        button.addEventListener('click', () => {
          const card = button.closest('[data-trip-status]');
          const summary = card?.querySelector('.text-right');
          if (summary) {
            summary.hidden = !summary.hidden;
            button.setAttribute('aria-expanded', String(!summary.hidden));
          }
        });
      }
    });
  }

  document.querySelectorAll('button').forEach((button) => {
    const label = button.textContent.trim();
    if (label === 'Timeline') {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => announce('Chronologie du séjour affichée.'));
    } else if (label === 'Carte') {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => {
        const tripName = document.getElementById('heroTripTitle')?.textContent || 'voyage';
        window.open(`https://maps.google.com/?q=${encodeURIComponent(tripName)}`, '_blank', 'noopener');
      });
    } else if (label.includes('Réservation 12306')) {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => window.open('https://www.12306.cn/en/', '_blank', 'noopener'));
    } else if (label.includes("Télécharger Carte d'Accès")) {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => {
        const card = button.parentElement;
        downloadFile('carte-acces-saik.txt', card.textContent.trim(), 'text/plain;charset=utf-8');
        announce('Récapitulatif de la carte d’accès téléchargé au format TXT.');
      });
    }
  });
}

window.handleNewTripSubmit = async (event) => {
  // Empêche le navigateur de recharger la page à l’envoi du formulaire.
  event.preventDefault();
  const { name, countries, start, end } = readTripForm();
  // Vérifie dans le navigateur le format du code pays avant l’appel réseau.
  if (countries.some(({ iso }) => !/^[A-Z]{2}$/.test(iso))) {
    announce('Chaque code pays doit contenir exactement deux lettres.');
    return;
  }
  // Refuse une date de fin antérieure à la date de départ.
  if (end < start) {
    announce('La date de fin doit être postérieure à la date de départ.');
    return;
  }

  const editingId = document.getElementById('newTripForm').dataset.editingTripId;
  const payload = { name, countries, start, end };
  try {
    if (editingId) {
      const trip = databaseTrips.find((item) => String(item.id) === String(editingId));
      if (!trip) throw new Error('Le voyage à modifier est introuvable.');
      await updateTrip(trip, payload);
      window.closeModal();
      announce('Voyage mis à jour.');
      return;
    }
    // Envoie les informations du voyage au serveur pour les enregistrer.
    const trip = await apiRequest('/api/trips', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    // Met à jour la liste locale et les différentes représentations du voyage.
    databaseTrips.push(trip);
    addTripToMenu(trip);
    appendSavedTripCard(trip);
    // Active le nouveau voyage sans afficher une deuxième confirmation.
    selectDatabaseTrip(trip, false);
    // Ferme le formulaire, le réinitialise puis confirme l’enregistrement.
    window.closeModal();
    announce('Voyage enregistré dans la base H2.');
  } catch (error) {
    // Affiche une explication si le serveur n’a pas pu enregistrer le voyage.
    announceRequestError(editingId ? 'Impossible de modifier le voyage' : 'Impossible d’enregistrer le voyage', error);
  }
};

function initializeTripCountryRows() {
  const container = document.getElementById('tripCountriesContainer');
  if (!container) return;
  const updateRemoveButtons = () => {
    const rows = [...container.querySelectorAll('[data-trip-country-row]')];
    rows.forEach((row) => {
      const remove = row.querySelector('[data-remove-trip-country]');
      remove.disabled = rows.length === 1;
      remove.classList.toggle('opacity-40', rows.length === 1);
    });
  };
  if (container.dataset.rowsInitialized !== 'true') {
    document.getElementById('addTripCountryBtn')?.addEventListener('click', () => {
      const row = container.querySelector('[data-trip-country-row]').cloneNode(true);
      row.querySelectorAll('input').forEach((input) => { input.value = ''; });
      container.append(row);
      updateRemoveButtons();
      row.querySelector('[data-country-name]').focus();
    });
    container.addEventListener('click', (event) => {
      const remove = event.target.closest('[data-remove-trip-country]');
      if (remove && container.querySelectorAll('[data-trip-country-row]').length > 1) {
        remove.closest('[data-trip-country-row]').remove();
        updateRemoveButtons();
      }
    });
    container.dataset.rowsInitialized = 'true';
  } else {
    [...container.querySelectorAll('[data-trip-country-row]')].slice(1).forEach((row) => row.remove());
  }
  updateRemoveButtons();
}

function requireSelectedTripId() {
  // Lit l’identifiant actuellement sélectionné et vérifié.
  const tripId = selectedDatabaseTripId();
  // Empêche l’enregistrement d’un titre sans voyage correspondant en base.
  if (!tripId) throw new Error('Crée d’abord un voyage ou sélectionne un voyage enregistré.');
  // Retourne l’identifiant prêt à être utilisé dans l’URL de l’API.
  return tripId;
}


export {
selectedTripStorageKey, databaseTrips, setDatabaseTrips, selectedDatabaseTripId,
clearDatabaseTripSelection, selectDatabaseTrip, addTripToMenu, appendSavedTripCard,
savedTripsContainer, initializeTrips, initializeDashboardControls, initializeTripCountryRows,
requireSelectedTripId
};
