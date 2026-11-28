import { announce, downloadFile } from './common.js';

// Comportements d’interface de la page budget.
// Les dépenses, totaux et diagrammes sont rendus par le serveur via BudgetController.

// Protège une valeur CSV contre les séparateurs, guillemets et formules de tableur.
function csvValue(value) {
  const safeValue = /^[=+\-@]/.test(value) ? `'${value}` : value;
  return `"${safeValue.replaceAll('"', '""')}"`;
}

// Génère un fichier CSV contenant les lignes de dépenses affichées.
function exportExpenses() {
  const rows = [...document.querySelectorAll('#expense-tbody tr.expense-row')];
  const content = [
    ['Dépense', 'Date', 'Catégorie', 'Provenance & Statut', 'Montant', 'Justificatif'].map(csvValue).join(';'),
    ...rows.map((row) => [...row.querySelectorAll('td[data-export]')].map((cell) => csvValue(cell.dataset.export)).join(';'))
  ].join('\r\n');
  downloadFile('depenses-saik.csv', `﻿${content}`, 'text/csv;charset=utf-8');
  announce('Export CSV téléchargé.');
}

// Ouvre / ferme une fenêtre modale à partir de boutons.
function bindModal(modalId, openId, closeIds) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  const open = () => { modal.classList.remove('hidden'); modal.classList.add('flex'); };
  const close = () => { modal.classList.add('hidden'); modal.classList.remove('flex'); };
  const opener = document.getElementById(openId);
  if (opener) {
    opener.dataset.appHandled = 'true';
    opener.addEventListener('click', open);
  }
  closeIds.forEach((id) => {
    const button = document.getElementById(id);
    if (!button) return;
    button.dataset.appHandled = 'true';
    button.addEventListener('click', close);
  });
}

function initializeBudget() {
  const exportButton = document.getElementById('btn-export-academic');
  if (exportButton) {
    exportButton.dataset.appHandled = 'true';
    exportButton.addEventListener('click', exportExpenses);
  }
  bindModal('expense-modal', 'btn-open-modal', ['btn-close-modal', 'btn-cancel-modal']);
  bindModal('budget-modal', 'btn-edit-budgets', ['btn-cancel-budget']);
  // Le navigateur n’envoie que le nom du fichier : on le recopie dans le champ caché receiptName.
  const file = document.getElementById('expense-receipt-file');
  file?.addEventListener('change', () => {
    const name = file.files[0]?.name || '';
    document.getElementById('expense-receipt-name').value = name;
    if (name) announce(`Nom du justificatif sélectionné : ${name}. Le fichier n’est pas téléversé.`);
  });
  // Les filtres sont envoyés au contrôleur dès qu’une liste change.
  document.querySelectorAll('#expense-filters select').forEach((select) => {
    select.addEventListener('change', () => select.form.submit());
  });
}

export { initializeBudget };
