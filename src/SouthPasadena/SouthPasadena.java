package connectx.SouthPasadena;

import java.util.HashMap;
import java.util.Random;

import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;

/**
 * Mike Garavani's Connect X software player
 */
public class SouthPasadena implements CXPlayer {

    private Random rand;

    // CONSTANTS

    // Constant used in the column heuristic score evaluation
    public static final int MULTIPLIER_1 = 1;

    // Constant used in the row heuristic score evaluation
    public static final int MULTIPLIER_2 = 1;

    // Constant used in the diagonal heuristic score evaluation
    public static final int MULTIPLIER_3 = 2;

    private int rowsNumber;
    private int columnsNumber;
    private int tokensToConnect;

    private CXGameState myWin;
    private CXGameState yourWin;

    private CXCellState myCell;
    private CXCellState yourCell;

    // Dealing with time
    private long startingTime;
    private long timeConstraintMillis;

    // Transposition tables
    private long[][][] zobristTable;
    private HashMap<Long, TranspositionEntry> transpositionTable;


    /* Default empty constructor */
    public SouthPasadena() {
	}

    /*
     * Initialize the Player
     */    
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs){

        rand = new Random();

        rowsNumber = M;
        columnsNumber = N;
        tokensToConnect = X;

        myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

        myCell = first ? CXCellState.P1 : CXCellState.P2;
        yourCell = first ? CXCellState.P2 : CXCellState.P1;

        timeConstraintMillis = timeout_in_secs * 1000;

        initZobrist();
        transpositionTable = new HashMap<>();

    }

    /*
     * Select the best column
     */
    public int selectColumn(CXBoard B){

        startingTime = System.currentTimeMillis();

        // If the board is empty, start in the center
        if (B.numOfMarkedCells() == 0){
            return columnsNumber/2;
        }

        // Initializing Minimax execution
        Integer[] columnsInOrder = orderColumns();
        // We set the first available column as the best one just for initialization purposes
        int bestColumn = B.getAvailableColumns()[0];
        int bestScore = Integer.MIN_VALUE;

        // DEPTH
        int depth = 1;

        /*
         * This segment of code iterates through all possible moves that SouthPasadena can make on the game board.
         * 
         * For each potential move:
         * 
         * 1. The move is temporarily played on the board.
         * 
         * 2. To assess the quality of the move, we invoke the 'alphaBetaMinimax' function.
         * 
         * Note that the 'isMaximizing' parameter set to false. This is because, at this stage,
         * we're evaluating the game from the perspective of SouthPasadena's opponent.
         * The move has already been made by SouthPasadena, so we're now interested in how the opponent
         * would respond to minimize SouthPasadena's advantage. Essentially, we're trying to predict
         * the opponent's best countermove to the one we're considering.
         * 
         * 3. After evaluating the move, it's "unplayed" by reverting the board to its previous state.
         * 
         * The algorithm then picks the move with the highest score.
         * 
         *  
         */
        while (!isTimeRunningOut()){
            for (int i=0; i<columnsNumber; i++){
                if (!B.fullColumn(columnsInOrder[i])){

                    int alpha = Integer.MIN_VALUE;
                    int beta = Integer.MAX_VALUE;

                    B.markColumn(columnsInOrder[i]);
                    int currentScore = alphaBetaMinimax(B, alpha, beta, depth, false);
                    B.unmarkColumn();

                    if (currentScore > bestScore){
                        bestScore = currentScore;
                        bestColumn = columnsInOrder[i];
                    }

                }
            }

            depth++;

        }

        return bestColumn;

    }

    /**
     * @param B
     * @param alpha
     * @param beta
     * @param depth
     * @param isMaximizing
     * @return The score in the current game State
     */
    private int alphaBetaMinimax(CXBoard B, int alpha, int beta, int depth, boolean isMaximizing){

        long hash = computeZobristHash(B);
        // Check if the board state is in the transposition table
        TranspositionEntry entry = transpositionTable.get(hash);
        if (entry != null && entry.depth >= depth) {
            // Use the stored evaluation
            return entry.evaluation;
        }

        if (depth == 0 || B.gameState() != CXGameState.OPEN || isTimeRunningOut()){
            int eval = heuristicScore(B);
            transpositionTable.put(hash, new TranspositionEntry(hash, eval, depth)); // Store in transposition table
            return eval;
        }

        if (isMaximizing){
            int value = Integer.MIN_VALUE;
            Integer[] columnsInOrder = orderColumns();
            for (int i=0; i<columnsNumber; i++){
                if (!B.fullColumn(columnsInOrder[i])){
                    B.markColumn(columnsInOrder[i]);
                    // Note that in the following call to alphaBetaMinimax the CXBoard B has been updated
                    value = Math.max(value, alphaBetaMinimax(B, alpha, beta, depth-1, false));
                    B.unmarkColumn();
                    if (value > beta){
                        // break β !!
                        break;
                    }
                    alpha = Math.max(alpha, value);
                }
            }
            return value;
        }

        else{
            int value = Integer.MAX_VALUE;
            Integer[] columnsInOrder = orderColumns();
            for (int i=0; i<columnsNumber; i++){
                if (!B.fullColumn(columnsInOrder[i])){
                    B.markColumn(columnsInOrder[i]);
                    // Note that in the following call to alphaBetaMinimax the CXBoard B has been updated
                    value = Math.min(value, alphaBetaMinimax(B, alpha, beta, depth-1, true));
                    B.unmarkColumn();
                    if (value < alpha){
                        // break α !!
                        break;
                    }
                    beta = Math.min(beta, value);
                }
            }
            return value;
        }
    }

    /**
     * Checks if the time to select a column is running out, that is if more than
     * 99 percent of it has passed.
     * 
     * @return Boolean
     */
    private boolean isTimeRunningOut(){

        long elapsedTimeMillis = System.currentTimeMillis() - startingTime;
        return (elapsedTimeMillis >= 0.98 * timeConstraintMillis);

    }

    /**
     * Orders the columns based on how far from the center of the board they are.
     * Columns will then be called in this order by the Minimax algorithms.
     * This is because tokens in columns near the center of the board tend to have more opportunities.
     * 
     * @param B
     * @return Array of the order the columns will be explored in
     */
    private Integer[] orderColumns(){
        
        Integer[] columnPriorities = new Integer[columnsNumber];
        int midPoint = columnsNumber/2;
        int iter = 0;
        int lowMidpoint = 0;

        columnPriorities[iter] = midPoint;
        iter = iter + 1;

        if (columnsNumber % 2 == 0){
            // there are two central columns
            lowMidpoint = midPoint - 1;
            columnPriorities[iter] = lowMidpoint;
            iter = iter + 1;
        }
        else{
            lowMidpoint = midPoint;
        }

        int factor = 1;
        while (iter < columnsNumber){
            columnPriorities[iter] = lowMidpoint - factor;
            iter = iter + 1;
            columnPriorities[iter] = midPoint + factor;
            iter = iter + 1;

            factor = factor + 1;
        }

        return columnPriorities;

    }

    /**
     * Initializes the Zobrist Table with random values
     */
    private void initZobrist(){
        zobristTable = new long[rowsNumber][columnsNumber][3];
        for (int i=0; i<rowsNumber; i++){
            for (int j=0; j<columnsNumber; j++){
                for (int k=0; k<3; k++){
                    zobristTable[i][j][k] = rand.nextLong();
                }
            }
        }
    }


    /**
     * Computes the hash key according to the board's current state, by XORing the random values based on
     * the cell states across the entire board.
     * 
     * @param B
     * @return The hash key that uniquely represents the board's current state
     */
    public long computeZobristHash(CXBoard B) {

        long hash = 0L;
        for (int i = 0; i < rowsNumber; i++) {
            for (int j = 0; j < columnsNumber; j++) {

                int state;
                if (B.cellState(i, j) == CXCellState.P1){
                    state = 0;
                }
                else if (B.cellState(i, j) == CXCellState.P2){
                    state = 1;
                }
                else{
                    state = 2;
                }
                
                hash ^= zobristTable[i][j][state];

            }
        }
        
        return hash;
    }




    /**
     * Determines the heuristic score of the game in the current position, by returning
     * the outcome of the game (if the game is in a final state),
     * or by calling a helper function that determines the score in a non-terminal state.
     * 
     * @param B - CXBoard object representing the current state of the game
     * @return The heuristic score in the current state of the game
     */
    private int heuristicScore(CXBoard B){

        // Terminal states evaluation
        if (B.gameState() == myWin){
            // SouthPasadena has won
            return Integer.MAX_VALUE;
        }
        else if (B.gameState() == yourWin){
            // The opponent has won
            return Integer.MIN_VALUE;
        }
        else if (B.gameState() == CXGameState.DRAW){
            // The game is a draw
            return 0;
        }

        // Non-Terminal states evaluation
        else{
            return nonTerminalHeuristicScore(B);
        }
    }

    /**
     * Function called by heuristicScore if the current state of the game is not terminal.
     * The score in the current game position is determined by assessing the potential 
     * row, column and diagonal winning opportunities for each player.
     * <p>
     * 
     * Note:
     * By entering this function, we know that the current state of the game is non-terminal,
     * and therefore we can assume that there won't be any instances on the board where a player
     * has adjacent tokens in a quantity equal to or exceeding the number required for a win
     * ('tokensToConnect').
     * 
     * @param B - CXBoard object representing the current state of the game
     * 
     * @return The heuristic score in the current state of the game, which is non-terminal
     * 
     */
    private int nonTerminalHeuristicScore(CXBoard B){

        // Initializing the heuristic score
        int score = 0;

        // VERTICAL SCORE
        for (int j=0; j<B.getAvailableColumns().length; j++){

            // Variable that iterates the current column, top to bottom
            int topDownIter = 0;
            /*
             * 
             *      0   1   2   3   4   columns
             * 0   tpdwn
             * 1    |
             * 2    |
             * 3    v
             * 4
             * rows
             * 
             */

            // Counts how many tokens from the same player there are at the top of the column
            int count = 0;

            // Establishes if the top token of the column is SouthPasadena's or the opponent's
            boolean isMyColumn = true;

            // Marks the first token top to bottom that is the other player's, stopping the loop
            boolean change = false;
            

            // Going down the j-th non-full column with 'topDownIter' until a cell is not empty
            while (topDownIter < rowsNumber && (B.cellState(topDownIter, B.getAvailableColumns()[j]) == CXCellState.FREE)){
                topDownIter++;
            }

            // The number of empty cells in the column
            int emptyCellsAbove = topDownIter;

            while (topDownIter < rowsNumber && change == false){

                // First non empty cell found
                if (count == 0){
                    count++;
                    if (B.cellState(topDownIter, B.getAvailableColumns()[j]) == myCell){
                        isMyColumn = true;
                    }
                    else{
                        isMyColumn = false;
                    }
                }

                // The cell has the same color as the cells above it
                else if (count > 0 && isMyColumn == true && B.cellState(topDownIter, B.getAvailableColumns()[j]) == myCell){
                    count++;
                }

                // The cell has the same color as the cells above it
                else if (count > 0 && isMyColumn == false && B.cellState(topDownIter, B.getAvailableColumns()[j]) == yourCell){
                    count++;
                }

                // The cell has a different color than the cell above it
                else{
                    change = true;
                }

                topDownIter++;
            }

            if (count > 0 && isMyColumn && (emptyCellsAbove + count >= tokensToConnect)){
                score = score + (int)Math.pow(count, 2) * MULTIPLIER_1;
            }
            else if (count > 0 && !isMyColumn && (emptyCellsAbove + count >= tokensToConnect)){
                score = score - (int)Math.pow(count, 2) * MULTIPLIER_1;
            }

        }

        // HORIZONTAL SCORE
        for (int i=0; i<rowsNumber; i++){
            
            // Checking if we can skip the current row
            int nonEmptyCells = 0;
            for (int k=0; k<columnsNumber; k++){
                if (B.cellState(i, k) != CXCellState.FREE){
                    nonEmptyCells++;
                }
            }
            if (nonEmptyCells == 0){
                // If the row we're considering is completely empty, we can skip it.
                continue;
            }
            else if (nonEmptyCells == columnsNumber){
                // If the row we're considering is completely filled, it won't affect the row score and we can break out of the loop.
                // This is because we can assume that all the rows below the current one are also full. 
                break;
            }

            for(int j=0; j<=columnsNumber-tokensToConnect; j++){
                int myTokens = 0;
                int yourTokens = 0;
                for(int k=0; k<tokensToConnect; k++){
                    if (B.cellState(i, j+k) == myCell){
                        myTokens++;
                    }
                    else if (B.cellState(i, j+k) == yourCell){
                        yourTokens++;
                    }
                    // Nothing happens if the cell is empty
                }
                if(myTokens > 0 && yourTokens == 0){
                    score = score + (int)Math.pow(myTokens, 2) * MULTIPLIER_2;
                }
                else if (yourTokens > 0 && myTokens == 0){
                    score = score - (int)Math.pow(yourTokens, 2) * MULTIPLIER_2;
                }
            }

        }

        // DIAGONAL SCORE
        
        // Descending diagonal
        for (int i=0; i <= rowsNumber - tokensToConnect; i++){
            for (int j=0; j <= (columnsNumber - tokensToConnect); j++){

                int iter = tokensToConnect;
                int myTotalCells = 0;
                int yourTotalCells = 0;
                int startingRow = i;
                int startingColumn = j;

                while (iter > 0){

                    if (B.cellState(startingRow, startingColumn) == myCell){
                        myTotalCells++;
                    }
                    else if (B.cellState(startingRow, startingColumn) == yourCell){
                        yourTotalCells++;
                    }
                    // Nothing happens if the cell is empty

                    startingRow++;
                    startingColumn++;
                    iter--;
                }

                if (myTotalCells > 0 && yourTotalCells == 0){
                    score = score + ((int)Math.pow(myTotalCells, 2)) * MULTIPLIER_3;
                }
                else if (myTotalCells == 0 && yourTotalCells > 0){
                    score = score - ((int)Math.pow(yourTotalCells, 2)) * MULTIPLIER_3;
                }

            }
        }

        // Ascending diagonal
        for (int i=0; i <= rowsNumber - tokensToConnect; i++){
            for (int j = tokensToConnect - 1; j < columnsNumber; j++){

                int iter = tokensToConnect;
                int myTotalCells = 0;
                int yourTotalCells = 0;
                int startingRow = i;
                int startingColumn = j;

                while (iter > 0){

                    if (B.cellState(startingRow, startingColumn) == myCell){
                        myTotalCells++;
                    }
                    else if (B.cellState(startingRow, startingColumn) == yourCell){
                        yourTotalCells++;
                    }
                    // Nothing happens if the cell is empty

                    startingRow++;
                    startingColumn--;
                    iter--;
                }

                if (myTotalCells > 0 && yourTotalCells == 0){
                    score = score + ((int)Math.pow(myTotalCells, 2)) * MULTIPLIER_3;
                }
                else if (myTotalCells == 0 && yourTotalCells > 0){
                    score = score - ((int)Math.pow(yourTotalCells, 2)) * MULTIPLIER_3;
                }

            }
        }

        return score;

    }

    /*
     * My software player's name
     */
    public String playerName() {
		return "SouthPasadena";
	}
    
}