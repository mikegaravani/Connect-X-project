package connectx.SouthPasadena;

import java.util.Random;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;

public class SouthPasadena implements CXPlayer {

    private Random rand;

    // CONSTANTS

    // Constant used in the row heuristic score evaluation
    public static final int MULTIPLIER = 10;

    // Constant used in the column heuristic score evaluation
    public static final int MULTIPLIER_2 = 6;

    private int rowsNumber;
    private int columnsNumber;
    private int tokensToConnect;

    private CXGameState myWin;
	private CXGameState yourWin;

    private CXCellState myCell;
    private CXCellState yourCell;

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

    }

    public int selectColumn(CXBoard B){
        Integer[] L = B.getAvailableColumns();
		return L[rand.nextInt(L.length)];
    }

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
     * Function called by heuristicScore if the current state of the game is not terminal
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
     * @return The heuristic score in the current state of the game
     * 
     */
    private int nonTerminalHeuristicScore(CXBoard B){

        // Initializing the heuristic score
        int score = 0;

        // ROW EVAL
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
                // This is because we can assume that all the rows below the current one will also be full. 
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
                    score = score + ((int)Math.pow(myTotalCells, 2)/tokensToConnect) * MULTIPLIER;
                }
                else if (myTotalCells == 0 && yourTotalCells > 0){
                    score = score - ((int)Math.pow(yourTotalCells, 2)/tokensToConnect) * MULTIPLIER;
                }
                myTotalCells = 0;
                yourTotalCells = 0;
                iter++;
            }
        }

        // COLUMN EVAL
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

            // Establishes if the top tokens of the column are SouthPasadena's or the opponent's
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

        }

        // DIAGONAL EVAL


        return score;

    }

    public String playerName() {
		return "SouthPasadena";
	}
    
}
