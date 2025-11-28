package games.escampe;

import java.util.ArrayList;

public class EscampeGame {

    public static void main(String[] args) {

        System.out.println("=== Initialisation du plateau depuis fichier ===");
        EscampeBoard board = new EscampeBoard();
        board.setFromFile(".\\src\\main\\java\\games\\escampe\\plateau.txt"); // fichier texte de départ

        System.out.println("Plateau initial :");
        printBoard(board);

        EscampeRole player = EscampeRole.WHITE;

        System.out.println("\n=== Coups possibles pour " + player + " ===");
        ArrayList<EscampeMove> moves = board.possibleMoves(player);
        for (int i = 0; i < Math.min(5, moves.size()); i++) { // on n'affiche que 5 coups pour l'exemple
            System.out.println(moves.get(i));
        }

        if (!moves.isEmpty()) {
            EscampeMove move = moves.get(0); // on joue le premier coup possible
            System.out.println("\nJoue le coup : " + move);
            board = board.play(move, player);
        }

        System.out.println("\nPlateau après le coup :");
        printBoard(board);

        System.out.println("\nSauvegarde du plateau modifié dans 'plateau_modifie.txt'");
        board.saveToFile("plateau_modifie.txt");
    }

    /** Affiche le plateau de manière simple pour vérifier visuellement */
    private static void printBoard(EscampeBoard board) {
        for (int row = 5; row >= 0; row--) {
            for (int col = 0; col < 6; col++) {
                int index = row * 6 + col;
                char c = '-';
                if ((board.getWhiteUnicorn() & (1L << index)) != 0) c = 'B';
                else if ((board.getWhitePaladins() & (1L << index)) != 0) c = 'b';
                else if ((board.getBlackUnicorn() & (1L << index)) != 0) c = 'N';
                else if ((board.getBlackPaladins() & (1L << index)) != 0) c = 'n';
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }
}
