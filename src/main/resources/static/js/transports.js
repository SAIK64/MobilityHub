import { announce } from './common.js';

// Comportements d’interface de la page Transports.
// Le formulaire de billet est rendu et traité par TransportController (TicketForm) :
// ce fichier adapte seulement l’affichage au type de transport choisi.

// Libellés propres à chaque transport terrestre ou maritime.
const groundLabels = {
  'Train': ['Billet de train', 'Gare de départ', 'Gare d’arrivée', 'Opérateur ferroviaire', 'Numéro de train', 'Voiture et siège'],
  'VTC/Taxi': ['Course VTC / taxi', 'Lieu de prise en charge', 'Lieu de dépose', 'Opérateur / chauffeur', 'Référence de course', 'Instructions de prise en charge'],
  'Bus': ['Billet de bus', 'Arrêt de départ', 'Arrêt d’arrivée', 'Compagnie de bus', 'Ligne / numéro de trajet', 'Place'],
  'Ferry': ['Billet de ferry', 'Port de départ', 'Port d’arrivée', 'Compagnie maritime', 'Nom du navire', 'Cabine / siège']
};
const groundFields = ['departure', 'arrival', 'operatorName', 'serviceNumber', 'seatInfo'];

// Change le libellé d’un champ en conservant l’astérisque des champs obligatoires.
function setLabel(fieldId, text) {
  const label = document.querySelector(`label[for="${fieldId}"]`);
  if (label) label.textContent = document.getElementById(fieldId)?.required ? `${text} *` : text;
}

// Affiche la partie « vol » ou « trajet terrestre » selon le type choisi.
// Une partie masquée est désactivée : ses champs ne sont ni validés ni envoyés.
function updateTicketType(form) {
  const type = form.querySelector('input[name="type"]:checked')?.value || 'Avion';
  const isFlight = type === 'Avion';
  form.querySelectorAll('[data-ticket-panel]').forEach((panel) => {
    const active = (panel.dataset.ticketPanel === 'flight') === isFlight;
    panel.disabled = !active;
    panel.style.display = active ? '' : 'none';
  });
  // Pour un vol : PNR et heure d’arrivée obligatoires.
  const reference = document.getElementById('reference');
  reference.required = isFlight;
  reference.pattern = isFlight ? '[A-Za-z0-9]{6}' : '.*';
  setLabel('reference', isFlight ? 'Référence de réservation / PNR (6 caractères)' : 'Référence de réservation');
  document.getElementById('arrivalTime').required = isFlight;
  setLabel('arrivalTime', 'Date et heure d’arrivée (heure locale)');
  if (!isFlight) {
    const [title, ...labels] = groundLabels[type];
    form.querySelector('[data-ground-label="title"]').textContent = title;
    groundFields.forEach((id, index) => setLabel(id, labels[index]));
  }
}

// Aperçu de la durée du vol à partir des heures locales et des décalages UTC (le serveur fait le calcul officiel).
function updateDurationPreview() {
  const preview = document.getElementById('flightDurationPreview');
  const value = (id) => document.getElementById(id)?.value;
  const departure = value('departureTime');
  const arrival = value('arrivalTime');
  const departureOffset = value('flight.departureUtcOffsetMinutes');
  const arrivalOffset = value('flight.arrivalUtcOffsetMinutes');
  if (!preview) return;
  if (!departure || !arrival || departureOffset === '' || arrivalOffset === '') {
    preview.textContent = 'Durée totale : renseignez les horaires et les décalages UTC.';
    return;
  }
  const utcMinutes = (local, offset) => Date.parse(`${local}:00Z`) / 60000 - Number(offset);
  const minutes = utcMinutes(arrival, arrivalOffset) - utcMinutes(departure, departureOffset);
  preview.textContent = minutes > 0
    ? `Durée totale estimée : ${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')}`
    : 'Vérifiez les horaires locaux et les décalages UTC.';
}

// Relie le formulaire de billet : type de transport, escales, durée, justificatif.
function initializeTickets() {
  const form = document.getElementById('new-ticket-form');
  if (!form) return;
  form.querySelectorAll('input[name="type"]').forEach((radio) => radio.addEventListener('change', () => updateTicketType(form)));
  updateTicketType(form);

  // Le détail des escales n’est demandé que s’il y en a.
  const stopCount = document.getElementById('flight.stopCount');
  const stopDetails = document.getElementById('flight.stopDetails');
  const updateStops = () => {
    stopDetails.required = Number(stopCount.value) > 0;
    setLabel('flight.stopDetails', 'Aéroports et durée des escales');
  };
  stopCount?.addEventListener('input', updateStops);
  if (stopCount) updateStops();

  ['departureTime', 'arrivalTime', 'flight.departureUtcOffsetMinutes', 'flight.arrivalUtcOffsetMinutes']
    .forEach((id) => document.getElementById(id)?.addEventListener('input', updateDurationPreview));
  updateDurationPreview();

  // Seul le nom du justificatif est envoyé au serveur.
  const file = document.getElementById('ticket-attachment-file');
  file?.addEventListener('change', () => {
    const chosen = file.files[0];
    if (!chosen) return;
    if (chosen.size > 10 * 1024 * 1024) {
      file.value = '';
      announce('Le fichier dépasse la limite de 10 Mo.');
      return;
    }
    document.getElementById('attachmentName').value = chosen.name;
    form.querySelector('[data-attachment-label]').textContent = chosen.name;
  });
}

// Filtres « Tous / Vols / Trains / Taxis & Navettes » : masque les cartes selon le type enregistré du billet.
function initializeTransportControls() {
  const container = document.getElementById('view-mes-billets');
  const filters = [...document.querySelectorAll('#billets-filters button')];
  const typesByFilter = {
    'Vols': ['Avion'],
    'Trains': ['Train'],
    'Taxis & Navettes': ['VTC/Taxi', 'Bus', 'Ferry']
  };
  filters.forEach((button) => {
    button.dataset.appHandled = 'true';
    button.addEventListener('click', () => {
      const types = typesByFilter[button.textContent.trim()];
      container.querySelectorAll('[data-transport-type]').forEach((card) => {
        card.hidden = Boolean(types) && !types.includes(card.dataset.transportType);
      });
      filters.forEach((filter) => filter.setAttribute('aria-pressed', String(filter === button)));
    });
  });
}

export { initializeTickets, initializeTransportControls };
