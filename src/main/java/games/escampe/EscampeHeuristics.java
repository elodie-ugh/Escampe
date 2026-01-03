package games.escampe;

import iialib.games.algs.IHeuristic;

public class EscampeHeuristics {

    public static final int VICTORY = 1000000;
    public static final int DEFEAT  = -1000000;

    public static IHeuristic<EscampeBoard, EscampeRole> hWhite =
            (board, role) -> evaluate(board, EscampeRole.WHITE);

    public static IHeuristic<EscampeBoard, EscampeRole> hBlack =
            (board, role) -> evaluate(board, EscampeRole.BLACK);

    private static int evaluate(EscampeBoard board, EscampeRole role) {
        boolean isWhite = (role == EscampeRole.WHITE);
        EscampeRole oppRole = isWhite ? EscampeRole.BLACK : EscampeRole.WHITE;

        long myPaladins  = isWhite ? board.getWhitePaladins() : board.getBlackPaladins();
        long myUni       = isWhite ? board.getWhiteUnicorn() : board.getBlackUnicorn();
        long oppPaladins = isWhite ? board.getBlackPaladins() : board.getWhitePaladins();
        long oppUni      = isWhite ? board.getBlackUnicorn() : board.getWhiteUnicorn();
        long allPieces   = myPaladins | myUni | oppPaladins | oppUni;

        // --- 0) Sécurité immédiate ---
        if (myUni == 0)  return DEFEAT;
        if (oppUni == 0) return VICTORY;

        int score = 0;
        int myUniIdx = Long.numberOfTrailingZeros(myUni);
        int oppUniIdx = Long.numberOfTrailingZeros(oppUni);
        int myUniX = myUniIdx % 6;
        int myUniY = myUniIdx / 6;

        // --- 1) MOBILITÉ & RESTRICTION ---
        int myMoves  = board.possibleMoves(role).size();
        int oppMoves = board.possibleMoves(oppRole).size();
        score += 15 * myMoves - 20 * oppMoves;
        if (oppMoves == 0) score += 2000;

        // --- 2) DIVERSITÉ DES LISERÉS ---
        int lisereMask = 0;
        long temp = myPaladins;
        while (temp != 0) {
            int idx = Long.numberOfTrailingZeros(temp);
            int lisere = EscampeBoard.getLisereType(idx);
            if (lisere > 0) lisereMask |= (1 << lisere);
            temp &= (temp - 1);
        }
        score += 150 * Integer.bitCount(lisereMask);

        // --- 3) SÉCURITÉ DE MA LICORNE (Défense) ---
        // a) Danger direct : Paladins adverses pouvant m'atteindre
        score -= calculateThreatWithDistance(oppPaladins, myUniIdx, allPieces, true);

        // b) Prudence Géographique : Empêcher la licorne d'aller au centre
        if (isWhite) {
            if (myUniY > 1) score -= (myUniY * 400); // Malus si elle descend trop
        } else {
            if (myUniY < 4) score -= ((5 - myUniY) * 400); // Malus si elle monte trop
        }

        // c) Garde rapprochée : Bonus pour les paladins adjacents (bloqueurs)
        temp = myPaladins;
        while (temp != 0) {
            int pIdx = Long.numberOfTrailingZeros(temp);
            int dist = Math.abs(myUniX - (pIdx % 6)) + Math.abs(myUniY - (pIdx / 6));
            if (dist == 1) score += 250; // Bonus pour chaque garde du corps direct
            temp &= (temp - 1);
        }

        // --- 4) AGRESSIVITÉ (Attaque) ---
        // Menace sur la licorne adverse
        score += calculateThreatWithDistance(myPaladins, oppUniIdx, allPieces, false);

        return score;
    }

    private static int calculateThreatWithDistance(long attackers, int targetIdx, long allPieces, boolean isDefensive) {
        int threatScore = 0;
        long temp = attackers;

        while (temp != 0) {
            int attackerIdx = Long.numberOfTrailingZeros(temp);
            long[] paths = EscampeBoard.PATH_CACHE[attackerIdx][targetIdx];

            if (paths != null) {
                int minSteps = Integer.MAX_VALUE;
                for (long pathMask : paths) {
                    if ((pathMask & allPieces) == 0) {
                        int steps = Long.bitCount(pathMask); 
                        if (steps < minSteps) minSteps = steps;
                    }
                }
                if (minSteps != Integer.MAX_VALUE) {
                    // Score très élevé (1000) pour une capture en 1 coup (minSteps 0, 1 ou 2 selon liseré)
                    // On utilise une valeur plancher pour que la capture soit toujours prioritaire
                    int val = 1000 / (minSteps + 1);
                    threatScore += val;
                }
            }
            temp &= (temp - 1);
        }

        return isDefensive ? (threatScore * 2) : threatScore;
    }
}