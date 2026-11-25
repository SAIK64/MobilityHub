import { apiRequest, announce, announceRequestError, createTextElement } from './common.js';
import {
  selectedTripStorageKey, databaseTrips, setDatabaseTrips, selectedDatabaseTripId,
  clearDatabaseTripSelection, selectDatabaseTrip, addTripToMenu, appendSavedTripCard,
  savedTripsContainer, initializeTrips, initializeDashboardControls, initializeTripCountryRows
} from './trips.js';
import { appendTicket, initializeTickets, initializeTransportControls } from './transports.js';
import { initializeBudget, initializeCategoryBudgets, renderExpenses, setCategoryBudgets } from './budget.js';
import { initializeProfile, displayProfile } from './profile.js';

// Configure les éléments communs aux pages : navigation, recherche et boutons partagés.
function initializeSharedControls() {
  const titles = {
    '/': 'Tableau de bord | SAIK Travel',
    '/dashboard': 'Tableau de bord | SAIK Travel',
    '/transports': 'Transports & billets | SAIK Travel',
    '/budget': 'Budget & dépenses | SAIK Travel',
    '/securite': 'Sécurité | SAIK Travel',
    '/profil': 'Mon profil | SAIK Travel'
  };
  document.title = titles[window.location.pathname] || 'SAIK Travel';

  document.querySelectorAll('[data-path]').forEach((link) => {
    const path = {
      'tableau-de-bord': '/',
      'transports-&-billets': '/transports',
      'budget-&-dépenses': '/budget',
      'kit-de-survie-&-urgences': '/securite',
      'mon-profil': '/profil'
    }[link.dataset.path];
    if (path) link.href = path;
  });

  document.querySelectorAll('a[href="#"]').forEach((link) => {
    if (link.textContent.trim() !== 'Ouvrir GPS') return;
    link.addEventListener('click', (event) => {
      event.preventDefault();
      if (!navigator.geolocation) {
        announce('La géolocalisation n’est pas disponible dans ce navigateur.');
        return;
      }
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          window.location.href = `https://maps.google.com/?q=${coords.latitude},${coords.longitude}`;
        },
        () => announce('Position indisponible : vérifiez les autorisations de géolocalisation.')
      );
      event.stopImmediatePropagation();
    });
  });

  document.querySelectorAll('button').forEach((button) => {
    const label = button.textContent.toLowerCase();
    if (label.includes('nouveau voyage') && !button.hasAttribute('onclick')) {
      button.addEventListener('click', () => {
        if (document.getElementById('addTripModal') && typeof window.openModal === 'function') {
          window.openModal();
        } else {
          window.location.href = '/?action=new-trip';
        }
      });
      button.dataset.appHandled = 'true';
    }
    if (label.includes('notifications')) {
      button.addEventListener('click', () => announce('Vous êtes à jour : aucune nouvelle notification.'));
      button.dataset.appHandled = 'true';
    }
  });

  document.querySelectorAll('button').forEach((button) => {
    if (button.textContent.includes('Télécharger Certificat')) {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => announce('Aucun profil opérateur réel n’est configuré : aucun certificat mobileconfig ne peut être généré.'));
    }
  });

  document.addEventListener('click', (event) => {
    const button = event.target.closest('button[onclick^="alert("]');
    if (!button) return;
    const action = button.getAttribute('onclick');
    if (action.includes('Export de vos données')) {
      event.preventDefault();
      event.stopImmediatePropagation();
      window.exportTravelData();
    } else if (action.includes('sessions ont été révoquées')) {
      event.preventDefault();
      event.stopImmediatePropagation();
      window.revokeSessions();
    }
  }, true);

  document.querySelectorAll('a[href="#"]:not([data-path])').forEach((link) => {
    if (link.textContent.trim() === 'Ouvrir GPS') return;
    link.addEventListener('click', (event) => {
      event.preventDefault();
      announce('Ce lien de démonstration ne mène pas vers un service connecté.');
    });
  });

  const search = document.querySelector('input[type="search"][placeholder*="Rechercher billet"]');
  search?.addEventListener('keydown', (event) => {
    if (event.key !== 'Enter' || !search.value.trim()) return;
    const query = search.value.trim().toLowerCase();
    const match = [...document.querySelectorAll('main h1, main h2, main h3, main h4, main p')]
      .find((element) => element.textContent.toLowerCase().includes(query));
    if (match) {
      match.scrollIntoView({ behavior: 'smooth', block: 'center' });
      announce(`Résultat trouvé : ${match.textContent.trim()}`);
    } else {
      announce('Aucun résultat trouvé dans cette page.');
    }
  });
}

// Ajoute un message explicatif aux boutons de démonstration sans action connectée.
function initializeUnwiredButtons() {
  document.querySelectorAll('button:not([onclick])').forEach((button) => {
    if (button.dataset.appHandled === 'true'
      || button.type === 'submit'
      || button.hasAttribute('form')) return;
    button.addEventListener('click', () => {
      const label = button.title || button.textContent.trim() || 'Cette action';
      announce(`${label} n’est pas relié à un service externe dans cette démonstration.`);
    });
    button.dataset.appHandled = 'true';
  });
}

