# Bilan du projet JEE — SAIK Travel

**État constaté le 16 novembre 2026**, sur la branche locale `template`, avec
ses modifications non commitées. Ce bilan distingue le code présent des
fonctionnalités vérifiées en conditions réelles.

> **Correction du bilan précédent :** le CRUD REST n'est pas complet, le fichier
> de base H2 n'est pas versionné sur cette branche et le projet ne peut donc pas
> encore être considéré comme conforme à toutes les consignes. Aucune note ni
> durée de travail fiable ne peut être déduite de la grille fournie.

## Synthèse

| Exigence | État | Bilan |
|---|---|---|
| Spring Boot, Spring Data JPA, Spring MVC et Thymeleaf | Présent | Dépendances et architecture en place. |
| Base H2 embarquée et présente dans le dépôt | Partiel | H2 utilise un fichier local, mais seul `data/.gitkeep` est suivi par Git sur la branche courante. |
| Au moins cinq entités et relations 1-1, 1-N et N-N | Présent | Huit entités JPA et les trois types de relations sont déclarés. |
| Association et dissociation graphiques 1-N et N-N | Partiel | Actions présentes pour les pays et les billets/dépenses ; parcours à valider et sens de la dissociation 1-N à clarifier. |
| Logique métier | Présent | Calculs, validations et création de dépenses associées aux billets notamment. |
| API REST avec CRUD complet | À compléter | Les opérations existent mais aucun CRUD complet n'a été identifié pour une même ressource métier. |
| Recherche et interface graphique | Partiel | Recherche et filtres présents pour les billets et dépenses ; présentation et parcours à tester. |
| MVC et données transmises aux vues | Partiel | Contrôleurs et services présents ; les pages récupèrent surtout leurs données par JavaScript et API, pas directement par un modèle Thymeleaf. |
| Soutenance et contributions | À préparer | Répartition de la parole, démonstration et contributions individuelles à confirmer. |

## Architecture et données

Le [pom.xml](./pom.xml) déclare Spring Boot, Spring Data JPA, Spring MVC,
Thymeleaf et H2. Les entités se trouvent dans
[model](./src/main/java/projet/saik/model), les accès aux données dans
[repository](./src/main/java/projet/saik/repository), les règles dans
[service](./src/main/java/projet/saik/service) et les contrôleurs dans
[controller](./src/main/java/projet/saik/controller).

La configuration dans
[application.properties](./src/main/resources/application.properties) utilise
`jdbc:h2:file:./data/saikdb` et Hibernate avec `ddl-auto=update`. Un fichier
local `data/saikdb.mv.db` est présent dans l'environnement, mais il n'est pas
suivi par Git sur la branche examinée. Pour respecter explicitement la consigne,
préparer une base de démonstration sans données personnelles ni secrets, puis
vérifier qu'elle est bien versionnée dans la branche livrée. Faire attention
aux fichiers H2 actifs avant toute copie.

### Entités et relations

Les huit entités JPA sont `User`, `Trip`, `Country`, `Budget`, `Expense`,
`TransportTicket`, `Role` et `Permission`. `FlightDetails` est un objet
`@Embeddable`, pas une neuvième entité JPA.

Exemples de relations :

- **1-1** : voyage–budget et billet–dépense générée.
- **1-N** : utilisateur–voyages, voyage–billets, budget–dépenses.
- **N-N** : voyages–pays, utilisateurs–rôles, rôles–permissions.

La gestion multi-pays ajoutée localement permet de saisir plusieurs pays à la
création et de les ajouter ou retirer d'un voyage enregistré. Les routes et
contrôles sont présents, mais il reste à vérifier le parcours complet dans un
navigateur, notamment après rechargement. Ces modifications ne sont pas encore
commitées.

Pour les relations 1-N, la création d'un billet ou d'une dépense le rattache à
son voyage ou son budget ; la suppression supprime l'objet. Vérifier si
l'enseignant accepte cette suppression comme dissociation ou s'il attend
explicitement le retrait ou le transfert du lien tout en conservant l'entité.

## Fonctionnalités et logique métier

Fonctionnalités présentes dans le code :

- Création de compte, connexion, profil et isolation des données par compte.
- Création et sélection de voyages, avec affichage du statut et des dates.
- Gestion de billets et dépenses ; budgets par catégorie.
- Génération d'une dépense liée à un billet et suppression de cette dépense
  lors de la suppression du billet.
- Validation des dates du voyage et calcul de la durée d'un vol tenant compte
  des décalages horaires.
- Recherche et filtrage côté navigateur des billets et des dépenses.

Les réservations partenaires ne sont pas connectées et les pièces jointes ne
conservent que leur nom, sans téléversement. La page de sécurité fournit des
conseils mais n'envoie pas d'alerte. Ces limites sont précisées dans le
[README](./README.md).

Les templates utilisent notamment Tailwind CSS et un système de styles
commun. La qualité visuelle, le responsive et l'accessibilité restent à
vérifier sur les écrans et appareils de démonstration.

## API REST

Les routes sont définies dans
[TravelDataController.java](./src/main/java/projet/saik/controller/TravelDataController.java).
Le préfixe `/api` est utilisé pour les données métier ; `/api/v1` est utilisé
pour l'authentification. Un préfixe `/api/v1` n'est pas imposé par l'énoncé.

