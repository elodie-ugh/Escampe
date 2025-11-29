package games.escampe;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import iialib.games.model.IBoard;
import iialib.games.model.Score;

public class EscampeBoard implements Partie1, IBoard<EscampeMove,EscampeRole,EscampeBoard> {

    // ------------ Constantes ------------

    // Masques pour les liserés (Bit 0 = case libre, Bit 1 = liseré)
    private static final long LISERE_1 = 0b100010_010100_001010_010001_101001_000100L; // 0b pour binaire et L pour long
    private static final long LISERE_2 = 0b011001_000001_100100_100100_000001_011001L;
    private static final long LISERE_3 = 0b000100_101010_010001_001010_010100_100010L;

    private static final String[] COORD_CACHE = new String[36]; // Cache des coordonnées des cases pour éviter de les recalculer
    private static final long[][][] PATH_CACHE = new long[36][36][]; // Cache des chemins entre chaque paire de cases (null si impossible)

    // ------------ Variables d'etat ------------

    private long whitePaladins, blackPaladins, whiteUnicorn, blackUnicorn; // Positions des pièces sur le plateau
    private EscampeRole currentTurn; // 0 = blanc, 1 = noir
    private int nextMoveConstraint; // 0 = aucun, 1 = liseré1, 2 = liseré2, 3 = liseré3

    // ------------ Initialisation statique ------------

    static {
        for (int i = 0; i < 36; i++) {
            char colChar = (char) ('A' + (i % 6));
            char ligneChar = (char) ('1' + (i / 6));
            COORD_CACHE[i] = new String(new char[]{colChar, ligneChar}); // Évite une concaténation plus coûteuse
        }
    }

    static{
        precomputePaths();
    }

    // ------------ Outils de conversion ------------

    /** Convertit une portion de chaîne (ex: "A1" dans "A1-B2") en index entier.
     * @param s la chaîne complète (ex: "A1-B2")
     * @param offset l'endroit où commence la case (0 pour le début, 3 pour la seconde partie)
     * @return l'index entier (0-35)
     */
    private static int stringToIndex(String s, int offset) {
        char colChar = s.charAt(offset);
        char rowChar = s.charAt(offset + 1);

        return (rowChar - '1') * 6 + (colChar - 'A');
    }

    /** Convertit un index entier en une chaîne de caractères représentant une case (ex : "A1")
     * @param index l'index entier
     * @return la chaîne de caractères correspondante
     */
    private static String indexToString(int index) {
        return COORD_CACHE[index];
    }

    // --------------------- Gestion des fichiers ---------------------

