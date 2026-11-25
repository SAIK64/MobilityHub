import { announce } from './common.js';
import { initializeTripCountryRows, initializeConfirmForms } from './trips.js';
import { initializeTickets, initializeTransportControls } from './transports.js';
import { initializeBudget } from './budget.js';
import { initializeProfile } from './profile.js';

// Point d’entrée JavaScript commun aux pages connectées.
// Les données affichées sont fournies par les contrôleurs Spring MVC (Model -> Thymeleaf) :
// ce fichier ne gère que des comportements d’interface.

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

// Attend que le HTML soit prêt avant d’attacher les contrôles et charger les données.
document.addEventListener('DOMContentLoaded', () => {
  const sidebar = document.querySelector('.app-sidebar-details');
  if (sidebar) {
    const mobile = window.matchMedia('(max-width: 767px)');
    sidebar.open = !mobile.matches;
    mobile.addEventListener('change', (event) => { sidebar.open = !event.matches; });
  }
  // Masque après quelques secondes le message flash envoyé par le contrôleur.
  const toast = document.getElementById('app-toast');
  if (toast) window.setTimeout(() => toast.classList.add('hidden'), 5000);
  initializeSharedControls();
  initializeConfirmForms();
  if (document.getElementById('newTripForm')) initializeTripCountryRows();
  if (document.getElementById('new-ticket-form')) {
    initializeTickets();
    initializeTransportControls();
  }
  if (document.getElementById('add-expense-form')) initializeBudget();
  if (document.getElementById('profile-form')) initializeProfile();
  initializeUnwiredButtons();
});