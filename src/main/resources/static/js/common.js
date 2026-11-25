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


export { apiRequest, announce, announceRequestError, createTextElement, downloadFile };
