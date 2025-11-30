package games.escampe;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class EscampeGame {

    private EscampeBoard board;
    private boolean humanIsWhite; // vrai si le joueur humain est blanc
    private Scanner scanner;
    private static final String PLATEAU_FILE = ".\\src\\main\\java\\games\\escampe\\plateau.txt";

    public EscampeGame() {
        board = new EscampeBoard();
        scanner = new Scanner(System.in);

        // Forcer un plateau vide au début
        board.clearBoard();
        System.out.println("Plateau initialisé vide :");
        printBoard();
        // Tirage aléatoire pour déterminer la couleur du joueur humain
        Random rand = new Random();
        humanIsWhite = rand.nextBoolean();
        System.out.println("Le joueur humain est " + (humanIsWhite ? "Blanc" : "Noir"));
    }

    public void start() {
        System.out.println("Placement initial des pièces :");

        // ------------------- Placement du noir -------------------
        if (!humanIsWhite) {
            humanPlacePieces(EscampeRole.BLACK);
        } else {
            aiPlacePieces(EscampeRole.BLACK);
        }

        // ------------------- Placement du blanc -------------------
        if (humanIsWhite) {
            humanPlacePieces(EscampeRole.WHITE);
        } else {
            aiPlacePieces(EscampeRole.WHITE);
        }

        // Après le placement, Blanc joue en premier
        board.switchTurn(); // S'assurer que Blanc commence
        playGameLoop();
    }

    private void humanPlacePieces(EscampeRole role) {
        System.out.println("Placement des pièces pour le joueur " + role);
        EscampeMove move = null;
        while (true) {
            System.out.print("Entrez votre placement (ex: C6/A6/B5/D5/E6/F5) : ");
            String input = scanner.nextLine();
            move = new EscampeMove(input);
            if (board.isValidMove(move, role)) break;
            System.out.println("Placement invalide, réessayez.");
        }
        board.playVoid(move, role);
        printBoard();
        board.saveToFile(PLATEAU_FILE);
    }

    private void aiPlacePieces(EscampeRole role) {
        System.out.println("Placement IA pour " + role);
        ArrayList<EscampeMove> moves = board.possibleMoves(role);
        if (!moves.isEmpty()) {
            EscampeMove move = moves.get(new Random().nextInt(moves.size()));
            board.playVoid(move, role);
            System.out.println("IA a placé : " + move);
            printBoard();
            board.saveToFile(PLATEAU_FILE); // Sauvegarde après placement
        }
    }

    private void playGameLoop() {
        while (!board.isGameOver()) {
            EscampeRole current = board.getCurrentTurn();
            System.out.println("Au tour de " + current);
            EscampeMove move;

            boolean isHuman = (current == EscampeRole.WHITE && humanIsWhite) ||
                              (current == EscampeRole.BLACK && !humanIsWhite);

            if (isHuman) {
                move = humanMove(current);
            } else {
                move = aiMove(current);
            }

            board.playVoid(move, current);
            printBoard();
            board.saveToFile(PLATEAU_FILE);
        }

        // Affichage du résultat
        for (var score : board.getScores()) {
            System.out.println(score.getRole() + " : " + score.getStatus());
        }
    }

    private EscampeMove humanMove(EscampeRole role) {
        EscampeMove move = null;
        while (true) {
            System.out.print("Entrez votre coup (ex: A1-B2) : ");
            String input = scanner.nextLine().trim();

            // Vérifie que l'entrée n'est pas vide
            if (input.isEmpty()) {
                System.out.println("Entrée vide, réessayez.");
                continue;
            }

            try {
                move = new EscampeMove(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Syntaxe invalide : " + e.getMessage());
                continue; // Retourne au début de la boucle
            }

            if (!board.isValidMove(move, role)) {
                System.out.println("Coup invalide sur le plateau, réessayez.");
                continue;
            }

            break; // Coup correct
        }
        return move;
    }

    private EscampeMove aiMove(EscampeRole role) {
        ArrayList<EscampeMove> moves = board.possibleMoves(role);
        if (!moves.isEmpty()) {
            EscampeMove move = moves.get(new Random().nextInt(moves.size()));
            System.out.println("IA joue : " + move);
            return move;
        }
        System.out.println("IA doit passer son tour !");
        return new EscampeMove("E"); // Pass si aucun coup
    }

    private void printBoard() {
        char[] cols = {'A','B','C','D','E','F'};
        System.out.print("   ");
        for (char c : cols) System.out.print(c + " ");
        System.out.println();
        for (int row = 0; row < 6; row++) {
            System.out.print((row + 1) + "  ");
            for (int col = 0; col < 6; col++) {
                int index = row * 6 + col;
                char c = '-';
                if ((board.getWhiteUnicorn() & (1L << index)) != 0) c = 'B';
                else if ((board.getWhitePaladins() & (1L << index)) != 0) c = 'b';
                else if ((board.getBlackUnicorn() & (1L << index)) != 0) c = 'N';
                else if ((board.getBlackPaladins() & (1L << index)) != 0) c = 'n';
                System.out.print(c + " ");
            }
            System.out.println(" " + (row + 1));
        }
        System.out.print("   ");
        for (char c : cols) System.out.print(c + " ");
        System.out.println("\n");
    }

    private void printLisereTypes() {
        System.out.println("Types de liserés par case :");
        for (int i = 0; i < 36; i++) {
            int lisere = EscampeBoard.getLisereType(i); // méthode statique
            System.out.print(lisere + " ");
            if (i % 6 == 5) System.out.println(); // Nouvelle ligne toutes les 6 colonnes
        }
    }


    public static void main(String[] args) {
        EscampeGame game = new EscampeGame();
        game.start();
        //game.printLisereTypes();
    }
}
