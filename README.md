# SAIK — Gestion de voyages étudiants

SAIK est un projet réalisé dans le cadre de la formation **ING2**, pour le
module **JEE**. Cette application web permet de préparer et de suivre des
voyages, leurs transports et leurs dépenses.

## Présentation

L'application propose :

- la création d'un compte, la connexion et la modification du profil ;
- la création et la sélection de voyages, avec un ou plusieurs pays ;
- l'ajout et le retrait de pays sur un voyage existant ;
- la gestion des titres de transport et des dépenses, avec recherche et
  filtres ;
- le suivi des budgets par catégorie et la création d'une dépense associée
  à un billet lorsque nécessaire ;
- une page de conseils de sécurité et des liens vers des sources officielles.

Les données sont accessibles depuis les pages web et une API REST. Les
réservations externes, le téléversement des pièces jointes et l'envoi d'alertes
ne sont pas connectés. L'API ne propose pas encore un CRUD complet pour chaque
ressource, notamment la modification et la suppression d'un voyage.

Pour une analyse du projet et des points restant à vérifier, consulter le
[bilan du projet](./BILAN_PROJET_JEE.md).

## Équipe et cadre du projet

Le projet est porté par l'équipe **SAIK**, dans le cadre du module JEE de la
formation ING2. Les documents actuellement disponibles dans le dépôt ne
précisent pas les noms des membres ni leur répartition des rôles ; ils ne sont
donc pas renseignés ici.

## Technologies

- Java 17
- Spring Boot 4.1.2
- Spring MVC et Thymeleaf pour l'application web
- Spring Security pour l'authentification et BCrypt pour les mots de passe
- Spring Data JPA pour la persistance
- H2 comme base de données embarquée
- Maven, avec Maven Wrapper

## Prérequis

- Un JDK compatible avec Java 17
- Aucun Maven système requis : le projet fournit le Maven Wrapper

## Lancer le projet

Depuis la racine du dépôt :

```bash
./mvnw spring-boot:run
```

Sous Windows, utiliser `mvnw.cmd` à la place de `./mvnw`.

