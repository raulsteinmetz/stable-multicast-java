import StableMulticast.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client implements IStableMulticast {

    private StableMulticast stableMulticast;
    private ArrayList<String> messageBuffer;

    public Client(String ip, int port) {
        this.stableMulticast = new StableMulticast(ip, port, this);
        this.messageBuffer = new ArrayList<>();
    }

    public void deliver(String msg) {
        messageBuffer.add(msg);
    }

    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException ex) {
            //
        }
    }

    private void menu() {
        Scanner scanner = new Scanner(System.in);

        try {
            Thread.sleep(2000); // waits for id discovery
        } catch (InterruptedException e) {
            System.out.println("Interrupted during sleep: " + e.getMessage());
        }

        while (true) {
            clearScreen();
            System.out.println("Client id: " + this.stableMulticast.getClientId());
            System.out.println("\nMenu:");
            System.out.println("1. Send message");
            System.out.println("2. Read previous messages");
            System.out.println("3. Read Stable Multicast Buffer");
            System.out.println("4. Print Current Lamport Matrix");
            System.out.println("5. Leave");

            System.out.print("Choose an option: ");
            String option = scanner.nextLine();

            switch (option) {
                case "1":
                    System.out.print("Message to be sent: ");
                    String msg = scanner.nextLine();
                    this.stableMulticast.msend(msg);
                    break;
                case "2":
                    System.out.println("\nPrevious Messages:");
                    for (String message : messageBuffer) {
                        System.out.println(message);
                    }
                    System.out.print("Press enter to go back to menu: ");
                    scanner.nextLine();
                    break;
                case "3":
                    System.out.println("\nStable Multicast Buffer:");
                    List<Message> buffer = stableMulticast.getMessageBuffer();
                    for (Message message : buffer) {
                        System.out.println(message);
                    }
                    System.out.print("Press enter to go back to menu: ");
                    scanner.nextLine();
                    break;
                case "4":
                    System.out.println("\nCurrent Lamport Matrix:");
                    printCurrentLamportMatrix();
                    System.out.print("Press enter to go back to menu: ");
                    scanner.nextLine();
                    break;
                case "5":
                    stableMulticast.leaveGroup();
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printCurrentLamportMatrix() {
        int[][] lamportMatrix = stableMulticast.getCurrentLamportMatrix();
        for (int[] row : lamportMatrix) {
            System.out.println(Arrays.toString(row));
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <ip> <port>");
            return;
        }

        String ip = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port must be an integer.");
            return;
        }

        Client client = new Client(ip, port);
        client.menu();
    }
}
