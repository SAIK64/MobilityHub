(() => {
  const storageKey = 'saik.travel-data.v1';
  const defaultState = () => ({ trips: [], tickets: [], expenses: [], profile: null });

  function readState() {
    const stored = localStorage.getItem(storageKey);
    if (!stored) return defaultState();

    const state = JSON.parse(stored);
    if (!state || !Array.isArray(state.trips) || !Array.isArray(state.tickets) || !Array.isArray(state.expenses)) {
      throw new Error('Les données de voyage enregistrées sont illisibles.');
    }
    return state;
  }

  function saveState(state) {
    localStorage.setItem(storageKey, JSON.stringify(state));
  }

  function announce(message) {
    let toast = document.getElementById('app-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'app-toast';
      toast.setAttribute('role', 'status');
      toast.setAttribute('aria-live', 'polite');
      toast.className = 'fixed bottom-6 right-6 z-50 max-w-sm rounded-lg bg-primary px-space-md py-3 text-on-primary shadow-lg';
      document.body.append(toast);
    }
    toast.textContent = message;
    toast.classList.remove('hidden');
    window.clearTimeout(toast.hideTimer);
    toast.hideTimer = window.setTimeout(() => toast.classList.add('hidden'), 3500);
  }

  function makeId() {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  }

  function createTextElement(tag, className, value) {
    const element = document.createElement(tag);
    element.className = className;
    element.textContent = value;
    return element;
  }

  function downloadFile(name, content, type) {
    const url = URL.createObjectURL(new Blob([content], { type }));
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  function addTripToMenu(trip) {
    const menu = document.getElementById('tripDropdownMenu');
    if (!menu) return;

    const button = document.createElement('button');
    button.type = 'button';
    button.dataset.savedTrip = trip.id;
    button.dataset.appHandled = 'true';
    button.className = 'w-full text-left px-space-md py-2.5 hover:bg-surface-container-low flex items-center justify-between group transition-colors';
    button.append(createTextElement('span', 'font-label-lg text-label-lg text-on-surface', trip.name));
    button.append(createTextElement('span', 'font-label-md text-label-md text-on-surface-variant', trip.country));
    button.addEventListener('click', () => {
      const daysRemaining = Math.max(0, Math.ceil((new Date(`${trip.start}T00:00:00`) - new Date()) / 86400000));
      window.switchTrip(trip.name, trip.iso, trip.start, trip.end, daysRemaining, 'Voyage enregistré');
      const state = readState();
      state.selectedTrip = trip.id;
      saveState(state);
    });
    menu.append(button);
  }

  function initializeTrips() {
    const state = readState();
    state.trips.forEach(addTripToMenu);
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
        document.getElementById('selectedTripName').textContent = title;
        document.getElementById('heroTripTitle').textContent = title;
        document.getElementById('heroTripIso').textContent = iso;
        document.getElementById('heroTripStatusBadge').textContent = status;
        const dates = button.querySelector('.text-on-surface-variant')?.textContent.trim();
        if (dates) document.getElementById('heroTripDates').textContent = dates;
        if (countdown) document.getElementById('countdownDays').textContent = countdown;
        const current = readState();
        current.selectedTrip = `static:${title}`;
        saveState(current);
        menu.classList.add('hidden');
        document.getElementById('tripDropdownChevron')?.classList.remove('rotate-180');
        announce(`Voyage sélectionné : ${title}.`);
      });
    });
    const selectedTrip = state.trips.find((trip) => trip.id === state.selectedTrip);
    if (selectedTrip && typeof window.switchTrip === 'function') {
      const daysRemaining = Math.max(0, Math.ceil((new Date(`${selectedTrip.start}T00:00:00`) - new Date()) / 86400000));
      window.switchTrip(selectedTrip.name, selectedTrip.iso, selectedTrip.start, selectedTrip.end, daysRemaining, 'Voyage enregistré');
    }

    if (new URLSearchParams(window.location.search).get('action') === 'new-trip') {
      window.openModal?.();
    }
  }

  function savedTripsContainer() {
    const heading = [...document.querySelectorAll('h3')]
      .find((element) => element.textContent.includes('Voyages & Escapades Enregistrés'));
    const section = heading?.closest('div.bg-surface-container-lowest');
    return section?.children[1] || null;
  }

  function selectTripCard(card) {
    const name = card.querySelector('h4')?.textContent.trim();
    if (!name) return;
    const state = readState();
    state.selectedTrip = `static:${name}`;
    saveState(state);
    document.getElementById('selectedTripName').textContent = name;
    document.getElementById('heroTripTitle').textContent = name;
    document.getElementById('heroTripIso').textContent = card.querySelector('.w-12')?.textContent.trim() || '';
    document.getElementById('heroTripStatusBadge').textContent = card.dataset.tripStatus || 'En cours';
    const dates = card.querySelector('p')?.textContent.trim();
    if (dates) document.getElementById('heroTripDates').textContent = dates;
    announce(`Voyage sélectionné : ${name}.`);
  }

  function appendSavedTripCard(trip) {
    const container = savedTripsContainer();
    if (!container || container.querySelector(`[data-saved-trip-card="${trip.id}"]`)) return;
    const today = new Date().toISOString().slice(0, 10);
    const status = trip.end < today ? 'Archivé' : trip.start > today ? 'Futur' : 'Actif';
    const card = document.createElement('div');
    card.dataset.savedTripCard = trip.id;
    card.dataset.tripStatus = status;
    card.className = 'bg-surface-container-lowest p-space-md rounded-xl flex flex-col md:flex-row md:items-center justify-between gap-space-md shadow-sm';
    const details = document.createElement('div');
    details.className = 'flex items-center gap-space-md';
    details.append(createTextElement('div', 'w-12 h-12 rounded-lg bg-surface-container flex items-center justify-center font-bold text-headline-sm text-primary shrink-0', trip.iso));
    const copy = document.createElement('div');
    const heading = document.createElement('h4');
    heading.className = 'font-label-lg text-label-lg text-primary font-bold';
    heading.textContent = trip.name;
    const badge = createTextElement('span', 'ml-2 rounded-full bg-surface-container-high px-2 py-0.5 text-[10px] font-semibold text-on-surface-variant', status);
    const title = document.createElement('div');
    title.append(heading, badge);
    copy.append(title, createTextElement('p', 'mt-0.5 text-body-sm text-on-surface-variant', `${trip.country} (${trip.iso}) · ${trip.start} - ${trip.end}`));
    details.append(copy);
    const select = createTextElement('button', 'rounded-lg bg-surface-container-high px-space-md py-2 font-label-md text-label-md text-on-surface hover:bg-surface-container transition-colors', 'Sélectionner');
    select.type = 'button';
    select.dataset.appHandled = 'true';
    select.addEventListener('click', () => selectTripCard(card));
    card.append(details, select);
    container.append(card);
  }

  function initializeDashboardControls() {
    const container = savedTripsContainer();
    if (container) {
      container.querySelectorAll(':scope > div').forEach((card) => {
        const text = card.textContent;
        card.dataset.tripStatus = text.includes('Archivé') ? 'Archivé'
          : text.includes('À venir') || text.includes('Prévu') ? 'Futur' : 'Actif';
      });
      readState().trips.forEach(appendSavedTripCard);
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

  window.handleNewTripSubmit = (event) => {
    event.preventDefault();
    const name = document.getElementById('tripNameInput').value.trim();
    const country = document.getElementById('tripCountryInput').value.trim();
    const iso = document.getElementById('tripIsoInput').value.trim().toUpperCase();
    const start = document.getElementById('tripStartInput').value;
    const end = document.getElementById('tripEndInput').value;
    if (!/^[A-Z]{2}$/.test(iso)) {
      announce('Le code pays doit contenir exactement deux lettres.');
      return;
    }
    if (end < start) {
      announce('La date de fin doit être postérieure à la date de départ.');
      return;
    }

    const trip = { id: makeId(), name, country, iso, start, end };
    const state = readState();
    state.trips.push(trip);
    state.selectedTrip = trip.id;
    saveState(state);
    addTripToMenu(trip);
    const daysRemaining = Math.max(0, Math.ceil((new Date(`${start}T00:00:00`) - new Date()) / 86400000));
    window.switchTrip(name, iso, start, end, daysRemaining, 'Voyage enregistré');
    window.closeModal();
    document.getElementById('newTripForm').reset();
    announce('Voyage enregistré dans ce navigateur.');
  };

  function appendTicket(ticket) {
    const container = document.getElementById('view-mes-billets');
    if (!container || container.querySelector(`[data-saved-ticket="${ticket.id}"]`)) return;

    const card = document.createElement('article');
    card.dataset.savedTicket = ticket.id;
    card.dataset.transportType = ticket.type;
    card.dataset.appHandled = 'true';
    card.className = 'mt-space-md rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-space-md shadow-sm';
    const heading = document.createElement('div');
    heading.className = 'flex items-start justify-between gap-space-md';
    heading.append(createTextElement('h3', 'font-headline-sm text-headline-sm text-primary font-bold', ticket.description || `Transport ${ticket.type}`));
    const remove = createTextElement('button', 'rounded-lg px-3 py-1 text-label-md text-error hover:bg-error-container', 'Supprimer');
    remove.type = 'button';
    remove.dataset.appHandled = 'true';
    remove.addEventListener('click', () => {
      const state = readState();
      state.tickets = state.tickets.filter((item) => item.id !== ticket.id);
      saveState(state);
      card.remove();
      announce('Titre supprimé.');
    });
    heading.append(remove);
    card.append(heading);
    const details = [ticket.type, ticket.departure, ticket.arrival, ticket.departureTime, `${Number(ticket.price || 0).toFixed(2)} €`]
      .filter(Boolean)
      .join(' · ');
    card.append(createTextElement('p', 'mt-2 text-body-sm text-on-surface-variant', details));
    if (ticket.reference) card.append(createTextElement('p', 'mt-1 text-label-md text-on-surface-variant', `Référence : ${ticket.reference}`));
    if (ticket.attachment) card.append(createTextElement('p', 'mt-1 text-label-md text-on-surface-variant', `Fichier sélectionné : ${ticket.attachment} (non téléversé)`));
    container.append(card);
  }

  function initializeTickets() {
    readState().tickets.forEach(appendTicket);
    const form = document.getElementById('new-ticket-form');
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

  function saveTicket(ticket, confirmation = 'Titre enregistré dans ce navigateur.') {
    const state = readState();
    state.tickets.push({ id: makeId(), ...ticket });
    saveState(state);
    const savedTicket = state.tickets[state.tickets.length - 1];
    appendTicket(savedTicket);
    window.switchMainTab?.('mes-billets');
    announce(confirmation);
  }

  window.handleFormSubmit = (event) => {
    event.preventDefault();
    const form = document.getElementById('new-ticket-form');
    const type = form.querySelector('input[name="transportType"]:checked')?.value || 'Transport';
    saveTicket({
      type,
      description: document.getElementById('manualDescription').value.trim(),
      departure: form.querySelector('input[placeholder*="Départ"]')?.value || '',
      arrival: form.querySelector('input[placeholder*="Arrivée"]')?.value || '',
      departureTime: document.getElementById('departureTime').value,
      arrivalTime: document.getElementById('arrivalTime').value,
      price: document.getElementById('ticketPrice').value,
      reference: document.getElementById('apiBookingReference').value.trim(),
      attachment: form.dataset.attachment || ''
    });
    window.toggleModal(false);
    form.reset();
    delete form.dataset.attachment;
  };

  window.bookApiOffer = (title, price, type) => {
    saveTicket(
      { type, description: title, price, reference: 'Offre de démonstration' },
      'Offre ajoutée à vos titres de démonstration ; aucun achat externe n’a été effectué.'
    );
  };

  function appendExpense(expense) {
    const body = document.getElementById('expense-tbody');
    if (!body || body.querySelector(`[data-saved-expense="${expense.id}"]`)) return;

    const row = document.createElement('tr');
    row.className = 'expense-row hover:bg-surface-container-low/50 transition-colors';
    row.dataset.savedExpense = expense.id;
    row.dataset.category = expense.category;
    row.dataset.sync = expense.sync;
    [
      expense.date,
      expense.title,
      expense.categoryLabel,
      `${Number(expense.amount).toFixed(2)} €`,
      expense.paymentLabel,
      expense.receipt || '—'
    ].forEach((value) => {
      const cell = document.createElement('td');
      cell.className = 'px-4 py-3 text-body-sm text-on-surface';
      cell.textContent = value;
      row.append(cell);
    });
    body.append(row);
  }

  function filterExpenses() {
    const category = document.getElementById('filter-category')?.value || 'ALL';
    const sync = document.getElementById('filter-type')?.value || 'ALL';
    const search = document.getElementById('filter-search')?.value.toLowerCase().trim() || '';
    const normalizeCategory = (value) => ({
      'Hébergement': 'Logement',
      'Formalités': 'Assurance',
      'Restauration': 'Vie courante',
      'Activités': 'Loisirs'
    }[value] || value);
    document.querySelectorAll('.expense-row').forEach((row) => {
      const matches = (category === 'ALL' || normalizeCategory(row.dataset.category) === normalizeCategory(category))
        && (sync === 'ALL' || row.dataset.sync === sync)
        && (!search || row.textContent.toLowerCase().includes(search));
      row.style.display = matches ? '' : 'none';
    });
  }

  window.saveExpense = (form) => {
    const selects = form.querySelectorAll('select');
    const amount = form.querySelector('input[type="number"]')?.value;
    const expense = {
      id: makeId(),
      title: form.querySelector('input[type="text"]')?.value.trim() || '',
      amount: amount || '0',
      date: form.querySelector('input[type="date"]')?.value || '',
      category: selects[0]?.value || 'AUTRE',
      categoryLabel: selects[0]?.selectedOptions[0]?.textContent.trim() || 'Autre',
      paymentLabel: selects[1]?.selectedOptions[0]?.textContent.trim() || 'Non précisé',
      sync: form.querySelector('#check-auto')?.checked ? 'AUTO' : 'MANUAL',
      receipt: form.querySelector('input[type="file"]')?.files[0]?.name
        ? `${form.querySelector('input[type="file"]').files[0].name} (nom seulement, non téléversé)`
        : ''
    };
    const state = readState();
    state.expenses.push(expense);
    saveState(state);
    appendExpense(expense);
    filterExpenses();
    form.reset();
    announce('Dépense enregistrée dans ce navigateur.');
  };

  function csvValue(value) {
    const safeValue = /^[=+\-@]/.test(value) ? `'${value}` : value;
    return `"${safeValue.replaceAll('"', '""')}"`;
  }

  function exportExpenses() {
    const rows = [...document.querySelectorAll('#expense-tbody tr')];
    const content = [
      ['Date', 'Dépense', 'Catégorie', 'Montant', 'Paiement', 'Justificatif'].map(csvValue).join(';'),
      ...rows.map((row) => [...row.cells].map((cell) => csvValue(cell.textContent.trim())).join(';'))
    ].join('\r\n');
    downloadFile('depenses-saik.csv', `\uFEFF${content}`, 'text/csv;charset=utf-8');
    announce('Export CSV téléchargé.');
  }

  function initializeBudget() {
    const state = readState();
    state.expenses.forEach(appendExpense);
    const exportButton = document.getElementById('btn-export-academic');
    if (exportButton) {
      exportButton.dataset.appHandled = 'true';
      exportButton.textContent = 'Exporter en CSV';
      exportButton.addEventListener('click', exportExpenses);
    }
    ['btn-open-modal', 'btn-close-modal', 'btn-cancel-modal', 'toast-close'].forEach((id) => {
      const button = document.getElementById(id);
      if (button) button.dataset.appHandled = 'true';
    });
    document.querySelectorAll('#expense-tbody button[title]').forEach((button) => {
      button.dataset.appHandled = 'true';
      button.addEventListener('click', () => announce(`${button.title} : aucun fichier n’est joint à cette dépense de démonstration.`));
    });
    const categoryFilter = document.getElementById('filter-category');
    if (categoryFilter && ![...categoryFilter.options].some((option) => option.value === 'Tech')) {
      categoryFilter.add(new Option('Télécoms & Tech', 'Tech'));
    }
    ['filter-category', 'filter-type', 'filter-search'].forEach((id) => {
      document.getElementById(id)?.addEventListener('input', filterExpenses);
      document.getElementById(id)?.addEventListener('change', filterExpenses);
    });
    document.querySelector('input[type="file"]')?.addEventListener('change', (event) => {
      const file = event.target.files[0];
      if (file) announce(`Nom du justificatif sélectionné : ${file.name}. Le fichier n’est pas téléversé.`);
    });
  }

  function profileFields() {
    return ['firstName', 'lastName', 'birthDate', 'nationality', 'email', 'phone', 'passport'];
  }

  function loadProfile() {
    const profile = readState().profile;
    if (!profile) return;
    profileFields().forEach((id) => {
      const field = document.getElementById(id);
      if (field && profile[id] !== undefined) field.value = profile[id];
    });
  }

  window.showSavedToast = () => {
    const profile = {};
    profileFields().forEach((id) => {
      const field = document.getElementById(id);
      if (field) profile[id] = field.value.trim();
    });
    const state = readState();
    state.profile = profile;
    saveState(state);
    const status = document.getElementById('save-status');
    status?.classList.remove('opacity-0');
    announce('Profil enregistré dans ce navigateur.');
  };

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

  window.exportTravelData = () => {
    downloadFile('donnees-voyage-saik.json', JSON.stringify(readState(), null, 2), 'application/json');
    announce('Données locales exportées.');
  };

  window.revokeSessions = () => announce('Aucun service de compte distant n’est configuré : aucune session n’a été révoquée.');

  function initializeProfile() {
    loadProfile();
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

  window.toggleDetails = (sectionId) => {
    const instructions = {
      zairyuDetails: 'Préparez vos documents de séjour et suivez les consignes de l’immigration. Vérifiez auprès de la mairie de votre adresse les pièces et délais applicables à votre situation.',
      nhiDetails: 'Demandez à la mairie de votre adresse quelles démarches s’appliquent à votre statut. Conservez les justificatifs et vérifiez votre couverture auprès de votre établissement ou assureur.',
      douaneDetails: 'Les règles varient selon la substance et la quantité. Vérifiez chaque médicament auprès des autorités japonaises (MHLW) et de votre médecin avant le départ. Ce guide ne remplace pas un avis médical ou juridique.'
    };
    const trigger = [...document.querySelectorAll('button[onclick]')]
      .find((button) => button.getAttribute('onclick') === `toggleDetails('${sectionId}')`);
    if (!trigger || !instructions[sectionId]) {
      announce('Les informations détaillées ne sont pas disponibles.');
      return;
    }

    const existing = trigger.nextElementSibling;
    if (existing?.dataset.safetyDetails === sectionId) {
      existing.remove();
      trigger.setAttribute('aria-expanded', 'false');
      return;
    }

    const details = createTextElement('p', 'mt-2 rounded-lg bg-surface-container-low p-space-sm text-body-sm text-on-surface-variant leading-relaxed', instructions[sectionId]);
    details.dataset.safetyDetails = sectionId;
    trigger.after(details);
    trigger.setAttribute('aria-expanded', 'true');
  };

  window.showQRCode = (planName, planPrice) => {
    document.getElementById('modalPlanName').textContent = planName;
    document.getElementById('modalPlanPrice').textContent = `${planPrice} · démonstration, aucun opérateur n’est connecté`;
    document.getElementById('esimModal').classList.remove('hidden');
  };

  window.openBookingPocketWifi = () => announce('La réservation Pocket Wi-Fi nécessite une connexion au fournisseur, non configurée ici.');

  window.triggerUniversitySOS = () => {
    const banner = document.getElementById('sosBanner');
    const button = document.getElementById('sosTriggerBtn');
    const confirmed = window.confirm('Cette application ne transmet ni votre position ni une alerte. Appeler le 119 (urgences au Japon) ?');
    if (!confirmed) return;
    banner?.classList.remove('hidden');
    if (banner) {
      const message = banner.querySelector('.font-label-lg');
      if (message) message.textContent = 'SAIK n’envoie pas d’alerte. Appelez le 119 pour une ambulance ou les pompiers, ou le 110 pour la police.';
    }
    if (button) button.textContent = 'Appeler les secours (119)';
    window.location.href = 'tel:119';
  };

  window.dismissSOS = () => {
    document.getElementById('sosBanner')?.classList.add('hidden');
    const button = document.getElementById('sosTriggerBtn');
    if (button) button.textContent = 'Alerte SOS : informations d’urgence';
  };

  function copyText(value, successMessage) {
    if (!navigator.clipboard) {
      announce('La copie automatique est indisponible dans ce contexte sécurisé.');
      return;
    }
    navigator.clipboard.writeText(value)
      .then(() => announce(successMessage))
      .catch(() => announce('La copie a échoué. Sélectionnez le texte et copiez-le manuellement.'));
  }

  window.copyConsularAddress = () => copyText(
    '〒106-8514 東京都港区南麻布4-11-44 フランス大使館領事部',
    'Adresse consulaire copiée.'
  );
  window.copyLPAString = () => copyText(
    'LPA:1$smdp.japan-telecom.net$SAIK-STUDENT-JP-2025-X992',
    'Code de démonstration copié (non activable auprès d’un opérateur).'
  );

  function initializeSharedControls() {
    const titles = {
      '/': 'Tableau de bord | SAIK Travel',
      '/dashboard': 'Tableau de bord | SAIK Travel',
      '/transports': 'Transports & billets | SAIK Travel',
      '/budget': 'Budget & dépenses | SAIK Travel',
      '/securite': 'Kit de survie | SAIK Travel',
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

    const sosButton = document.getElementById('sosTriggerBtn');
    if (sosButton) {
      const label = sosButton.querySelector('span:last-child');
      if (label) label.textContent = 'Appeler les secours (119 Japon)';
      const dismissButton = document.querySelector('#sosBanner button');
      if (dismissButton) dismissButton.textContent = 'Fermer';
    }

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

  document.addEventListener('DOMContentLoaded', () => {
    initializeSharedControls();
    if (document.getElementById('newTripForm')) {
      initializeTrips();
      initializeDashboardControls();
    }
    if (document.getElementById('new-ticket-form')) {
      initializeTickets();
      initializeTransportControls();
      const notice = [...document.querySelectorAll('p')].find((paragraph) => paragraph.textContent.includes('synchronisera automatiquement'));
      if (notice) notice.textContent = 'Les titres ajoutés sont enregistrés localement dans ce navigateur. Aucune réservation ou synchronisation externe n’est configurée.';
    }
    if (document.getElementById('add-expense-form')) {
      initializeBudget();
      const notice = [...document.querySelectorAll('p')].find((paragraph) => paragraph.textContent.includes('synchronisera automatiquement'));
      if (notice) notice.textContent = 'Cette dépense est enregistrée localement dans ce navigateur ; aucune synchronisation avec une base de données n’est configurée.';
    }
    if (document.getElementById('profile-form')) initializeProfile();
    initializeUnwiredButtons();
  });
})();