Ouvrir ensuite [http://localhost:8080](http://localhost:8080), créer un compte
sur la page d'inscription et se connecter. Pour utiliser un autre port si
8080 est occupé :

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

L'application sera alors accessible sur `http://localhost:8081`.

## Exécuter les tests

```bash
./mvnw test
```

## Structure actuelle

```text
src/
├── main/
│   ├── java/projet/saik/       # Application Spring Boot
│   └── resources/
│       ├── static/             # Ressources statiques
│       ├── templates/          # Vues Thymeleaf
│       └── application.properties
└── test/java/projet/saik/      # Tests
```

La page d'accueil publique est accessible sur `/` et `/accueil` ; elle présente
l'application et donne accès à la connexion et à l'inscription. Après
connexion, `/` affiche le tableau de bord.

Les pages partagent la palette et les composants définis dans [DESIGN.md](./DESIGN.md).
Leur navigation commune se trouve dans `templates/fragments/layout.html` ;
les styles et paramètres partagés sont dans `static/css/design.css` et
`static/js/design-tokens.js`.

Le JavaScript des pages connectées utilise des modules ES : `travel-app.js`
initialise l'application, `common.js` regroupe les fonctions partagées, et
`trips.js`, `transports.js`, `budget.js` et `profile.js` gèrent leurs
fonctionnalités respectives.

L'interface connectée comprend cinq vues Thymeleaf : le tableau de bord (`/` ou `/dashboard`), les
transports (`/transports`), le budget (`/budget`), le kit de sécurité
(`/securite`) et le profil (`/profil`). Les voyages, titres, dépenses et
informations de profil modifiables sont envoyés aux routes `/api/...` et
persistés par Spring Data JPA dans la base H2 locale `./data/saikdb`.
Hibernate crée ou met à jour les tables à partir des entités Java.
Le fichier de base H2 est local et ignoré par Git.

## API métier

| Fonction | Routes |
|---|---|
| Lister et créer des voyages | `GET`, `POST /api/trips` |
| Ajouter ou retirer un pays | `POST /api/trips/{tripId}/countries`, `DELETE /api/trips/{tripId}/countries/{iso}` |
| Lister et ajouter des billets | `GET`, `POST /api/trips/{tripId}/tickets` |
| Supprimer un billet | `DELETE /api/tickets/{ticketId}` |
| Consulter et modifier le budget | `GET`, `PUT /api/trips/{tripId}/budget` |
| Lister et ajouter des dépenses | `GET`, `POST /api/trips/{tripId}/expenses` |
| Supprimer une dépense | `DELETE /api/expenses/{expenseId}` |
| Consulter et modifier le profil | `GET`, `PUT /api/profile` |

## Authentification

Créer un compte sur `/register`, puis se connecter sur `/login` avec son e-mail
et son mot de passe. L'adresse e-mail est normalisée en minuscules ; le mot
de passe doit contenir au moins 8 caractères et au plus 72 octets. Il est
enregistré sous forme d'empreinte BCrypt. Les pages et données de voyage
nécessitent une connexion ; chaque compte ne peut accéder qu'à ses propres
voyages, billets, dépenses et profil. La déconnexion est disponible dans
l'en-tête. Les requêtes modifiant les données et la déconnexion sont protégées
contre les attaques CSRF.

L'API propose également des routes d'authentification :

- `POST /api/v1/auth/register` accepte `{"firstName":"...","lastName":"...","email":"...","password":"..."}`.
- `POST /api/v1/auth/login` accepte les mêmes champs et retourne un jeton
  `Bearer` (`token`, `type`, `expiresInSeconds`).
- Pour les autres routes `/api/...`, transmettre ce jeton dans l'en-tête
  `Authorization: Bearer <token>` ; l'interface web utilise quant à elle une
  session et un jeton CSRF.

La durée de validité par défaut du jeton est de 60 minutes, configurable via
`APP_JWT_EXPIRATION_MINUTES`. Pour conserver les jetons après un redémarrage,
définir `APP_JWT_SECRET` avec un secret aléatoire d'au moins 32 octets, gardé
hors du dépôt. Sans cette variable, une clé aléatoire temporaire est générée
au démarrage et les jetons expirent au redémarrage. La déconnexion web invalide
la session, mais ne révoque pas les jetons API déjà émis.

Pour une base existante contenant l'ancien compte de démonstration,
sauvegarder d'abord `data/saikdb.mv.db`, puis définir la variable
`SAIK_LEGACY_DEMO_PASSWORD` avec un **nouveau** mot de passe conforme avant le
premier démarrage. L'ancien mot de passe de démonstration est remplacé par une
empreinte BCrypt ; les données existantes restent associées au compte. Le
démarrage échoue explicitement tant que ce nouveau mot de passe n'est pas
fourni. Une fois la migration réussie, retirer cette variable. Les autres mots
de passe historiques en clair sont également convertis en BCrypt au démarrage.
Sur une base neuve, aucun compte de démonstration n'est créé.

### Compte de démonstration Iris

Pour créer volontairement le compte du template, définir un mot de passe
personnel d'au moins 8 caractères et de 72 octets maximum dans
`SAIK_IRIS_DEMO_PASSWORD`, puis démarrer l'application. Par exemple :

```bash
read -rs -p "Mot de passe du compte Iris : " SAIK_IRIS_DEMO_PASSWORD; echo
export SAIK_IRIS_DEMO_PASSWORD
./mvnw spring-boot:run
```

Le compte utilise l'adresse fictive `iris.geyer@email.com` du template et le
mot de passe choisi, stocké avec BCrypt. Son profil et le voyage « Chine —
Découverte & Aventure », du 1er au 21 mars 2026, sont enregistrés en base avec
un budget vide. Les billets et dépenses ne sont pas créés automatiquement.
Ce séjour est passé et apparaît donc archivé.

La création est facultative et ne se produit qu'au démarrage lorsque la variable
est renseignée. Si un compte avec cette adresse existe déjà, il n'est jamais
modifié et son mot de passe n'est pas réinitialisé. Après création, retirer la
variable de l'environnement pour éviter de recréer le compte s'il est supprimé.
Si l'ancienne base contient le compte de démonstration historique, suivre aussi
les instructions de migration ci-dessus.

La console H2 est désactivée et bloquée par défaut. Avant une mise en
production, utiliser HTTPS, gérer les secrets et la base de façon appropriée,
et ajouter notamment une limitation des tentatives de connexion ainsi qu'une
procédure sécurisée de réinitialisation des mots de passe.

La page de sécurité affiche le voyage sélectionné et renvoie vers les conseils
officiels : elle ne fournit pas de numéros d'urgence par destination et ne
transmet aucune alerte.

Le profil affiche les informations enregistrées du compte. L'âge est calculé
à partir de la date de naissance ; saisir celle-ci au format `AAAA-MM-JJ` ou
avec un mois en français, puis enregistrer les modifications.

Les réservations auprès de partenaires ne sont pas connectées ;
les pièces jointes ne conservent que leur nom de fichier et ne sont pas
téléversées. Les actions de sécurité et de compte qui nécessitent un service
externe restent explicitement indiquées comme non connectées.


### Administration et sécurité

- **Premier administrateur** : définir `SAIK_ADMIN_EMAIL` et `SAIK_ADMIN_PASSWORD`
  (8 caractères minimum, 72 octets maximum) avant le démarrage. Le compte est créé
  avec le rôle `ROLE_ADMIN` (prénom « Administrateur », nom « SAIK ») ; s'il existe
  déjà, le rôle lui est ajouté sans changer son mot de passe. Sans `SAIK_ADMIN_EMAIL`,
  aucun administrateur n'est créé. Retirer `SAIK_ADMIN_PASSWORD` de l'environnement
  une fois le compte créé.
- **Routes d'administration** (session web ou jeton `Bearer`, rôle `ROLE_ADMIN`) :
  `GET /api/admin/users` liste les comptes (sans mot de passe) ;
  `DELETE /api/admin/users/{id}` supprime un compte avec ses voyages et exige la
  permission `USER_DELETE`. Un administrateur ne peut pas supprimer son propre compte.
  Les routes sont protégées par l'URL (`SecurityConfig`) et par `@PreAuthorize`.
- **Rôles et permissions** : les rôles (`ROLE_…`) et leurs permissions (`TRIP_READ`,
  `TRIP_WRITE`, `USER_DELETE`) sont transmis comme autorisations Spring Security,
  y compris dans les claims `roles` et `permissions` du jeton JWT.
- **Blocage des connexions** : après `SAIK_MAX_LOGIN_ATTEMPTS` échecs (5 par défaut)
  sur une adresse, la connexion est refusée pendant `SAIK_LOCKOUT_MINUTES` minutes
  (15 par défaut), sur le formulaire web comme sur l'API. Le compteur est en mémoire
  et repart de zéro au redémarrage.
- **Changement d'adresse e-mail** : `PUT /api/profile` exige `currentPassword` lorsque
  l'adresse change.
- **Cookie de session** : `HttpOnly` et `SameSite=Lax`. Définir `SAIK_COOKIE_SECURE=true`
  lorsque l'application est servie en HTTPS.
- `SAIK_SHOW_SQL=true` réactive l'affichage des requêtes SQL en développement.
