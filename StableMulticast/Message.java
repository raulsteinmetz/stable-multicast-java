package StableMulticast;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message;
    private final int[] lamportVector;
    private int senderId;

    public Message(String message, int[] lamportVector, int senderId) {
        this.message = message;
        this.lamportVector = Arrays.copyOf(lamportVector, lamportVector.length);
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public int[] getLamportVector() {
        return lamportVector;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLamportVector(int[] lamportVector) {
        System.arraycopy(lamportVector, 0, this.lamportVector, 0, lamportVector.length);
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    private String formatVector() {
        return Arrays.toString(lamportVector);
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", senderId=" + senderId +
                ", lamportVector=" + formatVector() +
                '}';
    }
}
