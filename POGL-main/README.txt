#Rapport de POGL

Notre projet est construit sur le modèle MVC (Modèle-Vue-Controleur).

Notre projet est divisé en 4 parties :
  - Le Main, c'est ici qu'on lancera le jeu
  - Le CVue, c'est ici qu'on implémentera la vue du jeu, l'interface graphique
  - Le CModele, c'est ici qu'on implémentera les données du jeu, les données à afficher
  - Le Controleur, contiendra la logique des actions effectuées par l'utilisateur

Dans le CVue nous pouvons trouver l'impléménetation des tous les boutons utilisables par l'utilisateur, ainsi que les intércations qui y sont reliés.
Il y a notamment le colorisation des tuiles en fonction des attributs, des joueurs...

Dans le CModèle il y a l'implémentation des données du jeu : La class Aventurier, Tuile, CModèle...
Dedans se trouve l'initialisation du jeu en lui-même. C'est à dire l'implémentation de sa grille, des joueurs/aventuriers, de la zone de jeu...

Concernant le contrôle par l'utilisateur, nous avons décider de les implémenter dans le CVue car c'est plus simple des les implémenter des cette manière (comme indiqué dans le fichier Conway).

Concernant la répartition des tâches, nous avons travailler ensemble sur l'ensemble du code.
Nous n'avons pas rencontrer de difficultés particulières sur les fonctionnalités implémentées.
Malheureusement par manque de temps, nous n'avons pas réussi à implémenter l'ensemble des fonctionnalités souhaitées (Les 6 rôles d'Aventurier, La fin du jeu, ...)