// Charge les données depuis l’API et les affiche uniquement sur les pages concernées.
async function initializeDatabaseData() {
  try {
    // Charge d’abord les voyages pour choisir celui auquel rattacher les autres données.
    setDatabaseTrips(await apiRequest('/api/trips'));
    // Relit le dernier voyage sélectionné dans cette session.
    const savedId = sessionStorage.getItem(selectedTripStorageKey);
    // Garde la sélection si elle existe encore, sinon choisit le premier voyage disponible.
    const selectedTrip = databaseTrips.find((trip) => String(trip.id) === String(savedId)) || databaseTrips[0];
    // Active le voyage retenu ou efface une sélection devenue invalide.
    if (selectedTrip) selectDatabaseTrip(selectedTrip, false);
    else {
      clearDatabaseTripSelection();
      document.querySelectorAll('[data-selected-trip]').forEach((element) => {
        element.textContent = 'Aucun voyage sélectionné. Créez un voyage depuis le tableau de bord.';
      });
    }

    // Remplit le menu et la liste des voyages uniquement sur la page qui les affiche.
    if (document.getElementById('newTripForm')) {
      databaseTrips.forEach((trip) => {
        addTripToMenu(trip);
        appendSavedTripCard(trip);
      });
      if (!databaseTrips.length) {
        const empty = createTextElement('p', 'text-on-surface-variant', 'Aucun voyage enregistré. Créez votre premier voyage.');
        empty.dataset.emptyTrips = 'true';
        savedTripsContainer()?.append(empty);
      }
    }

    // Récupère l’identifiant retenu pour charger billets et dépenses.
    const tripId = selectedDatabaseTripId();
    // Charge les billets de la page transports, ou invite à créer un voyage.
    if (document.getElementById('new-ticket-form')) {
      if (tripId) {
        const tickets = await apiRequest(`/api/trips/${tripId}/tickets`);
        tickets.forEach(appendTicket);
        if (!tickets.length) {
          const empty = createTextElement('p', 'text-on-surface-variant', 'Aucun billet enregistré pour ce voyage.');
          empty.dataset.emptyTickets = 'true';
          document.getElementById('view-mes-billets').append(empty);
        }
      } else {
        const empty = createTextElement('p', 'text-on-surface-variant', 'Créez un voyage pour enregistrer des billets.');
        empty.dataset.emptyTickets = 'true';
        document.getElementById('view-mes-billets').append(empty);
        announce('Crée d’abord un voyage pour enregistrer des titres dans H2.');
      }
    }
    // Charge les dépenses de la page budget, ou affiche sa liste vide.
    if (document.getElementById('add-expense-form')) {
      if (tripId) {
        const budget = await apiRequest(`/api/trips/${tripId}/budget`);
        setCategoryBudgets(budget.categoryBudgets || {});
        initializeCategoryBudgets(tripId);
        const expenses = await apiRequest(`/api/trips/${tripId}/expenses`);
        renderExpenses(expenses);
      } else {
        initializeCategoryBudgets(null);
        renderExpenses([]);
        announce('Crée d’abord un voyage pour enregistrer des dépenses dans H2.');
      }
    }
  } catch (error) {
    document.querySelectorAll('[data-selected-trip]').forEach((element) => {
      element.textContent = 'Impossible de charger le voyage sélectionné.';
    });
    // Affiche les erreurs de chargement au lieu de laisser l’interface dans un faux état de réussite.
    announceRequestError('Impossible de charger les données depuis H2', error);
  }
  if (document.getElementById('profile-form')) {
    try {
      displayProfile(await apiRequest('/api/profile'));
    } catch (error) {
      document.querySelectorAll('[data-profile-summary]').forEach((element) => { element.textContent = 'Impossible de charger le profil.'; });
      announceRequestError('Impossible de charger le profil', error);
    }
  }
}

// Attend que le HTML soit prêt avant d’attacher les contrôles et charger les données.
document.addEventListener('DOMContentLoaded', () => {
  const sidebar = document.querySelector('.app-sidebar-details');
  if (sidebar) {
    const mobile = window.matchMedia('(max-width: 767px)');
    sidebar.open = !mobile.matches;
    mobile.addEventListener('change', (event) => { sidebar.open = !event.matches; });
  }
  const header = document.querySelector('header > div');
  if (header) {
    const button = document.createElement('button');
    button.type = 'button';
    button.dataset.appHandled = 'true';
    button.className = 'text-sm font-semibold text-primary shrink-0';
    button.textContent = 'Déconnexion';
    button.addEventListener('click', async () => {
      try {
        const response = await fetch('/api/csrf', { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(`Erreur serveur ${response.status}`);
        const csrf = await response.json();
        const form = document.createElement('form');
        form.method = 'post';
        form.action = '/logout';
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = csrf.parameterName;
        input.value = csrf.token;
        form.append(input);
        document.body.append(form);
        sessionStorage.removeItem(selectedTripStorageKey);
        form.submit();
      } catch (error) {
        announceRequestError('Déconnexion impossible', error);
      }
    });
    header.append(button);
  }
  initializeSharedControls();
  if (document.getElementById('newTripForm')) {
    initializeTrips();
    initializeDashboardControls();
    initializeTripCountryRows();
  }
  if (document.getElementById('new-ticket-form')) {
    initializeTickets();
    initializeTransportControls();
    const notice = [...document.querySelectorAll('p')].find((paragraph) => paragraph.textContent.includes('synchronisera automatiquement'));
    if (notice) notice.textContent = 'Les titres ajoutés sont enregistrés dans H2. Aucune réservation ou synchronisation externe n’est configurée.';
  }
  if (document.getElementById('add-expense-form')) {
    initializeBudget();
    const notice = [...document.querySelectorAll('p')].find((paragraph) => paragraph.textContent.includes('synchronisera automatiquement'));
    if (notice) notice.textContent = 'Cette dépense est enregistrée dans H2. Les justificatifs restent des noms de fichiers : aucun fichier n’est téléversé.';
  }
  if (document.getElementById('profile-form')) initializeProfile();
  initializeUnwiredButtons();
  initializeDatabaseData();
});