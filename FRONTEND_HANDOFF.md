# Passation de l’interface web

La refonte visuelle est prise en charge par l’équipe interface. Les fichiers `web/index.html`, `web/style.css`, `web/app.js` et `web/favicon.svg` sont un premier client fonctionnel, à reprendre librement. Les changements du frontend sont coordonnés séparément des fichiers Java, des tests et de la compilation du moteur.

## Un moteur Java, deux interfaces

`src/main/java/fr/badreddine/island/Engine.java` contient toutes les règles. `Desktop.java` est le client Swing. `Bridge.java` exporte trois fonctions ES modules via TeaVM 0.16.0. La démo n’est pas une réécriture JavaScript des règles.

```sh
./mvnw -B -Pweb verify
python -m http.server 8080 --directory web
```

Sous Windows, utiliser `mvnw.cmd`. Java 17+ requis pour construire. `web/generated/engine.js` est généré, ignoré par Git ; ne jamais l’éditer. Le site compilé est le dossier `web/` complet. Les URL doivent rester relatives pour un déploiement sous un chemin GitHub Pages.

```js
import { reset, command, state } from './generated/engine.js';
let game = JSON.parse(reset(2024, 2, 1)); // seed entier, 2–4 joueurs, difficulté 1–4
game = JSON.parse(command('move:7'));
console.log(game.accepted, game.message);
game = JSON.parse(state());
```

Chaque appel est synchrone. Les trois fonctions renvoient une **chaîne JSON**, pas un objet. `reset` rejette une configuration invalide ; valider les inputs avant appel. Une commande invalide renvoie `accepted:false`, met à jour `message`, et ne consomme pas d’action. Pas d’accès réseau, de compte ni de chronomètre.

## Commandes

Les identifiants sont des entiers, joueurs et tuiles indexés à partir de zéro. Ne déduire aucune règle du dessin : utiliser les champs de possibilités du snapshot, puis laisser le moteur valider.

| Commande | Effet |
|---|---|
| `move:T` | Déplacer le joueur actif sur T. Utiliser `tiles[T].move`. |
| `shore:T` | Assécher T. Utiliser `tiles[T].shore`. |
| `capture` | Récupérer la relique du site courant. Utiliser `canCapture`. |
| `give:P:C` | Donner la carte relique C au joueur P ; actif seulement, 1 action. |
| `nav:P:T` | Navigateur : déplacer P sur T, 1 action. T doit être dans `players[P].navigate`. |
| `end` | Terminer les actions, piocher 2 cartes, inonder, puis changer de joueur. Peut interrompre sur DISCARD ou RESCUE. |
| `discard:P:C` | Défausser C de P, seulement si `phase=DISCARD` et `pendingPlayer=P`. |
| `rescue:T` | Déplacer `pendingPlayer` hors d’une tuile engloutie. Utiliser `tiles[T].rescue`. |
| `sand:P:T` | Jouer le sac de sable du joueur P sur T. Gratuit, T doit être inondée. |
| `heli:P:T:M` | Jouer l’hélicoptère de P : transporter vers T les passagers du masque M. Ils doivent partager la même tuile de départ. |
| `escape:P` | Jouer l’hélicoptère de P pour gagner : 4 reliques + tous sur l’héliport. |
| `pause` | Basculer la pause. Toutes les autres commandes sont bloquées pendant celle-ci. |

Masque passagers : joueur 0 = 1, joueur 1 = 2, joueur 2 = 4, joueur 3 = 8 ; additionner, par exemple les joueurs 0 et 2 donnent `5`. Le porteur de la carte peut transporter d’autres joueurs et n’est pas obligé d’embarquer.

Cartes : 0 feu, 1 vent, 2 eau, 3 terre, 4 hélicoptère, 5 sacs de sable, 6 montée des eaux. La montée des eaux est résolue immédiatement, jamais conservée dans la main.

## Snapshot stable, version 2

