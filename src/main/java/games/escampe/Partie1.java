package games.escampe;

public interface Partie1 {

    /** initialise un plateau `a partir d’un fichier texte
     * @param fileName le nom du fichier `a lire
     */
    public void setFromFile(String fileName);


    /** sauve la configuration de l’´etat courant (plateau et pi`eces restantes) dans un fichier
     * @param fileName le nom du fichier `a sauvegarder
     * Le format doit ^etre compatible avec celui utilis´e pour la lecture.
     */
    public void saveToFile(String fileName);

    // /** indique si le coup <move> est valide pour le joueur <player> sur le plateau courant
    //  * @param move le coup `a jouer,
    //  * sous la forme "B1-D1" en g´en´eral,
    //  * sous la forme "C6/A6/B5/D5/E6/F5" pour le coup qui place les pi`eces
    //  * @param player le joueur qui joue, repr´esent´e par "noir" ou "blanc"
    //  */
    // public boolean isValidMove(String move, String player);


    // /** calcule les coups possibles pour le joueur <player> sur le plateau courant
    //  * @param player le joueur qui joue, repr´esent´e par "noir" ou "blanc"
    //  */
    // public String[] possiblesMoves(String player);


    // /** modifie le plateau en jouant le coup move avec la pi`ece choose
    //  * @param move le coup `a jouer, sous la forme "C1-D1" ou "C6/A6/B5/D5/E6/F5"
    //  * @param player le joueur qui joue, repr´esent´e par "noir" ou "blanc"
    //  */
    // public void play(String move, String player);


    // /** vrai lorsque le plateau corespond `a une fin de partie
    //  */
    // public boolean gameOver();
}
