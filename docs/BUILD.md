# Construire, lancer et vérifier

## Prérequis

Java 17 ou plus récent. Le wrapper télécharge Maven 3.10.0 au premier lancement, avec SHA-256 épinglé. Les dépendances Maven viennent de Maven Central. Node 22+ est nécessaire pour le serveur de prévisualisation et la parité JVM/JavaScript ; aucune dépendance npm n’est requise.

Sous Windows, définir `JAVA_HOME` vers le JDK si celui-ci n’est pas détecté. Exécuter `mvnw.cmd` à la place de `./mvnw`.

## Application native

```sh
./mvnw -B verify
java -jar target/ile-interdite-2.0.0.jar
```

Le JAR inclut le moteur et Swing. Il n’exige ni navigateur ni serveur. Le bureau doit être disponible pour ouvrir la fenêtre. La partie native reste en mémoire ; fermer la fenêtre l’arrête. La sauvegarde/reprise locale est une fonction du client web.

## Client web

```sh
./mvnw -B -Pweb verify
npm test
npm start
```

Ouvrir `http://127.0.0.1:8080`. Le serveur est limité à la boucle locale, refuse les chemins hors `web/` et ne sert que des fichiers. Le port se configure via `PORT`.

Le profil `web` compile les mêmes classes Java avec TeaVM 0.16.0 en un module ES2015. Le résultat `web/generated/engine.js` est ignoré dans Git. Les notices de dépendances sont copiées dans `web/generated/licenses/` durant la construction. Elles doivent accompagner le module distribué. Ne pas utiliser l’ancien dossier de fichiers `.class` de `POGL-main/` pour lancer la refonte.

Pour héberger sur GitHub Pages ou un autre serveur statique, copier le dossier `web/` **après** la construction. Aucun serveur Java n’est nécessaire en production. Les chemins d’import sont relatifs et fonctionnent dans un sous-répertoire. La publication est gérée séparément ; le workflow de ce dépôt ne publie pas automatiquement.

## Contrôles

```sh
./mvnw -B -Pweb verify
npm test
java -cp target/classes:target/test-classes fr.badreddine.island.DesktopSmoke
```

Sous Windows, le séparateur de classpath est `;` ; entourer le classpath de guillemets. Sous Linux sans écran, utiliser `xvfb-run -a` devant la dernière commande. On peut passer un chemin PNG en argument de `DesktopSmoke` pour conserver le rendu, hors dépôt.

La CI utilise Java 17, Node 22, JUnit, la compilation TeaVM, la comparaison de traces et le smoke Swing sous Xvfb. Elle conserve deux artefacts : `island-web` et le JAR `island-desktop`. Une CI distante ne doit être annoncée comme réussie qu’après observation de son vrai résultat.

## Maintenance

- Ne jamais éditer `web/generated/engine.js` à la main. Recompiler après une modification Java.
- Le snapshot et le protocole sont documentés dans `FRONTEND_HANDOFF.md` ; incrémenter la version si une sauvegarde existante devient incompatible.
- Les interfaces doivent utiliser les possibilités calculées par le moteur et afficher son message de refus.
- `POGL-main/` est l’archive historique inchangée. Le build actuel ne la compile pas.
- Les nouveaux commits portent leur date réelle. Aucun auteur ni ancien commit n’a été réécrit.
- Les sources Java actuelles utilisent le style Google Java Format ; le formatteur n’est pas nécessaire au runtime.

## Dépendances distribuées

TeaVM et sa bibliothèque de classes : Apache-2.0, notices exactes dans `LICENSES/`. Le wrapper Maven comporte ses propres mentions Apache dans ses scripts. Les nouveaux fichiers relèvent de `LICENSE-NEW-CODE.txt`. Le code historique n’avait pas de licence déclarée dans le dépôt au moment de sa reprise ; cette licence ne lui est pas appliquée rétroactivement.