| Ressource | Création | Lecture | Modification | Suppression |
|---|---|---|---|---|
| Voyages | `POST /api/trips` | `GET /api/trips` | Pas de mise à jour du voyage ; ajout/retrait de pays possibles | Pas de suppression du voyage |
| Billets | `POST /api/trips/{tripId}/tickets` | `GET /api/trips/{tripId}/tickets` | Absente | `DELETE /api/tickets/{ticketId}` |
| Dépenses | `POST /api/trips/{tripId}/expenses` | `GET /api/trips/{tripId}/expenses` | Absente | `DELETE /api/expenses/{expenseId}` |
| Budget | Créé avec le voyage | `GET /api/trips/{tripId}/budget` | `PUT /api/trips/{tripId}/budget` | Pas de suppression dédiée |
| Profil | Compte créé à l'inscription | `GET /api/profile` | `PUT /api/profile` | Pas de suppression dédiée |

Pour les pays d'un voyage : `POST /api/trips/{tripId}/countries` et
`DELETE /api/trips/{tripId}/countries/{iso}`. Les méthodes sont déclarées dans
l'état local, mais elles nécessitent encore un test HTTP et graphique complet.

**Priorité :** choisir une ressource métier, par exemple `Trip`, et proposer
un CRUD REST complet cohérent : création, lecture, modification et suppression,
avec contrôle de propriété, validation et tests. Préparer ensuite un scénario
Bruno ou un autre client HTTP avec authentification, décrit dans le
[README](./README.md).

## MVC et vues

[PageController.java](./src/main/java/projet/saik/controller/PageController.java)
retourne les noms des vues. Les données métier passent principalement par
[TravelDataController.java](./src/main/java/projet/saik/controller/TravelDataController.java)
et sont chargées par
[travel-app.js](./src/main/resources/static/js/travel-app.js), puis affichées
dans les templates. C'est une séparation contrôleur/service/vue, mais ce n'est
pas une transmission systématique des données via un modèle Thymeleaf.

Comme la grille dit que « chaque vue manipule des données transmises par son
contrôleur », faire valider cette approche API + JavaScript, ou adapter les
vues concernées pour transmettre des données directement par Spring MVC.

## Tests

Lors de la dernière exécution de `./mvnw -q test`, **12 tests sur 13 ont
réussi**. L'échec est
`AuthenticationTests.homeIsPublicAndLinksToAuthentication` : le test attend
une chaîne HTML exacte, alors que le lien de retour est présent avec des
espaces et retours à la ligne supplémentaires dans le rendu.

À faire :

- Corriger l'assertion ou harmoniser le rendu, puis relancer toute la suite.
- Ajouter des tests HTTP pour la validation, les droits et les opérations
  multi-pays, ainsi que pour le CRUD retenu.
- Tester manuellement la création, l'affichage, l'ajout et le retrait des pays
  après rechargement, ainsi que les parcours billets et dépenses.

## Dépôt et soutenance

L'historique de la branche courante montre des commits attribués à Ambre et à
deux identités liées à Iris. Cela ne permet pas de déduire toute la
contribution réelle de chaque membre ni la régularité des contributions sur la
branche qui sera livrée. Vérifier l'historique et la branche de livraison,
faire des commits réguliers attribués correctement et documenter les rôles
réels. Ne pas réécrire l'historique partagé sans accord de l'équipe.

Vérifier également que l'URL du dépôt a été inscrite dans le Google Sheet et
que les droits d'accès sont corrects : ces éléments ne sont pas vérifiables
à partir du code local.

Pour la soutenance de **15 minutes suivies de 5 minutes de questions** :

1. Présenter le besoin, l'équipe et le parcours utilisateur.
2. Montrer les entités et les relations sur un schéma.
3. Démontrer une association et une dissociation, puis un calcul métier.
4. Montrer le CRUD REST complet depuis un client HTTP.
5. Présenter les tests, les limites et le reste à faire.
6. Répartir la parole et répéter en chronométrant.

L'énoncé indique un rendu avant le **4 décembre 2026 à 23 h 59**, des
soutenances les **2 et 4 décembre**, et demande aussi de détailler le reste à
faire jusqu'au **15 décembre** si nécessaire. Confirmer cette dernière date
avec l'enseignant si elle semble contradictoire avec le rendu final.

## Plan d'action prioritaire

- [ ] Compléter et tester un CRUD REST sur une même ressource.
- [ ] Préparer et versionner une base H2 de démonstration non sensible.
- [ ] Valider les associations et dissociations graphiques 1-N et N-N.
- [ ] Vérifier l'interprétation du critère MVC / données des vues.
- [ ] Corriger le test échoué et relancer tous les tests.
- [ ] Tester le parcours complet et l'API depuis un client HTTP.
- [ ] Mettre à jour le README, dont la présentation initiale décrit encore
      des fonctionnalités de mobilité prévues qui ne correspondent pas
      entièrement au périmètre actuellement implémenté.
- [ ] Vérifier les contributions, les droits du dépôt et préparer la soutenance.
