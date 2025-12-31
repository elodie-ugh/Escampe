package games.escampe;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.algorithms.AlphaBeta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Générateur d'ouvertures pour le jeu Escampe.
 * Ce programme calcule les meilleures ouvertures pour les Noirs (premier joueur)
 * en utilisant AlphaBeta, puis calcule les meilleures réponses Blanches.
 */
public class OpeningGenerator {

    private static final String OPENINGS_FILE = ".\\src\\main\\java\\games\\escampe\\openings.txt";
    private static final int OPENING_DEPTH = 6; // Profondeur de recherche AlphaBeta
    private static final int TOP_N_OPENINGS = 20; // Nombre d'ouvertures Noires à calculer

    public static void main(String[] args) {
        System.out.println("=== Générateur d'Ouvertures Escampe ===\n");

        OpeningGenerator generator = new OpeningGenerator();
        generator.generateOpenings();

        System.out.println("\n=== Génération terminée ===");
        System.out.println("Les ouvertures ont été sauvegardées dans : " + OPENINGS_FILE);
    }

    public void generateOpenings() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OPENINGS_FILE))) {
            writer.write("% Fichier d'ouvertures pour le jeu Escampe\n");
            writer.write("% Généré automatiquement par OpeningGenerator avec AlphaBeta\n");
            writer.write("% Format: BLACK:placement ou WHITE:placementNoir:réponse\n");
            writer.write("% NOTE: Les NOIRS jouent en PREMIER\n");
            writer.write("% Profondeur AlphaBeta: " + OPENING_DEPTH + "\n");
            writer.write("%\n\n");

            // 1. Calculer les TOP_N meilleures ouvertures pour les Noirs (avec AlphaBeta)
            System.out.println("Calcul des " + TOP_N_OPENINGS + " meilleures ouvertures NOIRES (profondeur " + OPENING_DEPTH + ")...");
            ArrayList<String> topBlackOpenings = findTopBlackOpenings();

            writer.write("% Meilleures ouvertures pour les Noirs (premier joueur)\n");
            for (String opening : topBlackOpenings) {
                writer.write("BLACK:" + opening + "\n");
            }
            System.out.println("✓ " + topBlackOpenings.size() + " meilleures ouvertures Noires calculées");

            // 2. Calculer les meilleures réponses Blanches pour chaque ouverture Noire (avec AlphaBeta)
            System.out.println("\nCalcul des meilleures réponses BLANCHES (profondeur " + OPENING_DEPTH + ")...");
            writer.write("\n% Meilleures réponses Blanches (en réponse aux ouvertures noires)\n");

            for (String blackOpening : topBlackOpenings) {
                String whiteResponse = findBestWhiteResponse(blackOpening);
                writer.write("WHITE:" + blackOpening + ":" + whiteResponse + "\n");
                System.out.println("  → " + blackOpening + " : " + whiteResponse);
            }
            System.out.println("✓ " + topBlackOpenings.size() + " réponses Blanches calculées");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Trouve les TOP_N meilleures ouvertures pour les Noirs en utilisant AlphaBeta.
     * Les Noirs jouent en premier sur un plateau vide (sans adversaire).
     * @return liste des x meilleures ouvertures Noires
     */
    private ArrayList<String> findTopBlackOpenings() {
        // Créer un plateau vide
        EscampeBoard emptyBoard = new EscampeBoard();
        emptyBoard.clearBoard();

        // Obtenir tous les placements possibles pour les Noirs
        ArrayList<EscampeMove> blackPlacements = emptyBoard.possibleMoves(EscampeRole.BLACK);

        System.out.println("  Évaluation de " + blackPlacements.size() + " placements Noirs avec AlphaBeta...");

        // Créer l'IA Noire avec AlphaBeta
        AlphaBeta<EscampeMove, EscampeRole, EscampeBoard> algorithm =
            new AlphaBeta<>(EscampeRole.BLACK, EscampeRole.WHITE, EscampeHeuristics.hBlack, OPENING_DEPTH);
        AIPlayer<EscampeMove, EscampeRole, EscampeBoard> blackAI = new AIPlayer<>(EscampeRole.BLACK, algorithm);

        // Évaluer chaque placement Noir avec AlphaBeta
        Map<String, Integer> placementScores = new HashMap<>();

        for (EscampeMove blackMove : blackPlacements) {
            EscampeBoard testBoard = new EscampeBoard();
            testBoard.clearBoard();
            testBoard.playVoid(blackMove, EscampeRole.BLACK);

            // Utiliser l'heuristique pour évaluer la position après placement
            // (on ne peut pas utiliser bestMove car les Blancs n'ont pas encore placé)
            int score = EscampeHeuristics.hBlack.eval(testBoard, EscampeRole.BLACK);
            placementScores.put(blackMove.toString(), score);

            System.out.println("    " + blackMove + " → score: " + score);
        }

        // Trier et retourner les TOP_N meilleures
        ArrayList<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(placementScores.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue())); // Tri décroissant

        ArrayList<String> topOpenings = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_N_OPENINGS, sortedEntries.size()); i++) {
            topOpenings.add(sortedEntries.get(i).getKey());
        }

        return topOpenings;
    }

    /**
     * Trouve la meilleure réponse Blanche pour une ouverture Noire donnée.
     * Utilise AlphaBeta pour évaluer toutes les réponses possibles.
     * @param blackOpening le placement Noir (format "C1/A1/B2/D2/E1/F2")
     * @return la meilleure réponse Blanche
     */
    private String findBestWhiteResponse(String blackOpening) {
        // Créer un plateau avec le placement Noir
        EscampeBoard board = new EscampeBoard();
        board.clearBoard();
        EscampeMove blackMove = new EscampeMove(blackOpening);
        board.playVoid(blackMove, EscampeRole.BLACK);

        // Créer l'IA Blanche avec AlphaBeta
        AlphaBeta<EscampeMove, EscampeRole, EscampeBoard> algorithm =
            new AlphaBeta<>(EscampeRole.WHITE, EscampeRole.BLACK, EscampeHeuristics.hWhite, OPENING_DEPTH);
        AIPlayer<EscampeMove, EscampeRole, EscampeBoard> whiteAI = new AIPlayer<>(EscampeRole.WHITE, algorithm);

        // Utiliser AlphaBeta pour trouver le meilleur placement Blanc
        EscampeMove bestMove = whiteAI.bestMove(board);

        return bestMove != null ? bestMove.toString() : "A6/B6/C6/D6/E6/F6";
    }
}

