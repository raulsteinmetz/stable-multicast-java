import StableMulticast.*;
import java.util.ArrayList;
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

    private void menu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            final String clearScreen = "\033[H\033[2J";
            System.out.print(clearScreen);
            System.out.println("\nMenu:");
            System.out.println("1. Send message");
            System.out.println("2. Read previous messages");
            System.out.println("3. Leave");

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
                    stableMulticast.leaveGroup();
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
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
