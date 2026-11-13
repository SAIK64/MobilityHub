(() => {
  // Regroupe les interactions du navigateur avec les pages SAIK et leur API.
  // Mémorise le voyage choisi pendant la session du navigateur.
  const selectedTripStorageKey = 'saik.selected-trip-id';
  // Contient les voyages chargés depuis l’API.
  let databaseTrips = [];
  // Identifiant du voyage actuellement utilisé par les formulaires.
  let currentTripId = null;

  // Centralise les appels HTTP pour envoyer les bons en-têtes et traiter les erreurs.
  async function apiRequest(path, options = {}) {
    // Demande une réponse JSON et conserve les éventuels en-têtes fournis par l’appelant.
    const headers = { Accept: 'application/json', ...options.headers };
    // Indique au serveur que le corps des requêtes est encodé en JSON.
    if (options.body) headers['Content-Type'] = 'application/json';
    // Les requêtes qui modifient les données doivent présenter le jeton CSRF.
    if (options.method && !['GET', 'HEAD', 'OPTIONS'].includes(options.method.toUpperCase())) {
      // Récupère auprès du serveur le nom d’en-tête et la valeur du jeton CSRF.
      const csrfResponse = await fetch('/api/csrf', { headers: { Accept: 'application/json' } });
      // Arrête la requête si la protection CSRF ne peut pas être récupérée.
      if (!csrfResponse.ok) throw new Error(`Impossible d’obtenir la protection CSRF (${csrfResponse.status}).`);
      // Lit le jeton renvoyé par le serveur.
      const csrf = await csrfResponse.json();
      // Ajoute le jeton sous le nom d’en-tête attendu par Spring Security.
      headers[csrf.headerName] = csrf.token;
    }
    // Envoie la requête demandée avec les options et en-têtes préparés.
    const response = await fetch(path, { ...options, headers });
    // Convertit les réponses HTTP en erreurs compréhensibles quand le serveur échoue.
    if (!response.ok) {
      // Utilise le texte HTTP comme message par défaut.
      let detail = response.statusText;
      try {
        // Essaie de récupérer un message plus précis dans le corps JSON de l’erreur.
        const problem = await response.json();
        detail = problem.detail || problem.message || problem.error || detail;
      } catch (error) {
        // Tolère un corps qui n’est pas du JSON, mais propage les autres erreurs.
        if (!(error instanceof SyntaxError)) throw error;
      }
      // Inclut le code HTTP et, s’il existe, le détail envoyé par le serveur.
      throw new Error(`Erreur serveur ${response.status}${detail ? ` : ${detail}` : ''}`);
    }
    // Une suppression réussie peut ne renvoyer aucun corps.
    if (response.status === 204) return null;
    // Pour les autres réponses, retourne les données JSON à l’appelant.
    return response.json();
  }

  // Affiche une erreur de requête à la personne sans masquer son message utile.
  function announceRequestError(action, error) {
    announce(`${action} : ${error.message || 'une erreur inattendue est survenue.'}`);
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
    // Garde l’identifiant sous forme de texte pour comparer uniformément les valeurs du DOM.
    currentTripId = String(trip.id);
    // Conserve la sélection pendant la session du navigateur.
    sessionStorage.setItem(selectedTripStorageKey, currentTripId);
    // Calcule le nombre de jours avant le départ, sans afficher de valeur négative.
    const daysRemaining = Math.max(0, Math.ceil((new Date(`${trip.start}T00:00:00`) - new Date()) / 86400000));
    // Met à jour le bandeau du voyage si le tableau de bord expose cette fonction.
    window.switchTrip?.(trip.name, trip.iso, trip.start, trip.end, daysRemaining, 'Voyage enregistré dans H2');
    // Confirme la sélection sauf si l’appelant demande une mise à jour silencieuse.
    if (announceSelection) announce(`Voyage sélectionné : ${trip.name}.`);
  }

  // Affiche un message temporaire réutilisable pour les confirmations et les erreurs.
  function announce(message) {
    // Réutilise la notification existante si elle a déjà été créée.
    let toast = document.getElementById('app-toast');
    if (!toast) {
      // Crée l’élément de notification lorsqu’il n’existe pas encore.
      toast = document.createElement('div');
      // Donne un identifiant stable pour pouvoir le retrouver au prochain message.
      toast.id = 'app-toast';
      // Annonce le message aux technologies d’assistance.
      toast.setAttribute('role', 'status');
      toast.setAttribute('aria-live', 'polite');
      // Applique la présentation visuelle de la notification.
      toast.className = 'fixed bottom-6 right-6 z-50 max-w-sm rounded-lg bg-primary px-space-md py-3 text-on-primary shadow-lg';
      // Ajoute la notification à la page.
      document.body.append(toast);
    }
    // Remplace le texte affiché par le nouveau message.
    toast.textContent = message;
    // Rend la notification visible.
    toast.classList.remove('hidden');
    // Annule la disparition programmée du message précédent.
    window.clearTimeout(toast.hideTimer);
    // Masque le message après quelques secondes.
    toast.hideTimer = window.setTimeout(() => toast.classList.add('hidden'), 3500);
  }

  // Crée un élément texte sans interpréter sa valeur comme du HTML.
  function createTextElement(tag, className, value) {
    // Construit l’élément demandé.
    const element = document.createElement(tag);
    // Lui applique les classes visuelles reçues.
    element.className = className;
    // Insère le contenu comme du texte pour éviter d’interpréter du HTML fourni.
    element.textContent = value;
    // Retourne l’élément prêt à être ajouté à la page.
    return element;
  }

  // Prépare un téléchargement local à partir d’un contenu généré dans le navigateur.
  function downloadFile(name, content, type) {
    // Crée une URL temporaire qui représente le fichier à télécharger.
    const url = URL.createObjectURL(new Blob([content], { type }));
    // Crée un lien de téléchargement sans l’ajouter visiblement à la page.
    const link = document.createElement('a');
    // Associe au lien le contenu temporaire.
    link.href = url;
    // Définit le nom proposé au moment du téléchargement.
    link.download = name;
    // Déclenche le téléchargement comme si la personne avait cliqué sur le lien.
    link.click();
    // Libère l’URL temporaire une fois que le navigateur a commencé le téléchargement.
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
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
    button.append(createTextElement('span', 'font-label-md text-label-md text-on-surface-variant', trip.country));
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
    // Lit et nettoie les champs saisis par la personne.
    const name = document.getElementById('tripNameInput').value.trim();
    const country = document.getElementById('tripCountryInput').value.trim();
    const iso = document.getElementById('tripIsoInput').value.trim().toUpperCase();
    const start = document.getElementById('tripStartInput').value;
    const end = document.getElementById('tripEndInput').value;
    // Vérifie dans le navigateur le format du code pays avant l’appel réseau.
    if (!/^[A-Z]{2}$/.test(iso)) {
      announce('Le code pays doit contenir exactement deux lettres.');
      return;
    }
    // Refuse une date de fin antérieure à la date de départ.
    if (end < start) {
      announce('La date de fin doit être postérieure à la date de départ.');
      return;
    }

    try {
      // Envoie les informations du voyage au serveur pour les enregistrer.
      const trip = await apiRequest('/api/trips', {
        method: 'POST',
        body: JSON.stringify({ name, country, iso, start, end })
      });
      // Met à jour la liste locale et les différentes représentations du voyage.
      databaseTrips.push(trip);
      addTripToMenu(trip);
      appendSavedTripCard(trip);
      // Active le nouveau voyage sans afficher une deuxième confirmation.
      selectDatabaseTrip(trip, false);
      // Ferme le formulaire, le réinitialise puis confirme l’enregistrement.
      window.closeModal();
      document.getElementById('newTripForm').reset();
      announce('Voyage enregistré dans la base H2.');
    } catch (error) {
      // Affiche une explication si le serveur n’a pas pu enregistrer le voyage.
      announceRequestError('Impossible d’enregistrer le voyage', error);
    }
  };

  // Ajoute à la page un titre de transport déjà enregistré.
  function appendTicket(ticket) {
    // Cherche la zone des billets et évite d’insérer deux fois le même titre.
    const container = document.getElementById('view-mes-billets');
    if (!container || container.querySelector(`[data-saved-ticket="${ticket.id}"]`)) return;

    // Crée une carte pour présenter le titre.
    const card = document.createElement('article');
    // Enregistre son identifiant et son type pour permettre les filtres ultérieurs.
    card.dataset.savedTicket = ticket.id;
    card.dataset.transportType = ticket.type;
    card.dataset.appHandled = 'true';
    // Applique l’apparence de carte définie par l’interface.
    card.className = 'mt-space-md rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-space-md shadow-sm';
    // Prépare la ligne de titre et le bouton de suppression.
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
        announce('Titre supprimé de la base H2.');
      } catch (error) {
        // Préserve la carte à l’écran et explique l’échec si l’API refuse la suppression.
        announceRequestError('Impossible de supprimer le titre', error);
      }
    });
    heading.append(remove);
    card.append(heading);
    // Assemble les informations présentes et les sépare par un point médian.
    const details = [ticket.type, ticket.departure, ticket.arrival, ticket.departureTime, `${Number(ticket.price || 0).toFixed(2)} €`]
      .filter(Boolean)
      .join(' · ');
    card.append(createTextElement('p', 'mt-2 text-body-sm text-on-surface-variant', details));
    // Affiche la référence uniquement si elle a été renseignée.
    if (ticket.reference) card.append(createTextElement('p', 'mt-1 text-label-md text-on-surface-variant', `Référence : ${ticket.reference}`));
    // Précise que seul le nom du fichier est conservé, pas son contenu.
    if (ticket.attachmentName) card.append(createTextElement('p', 'mt-1 text-label-md text-on-surface-variant', `Fichier sélectionné : ${ticket.attachmentName} (non téléversé)`));
    // Ajoute la carte complète à la liste des billets.
    container.append(card);
  }

  // Rend la zone de justificatif utilisable par clic ou glisser-déposer.
  function initializeTickets() {
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

  function requireSelectedTripId() {
    // Lit l’identifiant actuellement sélectionné et vérifié.
    const tripId = selectedDatabaseTripId();
    // Empêche l’enregistrement d’un titre sans voyage correspondant en base.
    if (!tripId) throw new Error('Crée d’abord un voyage ou sélectionne un voyage enregistré.');
    // Retourne l’identifiant prêt à être utilisé dans l’URL de l’API.
    return tripId;
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
      // Assemble les valeurs du formulaire dans le format attendu par l’API.
      await saveTicket({
        type,
        description: document.getElementById('manualDescription').value.trim(),
        departure: form.querySelector('input[placeholder*="Départ"]')?.value || '',
        arrival: form.querySelector('input[placeholder*="Arrivée"]')?.value || '',
        departureTime: document.getElementById('departureTime').value,
        arrivalTime: document.getElementById('arrivalTime').value || null,
        price: Number(document.getElementById('ticketPrice').value),
        reference: document.getElementById('apiBookingReference').value.trim(),
        attachmentName: form.dataset.attachment || ''
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

  // Construit une ligne de tableau représentant une dépense enregistrée.
  function appendExpense(expense) {
    // Récupère le corps du tableau et évite d’insérer une dépense déjà affichée.
    const body = document.getElementById('expense-tbody');
    if (!body || body.querySelector(`[data-saved-expense="${expense.id}"]`)) return;
    // Enlève le message « aucune dépense » lorsqu’une première dépense arrive.
    body.querySelector('[data-empty-expenses]')?.remove();

    // Crée une ligne du tableau pour présenter cette dépense.
    const row = document.createElement('tr');
    // Applique les styles et mémorise les valeurs utilisées par les filtres.
    row.className = 'expense-row hover:bg-surface-container-low/50 transition-colors';
    row.dataset.savedExpense = expense.id;
    row.dataset.category = expense.category;
    row.dataset.sync = expense.autoGenerated ? 'AUTO' : 'MANUAL';
    // Traduit les catégories techniques en libellés lisibles dans le tableau.
    const categoryLabels = {
      Transport: 'Transports',
      Hébergement: 'Hébergements',
      Formalités: 'Formalités & Démarches',
      Activités: 'Activités & Visites',
      Restauration: 'Vie courante & Restauration',
      Tech: 'Télécoms & Tech'
    };
    // Prépare les six cellules affichées pour chaque dépense.
    [
      expense.description,
      expense.date ? new Date(`${expense.date}T00:00:00`).toLocaleDateString('fr-FR') : '',
      categoryLabels[expense.category] || expense.category,
      `${expense.autoGenerated ? 'Automatique' : 'Manuelle'} · ${expense.paymentMethod || 'Paiement non précisé'}`,
      `${Number(expense.amount).toFixed(2)} €`,
      expense.receiptName || '—'
    ].forEach((value) => {
      // Crée une cellule pour la valeur courante.
      const cell = document.createElement('td');
      cell.className = 'px-4 py-3 text-body-sm text-on-surface';
      // Insère la valeur comme du texte, sans interpréter de code HTML.
      cell.textContent = value;
      // Ajoute cette cellule à la ligne du tableau.
      row.append(cell);
    });
    // La dernière cellule contient le nom du justificatif.
    const receiptCell = row.lastElementChild;
    // Garde sa valeur d’origine pour l’export éventuel.
    receiptCell.dataset.exportValue = expense.receiptName || '—';
    // Crée le bouton qui permettra de supprimer la dépense.
    const remove = createTextElement('button', 'ml-2 text-label-sm text-error hover:underline', 'Supprimer');
    remove.type = 'button';
    remove.dataset.appHandled = 'true';
    remove.addEventListener('click', async () => {
      try {
        // Demande d’abord la suppression au serveur.
        await apiRequest(`/api/expenses/${expense.id}`, { method: 'DELETE' });
        // Retire la ligne et recalcule ce qui reste visible.
        row.remove();
        filterExpenses();
        announce('Dépense supprimée de la base H2.');
      } catch (error) {
        // Indique l’échec sans cacher les détails renvoyés par l’API.
        announceRequestError('Impossible de supprimer la dépense', error);
      }
    });
    // Place le bouton dans la cellule du justificatif puis ajoute la ligne au tableau.
    receiptCell.append(remove);
    body.append(row);
  }

  // Reconstruit le tableau à partir des dépenses chargées pour le voyage actif.
  function renderExpenses(expenses) {
    // Cherche le tableau et abandonne si la page ne contient pas cette section.
    const body = document.getElementById('expense-tbody');
    if (!body) return;
    // Efface les anciennes lignes avant de rendre la nouvelle liste.
    body.replaceChildren();
    // Ajoute une ligne de tableau pour chaque dépense reçue.
    expenses.forEach(appendExpense);
    // Affiche un état vide explicite lorsqu’aucune dépense n’est enregistrée.
    if (!expenses.length) {
      const row = document.createElement('tr');
      row.dataset.emptyExpenses = 'true';
      const message = createTextElement('td', 'px-4 py-6 text-center text-body-sm text-on-surface-variant', 'Aucune dépense enregistrée pour ce voyage.');
      message.colSpan = 6;
      row.append(message);
      body.append(row);
    }
    // Applique les filtres courants après le rendu.
    filterExpenses();
  }

  // Filtre les lignes selon la catégorie, l’origine et le texte recherché.
  // Applique les critères actuellement choisis aux lignes de dépenses.
  function filterExpenses() {
    // Lit les critères sélectionnés, en utilisant ALL pour ne rien filtrer.
    const category = document.getElementById('filter-category')?.value || 'ALL';
    const sync = document.getElementById('filter-type')?.value || 'ALL';
    const search = document.getElementById('filter-search')?.value.toLowerCase().trim() || '';
    // Ramène les variantes de libellés de l’interface aux catégories utilisées par les données.
    const normalizeCategory = (value) => ({
      'Transports (vols, TGV, métro)': 'Transport',
      Transports: 'Transport',
      'Hébergements (hôtels, guesthouses)': 'Hébergement',
      Logement: 'Hébergement',
      'Formalités & Démarches (visa)': 'Formalités',
      Assurance: 'Formalités',
      'Activités & Visites': 'Activités',
      Loisirs: 'Activités',
      'Vie courante & Restauration': 'Restauration',
      'Vie courante': 'Restauration',
      'Télécoms & Tech': 'Tech'
    }[value] || value);
    // Vérifie chaque dépense et masque celles qui ne correspondent pas aux critères choisis.
    document.querySelectorAll('.expense-row').forEach((row) => {
      const matches = (category === 'ALL' || normalizeCategory(row.dataset.category) === normalizeCategory(category))
        && (sync === 'ALL' || row.dataset.sync === sync)
        && (!search || row.textContent.toLowerCase().includes(search));
      row.style.display = matches ? '' : 'none';
    });
  }

  window.saveExpense = async (form) => {
    // Récupère les listes de choix et le montant saisi.
    const selects = form.querySelectorAll('select');
    const amount = form.querySelector('input[type="number"]')?.value;
    // Prépare l’objet JSON à partir des champs du formulaire.
    const expense = {
      description: form.querySelector('input[type="text"]')?.value.trim() || '',
      amount: Number(amount || 0),
      date: form.querySelector('input[type="date"]')?.value || '',
      category: ({
        'Transports (vols, TGV, métro)': 'Transport',
        'Hébergements (hôtels, guesthouses)': 'Hébergement',
        'Formalités & Démarches (visa)': 'Formalités',
        'Activités & Visites': 'Activités',
        'Vie courante & Restauration': 'Restauration',
        'Télécoms & Tech': 'Tech'
      })[selects[0]?.value] || selects[0]?.value || 'Transport',
      paymentMethod: selects[1]?.value || 'Non précisé',
      autoGenerated: form.querySelector('#check-auto')?.checked || false,
      receiptName: form.querySelector('input[type="file"]')?.files[0]?.name || ''
    };
    try {
      // Vérifie qu’un voyage enregistré est sélectionné avant l’envoi.
      const tripId = requireSelectedTripId();
      // Envoie la dépense à l’API avec son voyage parent.
      const savedExpense = await apiRequest(`/api/trips/${tripId}/expenses`, {
        method: 'POST',
        body: JSON.stringify(expense)
      });
      // Affiche les données enregistrées par le serveur et réapplique les filtres.
      appendExpense(savedExpense);
      filterExpenses();
      // Réinitialise les champs puis confirme l’enregistrement.
      form.reset();
      announce('Dépense enregistrée dans la base H2.');
    } catch (error) {
      // Signale l’erreur d’enregistrement à la personne.
      announceRequestError('Impossible d’enregistrer la dépense', error);
    }
  };

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
      ...rows.map((row) => [...row.cells].map((cell) => csvValue(cell.dataset.exportValue || cell.textContent.trim())).join(';'))
    ].join('\r\n');
    downloadFile('depenses-saik.csv', `\uFEFF${content}`, 'text/csv;charset=utf-8');
    announce('Export CSV téléchargé.');
  }

  // Prépare les contrôles de la page budget et relie ses filtres et exports.
  function initializeBudget() {
    const dateInput = document.querySelector('#add-expense-form input[type="date"]');
    if (dateInput && dateInput.value < new Date().toISOString().slice(0, 10)) {
      dateInput.value = new Date().toISOString().slice(0, 10);
    }
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
    // Liste les champs autorisés à être chargés et enregistrés depuis le profil.
    return ['firstName', 'lastName', 'birthDate', 'nationality', 'email', 'phone', 'passport'];
  }

  // Recopie les informations renvoyées par le serveur dans le formulaire du profil.
  function displayProfile(profile) {
    // Traite chaque champ avec le même mécanisme pour éviter de répéter le code.
    profileFields().forEach((id) => {
      // Cherche le champ HTML correspondant au nom de la propriété.
      const field = document.getElementById(id);
      // N’écrase le champ que si le serveur a bien renvoyé cette propriété.
      if (field && profile[id] !== undefined) field.value = profile[id] ?? '';
    });
  }

  window.showSavedToast = async () => {
    // Prépare un objet contenant les valeurs actuellement saisies dans le formulaire.
    const profile = {};
    profileFields().forEach((id) => {
      // Ignore les champs absents de la page et retire les espaces périphériques.
      const field = document.getElementById(id);
      if (field) profile[id] = field.value.trim();
    });
    try {
      // Envoie les nouvelles coordonnées au serveur pour les enregistrer.
      const savedProfile = await apiRequest('/api/profile', {
        method: 'PUT',
        body: JSON.stringify(profile)
      });
      // Remplace le formulaire par les valeurs réellement confirmées par le serveur.
      displayProfile(savedProfile);
      // Rend visible le statut de sauvegarde s’il existe dans la page.
      document.getElementById('save-status')?.classList.remove('opacity-0');
      // Confirme la mise à jour du profil.
      announce('Profil enregistré dans la base H2.');
    } catch (error) {
      // Présente l’erreur renvoyée si la sauvegarde échoue.
      announceRequestError('Impossible d’enregistrer le profil', error);
    }
  };

  // Vérifie les champs du formulaire, sans prétendre modifier un mot de passe serveur.
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

  window.exportTravelData = async () => {
    try {
      // Charge tous les voyages appartenant à la personne connectée.
      const trips = await apiRequest('/api/trips');
      // Récupère le profil et les détails de chaque voyage en parallèle.
      const [profile, tripData] = await Promise.all([
        apiRequest('/api/profile'),
        // Ajoute à chaque voyage ses titres et ses dépenses enregistrés.
        Promise.all(trips.map(async (trip) => ({
          ...trip,
          tickets: await apiRequest(`/api/trips/${trip.id}/tickets`),
          expenses: await apiRequest(`/api/trips/${trip.id}/expenses`)
        })))
      ]);
      // Crée un fichier JSON lisible contenant le profil et les données de voyage.
      downloadFile('donnees-voyage-saik.json', JSON.stringify({ profile, trips: tripData }, null, 2), 'application/json');
      // Confirme le téléchargement.
      announce('Données de voyage exportées depuis H2.');
    } catch (error) {
      // Explique l’échec si les données ne peuvent pas être récupérées ou exportées.
      announceRequestError('Impossible d’exporter les données', error);
    }
  };

  // Précise que l’application ne propose pas encore de révocation de sessions distantes.
  window.revokeSessions = () => announce('Aucun service de compte distant n’est configuré : aucune session n’a été révoquée.');

  // Relie le choix d’une photo à un aperçu local dans la page profil.
  function initializeProfile() {
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

  // Affiche ou masque les consignes complémentaires d’une section de sécurité.
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

  // Ouvre la fenêtre d’information d’une offre eSIM de démonstration.
  window.showQRCode = (planName, planPrice) => {
    document.getElementById('modalPlanName').textContent = planName;
    document.getElementById('modalPlanPrice').textContent = `${planPrice} · démonstration, aucun opérateur n’est connecté`;
    document.getElementById('esimModal').classList.remove('hidden');
  };

  // Informe que la réservation Wi-Fi n’est pas reliée à un fournisseur.
  window.openBookingPocketWifi = () => announce('La réservation Pocket Wi-Fi nécessite une connexion au fournisseur, non configurée ici.');

  // Demande confirmation avant d’afficher les consignes et d’ouvrir l’appel d’urgence.
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

  // Ferme le bandeau SOS et remet son bouton dans son état initial.
  window.dismissSOS = () => {
    document.getElementById('sosBanner')?.classList.add('hidden');
    const button = document.getElementById('sosTriggerBtn');
    if (button) button.textContent = 'Alerte SOS : informations d’urgence';
  };

  // Copie un texte dans le presse-papiers et annonce le résultat.
  function copyText(value, successMessage) {
    if (!navigator.clipboard) {
      announce('La copie automatique est indisponible dans ce contexte sécurisé.');
      return;
    }
    navigator.clipboard.writeText(value)
      .then(() => announce(successMessage))
      .catch(() => announce('La copie a échoué. Sélectionnez le texte et copiez-le manuellement.'));
  }

  // Copie l’adresse du service consulaire préparée dans l’interface.
  window.copyConsularAddress = () => copyText(
    '〒106-8514 東京都港区南麻布4-11-44 フランス大使館領事部',
    'Adresse consulaire copiée.'
  );
  // Copie le code eSIM de démonstration en rappelant qu’il n’est pas activable.
  window.copyLPAString = () => copyText(
    'LPA:1$smdp.japan-telecom.net$SAIK-STUDENT-JP-2025-X992',
    'Code de démonstration copié (non activable auprès d’un opérateur).'
  );

  // Configure les éléments communs aux pages : navigation, recherche et boutons partagés.
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
      databaseTrips = await apiRequest('/api/trips');
      // Relit le dernier voyage sélectionné dans cette session.
      const savedId = sessionStorage.getItem(selectedTripStorageKey);
      // Garde la sélection si elle existe encore, sinon choisit le premier voyage disponible.
      const selectedTrip = databaseTrips.find((trip) => String(trip.id) === String(savedId)) || databaseTrips[0];
      // Active le voyage retenu ou efface une sélection devenue invalide.
      if (selectedTrip) selectDatabaseTrip(selectedTrip, false);
      else clearDatabaseTripSelection();

      // Remplit le menu et la liste des voyages uniquement sur la page qui les affiche.
      if (document.getElementById('newTripForm')) {
        databaseTrips.forEach((trip) => {
          addTripToMenu(trip);
          appendSavedTripCard(trip);
        });
      }

      // Récupère l’identifiant retenu pour charger billets et dépenses.
      const tripId = selectedDatabaseTripId();
      // Charge les billets de la page transports, ou invite à créer un voyage.
      if (document.getElementById('new-ticket-form')) {
        if (tripId) {
          (await apiRequest(`/api/trips/${tripId}/tickets`)).forEach(appendTicket);
        } else {
          announce('Crée d’abord un voyage pour enregistrer des titres dans H2.');
        }
      }
      // Charge les dépenses de la page budget, ou affiche sa liste vide.
      if (document.getElementById('add-expense-form')) {
        if (tripId) {
          const expenses = await apiRequest(`/api/trips/${tripId}/expenses`);
          renderExpenses(expenses);
        } else {
          renderExpenses([]);
          announce('Crée d’abord un voyage pour enregistrer des dépenses dans H2.');
        }
      }
      // Charge le profil uniquement quand la page contient son formulaire.
      if (document.getElementById('profile-form')) {
        displayProfile(await apiRequest('/api/profile'));
      }
    } catch (error) {
      // Affiche les erreurs de chargement au lieu de laisser l’interface dans un faux état de réussite.
      announceRequestError('Impossible de charger les données depuis H2', error);
    }
  }

  // Attend que le HTML soit prêt avant d’attacher les contrôles et charger les données.
  document.addEventListener('DOMContentLoaded', () => {
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
})();
