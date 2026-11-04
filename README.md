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

Le code actuellement présent initialise l'application Spring Boot et comprend
un test de chargement du contexte. Les entités, services, contrôleurs et
fonctionnalités métier décrits dans les spécifications restent à développer.
