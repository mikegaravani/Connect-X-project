package connectx.SouthPasadena;

public class TranspositionEntry {
    long zobristHash;
    int evaluation;
    int depth;

    // Constructor
    public TranspositionEntry(long zobristHash, int evaluation, int depth) {
        this.zobristHash = zobristHash;
        this.evaluation = evaluation;
        this.depth = depth;
    }

    // Getters and Setters as needed
    public long getZobristHash() {
        return zobristHash;
    }

    public int getEvaluation() {
        return evaluation;
    }

    public int getDepth() {
        return depth;
    }
}
