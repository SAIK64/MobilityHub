import { apiRequest, announce, announceRequestError, createTextElement, downloadFile } from './common.js';
import { requireSelectedTripId } from './trips.js';

// Ajoute à la page un titre de transport déjà enregistré.
function appendTicket(ticket) {
  // Cherche la zone des billets et évite d’insérer deux fois le même titre.
  const container = document.getElementById('view-mes-billets');
  if (!container || container.querySelector(`[data-saved-ticket="${ticket.id}"]`)) return;
  container.querySelector('[data-empty-tickets]')?.remove();

  // Crée une carte pour présenter le titre.
  const card = document.createElement('article');
  // Enregistre son identifiant et son type pour permettre les filtres ultérieurs.
  card.dataset.savedTicket = ticket.id;
  card.dataset.transportType = ticket.type;
  card.dataset.appHandled = 'true';
  // Applique l’apparence de carte définie par l’interface.
  card.className = 'mt-space-md overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm md:grid md:grid-cols-[2fr_3fr]';
  const flight = ticket.flightDetails;
  const formatTime = (value) => value ? String(value).slice(11, 16) : '';
  const formatDate = (value) => value ? new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${String(value).slice(0, 10)}T00:00:00`)) : '';
  const hasArrival = Boolean(ticket.arrivalTime);
  const dayShift = hasArrival ? Math.round((new Date(`${String(ticket.arrivalTime).slice(0, 10)}T00:00:00`) - new Date(`${String(ticket.departureTime).slice(0, 10)}T00:00:00`)) / 86400000) : 0;
  const iconLabel = (icon, text) => {
    const item = createTextElement('span', 'inline-flex items-center gap-1', '');
    item.append(createTextElement('span', 'material-symbols-outlined text-[18px] text-primary', icon), document.createTextNode(text));
    return item;
  };

  // Panneau sombre à gauche : type, trajet et classe/siège.
  const side = document.createElement('div');
  side.className = 'flex flex-col justify-between gap-space-lg p-space-lg text-white';
  side.style.background = 'linear-gradient(160deg, #1f3d1c 0%, #0f2410 100%)';
  const sideTop = document.createElement('div');
  sideTop.className = 'flex items-center justify-between gap-2 text-label-md';
  sideTop.append(
    createTextElement('span', 'rounded-full bg-white/15 px-3 py-1', ticket.type),
    createTextElement('span', 'opacity-80', flight?.flightNumber || ticket.reference || '')
  );
  const route = document.createElement('div');
  route.append(createTextElement('p', 'text-4xl font-bold tracking-tight', flight ? `${flight.departureIata} → ${flight.arrivalIata}` : '→'));
  route.append(createTextElement('p', 'mt-2 text-body-sm opacity-80', flight ? `${flight.departureAirport} à ${flight.arrivalAirport}` : `${ticket.departure || ''} → ${ticket.arrival || ''}`));
  if (!flight) route.firstChild.textContent = `${ticket.departure || '—'} → ${ticket.arrival || '—'}`;
  const sideBottom = document.createElement('div');
  sideBottom.className = 'flex justify-between gap-2 border-t border-white/20 pt-3 text-label-md font-semibold';
  sideBottom.append(
    createTextElement('span', '', flight?.fareType || ''),
    createTextElement('span', '', flight?.seatChoice ? `Siège ${flight.seatChoice}` : '')
  );
  side.append(sideTop, route, sideBottom);

  // Zone principale à droite.
  const main = document.createElement('div');
  main.className = 'flex flex-col gap-space-sm p-space-lg';
  const badges = document.createElement('div');
  badges.className = 'flex flex-wrap items-center gap-2 text-label-md';
  if (flight?.ticketStatus) badges.append(createTextElement('span', 'rounded-full bg-[#c9efc0] px-3 py-1 font-semibold text-[#1f3d1c]', `${flight.ticketStatus}${flight.airline ? ` • ${flight.airline}` : ''}`));
  if (ticket.reference) badges.append(createTextElement('span', 'rounded-md bg-surface-container px-2 py-1 font-mono', `PNR : ${ticket.reference}`));
  if (badges.childElementCount) main.append(badges);
  if (flight?.eticketNumber) main.append(createTextElement('p', 'text-body-md text-on-surface-variant', `• E-Billet n° ${flight.eticketNumber}`));
  const heading = document.createElement('div');
  heading.className = 'flex items-start justify-between gap-space-md';
  heading.append(createTextElement('h3', 'font-headline-sm text-headline-sm text-primary font-bold', ticket.description || `Transport ${ticket.type}`));
  const remove = createTextElement('button', 'rounded-lg px-3 py-1 text-label-md text-error hover:bg-error-container', 'Supprimer');
  remove.type = 'button';
  remove.dataset.appHandled = 'true';
  remove.addEventListener('click', async () => {
    try {
      // Demande au serveur de supprimer le billet avant de le retirer de l’écran.
      await apiRequest(`/api/tickets/${ticket.id}`, { method: 'DELETE' });
      // Retire la carte et confirme la suppression.
      card.remove();
      if (!container.querySelector('[data-saved-ticket]')) {
        const empty = createTextElement('p', 'text-on-surface-variant', 'Aucun billet enregistré pour ce voyage.');
        empty.dataset.emptyTickets = 'true';
        container.append(empty);
      }
      announce('Titre supprimé de la base H2.');
    } catch (error) {
      // Préserve la carte à l’écran et explique l’échec si l’API refuse la suppression.
      announceRequestError('Impossible de supprimer le titre', error);
    }
  });
  heading.append(remove);
  main.append(heading);
  const subtitle = [
    flight?.stopCount === 0 ? 'Vol direct' : flight?.stopCount ? `${flight.stopCount} escale(s)` : '',
    flight?.cabinBaggage ? `Cabine : ${flight.cabinBaggage}${flight.cabinBaggageWeightKg ? ` ${flight.cabinBaggageWeightKg} kg` : ''}` : '',
    flight?.checkedBaggage ? `Soute : ${flight.checkedBaggage}${flight.checkedBaggageWeightKg ? ` ${flight.checkedBaggageWeightKg} kg` : ''}` : ''
  ].filter(Boolean).join(' • ');
  if (subtitle) main.append(createTextElement('p', 'text-body-sm text-on-surface-variant', subtitle));
  main.append(createTextElement('p', 'text-3xl font-bold text-primary', `${Number(ticket.price || 0).toFixed(2).replace('.', ',')} €`));

  // Bloc horaires : départ, durée, arrivée.
  const timeline = document.createElement('div');
  timeline.className = 'grid grid-cols-[1fr_auto_1fr] items-center gap-3 rounded-xl bg-[#eef2e3] p-space-md text-on-surface';
  const departureBox = document.createElement('div');
  departureBox.append(
    createTextElement('p', 'text-label-md font-semibold uppercase text-on-surface-variant', 'Départ'),
    createTextElement('p', 'text-2xl font-bold', formatTime(ticket.departureTime) || '—'),
    createTextElement('p', 'text-body-sm text-on-surface-variant', [formatDate(ticket.departureTime), flight?.departureAirport].filter(Boolean).join(' • '))
  );
  const middle = document.createElement('div');
  middle.className = 'flex flex-col items-center gap-1 text-label-md font-semibold';
  const durationMinutes = flight?.durationMinutes;
  middle.append(createTextElement('p', '', durationMinutes ? `Durée : ${Math.floor(durationMinutes / 60)}h ${String(durationMinutes % 60).padStart(2, '0')}min` : ticket.type));
  middle.append(createTextElement('span', 'material-symbols-outlined text-primary', ticket.type === 'Train' ? 'train' : ticket.type === 'Avion' ? 'flight' : 'arrow_right_alt'));
  if (flight) middle.append(createTextElement('p', '', flight.stopCount === 0 ? 'Direct sans escale' : `${flight.stopCount} escale(s)`));
  const arrivalBox = document.createElement('div');
  arrivalBox.className = 'text-right';
  arrivalBox.append(
    createTextElement('p', 'text-label-md font-semibold uppercase text-on-surface-variant', `Arrivée${dayShift > 0 ? ` (+${dayShift}J)` : ''}`),
    createTextElement('p', 'text-2xl font-bold', formatTime(ticket.arrivalTime) || '—'),
    createTextElement('p', 'text-body-sm text-on-surface-variant', [formatDate(ticket.arrivalTime), flight?.arrivalAirport].filter(Boolean).join(' • '))
  );
  timeline.append(departureBox, middle, arrivalBox);
  main.append(timeline);

  // Pastilles de services et conditions.
  const perks = document.createElement('div');
  perks.className = 'flex flex-wrap gap-x-4 gap-y-1 text-body-sm text-on-surface-variant';
  if (flight?.specialMeal) perks.append(iconLabel('restaurant', flight.specialMeal));
  if (flight?.optionalServices) perks.append(iconLabel('add_circle', flight.optionalServices));
  if (flight?.modificationConditions) perks.append(iconLabel('sync_alt', flight.modificationConditions));
  if (flight?.cancellationConditions) perks.append(iconLabel('event_busy', flight.cancellationConditions));
  if (flight?.extraBaggage) perks.append(iconLabel('luggage', flight.extraBaggage));
  if (flight?.stopDetails) perks.append(iconLabel('connecting_airports', flight.stopDetails));
  if (ticket.attachmentName) perks.append(iconLabel('attach_file', `${ticket.attachmentName} (non téléversé)`));
  if (perks.childElementCount) main.append(perks);

  card.append(side, main);
  // Ajoute la carte complète à la liste des billets.
  container.append(card);
}

const ticketFieldClass = 'w-full px-3 py-2 bg-surface-container-lowest border border-outline-variant rounded-lg text-body-sm text-on-surface focus:outline-none focus:border-primary';

// Ajoute une astérisque aux libellés des champs obligatoires et la retire des autres.
function markRequiredFields(form) {
  form.querySelectorAll('label').forEach((label) => {
    const control = label.htmlFor
      ? document.getElementById(label.htmlFor)
      : label.parentElement.querySelector('input:not([type="radio"]):not([type="file"]), select, textarea');
    if (!control || label.querySelector('input')) return;
    const base = label.textContent.replace(/\s*\*$/, '');
    label.textContent = control.required ? `${base} *` : base;
  });
}

function appendTicketField(container, labelText, id, type = 'text', options = {}) {
  const wrapper = document.createElement('div');
  wrapper.className = options.wrapperClass || 'flex flex-col gap-1';
  const label = createTextElement('label', 'font-label-md text-label-md text-on-surface font-semibold', labelText);
  label.htmlFor = id;
  const input = document.createElement(type === 'select' ? 'select' : 'input');
  input.id = id;
  input.className = ticketFieldClass;
  if (type !== 'select') input.type = type;
  if (options.required) input.required = true;
  if (options.placeholder) input.placeholder = options.placeholder;
  if (options.pattern) input.pattern = options.pattern;
  if (options.maxLength) input.maxLength = options.maxLength;
  if (options.min !== undefined) input.min = String(options.min);
  if (options.max !== undefined) input.max = String(options.max);
  if (options.step !== undefined) input.step = String(options.step);
  if (options.value !== undefined) {
    input.value = String(options.value);
    input.defaultValue = String(options.value);
  }
  if (options.help) input.setAttribute('aria-description', options.help);
  if (type === 'select') {
    options.choices.forEach(([value, text]) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = text;
      input.append(option);
    });
  }
  wrapper.append(label, input);
  container.append(wrapper);
  return input;
}

function initializeCategoryTicketForms(form) {
  const categories = {
    'Train': {
      title: 'Billet de train',
      departure: 'Gare de départ',
      arrival: 'Gare d’arrivée',
      fields: [['Opérateur ferroviaire', 'operator'], ['Numéro de train', 'service'], ['Voiture et siège', 'seat']]
    },
    'VTC/Taxi': {
      title: 'Course VTC / taxi',
      departure: 'Lieu de prise en charge',
      arrival: 'Lieu de dépose',
      fields: [['Opérateur / chauffeur', 'operator'], ['Référence de course', 'service'], ['Instructions de prise en charge', 'seat']]
    },
    'Bus': {
      title: 'Billet de bus',
      departure: 'Arrêt de départ',
      arrival: 'Arrêt d’arrivée',
      fields: [['Compagnie de bus', 'operator'], ['Ligne / numéro de trajet', 'service'], ['Place', 'seat']]
    },
    'Ferry': {
      title: 'Billet de ferry',
      departure: 'Port de départ',
      arrival: 'Port d’arrivée',
      fields: [['Compagnie maritime', 'operator'], ['Nom du navire', 'service'], ['Cabine / siège', 'seat']]
    }
  };
  const panels = new Map();
  const picker = form.querySelector('input[name="transportType"]').closest('div.grid')?.parentElement || form.children[0];
  const panelContainer = document.createElement('div');
  panelContainer.className = 'flex flex-col gap-space-md';
  picker.after(panelContainer);

  const flightPanel = document.createElement('fieldset');
  flightPanel.className = 'flex flex-col gap-space-md rounded-xl border border-outline-variant p-space-md';
  flightPanel.dataset.ticketCategoryPanel = 'Avion';
  flightPanel.append(createTextElement('legend', 'px-1 font-label-lg text-label-lg text-primary font-bold', 'Billet d’avion'));
  const airportGrid = document.createElement('div');
  airportGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  appendTicketField(airportGrid, 'Aéroport de départ', 'flightDepartureAirport', 'text', { required: true, placeholder: 'Paris-Charles-de-Gaulle' });
  appendTicketField(airportGrid, 'Code IATA de départ', 'flightDepartureIata', 'text', { required: true, pattern: '[A-Za-z]{3}', maxLength: 3, placeholder: 'CDG' });
  appendTicketField(airportGrid, 'Aéroport d’arrivée', 'flightArrivalAirport', 'text', { required: true, placeholder: 'New York-JFK' });
  appendTicketField(airportGrid, 'Code IATA d’arrivée', 'flightArrivalIata', 'text', { required: true, pattern: '[A-Za-z]{3}', maxLength: 3, placeholder: 'JFK' });
  flightPanel.append(airportGrid);

  const stopsGrid = document.createElement('div');
  stopsGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  const stopCount = appendTicketField(stopsGrid, 'Nombre d’escales', 'flightStopCount', 'number', { min: 0, max: 20, step: 1, value: 0, required: true });
  const stopDetails = appendTicketField(stopsGrid, 'Aéroports et durée de chaque escale', 'flightStopDetails', 'text', { placeholder: 'Ex. : 2 h 15 à Doha (DOH)' });
  stopDetails.disabled = true;
  flightPanel.append(stopsGrid);
  stopCount.addEventListener('input', () => {
    stopDetails.disabled = Number(stopCount.value) === 0;
    stopDetails.required = Number(stopCount.value) > 0;
    if (stopDetails.disabled) stopDetails.value = '';
    markRequiredFields(stopDetails.form);
  });

  const carrierGrid = document.createElement('div');
  carrierGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  appendTicketField(carrierGrid, 'Compagnie opérant le vol', 'flightAirline', 'text', { required: true, placeholder: 'Ex. : Air France' });
  appendTicketField(carrierGrid, 'Numéro de vol', 'flightNumber', 'text', { placeholder: 'Ex. : AF 123' });
  appendTicketField(carrierGrid, 'Type de tarif', 'flightFareType', 'select', {
    required: true,
    choices: [['', 'Sélectionner un tarif'], ['Economy Light', 'Economy Light'], ['Economy Standard', 'Economy Standard'], ['Economy Flex', 'Economy Flex'], ['Premium Economy', 'Premium Economy'], ['Business', 'Business'], ['First', 'First'], ['Autre', 'Autre']]
  });
  appendTicketField(carrierGrid, 'Statut du billet', 'flightTicketStatus', 'select', {
    required: true,
    choices: [['Confirmé / OK', 'Confirmé / OK'], ['En attente', 'En attente'], ['Annulé', 'Annulé']]
  });
  flightPanel.append(carrierGrid);

  const timezoneNotice = createTextElement('p', 'text-body-sm text-on-surface-variant', 'Saisissez les heures locales indiquées sur le billet et le décalage UTC valable à la date du vol (en minutes : Paris en hiver +60, en été +120).');
  flightPanel.append(timezoneNotice);
  const offsetGrid = document.createElement('div');
  offsetGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  appendTicketField(offsetGrid, 'Décalage UTC au départ (minutes)', 'flightDepartureOffset', 'number', { min: -720, max: 840, step: 15, required: true, placeholder: 'Ex. : 60' });
  appendTicketField(offsetGrid, 'Décalage UTC à l’arrivée (minutes)', 'flightArrivalOffset', 'number', { min: -720, max: 840, step: 15, required: true, placeholder: 'Ex. : -300' });
  flightPanel.append(offsetGrid);
  const durationPreview = createTextElement('p', 'font-label-md text-label-md text-secondary font-semibold', 'Durée totale : renseignez les horaires et les décalages UTC.');
  durationPreview.id = 'flightDurationPreview';
  flightPanel.append(durationPreview);

  const baggageGrid = document.createElement('div');
  baggageGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  appendTicketField(baggageGrid, 'Bagage cabine inclus', 'flightCabinBaggage', 'select', {
    choices: [['À vérifier', 'À confirmer selon la compagnie et le tarif'], ['Aucun', 'Aucun'], ['Accessoire sous le siège', 'Accessoire personnel uniquement'], ['Cabine - repère 56 × 45 × 25 cm', 'Bagage cabine (repère IATA : 56 × 45 × 25 cm)'], ['Cabine + accessoire', 'Bagage cabine + accessoire personnel']]
  });
  appendTicketField(baggageGrid, 'Poids maximal du bagage cabine (kg)', 'flightCabinBaggageWeight', 'number', { min: 0, max: 50, step: 0.1, placeholder: 'Ex. : 8' });
  appendTicketField(baggageGrid, 'Dimensions maximales cabine (cm)', 'flightCabinBaggageDimensions', 'text', { placeholder: 'Ex. : 56 × 45 × 25 cm' });
  appendTicketField(baggageGrid, 'Accessoire personnel inclus', 'flightPersonalItem', 'select', {
    choices: [['À vérifier', 'À confirmer selon la compagnie et le tarif'], ['Oui', 'Oui'], ['Non', 'Non']]
  });
  appendTicketField(baggageGrid, 'Dimensions / poids de l’accessoire', 'flightPersonalItemDetails', 'text', { placeholder: 'Ex. : sous le siège, 40 × 30 × 15 cm' });
  appendTicketField(baggageGrid, 'Bagage(s) en soute inclus', 'flightCheckedBaggage', 'select', {
    choices: [['À vérifier', 'À confirmer selon la compagnie et le tarif'], ['Aucun', 'Aucun'], ['1 pièce', '1 pièce'], ['2 pièces', '2 pièces'], ['Autre', 'Autre']]
  });
  appendTicketField(baggageGrid, 'Poids maximal par bagage (kg)', 'flightBaggageWeight', 'number', { min: 0, max: 100, step: 0.1, placeholder: 'Ex. : 23' });
  appendTicketField(baggageGrid, 'Dimensions maximales (cm)', 'flightBaggageDimensions', 'text', { placeholder: 'Ex. : 158 cm (L + l + H)' });
  flightPanel.append(baggageGrid);
  flightPanel.append(createTextElement('p', 'text-body-sm text-on-surface-variant', 'Repères indicatifs IATA, pas une franchise garantie : cabine jusqu’à 56 × 45 × 25 cm ; un bagage en soute ne devrait généralement pas dépasser 23 kg et peut être limité à 158 cm (L + l + H) sur certains trajets. Les règles exactes et les dimensions de l’accessoire personnel dépendent de la compagnie, du trajet et du tarif.'));

  const conditionsGrid = document.createElement('div');
  conditionsGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  const conditionChoices = [['À vérifier', 'À vérifier'], ['Remboursable', 'Remboursable'], ['Modifiable sans frais', 'Modifiable sans frais'], ['Modifiable avec frais', 'Modifiable avec frais'], ['Non remboursable', 'Non remboursable']];
  appendTicketField(conditionsGrid, 'Modification', 'flightModification', 'select', { choices: conditionChoices });
  appendTicketField(conditionsGrid, 'Annulation', 'flightCancellation', 'select', { choices: conditionChoices });
  appendTicketField(conditionsGrid, 'Choix du siège', 'flightSeatChoice', 'select', {
    choices: [['À choisir', 'À choisir'], ['Siège standard', 'Siège standard'], ['Espace jambes supplémentaire', 'Espace jambes supplémentaire'], ['Siège premium', 'Siège premium'], ['Non sélectionné', 'Non sélectionné']]
  });
  appendTicketField(conditionsGrid, 'Repas spécial', 'flightSpecialMeal', 'select', {
    choices: [['Aucun', 'Aucun'], ['Végétarien', 'Végétarien'], ['Végétalien', 'Végétalien'], ['Sans gluten', 'Sans gluten'], ['Halal', 'Halal'], ['Casher', 'Casher'], ['Autre', 'Autre']]
  });
  flightPanel.append(conditionsGrid);
  appendTicketField(flightPanel, 'Bagages supplémentaires / équipements spéciaux', 'flightExtraBaggage', 'text', { placeholder: 'Ex. : 1 bagage en soute supplémentaire, équipement sportif' });
  appendTicketField(flightPanel, 'Services optionnels', 'flightOptionalServices', 'text', { placeholder: 'Ex. : salon, embarquement prioritaire' });
  const idGrid = document.createElement('div');
  idGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
  appendTicketField(idGrid, 'Numéro de billet électronique (13 chiffres)', 'flightEticketNumber', 'text', { required: true, pattern: '\\d{13}', maxLength: 13, placeholder: 'Ex. : 0571234567890' });
  flightPanel.append(idGrid);
  panelContainer.append(flightPanel);
  panels.set('Avion', flightPanel);

  Object.entries(categories).forEach(([type, category]) => {
    const panel = document.createElement('fieldset');
    panel.className = 'flex flex-col gap-space-md rounded-xl border border-outline-variant p-space-md';
    panel.dataset.ticketCategoryPanel = type;
    panel.append(createTextElement('legend', 'px-1 font-label-lg text-label-lg text-primary font-bold', category.title));
    const routeGrid = document.createElement('div');
    routeGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
    appendTicketField(routeGrid, category.departure, `${type}-departure`, 'text', { required: true });
    appendTicketField(routeGrid, category.arrival, `${type}-arrival`, 'text', { required: true });
    panel.append(routeGrid);
    const categoryGrid = document.createElement('div');
    categoryGrid.className = 'grid grid-cols-1 sm:grid-cols-2 gap-space-sm';
    category.fields.forEach(([label, key]) => appendTicketField(categoryGrid, label, `${type}-${key}`));
    panel.append(categoryGrid);
    panelContainer.append(panel);
    panels.set(type, panel);
  });

  const reference = document.getElementById('apiBookingReference');
  const referenceLabel = reference.previousElementSibling;
  const priceLabel = document.getElementById('ticketPrice').closest('div').previousElementSibling;
  const description = document.getElementById('manualDescription');
  const departureTime = document.getElementById('departureTime');
  const arrivalTime = document.getElementById('arrivalTime');
  const departureTimeLabel = departureTime.previousElementSibling;
  const arrivalTimeLabel = arrivalTime.previousElementSibling;
  const updateCategory = () => {
    const type = form.querySelector('input[name="transportType"]:checked')?.value || 'Avion';
    panels.forEach((panel, category) => {
      const active = category === type;
      // Le style direct prévaut sur la classe Tailwind "flex" qui annulerait l'attribut hidden.
      panel.style.display = active ? '' : 'none';
      panel.disabled = !active;
    });
    const isFlight = type === 'Avion';
    reference.required = isFlight;
    if (isFlight) reference.pattern = '[A-Za-z0-9]{6}';
    else reference.removeAttribute('pattern');
    reference.maxLength = isFlight ? 6 : 100;
    reference.placeholder = isFlight ? 'PNR à 6 caractères (ex. : A1B2C3)' : 'Référence facultative';
    referenceLabel.textContent = isFlight ? 'Référence de réservation / PNR (6 caractères)' : 'Référence de réservation';
    priceLabel.textContent = isFlight ? 'Prix total par passager (€)' : 'Prix en Euros (€)';
    departureTimeLabel.textContent = isFlight ? 'Date et heure de départ (heure locale)' : 'Date et heure de départ';
    arrivalTimeLabel.textContent = isFlight ? 'Date et heure d’arrivée (heure locale)' : 'Date et heure d’arrivée';
    departureTime.required = true;
    arrivalTime.required = isFlight;
    stopDetails.disabled = Number(stopCount.value) === 0;
    stopDetails.required = Number(stopCount.value) > 0;
    description.placeholder = isFlight ? 'Ex. : Vol AF 123, Paris → New York' : `Description du trajet ${type.toLowerCase()}`;
    markRequiredFields(form);
  };
  form.querySelectorAll('input[name="transportType"]').forEach((radio) => radio.addEventListener('change', updateCategory));
  [document.getElementById('departureTime'), document.getElementById('arrivalTime'),
    document.getElementById('flightDepartureOffset'), document.getElementById('flightArrivalOffset')]
    .forEach((input) => input.addEventListener('input', () => {
      const departure = document.getElementById('departureTime').value;
      const arrival = document.getElementById('arrivalTime').value;
      const departureOffset = Number(document.getElementById('flightDepartureOffset').value);
      const arrivalOffset = Number(document.getElementById('flightArrivalOffset').value);
      if (!departure || !arrival || !document.getElementById('flightDepartureOffset').value
          || !document.getElementById('flightArrivalOffset').value) {
        durationPreview.textContent = 'Durée totale : renseignez les horaires et les décalages UTC.';
        return;
      }
      const localMinutes = (value) => {
        const [date, time] = value.split('T');
        const [year, month, day] = date.split('-').map(Number);
        const [hours, minutes] = time.split(':').map(Number);
        return Date.UTC(year, month - 1, day, hours, minutes) / 60000;
      };
      const minutes = localMinutes(arrival) - arrivalOffset - localMinutes(departure) + departureOffset;
      if (minutes <= 0) {
        durationPreview.textContent = 'Vérifiez les horaires locaux et les décalages UTC.';
        return;
      }
      durationPreview.textContent = `Durée totale estimée : ${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')}`;
    }));
  form.addEventListener('reset', () => window.setTimeout(updateCategory));
  updateCategory();
}

