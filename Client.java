import StableMulticast.*;
import java.util.Scanner;

public class Client implements IStableMulticast {

    private StableMulticast stableMulticast;

    public Client(String ip, int port) {
        this.stableMulticast = new StableMulticast(ip, port, this);
    }

    public void deliver(String msg) {
        System.out.println("Received: " + msg);
    }

    private void menu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Message to be sent: ");
            String msg = scanner.nextLine();
            if (msg.equalsIgnoreCase("exit")) {
                stableMulticast.leaveGroup();
                break;
            } else {
                this.stableMulticast.msend(msg);
            }
        }
        scanner.close();
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