| Champ | Sens |
|---|---|
| `seed`, `difficulty`, `version` | Configuration/reproductibilité. |
| `phase` | `ACTION`, `DISCARD`, `RESCUE`, `WON`, `LOST`. |
| `active`, `actions`, `turn` | Joueur actif, actions restantes 0–3, tour commençant à 1. |
| `water`, `floodRate` | Niveau 1–10 et cartes inondation tirées 2–5. 10 déclenche la défaite. |
| `pendingPlayer` | Joueur qui doit défausser ou nager ; -1 sinon. |
| `paused`, `accepted`, `message` | Pause, résultat de la dernière commande, retour lisible. |
| `engineerBonus`, `pilotUsed` | Bonus d’assèchement gratuit disponible ; vol distant déjà utilisé. |
| `landing` | ID de l’héliport. |
| `treasures` | Tableau de 4 booléens, reliques communes récupérées. |
| `tiles` | 24 objets `{id,x,y,state,treasure,name,move,shore,rescue}`. |
| `players` | Objets `{role,name,tile,hand,navigate}`. `hand` est une liste de codes carte. |
| `decks` | Tailles des pioches `treasure`, `flood` ; défausses `floodDiscard` (ID tuiles) et `treasureDiscard` (cartes). |
| `journal` | 30 derniers événements, plus récent en premier. |

Une tuile : `state=0` sèche, `1` inondée, `2` engloutie définitivement. `treasure=-1` signifie aucun site de relique, sinon 0–3. Les coordonnées sont de 0 à 5, seules 24 positions existent. Plusieurs joueurs peuvent partager une tuile.

Rôles : `EXPLORER`, `ENGINEER`, `PILOT`, `DIVER`, `MESSENGER`, `NAVIGATOR`. Les noms français sont déjà fournis. Toutes les mains sont publiques, le jeu est coopératif sur le même écran.

## Transitions à ne pas masquer

- À plus de 5 cartes, DISCARD interrompt immédiatement une pioche ou un don. Présenter les cartes du joueur `pendingPlayer` et leur défausse. Une carte spéciale peut être jouée à la place. Le moteur reprend automatiquement la phase interrompue.
- Quand un joueur est englouti, RESCUE demande une destination. Les tuiles `rescue=true` sont les choix légaux. Après le clic, la suite des inondations peut imposer un autre sauvetage ou terminer la partie.
- En WON/LOST, garder le plateau inspectable mais bloquer les commandes de jeu. Montrer `message`, les reliques et le nombre de tours. `reset` crée une nouvelle partie.
- Le bonus Ingénieur peut autoriser `shore=true` avec `actions=0`. Ne pas désactiver toutes les commandes en bloc à zéro.
- Le Pilote peut faire un déplacement adjacent normal après son vol. `tiles[].move` prend en compte ce détail.
- Un joueur ne doit pas être masqué par le dessin de l’inondation. Couleur + texte/icône pour chacun des 3 états.

## Sauvegarde / replay

Enregistrer `{version:2, config:{seed,players,difficulty}, history:[commandes acceptées]}` en localStorage. Pour recharger, appeler `reset`, puis rejouer chaque commande, en vérifiant `accepted`. La pause est aussi une commande rejouable. Limiter la taille, valider types/configuration et ignorer une sauvegarde incompatible. Ne jamais restaurer un état en modifiant directement les tuiles.

Le client initial utilise la clé `island-expedition-v2`, sauvegarde après chaque action et exporte le journal JSON. Il rétablit le focus lors des rafraîchissements et propose Tab/Entrée, les flèches sur le plateau, M/S. Préserver l’accessibilité, `prefers-reduced-motion`, les messages `aria-live` et une vraie fermeture des dialogues au clavier.

## Règles de cette édition

Adaptation pédagogique inspirée de Forbidden Island, pas une édition officielle : graphismes et noms de lieux originaux ; rôles attribués par seed ; bonus Ingénieur limité à deux assèchements consécutifs ; sauvetage à résoudre avant toute carte spéciale ; Navigateur limité aux tuiles encore présentes, avec les diagonales de l’Exploratrice. Les 4 reliques, 24 tuiles, 6 rôles, les pioches et conditions de fin sont jouables. Les écarts sont documentés dans README/RULES.

Conserver l’attribution de Matt Leacock/Gamewright pour l’inspiration et celle du projet universitaire de 2024. Ne pas réutiliser les illustrations commerciales du jeu. Pas de date historique inventée pour cette refonte.
