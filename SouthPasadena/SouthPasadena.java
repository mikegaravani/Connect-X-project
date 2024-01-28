package connectx.SouthPasadena;

import java.util.Random;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

public class SouthPasadena implements CXPlayer {

    private Random rand;

    private int rowsNumber;
    private int columnsNumber;
    private int tokensToConnect;

    private CXGameState myWin;
	private CXGameState yourWin;

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

    private int nonTerminalHeuristicScore(CXBoard B){
        
        // Initializing the heuristic score
        int score = 0;

        // Initializing additional parameters
        int iter = 0;

        // Checking the row evaluation
        for (int i=0; i<rowsNumber; i++){

        }

        return score;

    }

    public String playerName() {
		return "SouthPasadena";
	}
    
}
