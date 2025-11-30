package games.escampe;

import iialib.games.model.IMove;

/**
 * Représente un coup dans Escampe.
 * Peut être :
 * - un déplacement (A1-B2)
 * - un placement de 6 pièces (C6/A6/B5/D5/E6/F5)
 * - un passage ("E")
 */
public class EscampeMove implements IMove {

    private final String rawMove;
    private final boolean placement;
    private final boolean pass;

    public EscampeMove(String move) {
        if (move == null || move.trim().isEmpty()) {
            throw new IllegalArgumentException("Coup vide");
        }

        this.rawMove = move.trim();
        this.pass = rawMove.equalsIgnoreCase("E");

        // Détecte si c'est un placement (6 positions séparées par '/')
        String[] parts = rawMove.split("/");
        this.placement = !pass && parts.length == 6;

        // Vérifie la syntaxe du coup
        if (!pass && !placement) {
            // Coup normal de type "A1-B2"
            String cleanMove = rawMove.replaceAll("\\s", ""); // Supprime les espaces éventuels
            if (cleanMove.length() != 5 || cleanMove.charAt(2) != '-') {
                throw new IllegalArgumentException("Coup de déplacement invalide : " + rawMove);
            }
            if (!isValidCell(cleanMove.substring(0, 2)) || !isValidCell(cleanMove.substring(3, 5))) {
                throw new IllegalArgumentException("Coup de déplacement contient des cases invalides : " + rawMove);
            }
        } else if (placement) {
            // Placement de 6 pièces "C6/A6/B5/D5/E6/F5"
            if (parts.length != 6) {
                throw new IllegalArgumentException("Placement doit contenir 6 positions : " + rawMove);
            }
            for (String cell : parts) {
                if (!isValidCell(cell.trim())) {
                    throw new IllegalArgumentException("Placement contient une case invalide : " + cell);
                }
            }
        }
    }

    public boolean isPlacement() {
        return placement;
    }

    public boolean isPass() {
        return pass;
    }

    public int getFromIndex() {
        if (placement || pass) return -1;
        String cleanMove = rawMove.replaceAll("\\s", "");
        return stringToIndex(cleanMove.substring(0, 2));
    }

    public int getToIndex() {
        if (placement || pass) return -1;
        String cleanMove = rawMove.replaceAll("\\s", "");
        return stringToIndex(cleanMove.substring(3, 5));
    }

    public int[] getPlacementIndices() {
        if (!placement) return null;
        String[] cells = rawMove.split("/");
        int[] indices = new int[6];
        for (int i = 0; i < 6; i++) {
            indices[i] = stringToIndex(cells[i].trim());
        }
        return indices;
    }

    public String getRawMove() {
        return rawMove;
    }

    @Override
    public String toString() {
        return rawMove;
    }

    // --------------------- Méthodes internes ---------------------

    private boolean isValidCell(String cell) {
        if (cell.length() != 2) return false;
        char col = cell.charAt(0);
        char row = cell.charAt(1);
        return (col >= 'A' && col <= 'F') && (row >= '1' && row <= '6');
    }

    private int stringToIndex(String cell) {
        if (cell.length() != 2) {
            throw new IllegalArgumentException("Case invalide pour conversion en index : " + cell);
        }
        char col = cell.charAt(0);
        char row = cell.charAt(1);
        return (row - '1') * 6 + (col - 'A');
    }
}
