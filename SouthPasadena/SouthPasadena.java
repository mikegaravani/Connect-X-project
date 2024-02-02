package connectx.SouthPasadena;

import java.util.Random;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;

/**
 * Mike Garavani's Connect X software player
 */
public class SouthPasadena implements CXPlayer {

    private Random rand;

    // CONSTANTS

    // Constant used in the row heuristic score evaluation
    public static final int MULTIPLIER_1 = 10;

    // Constant used in the column heuristic score evaluation
    public static final int MULTIPLIER_2 = 6;

    // Constant used in the diagonal heuristic score evaluation
    public static final int MULTIPLIER_3 = 8;

    private int rowsNumber;
    private int columnsNumber;
    private int tokensToConnect;

    private CXGameState myWin;
	private CXGameState yourWin;

    private CXCellState myCell;
    private CXCellState yourCell;

    // Dealing with time
    private long startingTime;
    private long timeConstraintMillis; // TODO maybe int and keep it in seconds?????????????????


    /* Default empty constructor */
	public SouthPasadena() {
	}

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs){
        // INITIALIZE PLAYER
        rand = new Random(System.currentTimeMillis());

        rowsNumber = M;
        columnsNumber = N;
        tokensToConnect = X;

        myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

        myCell = first ? CXCellState.P1 : CXCellState.P2;
        yourCell = first ? CXCellState.P2 : CXCellState.P1;

        timeConstraintMillis = timeout_in_secs * 1000;
    }

    /**
     * Checks if the time to select a column is running out, that is if more than
     * 99 percent of it has passed.
     * 
     * @return Boolean
     */
    private boolean isTimeRunningOut(){
        long elapsedTimeMillis = System.currentTimeMillis() - startingTime;
        // TODO keep it 99% ?? sus
        return (elapsedTimeMillis >= 0.99 * timeConstraintMillis);
    }



    public int selectColumn(CXBoard B){

        // If the board is empty, start in the center
        if (B.numOfMarkedCells() == 0){
            return columnsNumber/2;
        }



        // TODO fix this tomfoolery
        /*
        // RANDOM DEFAULT COLUMN
        Integer[] L = B.getAvailableColumns();
		return L[rand.nextInt(L.length)];
        */
        return 0;
    }

    private int alphaBetaMinimax(CXBoard B, int alpha, int beta, int depth, boolean isMaximizing){

        if (depth == 0 || B.gameState() != CXGameState.OPEN || isTimeRunningOut()){
            return heuristicScore(B);
        }

        if (isMaximizing){
            int value = Integer.MIN_VALUE + 100;
            Integer[] avColumns = B.getAvailableColumns();
            for (int i=0; i<avColumns.length; i++){
                B.markColumn(avColumns[i]);
                // Note that in the following call to alphaBetaMinimax the CXBoard B has been updated
                value = Math.max(value, alphaBetaMinimax(B, alpha, beta, depth-1, !isMaximizing));
                B.unmarkColumn();
                if (value >= beta){
                    // break BETA !!
                    break;
                }
                alpha = Math.max(alpha, value);
            }
            return value;
        }

        else{
            int value = Integer.MAX_VALUE - 100;
            Integer[] avColumns = B.getAvailableColumns();
            for (int i=0; i<avColumns.length; i++){
                B.markColumn(avColumns[i]);
                // Note that in the following call to alphaBeta the CXBoard B has been updated
                value = Math.min(value, alphaBetaMinimax(B, alpha, beta, depth-1, isMaximizing));
                B.unmarkColumn();
                if (value <= alpha){
                    // break ALPHA !!
                    break;
                }
                beta = Math.min(beta, value);
            }
            return value;
        }
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
            return Integer.MAX_VALUE - 100;
        }
        else if (B.gameState() == yourWin){
            // The opponent has won
            return Integer.MIN_VALUE + 100;
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

        // HORIZONTAL SCORE
        for (int i=0; i<rowsNumber; i++){

            int iter = 0;
            int myTotalCells = 0;
            int yourTotalCells = 0;

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

            while (iter + tokensToConnect <= columnsNumber){
                for (int j=0; j<tokensToConnect; j++){
                    if (B.cellState(i, j+iter) == myCell){
                        myTotalCells++;
                    }
                    else if (B.cellState(i, j+iter) == yourCell){
                        yourTotalCells++;
                    }
                    // Nothing happens if the cell is empty
                }
                if (myTotalCells > 0 && yourTotalCells == 0){
                    score = score + ((int)Math.pow(myTotalCells, 2)/tokensToConnect) * MULTIPLIER_1;
                }
                else if (myTotalCells == 0 && yourTotalCells > 0){
                    score = score - ((int)Math.pow(yourTotalCells, 2)/tokensToConnect) * MULTIPLIER_1;
                }
                myTotalCells = 0;
                yourTotalCells = 0;
                iter++;
            }
        }

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
                score = score + (int)Math.pow(count, 2)/tokensToConnect * MULTIPLIER_2;
            }
            else if (count > 0 && !isMyColumn && (emptyCellsAbove + count >= tokensToConnect)){
                score = score - (int)Math.pow(count, 2)/tokensToConnect * MULTIPLIER_2;
            }

            // TODO IN REPORT: explain that normalizing by tokenstoconnect has virtually no effect
            // TODO: possibly remove normalizations by tokenstoconnect !!!!!!

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
                    score = score + ((int)Math.pow(myTotalCells, 2)/tokensToConnect) * MULTIPLIER_3;
                }
                else if (myTotalCells == 0 && yourTotalCells > 0){
                    score = score - ((int)Math.pow(yourTotalCells, 2)/tokensToConnect) * MULTIPLIER_3;
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
                    score = score + ((int)Math.pow(myTotalCells, 2)/tokensToConnect) * MULTIPLIER_3;
                }
                else if (myTotalCells == 0 && yourTotalCells > 0){
                    score = score - ((int)Math.pow(yourTotalCells, 2)/tokensToConnect) * MULTIPLIER_3;
                }

            }
        }


        return score;

    }

    public String playerName() {
		return "SouthPasadena";
	}
    
}
