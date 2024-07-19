package StableMulticast;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String message;
    private int[][] matrix;

    public Message(String message, int[][] matrix) {
        this.message = message;
        if (matrix.length == 4 && matrix[0].length == 4) {
            this.matrix = new int[4][4];
            for (int i = 0; i < 4; i++) {
                this.matrix[i] = Arrays.copyOf(matrix[i], 4);
            }
        } else {
            throw new IllegalArgumentException("Matrix must be 4x4");
        }
    }

    public String getMessage() {
        return message;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMatrix(int[][] matrix) {
        if (matrix.length == 4 && matrix[0].length == 4) {
            for (int i = 0; i < 4; i++) {
                this.matrix[i] = Arrays.copyOf(matrix[i], 4);
            }
        } else {
            throw new IllegalArgumentException("Matrix must be 4x4");
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", matrix=" + Arrays.deepToString(matrix) +
                '}';
    }
}
