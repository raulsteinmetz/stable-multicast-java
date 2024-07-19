package StableMulticast;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message;
    private final int[][] lamportMatrix;
    private int senderId;

    public Message(String message, int[][] lamportMatrix, int senderId) {
        this.message = message;
        this.lamportMatrix = new int[lamportMatrix.length][lamportMatrix.length];
        for (int i = 0; i < lamportMatrix.length; i++) {
            this.lamportMatrix[i] = Arrays.copyOf(lamportMatrix[i], lamportMatrix.length);
        }
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public int[][] getLamportMatrix() {
        return lamportMatrix;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLamportMatrix(int[][] lamportMatrix) {
        for (int i = 0; i < lamportMatrix.length; i++) {
            this.lamportMatrix[i] = Arrays.copyOf(lamportMatrix[i], lamportMatrix.length);
        }
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    private String formatMatrix() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int[] row : lamportMatrix) {
            sb.append("  ").append(Arrays.toString(row)).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", senderId=" + senderId +
                ", lamportMatrix=" + formatMatrix() +
                '}';
    }
}
