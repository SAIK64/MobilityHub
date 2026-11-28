// Comportements d’interface du tableau de bord.
// Les données (voyages, pays, statut…) sont rendues par le serveur via DashboardController :
// ce fichier ne fait qu’améliorer l’ergonomie des formulaires.

// Permet d’ajouter / retirer des lignes « pays » dans le formulaire de voyage.
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
  document.getElementById('addTripCountryBtn')?.addEventListener('click', () => {
    const row = container.querySelector('[data-trip-country-row]').cloneNode(true);
    row.querySelectorAll('input').forEach((input) => { input.value = ''; });
    row.querySelectorAll('select').forEach((select) => { select.value = ''; });
    container.append(row);
    updateRemoveButtons();
    row.querySelector('input').focus();
  });
  container.addEventListener('click', (event) => {
    const remove = event.target.closest('[data-remove-trip-country]');
    if (remove && container.querySelectorAll('[data-trip-country-row]').length > 1) {
      remove.closest('[data-trip-country-row]').remove();
      updateRemoveButtons();
    }
  });
  container.addEventListener('change', (event) => {
    const select = event.target.closest('[data-country-iso]');
    if (!select) return;
    const name = select.selectedOptions[0]?.textContent?.trim() || '';
    const hiddenName = select.closest('[data-trip-country-row]')?.querySelector('[data-country-name]');
    if (hiddenName) hiddenName.value = name;
  });
  container.querySelectorAll('[data-country-iso]').forEach((select) => {
    const hiddenName = select.closest('[data-trip-country-row]')?.querySelector('[data-country-name]');
    if (hiddenName && !hiddenName.value && select.value) {
      hiddenName.value = select.selectedOptions[0]?.textContent?.trim() || '';
    }
  });
  updateRemoveButtons();
}

// Demande une confirmation avant d’envoyer les formulaires portant data-confirm (suppressions).
function initializeConfirmForms() {
  document.querySelectorAll('form[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
  });
}

export { initializeTripCountryRows, initializeConfirmForms };
