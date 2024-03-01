import java.util.ArrayList;
import java.util.Random;

/**
 * Le modèle : le coeur de l'application.
 *
 * Le modèle étend la classe [Observable] : il va posséder un certain nombre
 * d'observateurs (ici, un : la partie de la vue responsable de l'affichage)
 * et devra les prévenir avec [notifyObservers] lors des modifications.
 * Voir la méthode [avance()] pour cela.
 */
class CModele extends Observable {
    /** On fixe la taille de la grille. */
    public static final int HAUTEUR=6, LARGEUR=6;
    /** On stocke un tableau de cellules. */
    private Tuile[][] jeu;
    private Aventurier aventurier = new Aventurier(3, 3, Artefact.NONE, 3);
    /** Construction : on initialise un tableau de cellules. */
    public CModele() {
        /**
         * Pour éviter les problèmes aux bords, on ajoute une ligne et une
         * colonne de chaque côté, dont les cellules n'évolueront pas.
         */

        jeu = new Tuile[LARGEUR+2][HAUTEUR+2];
        for(int i=0; i<LARGEUR+2; i++) {
            for(int j=0; j<HAUTEUR+2; j++) {
                jeu[i][j] = new Tuile(this, i, j, 1, false, Artefact.NONE, false, false);
            }
        }
        init();
    }

    /**
     * Initialisation aléatoire des cellules, exceptées celle des bords qui
     * ont été ajoutés.
     */
    public void init() {
        // On init les tuiles mer
        jeu[1][1].setMer();
        jeu[1][2].setMer();
        jeu[2][1].setMer();

        jeu[1][5].setMer();
        jeu[1][6].setMer();
        jeu[2][6].setMer();

        jeu[5][6].setMer();
        jeu[6][6].setMer();
        jeu[6][5].setMer();

        jeu[5][1].setMer();
        jeu[6][1].setMer();
        jeu[6][2].setMer();
        // + Les tuiles invisbles
        jeu[0][3].setMer();
        jeu[0][4].setMer();

        jeu[3][7].setMer();
        jeu[4][7].setMer();

        jeu[7][3].setMer();
        jeu[7][4].setMer();

        jeu[4][0].setMer();
        jeu[3][0].setMer();

        // On init les tuiles avec les artefacts
        // On place le joueur
        jeu[3][3].setAventurier();

        // On place les 6 zones inondés de manière aléatoire
        Random rand = new Random();

        jeu[(rand.nextInt(4-3))+3][1].decreaseEtat();
        jeu[(rand.nextInt(5-2))+2][2].decreaseEtat();
        jeu[(rand.nextInt(6-1))+1][3].decreaseEtat();
        jeu[(rand.nextInt(6-1))+1][4].decreaseEtat();
        jeu[(rand.nextInt(5-2))+2][5].decreaseEtat();
        jeu[(rand.nextInt(4-3))+3][6].decreaseEtat();

        // On place l'héliport de manière aléatoire

        jeu[(rand.nextInt(5-2))+2][(rand.nextInt(5-2))+2].setHeliport();

        // On place l'artefact

        jeu[(rand.nextInt(5-2))+2+1][(rand.nextInt(5-2))+2+1].setArtefact();
    }