// Rend la zone de justificatif utilisable par clic ou glisser-déposer.
function initializeTickets() {
  const form = document.getElementById('new-ticket-form');
  initializeCategoryTicketForms(form);
  const uploadZone = [...form.querySelectorAll('.cursor-pointer')]
    .find((element) => element.textContent.includes('Glisser-déposer le billet'));
  if (uploadZone) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf,image/*';
    input.className = 'hidden';
    form.append(input);
    uploadZone.addEventListener('click', () => input.click());
    uploadZone.addEventListener('dragover', (event) => event.preventDefault());
    uploadZone.addEventListener('drop', (event) => {
      event.preventDefault();
      const file = event.dataTransfer.files[0];
      if (file) selectTicketAttachment(file, input, uploadZone);
    });
    input.addEventListener('change', () => {
      if (input.files[0]) selectTicketAttachment(input.files[0], input, uploadZone);
    });
  }
}

// Connecte les filtres, recherches et boutons de la page transports.
function initializeTransportControls() {
  const container = document.getElementById('view-mes-billets');
  const filters = [...document.querySelectorAll('#billets-filters button')];
  let activeFilter = 'Tous';
  const search = document.querySelector('input[type="search"]');
  const applyFilters = () => {
    container?.querySelectorAll(':scope > *').forEach((card) => {
      const text = card.textContent.toLowerCase();
      const type = card.dataset.transportType || '';
      const matchesType = activeFilter === 'Tous'
        || (activeFilter === 'Vols' && /vol|air china|air france/.test(`${type} ${text}`))
        || (activeFilter === 'Trains' && /train|tgv|rail/.test(`${type} ${text}`))
        || (activeFilter === 'Taxis & Navettes' && /taxi|didi|navette|vtc|transfert/.test(`${type} ${text}`));
      const query = search?.value.trim().toLowerCase() || '';
      card.hidden = !matchesType || (query !== '' && !text.includes(query));
    });
  };

  filters.forEach((button) => {
    button.dataset.appHandled = 'true';
    button.addEventListener('click', () => {
      activeFilter = button.textContent.trim();
      filters.forEach((filter) => filter.setAttribute('aria-pressed', String(filter === button)));
      applyFilters();
    });
  });
  search?.addEventListener('input', applyFilters);

  container?.querySelectorAll('button').forEach((button) => {
    const label = button.textContent.trim();
    if (label.includes("Télécharger carte d'embarquement")) {
      button.dataset.appHandled = 'true';
      button.textContent = 'Télécharger le récapitulatif (TXT)';
      button.addEventListener('click', () => {
        const card = button.closest('div.relative');
        downloadFile('recapitulatif-billet.txt', card?.textContent.trim() || 'Récapitulatif du billet', 'text/plain;charset=utf-8');
        announce('Récapitulatif téléchargé au format TXT.');
      });
    } else if (label.includes('Afficher QR Billet')) {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => announce('Le QR affiché est une illustration. Aucun billet externe n’est synchronisé.'));
    } else if (label === 'Scanner porte') {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => announce('Le scan nécessite un service de caméra non configuré.'));
    } else if (['Modifier', 'Détails', 'Options'].includes(button.title)) {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => {
        const card = button.closest('div.relative');
        const title = card?.querySelector('h3')?.textContent.trim() || 'Ce titre de démonstration';
        if (button.title === 'Détails' && card) {
          let details = card.querySelector('[data-ticket-details]');
          if (details) {
            details.remove();
          } else {
            details = createTextElement('p', 'm-space-md rounded-lg bg-surface-container-low p-space-sm text-body-sm text-on-surface-variant', title);
            details.dataset.ticketDetails = 'true';
            button.closest('.flex.items-center')?.after(details);
          }
        } else {
          announce(`${title} n’est pas relié à un billet enregistré modifiable.`);
        }
      });
    }
  });

  document.querySelectorAll('button').forEach((button) => {
    if (button.textContent.includes('Rechercher via API')) {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => announce('Aucune API de transport partenaire n’est configurée.'));
    }
  });
}

// Valide le fichier choisi et ne conserve que son nom dans le formulaire.
function selectTicketAttachment(file, input, zone) {
  if (file.size > 10 * 1024 * 1024) {
    input.value = '';
    announce('Le fichier dépasse la limite de 10 Mo.');
    return;
  }
  document.getElementById('new-ticket-form').dataset.attachment = file.name;
  announce(`Fichier sélectionné : ${file.name}. Le fichier ne sera pas téléversé.`);
  zone.setAttribute('aria-label', `Fichier sélectionné : ${file.name}`);
}

// Enregistre un titre sur le voyage sélectionné et met à jour l’interface.
async function saveTicket(ticket, confirmation = 'Titre enregistré dans H2.') {
  // Vérifie qu’un voyage enregistré est sélectionné.
  const tripId = requireSelectedTripId();
  // Envoie le nouveau titre à l’API.
  const savedTicket = await apiRequest(`/api/trips/${tripId}/tickets`, {
    method: 'POST',
    body: JSON.stringify(ticket)
  });
  // Affiche le billet renvoyé par le serveur.
  appendTicket(savedTicket);
  // Bascule vers l’onglet des billets si cette fonction est disponible.
  window.switchMainTab?.('mes-billets');
  // Affiche le message de confirmation choisi par l’appelant.
  announce(confirmation);
}

window.handleFormSubmit = async (event) => {
  // Gère l’envoi du formulaire sans recharger la page.
  event.preventDefault();
  // Récupère le formulaire et le type de transport sélectionné.
  const form = document.getElementById('new-ticket-form');
  const type = form.querySelector('input[name="transportType"]:checked')?.value || 'Transport';
  try {
    const readValue = (id) => document.getElementById(id)?.value.trim() || '';
    const isFlight = type === 'Avion';
    const categoryFieldIds = {
      'Train': ['Train-operator', 'Train-service', 'Train-seat'],
      'VTC/Taxi': ['VTC/Taxi-operator', 'VTC/Taxi-service', 'VTC/Taxi-seat'],
      'Bus': ['Bus-operator', 'Bus-service', 'Bus-seat'],
      'Ferry': ['Ferry-operator', 'Ferry-service', 'Ferry-seat']
    };
    const categoryValues = (categoryFieldIds[type] || []).map(readValue).filter(Boolean);
    const description = [readValue('manualDescription'), ...categoryValues].filter(Boolean).join(' · ');
    const flightDetails = isFlight ? {
      departureAirport: readValue('flightDepartureAirport'),
      departureIata: readValue('flightDepartureIata').toUpperCase(),
      arrivalAirport: readValue('flightArrivalAirport'),
      arrivalIata: readValue('flightArrivalIata').toUpperCase(),
      stopCount: Number(readValue('flightStopCount')),
      stopDetails: readValue('flightStopDetails'),
      airline: readValue('flightAirline'),
      flightNumber: readValue('flightNumber'),
      fareType: readValue('flightFareType'),
      cabinBaggage: readValue('flightCabinBaggage'),
      cabinBaggageWeightKg: readValue('flightCabinBaggageWeight') ? Number(readValue('flightCabinBaggageWeight')) : null,
      cabinBaggageDimensions: readValue('flightCabinBaggageDimensions'),
      personalItem: readValue('flightPersonalItem'),
      personalItemDetails: readValue('flightPersonalItemDetails'),
      checkedBaggage: readValue('flightCheckedBaggage'),
      checkedBaggageWeightKg: readValue('flightBaggageWeight') ? Number(readValue('flightBaggageWeight')) : null,
      checkedBaggageDimensions: readValue('flightBaggageDimensions'),
      modificationConditions: readValue('flightModification'),
      cancellationConditions: readValue('flightCancellation'),
      seatChoice: readValue('flightSeatChoice'),
      extraBaggage: readValue('flightExtraBaggage'),
      specialMeal: readValue('flightSpecialMeal'),
      optionalServices: readValue('flightOptionalServices'),
      eticketNumber: readValue('flightEticketNumber'),
      ticketStatus: readValue('flightTicketStatus'),
      departureUtcOffsetMinutes: Number(readValue('flightDepartureOffset')),
      arrivalUtcOffsetMinutes: Number(readValue('flightArrivalOffset'))
    } : null;
    // Assemble les valeurs du formulaire dans le format attendu par l’API.
    await saveTicket({
      type,
      description,
      departure: isFlight
        ? `${flightDetails.departureAirport} (${flightDetails.departureIata})`
        : readValue(`${type}-departure`),
      arrival: isFlight
        ? `${flightDetails.arrivalAirport} (${flightDetails.arrivalIata})`
        : readValue(`${type}-arrival`),
      departureTime: document.getElementById('departureTime').value,
      arrivalTime: document.getElementById('arrivalTime').value || null,
      price: Number(document.getElementById('ticketPrice').value),
      reference: readValue('apiBookingReference').toUpperCase(),
      attachmentName: form.dataset.attachment || '',
      flightDetails
    });
    // Ferme la fenêtre de saisie et efface les anciennes valeurs.
    window.toggleModal(false);
    form.reset();
    // Efface le nom du justificatif conservé temporairement sur le formulaire.
    delete form.dataset.attachment;
  } catch (error) {
    // Signale l’erreur sans perdre silencieusement les informations saisies.
    announceRequestError('Impossible d’enregistrer le titre', error);
  }
};

// Enregistre une offre de démonstration comme titre sans effectuer d’achat externe.
window.bookApiOffer = async (title, price, type) => {
  try {
    // Convertit les données de l’offre en titre compatible avec le service d’enregistrement.
    await saveTicket(
      {
        type,
        description: title,
        price: Number(price),
        departureTime: new Date().toISOString().slice(0, 16),
        reference: 'Offre de démonstration'
      },
      'Offre de démonstration enregistrée dans H2 ; aucun achat externe n’a été effectué.'
    );
  } catch (error) {
    // Affiche l’échec de l’ajout de l’offre.
    announceRequestError('Impossible d’ajouter cette offre', error);
  }
};


export { appendTicket, initializeTickets, initializeTransportControls };