    /** Initialise un plateau à partir d’un fichier texte
     * @param fileName le nom du fichier à lire
     */
    @Override
    public void setFromFile(String fileName) {
        // Reset les positions
        whitePaladins = 0L; whiteUnicorn = 0L;
        blackPaladins = 0L; blackUnicorn = 0L;

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) { // Ouvrir le fichier avec un buffer
            String line;
            int lineCounter = 0; // Pour suivre la ligne du plateau

            while((line = br.readLine()) != null && lineCounter < 6) {
                line = line.trim();

                if(line.isEmpty() || line.charAt(0) == '%'){ // Ignore les lignes vides ou les commentaires (commencent par %)
                    continue;
                }
                if(!Character.isDigit(line.charAt(0))){ // Ignore les lignes qui ne commencent pas par un chiffre
                    continue;
                }

                int colCounter = 0; // Pour suivre la colonne du plateau
                for(int i = 0; i < line.length() && colCounter < 6; i++){
                    char c = line.charAt(i);

                    if(c == '-'){ // Si pas de pion on passe
                        colCounter++;
                        continue;
                    }

                    if(c == 'N' || c == 'B' || c == 'n' || c == 'b'){ // Un pion
                        long mask = 1L << lineCounter * 6 + colCounter; // Créer un masque avec l'index

                        switch(c){ // Selon le caractère, on place la pièce correspondante
                            case 'B' : whiteUnicorn |= mask; break;
                            case 'N' : blackUnicorn |= mask; break;
                            case 'b' : whitePaladins |= mask; break;
                            case 'n' : blackPaladins |= mask; break;
                        }
                        colCounter++;
                    }
                }
                if(colCounter == 6){
                    lineCounter++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** Sauvegarde la configuration de l’état courant (plateau et pièces restantes) dans un fichier
     * @param fileName le nom du fichier à sauvegarder
     * Le format doit être compatible avec celui utilisé pour la lecture.
     */
    @Override
    public void saveToFile(String fileName) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))){
            bw.write("%  ABCDEF"); // Premier commentaire pour afficher les numéros de colonnes
            bw.newLine();

            char[] line = new char[12]; // Liste de char pour réutiliser ceux qui ne changent pas

            line[0] = '0';
            line[2] = ' ';
            line[9] = ' ';
            line[10] = '0';

            for(int row = 0; row < 6; row++){ // Boucle sur les 6 lignes
                char rowChar = (char) ('1' + row); // Calcul du chiffre de la ligne
                line[1] = rowChar;
                line[11] = rowChar;

                int rowStartIndex = row * 6;

                for(int col = 0; col < 6; col++){ // Boucle sur les 6 colonnes
                    long mask = 1L << (rowStartIndex + col);

                    char c = '-';

                    // Test si un pion sur cette case
                    if((whitePaladins & mask) != 0) c = 'b';
                    else if ((blackPaladins & mask) != 0) c = 'n';
                    else if((whiteUnicorn & mask) != 0) c = 'B';
                    else if((blackUnicorn & mask) != 0) c = 'N';

                    line[3 + col] = c;
                }

                bw.write(line);
                bw.newLine();
            }
            bw.write("%  ABCDEF");
            bw.newLine();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // --------------------- Gestion des coups ---------------------

    /** Indique si le coup <move> est valide pour le joueur <player> sur le plateau courant
     * @param move le coup à jouer,
     * sous la forme "B1-D1" en général,
     * sous la forme "C6/A6/B5/D5/E6/F5" pour le coup qui place les pièces
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public boolean isValidMove(EscampeMove move, EscampeRole player) {
        if (move == null) return false;
        if (move.isPlacement()) return isValidPlacementMove(move, player);
        return isValidGameplayMove(move, player);
    }

    /** Vérifie si le coup de placement est valide
     * @param move le coup à jouer, sous la forme "C6/A6/B5/D5/E6/F5"
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     * @return vrai si le coup est valide
     */
    private boolean isValidPlacementMove(EscampeMove move, EscampeRole player) {
        int[] indices = move.getPlacementIndices();
        if (indices == null || indices.length != 6) return false; 

        boolean isWhite = player == EscampeRole.WHITE;
        long mine = isWhite ? (whiteUnicorn | whitePaladins) : (blackUnicorn | blackPaladins); // Mes pièces
        long opponent = isWhite ? (blackUnicorn | blackPaladins) : (whiteUnicorn | whitePaladins); // Pièces adverses

        if(mine != 0L) return false; // Si j'ai déjà des pièces sur le plateau, je ne peux pas placer d'autres pions.

        int minRow, maxRow;

        if(opponent == 0L){ // Si le plateau est vide, on peut choisir haut ou bas (lignes 1-2 ou 5-6)
            int firstRow = indices[0] / 6;
            if(firstRow == 0 || firstRow == 1){ // Choisi le haut
                minRow = 0;
                maxRow = 1;
            } else if(firstRow == 4 ||firstRow == 5) { // Choisi le bas
                minRow = 4;
                maxRow = 5;
            }
            else {
                return false; // Interdit
            }
        } else { // L'adversaire a déjà placé ses pions
            boolean opponentisTop = (opponent & 0xFFFL) != 0; // Vérifie si l'adversaire est en bas (lignes 5-6) (0xFFF est le masque des 12 premières cases)
            if(opponentisTop){ // Adversaire en haut, moi en bas
                minRow = 4; maxRow = 5;
            } else { // Adversaire en bas, moi en haut
                minRow = 0; maxRow = 1;
            }
        }

        long moveMask = 0L; // Masque des positions où je veux placer mes pièces

        for (int index : indices){ // 6 boucles avec pas de 3 caractères
            if(index < 0 || index > 35) return false; // Index invalide

            int row = index / 6;
            if(row < minRow || row > maxRow) return false; // En dehors des lignes autorisées

            long mask = 1L << index;
            if((moveMask & mask) != 0) return false; // Déjà placé ici

            moveMask |= mask;
        }
        return true;
    }

    /** Vérifie si le coup de jeu est valide
     * @param from l'index de la case de départ
     * @param to l'index de la case d'arrivée
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     * @return vrai si le coup est valide
     */
    private boolean isValidGameplayMove(EscampeMove move, EscampeRole player) {
        int from = move.getFromIndex();
        int to = move.getToIndex();
        if(from == to || from < 0 || from > 35 || to < 0 || to > 35) return false; // Mêmes cases ou index invalides

        // Récupération des bitboards
        boolean isWhite = player == EscampeRole.WHITE;
        long myPaladins = isWhite ? whitePaladins : blackPaladins;
        long myUnicorn = isWhite ? whiteUnicorn : blackUnicorn;
        long opponentPaladins = isWhite ? blackPaladins : whitePaladins;
        long opponentUnicorn = isWhite ? blackUnicorn : whiteUnicorn;

        // Vérifier que la case de départ contient une de mes pièces
        long fromMask = 1L << from;
        boolean isPaladin = (myPaladins & fromMask) != 0;
        boolean isUnicorn = (myUnicorn & fromMask) != 0;
        if(!isPaladin && !isUnicorn) return false; // Pas ma pièce

        // Vérifier la contrainte
        int lisereType = getLisereType(from);
        if(nextMoveConstraint != 0 && lisereType != nextMoveConstraint) return false; // Contrainte non respectée

        // Vérifier géométriquement s'il existe un chemin valide
        long[] potentialPaths = PATH_CACHE[from][to];
        if(potentialPaths == null) return false; // Pas de chemin possible géométriquement

        // Vérifier si un des chemins est libre (sans obstacle)
        long allPieces = whitePaladins | blackPaladins | whiteUnicorn | blackUnicorn;
        boolean pathFound = false;

        for(long pathMask : potentialPaths){
            if((allPieces & pathMask) == 0){ // Chemin libre
                pathFound = true;
                break; // On peut s'arrêter dès qu'on en trouve un
            }
        }
        if(!pathFound) return false; // Aucun chemin libre

        // Vérifier que la case d'arrivée est libre
        long toMask = 1L << to;
        if(((myPaladins | myUnicorn) & toMask) != 0) return false; // Ma pièce

        if(((opponentPaladins | opponentUnicorn) & toMask) != 0) { // Pièce adverse
            boolean targetIsUnicorn = (opponentUnicorn & toMask) != 0;
            if (isPaladin && targetIsUnicorn) {
                return true; // Le paladin peut capturer la licorne
            } else {
                return false; // Sinon interdit
            }
        }

        return true;
    }

    /** Calcule les coups possibles pour le joueur <player> sur le plateau courant
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public ArrayList<EscampeMove> possibleMoves(EscampeRole player) {
        ArrayList<EscampeMove> moves = new ArrayList<>();

        long myPieces = (player == EscampeRole.WHITE) ? (whitePaladins | whiteUnicorn) 
                                                    : (blackPaladins | blackUnicorn);

        // Boucle sur toutes les cases
        for (int from = 0; from < 36; from++) {
            if ((myPieces & (1L << from)) == 0) continue; // pas ma pièce

            for (int to = 0; to < 36; to++) {
                if (from == to) continue; // éviter de créer un coup inutile

                // Crée le coup sous forme "A1-B2" avec string
                String moveStr = indexToString(from) + "-" + indexToString(to);
                EscampeMove move = new EscampeMove(moveStr);

                if (isValidMove(move, player)) moves.add(move);
            }
        }

        // TODO : gérer le cas de placement initial pour licorne + paladins
        return moves;
    }

    /** Modifie le plateau en jouant le coup move avec la pièce choisie
     * @param move le coup à jouer, sous la forme "C1-D1" ou "C6/A6/B5/D5/E6/F5"
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public EscampeBoard play(EscampeMove move, EscampeRole player) { //string avant
        // Créer une copie du plateau courant
        EscampeBoard copy = new EscampeBoard();
        copy.whitePaladins = this.whitePaladins;
        copy.whiteUnicorn = this.whiteUnicorn;
        copy.blackPaladins = this.blackPaladins;
        copy.blackUnicorn = this.blackUnicorn;
        copy.currentTurn = this.currentTurn;
        copy.nextMoveConstraint = this.nextMoveConstraint;

        if (move.isPlacement()) {
            // Placement initial : indices de la licorne et paladins
            int[] indices = move.getPlacementIndices();
            if (player == EscampeRole.WHITE) {
                copy.whiteUnicorn |= 1L << indices[0]; // Licorne
                for (int i = 1; i < 6; i++) copy.whitePaladins |= 1L << indices[i];
            } else {
                copy.blackUnicorn |= 1L << indices[0]; // Licorne
                for (int i = 1; i < 6; i++) copy.blackPaladins |= 1L << indices[i];
            }
        } else {
            // Déplacement classique : A1-B2
            int from = move.getFromIndex();
            int to   = move.getToIndex();
            long fromMask = 1L << from;
            long toMask   = 1L << to;

            if (player == EscampeRole.WHITE) {
                if ((copy.whiteUnicorn & fromMask) != 0) {
                    copy.whiteUnicorn &= ~fromMask;
                    copy.whiteUnicorn |= toMask;
                } else {
                    copy.whitePaladins &= ~fromMask;
                    copy.whitePaladins |= toMask;
                }
            } else {
                if ((copy.blackUnicorn & fromMask) != 0) {
                    copy.blackUnicorn &= ~fromMask;
                    copy.blackUnicorn |= toMask;
                } else {
                    copy.blackPaladins &= ~fromMask;
                    copy.blackPaladins |= toMask;
                }
            }
        }

        // Changer le joueur courant
        copy.currentTurn = (player == EscampeRole.WHITE) ? EscampeRole.BLACK : EscampeRole.WHITE;
        return copy;
    }

    /** Vrai lorsque le plateau correspond à une fin de partie.
     */
    @Override
    public boolean isGameOver() {
        // TODO
        return false;
    }

    @Override
    public ArrayList<Score<EscampeRole>> getScores() {
        // TODO
        ArrayList<Score<EscampeRole>> scores = new ArrayList<Score<EscampeRole>>();
        return scores;
    }

    // --------------------- Outils internes ---------------------

    /** Récupère le type de liseré pour une case donnée
     * @param index l'index de la case (0-35)
     * @return le type de liseré (0 = aucun, 1 = liseré 1, 2 = liseré 2, 3 = liseré 3)
     */
    public static int getLisereType(int index) {
        long mask = 1L << index;
        if((LISERE_1 & mask) != 0) return 1;
        if((LISERE_2 & mask) != 0) return 2;
        if((LISERE_3 & mask) != 0) return 3;
        return 0; // Si erreur
    }

    /** Pré-calcul des chemins entre chaque paire de cases pour chaque type de liseré
     * Stocke les résultats dans PATH_CACHE
     */
    private static void precomputePaths() {
        for (int from = 0; from < 36; from++) { // Pour chaque case de départ
            int dist = getLisereType(from); // Récupérer la distance (type de liseré)

            for (int to = 0; to < 36; to++) { // Pour chaque case d'arrivée
                if (from == to) continue; // Ignorer les mêmes cases

                // Trouver tous les chemins valides géométriquement (sans penser aux obstacles)
                List<Long> pathsFound = new ArrayList<>();

                findPathsRecursive(from, to, dist, 0L, pathsFound); // Masque initial = 0 (aucune case intermédiaire)

                if (!pathsFound.isEmpty()) {
                    long[] masks = new long[pathsFound.size()];
                    for (int i = 0; i < pathsFound.size(); i++) { // Copier dans un tableau
                        masks[i] = pathsFound.get(i);
                    }
                    PATH_CACHE[from][to] = masks; // Stocker dans le cache
                }
            }
        }
    }

    /** Recherche récursive des chemins valides entre deux cases
     * @param currentCase la case courante
     * @param target la case cible
     * @param stepsLeft le nombre de pas restants à effectuer
     * @param currentPathMask le masque des cases déjà visitées dans le chemin actuel
     * @param results la liste des masques de chemins valides trouvés
     */
    private static void findPathsRecursive(int currentCase, int target, int stepsLeft, long currentPathMask, List<Long> results) {
        if (stepsLeft == 0) {
            if (currentCase == target) { // Si on est arrivé à la cible
                results.add(currentPathMask);
            }
            return;
        }

        // Calcul de la distance de Manhattan entre la case courante et la cible
        int cx = currentCase % 6, cy = currentCase / 6;
        int tx = target % 6, ty = target / 6;
        int manhattan = Math.abs(cx - tx) + Math.abs(cy - ty);

        if (manhattan > stepsLeft) return; // Trop loin
        if ((manhattan % 2) != (stepsLeft % 2)) return; // Parité incompatible

        // Exploration des 4 voisins : Haut, Bas, Gauche (-1), Droite (+1)
        int[] moves = {-6, 6, -1, 1};

        for (int move : moves) {
            int next = currentCase + move;

            if (next < 0 || next > 35) continue; // Vérification des limites du plateau
            if (Math.abs(move) == 1 && (currentCase / 6 != next / 6)) continue; // Vérification des sauts de ligne horizontaux quand on bouge à gauche/droite

            // Vérification pour ne pas repasser par une case déjà dans le masque
            long nextBit = 1L << next;
            boolean isLastStep = (stepsLeft == 1); // Vérifier si c'est le dernier pas

            if ((currentPathMask & nextBit) == 0) { // Si la case n'est pas déjà dans le chemin
                if (isLastStep) { // Dernier pas
                    if (next == target) { // On a atteint la cible
                        results.add(currentPathMask);
                    }
                } else {
                    if (next != target) { // Ne pas aller à la cible avant le dernier pas
                        findPathsRecursive(next, target, stepsLeft - 1, currentPathMask | nextBit, results); // Explorer la suite du chemin
                    }
                }
            }
        }
    }

    // ----------------------------Getters-----------------------------
    public long getWhitePaladins() { return whitePaladins; }
    public long getBlackPaladins() { return blackPaladins; }
    public long getWhiteUnicorn() { return whiteUnicorn; }
    public long getBlackUnicorn() { return blackUnicorn; }

}
