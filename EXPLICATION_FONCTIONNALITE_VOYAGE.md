# Comprendre la gestion des voyages

Ce document explique les fichiers qui participent à l’enregistrement des voyages, billets, dépenses et profils, ainsi que le fichier `DataInitializer.java` fourni avec la demande.

## Comment les fichiers travaillent ensemble

Quand une personne utilise le site, le navigateur envoie une requête à l’API. Le contrôleur reçoit cette requête, puis délègue le travail au service. Le service vérifie l’utilisateur et le voyage concernés, applique les règles métier et utilise les dépôts pour lire ou enregistrer les entités dans la base. Il renvoie ensuite un DTO — un objet conçu pour les échanges avec l’interface — que le navigateur affiche.

Au démarrage, `DataInitializer` prépare quelques rôles, permissions et pays si nécessaire. Les tests vérifient ensuite que les principales opérations fonctionnent avec une base H2 temporaire.

## Fichiers de l’API

### [`TravelDataController.java`](src/main/java/projet/saik/controller/TravelDataController.java)

Ce contrôleur expose les routes HTTP sous `/api`. Il fait le lien entre les requêtes du navigateur et `TravelDataService`, sans contenir lui-même les règles de gestion.

Les principales routes sont :

- `GET /api/trips` et `POST /api/trips` : consulter et créer des voyages ;
- `GET /api/trips/{tripId}/tickets` et `POST /api/trips/{tripId}/tickets` : consulter et ajouter les titres d’un voyage ;
- `DELETE /api/tickets/{ticketId}` : supprimer un titre ;
- `GET /api/trips/{tripId}/expenses` et `POST /api/trips/{tripId}/expenses` : consulter et ajouter les dépenses ;
- `DELETE /api/expenses/{expenseId}` : supprimer une dépense ;
- `GET /api/profile` et `PUT /api/profile` : consulter et modifier le profil.

Les annotations Spring associent chaque méthode à une route. `@Valid` déclenche la validation des données reçues. Pour les suppressions, le contrôleur renvoie `204 No Content` lorsque l’opération réussit.

### [`TravelDataService.java`](src/main/java/projet/saik/service/TravelDataService.java)

Ce service contient le travail métier réalisé par les routes :

- il retrouve la personne connectée à partir du contexte de sécurité ;
- il vérifie qu’un voyage appartient bien à cette personne avant d’en lire ou modifier les données ;
- il crée les voyages et leur budget ;
- il enregistre les titres et crée une dépense de transport automatique lorsque leur prix est supérieur à zéro ;
- il supprime la dépense automatique liée à un titre lorsque ce titre est supprimé ;
- il enregistre les dépenses manuelles et les informations du profil ;
- il convertit les entités de base de données en DTO de réponse.

Les méthodes annotées `@Transactional` regroupent les écritures dans une transaction. Les méthodes marquées `readOnly = true` servent uniquement à lire les données.

### [`TravelDataDtos.java`](src/main/java/projet/saik/web/TravelDataDtos.java)

Cette classe regroupe les objets échangés entre le navigateur et l’API. Les types `...Request` décrivent les données que le navigateur peut envoyer ; les types `...Response` décrivent les données renvoyées.

Par exemple, `TripRequest` contient le nom, le pays et les dates d’un nouveau voyage. Les annotations comme `@NotBlank`, `@NotNull`, `@Email` et `@DecimalMin` rejettent les champs absents ou mal formés avant leur traitement par le service.

## Modèles et accès aux données

### [`Expense.java`](src/main/java/projet/saik/model/Expense.java)

Cette entité JPA représente une dépense stockée dans la base. Elle contient notamment son libellé, son montant, sa catégorie et sa date. Les champs `paymentMethod` et `receiptName` enregistrent respectivement le moyen de paiement et le nom du justificatif sélectionné. Le fichier du justificatif lui-même n’est pas stocké par ces champs.

Une dépense est rattachée à un budget. Elle peut aussi être liée à un titre de transport lorsque le service l’a générée automatiquement.

### [`TransportTicket.java`](src/main/java/projet/saik/model/TransportTicket.java)

Cette entité représente un titre de transport associé à un voyage. Elle conserve le type de transport, la description, les lieux et horaires de départ et d’arrivée, le prix, la référence et le nom du justificatif.

La relation avec `Expense` permet de retrouver la dépense automatiquement créée pour le titre et de la supprimer avec lui.

### [`User.java`](src/main/java/projet/saik/model/User.java)

Cette entité représente un compte utilisateur. En plus de ses informations de connexion et de ses rôles, elle contient les informations de profil ajoutées ici : date de naissance, nationalité, téléphone et passeport.

Les relations vers les voyages et les rôles permettent à l’application de retrouver les voyages appartenant à une personne et ses autorisations.

### [`UserRepository.java`](src/main/java/projet/saik/repository/UserRepository.java)

Ce dépôt Spring Data JPA permet au service de retrouver et sauvegarder des comptes. Ses méthodes sont dérivées de leur nom : par exemple, `findByEmailIgnoreCase` recherche une adresse sans tenir compte des majuscules et minuscules, ce qui aide à vérifier qu’une adresse n’est pas déjà utilisée.

## Interface du navigateur

### [`travel-app.js`](src/main/resources/static/js/travel-app.js)

Ce fichier relie les pages HTML à l’API. Il :

- envoie les requêtes HTTP et ajoute le jeton CSRF aux requêtes qui modifient les données ;
- affiche les erreurs renvoyées par le serveur ;
- charge les voyages, billets, dépenses et le profil ;
- envoie les formulaires de création ou de mise à jour ;
- met à jour les listes et tableaux après une réponse réussie ;
- conserve l’identifiant du voyage sélectionné dans `sessionStorage` pendant la session du navigateur.

Pour les justificatifs, l’interface conserve uniquement le nom du fichier sélectionné. Le contenu du fichier n’est pas téléversé.

## Initialisation et tests

### [`DataInitializer.java`](src/main/java/projet/saik/config/DataInitializer.java)

Spring exécute la méthode `initDatabase` au démarrage grâce à `CommandLineRunner`. L’annotation `@Order(1)` demande que cette initialisation passe avant les autres `CommandLineRunner` ordonnés.

Si aucun rôle n’existe, le fichier crée les permissions `TRIP_READ`, `TRIP_WRITE` et `USER_DELETE`, puis les rôles `ROLE_USER` et `ROLE_ADMIN` avec leurs permissions respectives. Il vérifie ensuite séparément que `ROLE_USER` existe, afin de le créer s’il manque. Enfin, si aucun pays n’est enregistré, il ajoute la France, le Japon et l’Espagne. Cette classe ne crée pas de compte utilisateur.

### [`TravelDataServiceTests.java`](src/test/java/projet/saik/TravelDataServiceTests.java)

Ces tests démarrent l’application avec une base H2 en mémoire et une transaction de test. Avant chaque test, ils créent un compte de test et placent son identifiant dans le contexte de sécurité ; après le test, ils nettoient ce contexte.

Le premier test vérifie la création d’un voyage, l’ajout d’un billet, la création de la dépense automatique correspondante, l’ajout d’une dépense manuelle, le total du budget et la suppression de la dépense automatique avec le billet. Le second vérifie qu’un profil reste accessible après le changement d’adresse e-mail.

### [`SaikApplicationTests.java`](src/test/java/projet/saik/SaikApplicationTests.java)

Ce test vérifie que le contexte Spring démarre correctement. Il utilise également une base H2 en mémoire afin de ne pas dépendre de la base persistante du projet.
