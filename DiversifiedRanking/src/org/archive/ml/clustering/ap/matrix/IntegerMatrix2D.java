package org.archive.ml.clustering.ap.matrix;

public class IntegerMatrix2D {
	
	private static final long serialVersionUID = 1L;
    private Integer [][] matrix;
    private int N,  M;
    
    public IntegerMatrix2D(final int N, final int M, final Integer v) {
        this.N = N;
        this.M = M;
        this.matrix = new Integer[N][M];
        setValues(v);
    }
    
    
    
    public void setValues(final Integer v) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] = v;
            }
        }
    }
    
    public void set(final int i, final int j, final Integer v) {
        matrix[i][j] = v;
    }
    
    public Integer get(final int i, final int j) {
        return matrix[i][j];
    }
    
    public String toString() {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                res.append(this.matrix[i][j]);
                res.append(" ");
            }
            res.append("\n");

        }
        return res.toString();
    }
}
