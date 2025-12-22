package games.escampe;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.algorithms.AlphaBeta;

public class EscampePlayer implements IJoueur{

    private static final String PLATEAU_FILE = ".\\src\\main\\java\\games\\escampe\\plateau.txt";
    private EscampeBoard board;
    private int myColour;
    private EscampeRole myRole;
    private AIPlayer<EscampeMove, EscampeRole, EscampeBoard> aiPlayer;

    // Constructeur
    public EscampePlayer() {
        board = new EscampeBoard();
    }

    // Le joueur initialise son rôle et son algorithme IA en fonction de la couleur assignée.
    @Override
    public void initJoueur(int myColour) {
        this.myColour = myColour;

        // Déterminer le rôle
        if (myColour == BLANC) {
            myRole = EscampeRole.WHITE;
        } else {
            myRole = EscampeRole.BLACK;
        }

        // Donner le rôle adverse
        EscampeRole opponentRole = (myRole == EscampeRole.WHITE) ? EscampeRole.BLACK : EscampeRole.WHITE;

        // Choisir l'heuristique appropriée selon ma couleur
        GameAlgorithm<EscampeMove, EscampeRole, EscampeBoard> algorithm;
        if (myRole == EscampeRole.WHITE) {
            algorithm = new AlphaBeta<>(myRole, opponentRole, EscampeHeuristics.hWhite, 4);
        } else {
            algorithm = new AlphaBeta<>(myRole, opponentRole, EscampeHeuristics.hBlack, 4);
        }

        // Initialiser le joueur IA avec l'algorithme choisi
        aiPlayer = new AIPlayer<>(myRole, algorithm);

        // Charger l'état actuel du plateau depuis le fichier
        board.setFromFile(PLATEAU_FILE);
    }

    // Retourner la couleur du joueur
    @Override
    public int getNumJoueur() {
        return myColour;
    }

    // Choisir le meilleur mouvement à jouer
    @Override
    public String choixMouvement() {
        // Recharger le plateau depuis le fichier pour avoir l'état le plus récent
        board.setFromFile(PLATEAU_FILE);

        // Utiliser l'IA pour trouver le meilleur coup
        EscampeMove bestMove = aiPlayer.bestMove(board);

        if (bestMove == null) {
            return "PASSE";
        }

        // Jouer le coup sur notre copie du plateau (avec playVoid qui est optimisée)
        board.playVoid(bestMove, myRole);

        // Sauvegarder l'état mis à jour
        board.saveToFile(PLATEAU_FILE);

        // Retourner le coup au format string
        return bestMove.toString();
    }

    @Override
    public void declareLeVainqueur(int colour) {
        if (colour == myColour) {
            System.out.println("Victoire ! J'ai gagné !");
        } else if (colour == 0) { // VIDE = 0 (match nul)
            System.out.println("Match nul.");
        } else {
            System.out.println("Défaite... L'adversaire a gagné.");
        }
    }

    @Override
    public void mouvementEnnemi(String coup) {
        // Recharger le plateau depuis le fichier
        board.setFromFile(PLATEAU_FILE);

        // Créer le mouvement de l'ennemi
        EscampeMove ennemyMove = new EscampeMove(coup);

        // Déterminer le rôle adverse
        EscampeRole ennemyRole = (myRole == EscampeRole.WHITE) ? EscampeRole.BLACK : EscampeRole.WHITE;

        // Appliquer le mouvement ennemi sur notre plateau
        board.playVoid(ennemyMove, ennemyRole);

        // Sauvegarder l'état mis à jour
        board.saveToFile(PLATEAU_FILE);
    }

    @Override
    public String binoName() {
        return "Morgan_Elodie"; // TODO : Je sais pas si c'est ça qu'il faut mettre
    }
}