    /**
     * Mouvement avance du joueur
     */
    public void aventurierMonte() {
        /**
         * On procède en deux étapes.
         *  - D'abord, pour chaque cellule on évalue ce que sera son état à la
         *    prochaine génération.
         *  - Ensuite, on applique les évolutions qui ont été calculées.
         */
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX][posY-1].isValide() && this.aventurier.getNumberAction() >= 1) {
            this.aventurier.decreaseNumberAction();
            this.aventurier.deplaceAventurier(posX, posY-1);
            jeu[posX][posY].supprimeAventurier();
            jeu[posX][posY-1].setAventurier();
        }
        System.out.println("playerX : " + posX + ", playerY : " + posY);
        /**
         * Pour finir, le modèle ayant changé, on signale aux observateurs
         * qu'ils doivent se mettre à jour.
         */
        notifyObservers();
    }

    public void aventurierDescend() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX][posY+1].isValide() && this.aventurier.getNumberAction() >= 1) {
            this.aventurier.decreaseNumberAction();
            this.aventurier.deplaceAventurier(posX, posY+1);
            jeu[posX][posY].supprimeAventurier();
            jeu[posX][posY+1].setAventurier();
        }
        System.out.println("playerX : " + posX + ", playerY : " + posY);
        notifyObservers();
    }

    public void aventurierDroite() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX+1][posY].isValide() && this.aventurier.getNumberAction() >= 1) {
            this.aventurier.decreaseNumberAction();
            this.aventurier.deplaceAventurier(posX+1, posY);
            jeu[posX][posY].supprimeAventurier();
            jeu[posX+1][posY].setAventurier();
        }
        System.out.println("playerX : " + posX + ", playerY : " + posY);
        notifyObservers();
    }

    public void aventurierGauche() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX-1][posY].isValide() && this.aventurier.getNumberAction() >= 1) {
            this.aventurier.decreaseNumberAction();
            this.aventurier.deplaceAventurier(posX-1, posY);
            jeu[posX][posY].supprimeAventurier();
            jeu[posX-1][posY].setAventurier();
        }
        System.out.println("playerX : " + posX + ", playerY : " + posY);
        notifyObservers();
    }

    /**
     * Une méthode pour renvoyer la cellule aux coordonnées choisies (sera
     * utilisée par la vue).
     */
    public Tuile getTuile(int x, int y) {
        return jeu[x][y];
    }
    /**
     * Notez qu'à l'intérieur de la classe [CModele], la classe interne est
     * connue sous le nom abrégé [Cellule].
     * Son nom complet est [CModele.Cellule], et cette version complète est
     * la seule à pouvoir être utilisée depuis l'extérieur de [CModele].
     * Dans [CModele], les deux fonctionnent.
     */

    public void passeTour() {
        this.aventurier.resetNumberAction();
        Random rand = new Random();
        jeu[(rand.nextInt(4-3))+3][1].decreaseEtat();
        jeu[(rand.nextInt(5-2))+2][2].decreaseEtat();
        jeu[(rand.nextInt(6-1))+1][3].decreaseEtat();
        jeu[(rand.nextInt(6-1))+1][4].decreaseEtat();
        jeu[(rand.nextInt(5-2))+2][5].decreaseEtat();
        jeu[(rand.nextInt(4-3))+3][6].decreaseEtat();
    }

    public void assecheTuileS() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX][posY].getEtat() == 0 && (!jeu[posX][posY].isMer())) {
            jeu[posX][posY].increaseEtat();
        }
    }

    public void assecheTuileH() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX][posY-1].getEtat() == 0 && (!jeu[posX][posY-1].isMer())) {
            jeu[posX][posY-1].increaseEtat();
        }
    }

    public void assecheTuileB() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX][posY+1].getEtat() == 0 && (!jeu[posX][posY-1].isMer())) {
            jeu[posX][posY+1].increaseEtat();
        }
    }

    public void assecheTuileD() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX+1][posY].getEtat() == 0 && (!jeu[posX][posY-1].isMer())) {
            jeu[posX+1][posY].increaseEtat();
        }
    }

    public void assecheTuileG() {
        int posX = this.aventurier.getPositionX();
        int posY = this.aventurier.getPositionY();
        if (jeu[posX-1][posY].getEtat() == 0 && (!jeu[posX][posY-1].isMer())) {
            jeu[posX-1][posY].increaseEtat();
        }
    }

    public void recupereArtefact() {
        if (jeu[this.aventurier.getPositionX()][this.aventurier.getPositionY()].isArtefact() && jeu[this.aventurier.getPositionX()][this.aventurier.getPositionY()].isValide()) {
            jeu[this.aventurier.getPositionX()][this.aventurier.getPositionY()].supprimeArtefact();
            this.aventurier.recupereArtefact();
        }
    }
}

/** Fin de la classe CModele. */

/**
 * Définition de la classe zones pour les zones du jeu
 */

class Tuile {
    /**
     * L'Etat est défini par un entier
     * 1 = normale
     * 0 = inondee
     * -1 = submergee
     */
    private CModele modele;
    private boolean heliport;
    private int etat;
    private final int x, y;
    private Artefact artefact;
    private boolean arte;
    // Si c'est dans la mer
    // On ne peut pas accéder à cette tuile;
    private boolean mer;
    private boolean aventurier;
    private Aventurier player;

    public Tuile(CModele modele, int x, int y, int etat, boolean heliport, Artefact artefact, boolean aventurier, boolean arte) {
        this.x = x; this.y = y;
        this.modele = modele;
        this.etat = etat;
        this.heliport = heliport;
        this.artefact = artefact;
        this.mer = false;
        this.aventurier = aventurier;
        this.player = player;
        this.arte = arte;
    }

    public int getX() { return this.x; }

    public int getY() { return this.y; }

    public int getEtat() { return this.etat; }

    public Artefact getArtefact() { return this.artefact; }

    public void decreaseEtat() { this.etat--; }

    public  void increaseEtat() { this.etat++; }

    public boolean isNormale() { return this.etat == 1; }

    public boolean isInondee() { return this.etat == 0; }

    public boolean isSubmergee() { return this.etat == -1; }

    public boolean isHeliport() { return this.heliport; }

    public void setHeliport() { this.heliport = true; }

    public boolean isMer() { return this.mer; }

    public void setMer() { this.mer = true; }

    public boolean isAventurier() { return this.aventurier; }

    public void setAventurier() { this.aventurier = true; }

    public void supprimeAventurier() { this.aventurier = false; }

    public void supprimeArtefact() { this.arte = true; }

    public boolean isArtefact() {
        return this.arte;
    }

    public void setArtefact() {
        this.arte = true;
    }

    // Permet de savoir si une tuile est valide
    public boolean isValide() {
        if ((this.etat == -1) || (this.mer == true) || (this.aventurier == true)) {
            return false;
        } else {
            return true;
        }
    }
}

class Aventurier {

    private int positionX, positionY;
    private Artefact cle;
    private boolean artefact;
    private int numberAction;

    public Aventurier(int positionX, int positionY, Artefact cle, int numberAction) {
        this.cle = cle;
        this.numberAction = numberAction;
        this.positionX = positionX;
        this.positionY = positionY;
        this.artefact = false;
    }

    public int getPositionX() { return this.positionX; }

    public int getPositionY() { return this.positionY; }

    public void ajouteCle(Artefact cle) {
        this.cle = cle;
    }

    public int getNumberAction() { return this.numberAction; }

    public void decreaseNumberAction() {
        if (this.numberAction > 0) {
            this.numberAction--;
        }
    }

    public void resetNumberAction() { this.numberAction = 3; }

    public void deplaceAventurier(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public boolean haveArtefact() {
        return this.artefact;
    }

    public void recupereArtefact() {
        this.artefact = true;
    }

}

class Carte {

}

