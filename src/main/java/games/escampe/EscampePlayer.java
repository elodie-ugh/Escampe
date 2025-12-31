package games.escampe;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.algorithms.AlphaBeta;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EscampePlayer implements IJoueur{

    private static final String PLATEAU_FILE = ".\\src\\main\\java\\games\\escampe\\plateau.txt";
    private static final String OPENINGS_FILE = ".\\src\\main\\java\\games\\escampe\\openings.txt";
    private EscampeBoard board;
    private int myColour;
    private EscampeRole myRole;
    private AIPlayer<EscampeMove, EscampeRole, EscampeBoard> aiPlayer;

    // Stockage des ouvertures pré-calculées
    private String bestBlackOpening = null; // Meilleure ouverture pour les Noirs (premier joueur)
    private Map<String, String> whiteOpenings = new HashMap<>(); // Réponses Blanches selon placement Noir

    // Constructeur
    public EscampePlayer() {
        board = new EscampeBoard();
        loadOpenings();
    }


    // Charge les ouvertures pré-calculées depuis le fichier openings.txt
    // Format: BLACK:placement (meilleure ouverture Noire)
    //         WHITE:placementNoir:réponse (réponses Blanches)
    private void loadOpenings() {
        try (BufferedReader br = new BufferedReader(new FileReader(OPENINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("%")) continue;

                String[] parts = line.split(":");
                if (parts.length < 2) continue;

                if (parts[0].equals("BLACK")) {
                    // Format: BLACK:placement (meilleure ouverture pour les Noirs)
                    bestBlackOpening = parts[1];
                } else if (parts[0].equals("WHITE") && parts.length >= 3) {
                    // Format: WHITE:placementNoir:réponseBlanche
                    whiteOpenings.put(parts[1], parts[2]);
                }
            }
            System.out.println("Ouvertures chargées: " + (bestBlackOpening != null ? "1 Noir" : "0 Noir")
                             + ", " + whiteOpenings.size() + " Blancs");
        } catch (IOException e) {
            System.err.println("Impossible de charger les ouvertures: " + e.getMessage());
        }
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

        // Vérifier si c'est un placement initial
        long myPieces = (myRole == EscampeRole.WHITE)
                        ? (board.getWhiteUnicorn() | board.getWhitePaladins())
                        : (board.getBlackUnicorn() | board.getBlackPaladins());

        if (myPieces == 0L) {
            // C'est un placement initial - utiliser les ouvertures pré-calculées
            String openingMove = useOpeningBook();
            if (openingMove != null) {
                System.out.println("Utilisation de l'ouverture pré-calculée: " + openingMove);
                EscampeMove move = new EscampeMove(openingMove);
                board.playVoid(move, myRole);
                board.saveToFile(PLATEAU_FILE);
                return openingMove;
            }
        }

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

    // Utilise le livre d'ouvertures pour choisir le meilleur placement initial
    private String useOpeningBook() {
        if (myRole == EscampeRole.BLACK) {
            // Pour les Noirs (premier joueur), utiliser la meilleure ouverture précalculée
            return bestBlackOpening;
        } else {
            // Pour les Blancs, chercher une réponse au placement Noir
            long blackPieces = board.getBlackUnicorn() | board.getBlackPaladins();

            if (blackPieces != 0L) {
                // Les Noirs ont déjà placé leurs pièces
                // Reconstruire le placement Noir pour chercher dans le dictionnaire
                String blackPlacement = reconstructPlacement(board, EscampeRole.BLACK);

                if (blackPlacement != null && whiteOpenings.containsKey(blackPlacement)) {
                    return whiteOpenings.get(blackPlacement);
                }
            }
        }

        return null;
    }

    // Reconstruit le placement d'un joueur depuis le plateau
    private String reconstructPlacement(EscampeBoard board, EscampeRole role) {
        long unicorn = (role == EscampeRole.WHITE) ? board.getWhiteUnicorn() : board.getBlackUnicorn();
        long paladins = (role == EscampeRole.WHITE) ? board.getWhitePaladins() : board.getBlackPaladins();

        StringBuilder placement = new StringBuilder();

        // Trouver la licorne (première pièce)
        for (int i = 0; i < 36; i++) {
            if ((unicorn & (1L << i)) != 0) {
                placement.append(EscampeBoard.indexToString(i));
                break;
            }
        }

        // Trouver les paladins
        for (int i = 0; i < 36; i++) {
            if ((paladins & (1L << i)) != 0) {
                placement.append("/").append(EscampeBoard.indexToString(i));
            }
        }

        return placement.length() > 0 ? placement.toString() : null;
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
