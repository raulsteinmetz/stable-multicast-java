package  StableMulticast;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String message;
    private int[][] matrix;
    private int matrixSize;

    public Message(String message, int[][] matrix) {
        this.message = message;
        this.matrixSize = matrix.length;
        if (matrix.length > 0 && matrix.length == matrix[0].length) {
            this.matrix = new int[matrixSize][matrixSize];
            for (int i = 0; i < matrixSize; i++) {
                this.matrix[i] = Arrays.copyOf(matrix[i], matrixSize);
            }
        } else {
            throw new IllegalArgumentException("Matrix must be square (NxN)");
        }
    }

    public String getMessage() {
        return message;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public int getMatrixSize() {
        return matrixSize;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMatrix(int[][] matrix) {
        if (matrix.length == this.matrixSize && matrix.length == matrix[0].length) {
            for (int i = 0; i < matrixSize; i++) {
                this.matrix[i] = Arrays.copyOf(matrix[i], matrixSize);
            }
        } else {
            throw new IllegalArgumentException("Matrix must be square (NxN) and match the initialized size");
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", matrix=" + Arrays.deepToString(matrix) +
                ", matrixSize=" + matrixSize +
                '}';
    }
}
