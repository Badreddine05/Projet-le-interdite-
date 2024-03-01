import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Classe pour notre contrôleur rudimentaire.
 *
 * Le contrôleur implémente l'interface [ActionListener] qui demande
 * uniquement de fournir une méthode [actionPerformed] indiquant la
 * réponse du contrôleur à la réception d'un événement.
 */
class Controleur implements ActionListener {
    /**
     * On garde un pointeur vers le modèle, car le contrôleur doit
     * provoquer un appel de méthode du modèle.
     * Remarque : comme cette classe est interne, cette inscription
     * explicite du modèle est inutile. On pourrait se contenter de
     * faire directement référence au modèle enregistré pour la classe
     * englobante [VueCommandes].
     */
    CModele modele;
    public Controleur(CModele modele) { this.modele = modele; }

    /**
     * Controle des déplacements du joueur
     */

    public void actionPerformed(ActionEvent e) {

    }

}
/** Fin du contrôleur. */
