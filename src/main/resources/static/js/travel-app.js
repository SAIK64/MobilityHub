import { announce } from './common.js';
import { initializeTripCountryRows, initializeConfirmForms } from './trips.js';
import { initializeTickets, initializeTransportControls } from './transports.js';
import { initializeBudget } from './budget.js';
import { initializeProfile } from './profile.js';

// Point d’entrée JavaScript commun aux pages connectées.
// Les données affichées sont fournies par les contrôleurs Spring MVC (Model -> Thymeleaf) :
// ce fichier ne gère que des comportements d’interface.

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

// Attend que le HTML soit prêt avant d’attacher les comportements d’interface.
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
  initializeConfirmForms();
  if (document.getElementById('newTripForm')) initializeTripCountryRows();
  if (document.getElementById('new-ticket-form')) initializeTickets();
  if (document.getElementById('view-mes-billets')) initializeTransportControls();
  if (document.getElementById('add-expense-form')) initializeBudget();
  if (document.getElementById('profile-form')) initializeProfile();
  initializeUnwiredButtons();
});