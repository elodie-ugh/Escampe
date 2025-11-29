package games.escampe;

import iialib.games.model.IMove;

/**
 * Représente un coup dans Escampe.
 * Peut être :
 * - un déplacement (A1-B2)
 * - un placement de 6 pièces (C6/A6/B5/D5/E6/F5)
 */
public class EscampeMove implements IMove {

    private final String rawMove;
    private final boolean placement;

    public EscampeMove(String move) {
        this.rawMove = move;
        this.placement = move.length() > 5; // placement si > 5 (ex: 17 caractères)
    }

    public boolean isPlacement() {
        return placement;
    }

    public int getFromIndex() {
        if (placement) return -1;
        return stringToIndex(rawMove, 0);
    }

    public int getToIndex() {
        if (placement) return -1;
        return stringToIndex(rawMove, 3);
    }

    public int[] getPlacementIndices() { // Doit être de la forme "C6/A6/B5/D5/E6/F5"
        if (!placement) return null;
        int[] indices = new int[6];
        for (int i = 0; i < 17; i += 3) {
            indices[i / 3] = stringToIndex(rawMove, i);
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

    private int stringToIndex(String s, int offset) {
        return (s.charAt(offset + 1) - '1') * 6 + (s.charAt(offset) - 'A');
    }
}


