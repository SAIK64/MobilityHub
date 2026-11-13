# SAIK — Gestion de voyages et de mobilités étudiantes

SAIK est un projet réalisé dans le cadre de la formation **ING2**, pour le
module **JEE**. L'objectif est de concevoir une application web de gestion de
voyages et de séjours étudiants, avec un parcours dédié à la mobilité
académique.

> Le dépôt contient actuellement le socle Spring Boot de l'application. Les
> fonctionnalités métier décrites ci-dessous constituent le périmètre prévu
> dans les spécifications ; elles ne sont pas toutes implémentées à ce stade.

## Présentation

La plateforme vise à gérer les étudiants, leurs dossiers de mobilité, les
destinations partenaires, les logements et les documents administratifs. La
mobilité académique est conçue comme une option activable sur un dossier
étudiant. Lorsqu'elle est activée, le parcours doit notamment prendre en
compte les justificatifs requis, le choix d'une destination et le suivi de
l'éligibilité.

Les principales règles métier envisagées sont :

- suivre l'état d'avancement des dossiers (incomplet, en cours, validé ou
  refusé) ;
- vérifier les documents requis pour une mobilité ;
- associer les étudiants à des destinations et gérer les capacités d'accueil ;
- référencer les logements disponibles liés aux destinations ;
- proposer une interface web et des endpoints REST.

Pour le détail du périmètre, du modèle de données et des règles fonctionnelles,
consulter le [document de spécifications et d'architecture](./sp_cifications_et_architecture_du_projet.md).

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

L'interface connectée comprend cinq vues Thymeleaf : le tableau de bord (`/` ou `/dashboard`), les
transports (`/transports`), le budget (`/budget`), le kit de sécurité
(`/securite`) et le profil (`/profil`). Les voyages, titres, dépenses et
informations de profil modifiables sont envoyés aux routes `/api/...` et
persistés par Spring Data JPA dans la base H2 locale `./data/saikdb`.
Hibernate crée ou met à jour les tables à partir des entités Java.
Les principales routes sont `/api/trips`, `/api/trips/{id}/tickets`,
`/api/trips/{id}/expenses` et `/api/profile`.

## Authentification

Créer un compte sur `/register`, puis se connecter sur `/login` avec son e-mail
et son mot de passe. L'adresse e-mail est normalisée en minuscules ; le mot
de passe doit contenir au moins 8 caractères et au plus 72 octets. Il est
enregistré sous forme d'empreinte BCrypt. Les pages et données de voyage
nécessitent une connexion ; chaque compte ne peut accéder qu'à ses propres
voyages, billets, dépenses et profil. La déconnexion est disponible dans
l'en-tête. Les requêtes modifiant les données et la déconnexion sont protégées
contre les attaques CSRF.

Les routes de la branche `security` sont également disponibles :

- `POST /api/v1/auth/register` accepte `{"email":"...","password":"..."}`.
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
