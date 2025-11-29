package games.escampe;

import java.util.HashSet;
import iialib.games.algs.IHeuristic;

public class EscampeHeuristics {

    private static final int BLOCK_WEIGHT = 100; // poids énorme si adversaire bloqué

    public static IHeuristic<EscampeBoard,EscampeRole>  hWhite = (board,role) -> {

		//Condition 1 : Mobilité 
		int nbPossibleMovesWhite = board.possibleMoves(EscampeRole.WHITE).size();
		int nbPossibleMovesBlack = board.possibleMoves(EscampeRole.BLACK).size();
		int possibleMovesScore = nbPossibleMovesWhite - nbPossibleMovesBlack; 
		if(nbPossibleMovesBlack==0) possibleMovesScore =+ BLOCK_WEIGHT; // accorder un poids énorme aux situations où la mobilité de l'adversaire tombe à 0

		//Condition 2 : Sécurité de la Licorne 
		int dangerScore = 0;
		// long blackPaladins = board.getBlackPaladins();
        // long whiteUnicorn = board.getWhiteUnicorn();
        // for(int i=0; i<36; i++) {
        //     if((whiteUnicorn & (1L<<i))!=0) { // Licorne blanche
        //         for(int j=0; j<36; j++) {
        //             if((blackPaladins & (1L<<j))!=0) {
        //                 // Manhattan distance pour menace
        //                 int dx = Math.abs((i%6) - (j%6));
        //                 int dy = Math.abs((i/6) - (j/6));
        //                 int dist = dx + dy;
        //                 if(dist==1) dangerScore += 1; // menace immédiate
        //                 else dangerScore += 1.0/dist; // plus loin = moins dangereux
        //             }
        //         }
        //     }
        // }
		
		//Condition 3 : Diversité des contraintes 
		int diverseLisereScore = 0;
		// long myPieces = board.getWhitePaladins() | board.getWhiteUnicorn();
        // HashSet<Integer> lisereSet = new HashSet<>();
        // for(int i=0; i<36; i++) {
        //     if((myPieces & (1L<<i))!=0) {
        //         lisereSet.add(board.getLisereType(i));
        //     }
        // }
        // diverseLisereScore = lisereSet.size();

		return possibleMovesScore + dangerScore + diverseLisereScore ; // à voir pour ajuster les poids de chaque condition 

	};


	public static IHeuristic<EscampeBoard,EscampeRole> hBlack = (board,role) -> {
        
		//Condition 1 : Mobilité 
		int nbPossibleMovesWhite = board.possibleMoves(EscampeRole.WHITE).size();
		int nbPossibleMovesBlack = board.possibleMoves(EscampeRole.BLACK).size();
		int possibleMovesScore = nbPossibleMovesBlack - nbPossibleMovesWhite; 
		if(nbPossibleMovesWhite==0) possibleMovesScore =+ 100; // accorder un poids énorme aux situations où la mobilité de l'adversaire tombe à 0

		//Condition 2 : Sécurité de la Licorne 
		int dangerScore = 0;
		// long whitePaladins = board.getWhitePaladins();
        // long blackUnicorn = board.getBlackUnicorn();
        // for(int i=0; i<36; i++) {
        //     if((blackUnicorn & (1L<<i))!=0) { // Licorne noire
        //         for(int j=0; j<36; j++) {
        //             if((whitePaladins & (1L<<j))!=0) {
        //                 // Manhattan distance pour menace
        //                 int dx = Math.abs((i%6) - (j%6));
        //                 int dy = Math.abs((i/6) - (j/6));
        //                 int dist = dx + dy;
        //                 if(dist==1) dangerScore += 1; // menace immédiate
        //                 else dangerScore += 1.0/dist; // plus loin = moins dangereux
        //             }
        //         }
        //     }
        // }
		
		//Condition 3 : Diversité des contraintes 
		int diverseLisereScore = 0;
		// long myPieces = board.getBlackPaladins() | board.getBlackUnicorn();
        // HashSet<Integer> lisereSet = new HashSet<>();
        // for(int i=0; i<36; i++) {
        //     if((myPieces & (1L<<i))!=0) {
        //         lisereSet.add(board.getLisereType(i));
        //     }
        // }
        // diverseLisereScore = lisereSet.size();

		return possibleMovesScore + dangerScore + diverseLisereScore; // à voir pour ajuster les poids de chaque condition 
	};

}