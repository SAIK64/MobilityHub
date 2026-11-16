# Bilan du projet JEE — SAIK Travel

**État constaté le 16 novembre 2026**, à partir de la branche locale `template`
et des autres références Git disponibles localement. Le répertoire de travail
était propre lors de la comparaison ; la gestion multi-pays et le découpage
JavaScript sont présents dans le commit `1cc22e2` de `template`. Les
références `origin/*` sont les dernières connues localement : aucun
rafraîchissement depuis GitHub n'a été effectué pour ce bilan.

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

## Comparaison des branches

Une fonctionnalité présente dans une branche n'est pas nécessairement
disponible dans la branche qui sera livrée. Les constats ci-dessous portent
sur les commits des branches, sans les fusionner ni les exécuter.

| Branche ou référence | État observé | Intégration dans `template` |
|---|---|---|
| `main` / `origin/main` | Socle initial Spring Boot et documentation ; ne contient pas les fonctionnalités de voyage actuelles. | Le commit de `main` est un ancêtre de `template`. |
| `dev` | Branche de développement avec entités, contrôleurs et tests ; sa copie locale est en retard de quatre commits sur `origin/dev`. | `dev` et `origin/dev` sont des ancêtres de `template`. |
| `origin/dev` | État de développement antérieur aux deux commits spécifiques les plus récents de `template`. | Son historique est inclus dans `template`. |
| `template` / `origin/template` | Version examinée en détail dans ce bilan : interface, API métier, multi-pays et JavaScript découpé en modules. | Les deux références pointent localement sur `1cc22e2`. |
| `sauvegarde-template-avant-remplacement` / `origin/sauvegarde-template-avant-remplacement` | Sauvegarde d'un état antérieur de l'interface et du backend. | Le commit de sauvegarde est un ancêtre de `template`. |
| `origin/api-rest` | Ajoute deux DTO pour l'inscription et la réponse utilisateur. | Son commit spécifique n'est pas dans l'historique de `template` ; il ne constitue pas à lui seul un CRUD REST complet. |
| `origin/database` | Contient des commentaires supplémentaires et ajoute un champ `countryCount` à la réponse d'un voyage. | Trois commits ne sont pas dans l'historique de `template`. Le modèle et la base H2 existent déjà dans `template`, mais le champ `countryCount` spécifique n'y est pas exposé. |
| `origin/security` | Contient une implémentation d'inscription et de connexion, avec JWT et contrôleurs associés. | Son commit spécifique n'est pas dans l'historique de `template`. Cette dernière possède néanmoins sa propre implémentation d'authentification ; il ne faut pas supposer que les deux implémentations sont identiques. |

La comparaison avec `origin/api-rest` montre des DTO supplémentaires, pas
d'opérations CRUD supplémentaires pour les voyages. La branche
`origin/security` expose notamment les routes d'inscription et de connexion ;
elle ne résout pas non plus le manque de CRUD complet sur une ressource métier.
La branche `origin/database` fournit un nombre de pays, tandis que `template`
renvoie déjà la liste des pays du voyage.

Pour la base, **aucune des branches ou références examinées ne versionne de
fichier H2 `.mv.db`**. La présence d'un fichier local ne suffit donc pas à
satisfaire l'exigence « présente dans le repository ».

Avant de choisir une branche de livraison, actualiser les références distantes
avec l'équipe, vérifier les travaux éventuellement plus récents et comparer
les différences utiles. Une fusion éventuelle devra être décidée et testée
séparément : il peut y avoir des doublons ou des conflits entre implémentations.

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

La gestion multi-pays présente dans `template` permet de saisir plusieurs pays
à la création et de les ajouter ou retirer d'un voyage enregistré. Un parcours
de création, d'ajout et de retrait a été essayé dans un navigateur avec une API
simulée ; il reste à le vérifier avec la vraie application et la base H2,
notamment après rechargement.

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

Lors d'une exécution précédente de `./mvnw -q test` sur l'état local,
**12 tests sur 13 ont réussi**. Ce résultat n'a pas été revérifié pour chaque
branche. L'échec observé était
`AuthenticationTests.homeIsPublicAndLinksToAuthentication` : le test attend
une chaîne HTML exacte, alors que le lien de retour est présent avec des
espaces et retours à la ligne supplémentaires dans le rendu.

L'assertion a depuis été corrigée pour accepter ces espaces tout en vérifiant
le lien et son texte. Une nouvelle exécution de `./mvnw -q test` sur l'état
local a réussi : **13 tests sur 13**, sans échec ni erreur.

À faire :

- Ajouter des tests HTTP pour la validation, les droits et les opérations
  multi-pays, ainsi que pour le CRUD retenu.
- Tester manuellement la création, l'affichage, l'ajout et le retrait des pays
  après rechargement, ainsi que les parcours billets et dépenses.

## Dépôt et soutenance

L'historique de la branche courante montre des commits attribués à Ambre et à
deux noms d'auteur contenant « Iris ». Les références `origin/api-rest` et
`origin/security` contiennent également des commits attribués respectivement
à `sylia` et `Killian`, absents de l'historique de la branche `template`
examinée. Cela ne permet pas de déduire toute la contribution réelle de chaque
membre ni la régularité des contributions sur la branche qui sera livrée.
Vérifier l'historique et la branche de livraison,
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

- [ ] Actualiser les références distantes et choisir la branche de livraison
      avec l'équipe ; comparer et intégrer les travaux utiles si nécessaire.
- [ ] Compléter et tester un CRUD REST sur une même ressource.
- [ ] Préparer et versionner une base H2 de démonstration non sensible.
- [ ] Valider les associations et dissociations graphiques 1-N et N-N.
- [ ] Vérifier l'interprétation du critère MVC / données des vues.
- [x] Corriger le test échoué et relancer tous les tests.
- [ ] Tester le parcours complet et l'API depuis un client HTTP.
- [ ] Mettre à jour le README, dont la présentation initiale décrit encore
      des fonctionnalités de mobilité prévues qui ne correspondent pas
      entièrement au périmètre actuellement implémenté.
- [ ] Vérifier les contributions, les droits du dépôt et préparer la soutenance.
