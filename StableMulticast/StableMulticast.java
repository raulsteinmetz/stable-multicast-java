package StableMulticast;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StableMulticast {
    public static final int N_CLIENTS = 4;
    private IStableMulticast client;
    private String ip;
    private String multicastIp;
    private int multicastPort;
    private int unicastPort;
    private MulticastSocket multicastSocket;
    private DatagramSocket unicastSocket;
    private InetAddress group;
    private Set<InetSocketAddress> members;
    private int[][] lamport;
    private int clientId;
    private Set<InetSocketAddress> infoMessagesReceived;
    private List<Message> messageBuffer;

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.unicastPort = port;
        this.ip = ip;
        this.multicastPort = 4446; // defaulted for now
        this.multicastIp = "230.0.0.0";
        this.client = client;
        this.clientId = 0;
        this.members = new HashSet<>();
        this.infoMessagesReceived = new HashSet<>();
        this.messageBuffer = new ArrayList<>();

        this.lamport = new int[N_CLIENTS][N_CLIENTS]; // N_CLIENTS members in the group

        try {
            this.multicastSocket = new MulticastSocket(multicastPort);
            this.group = InetAddress.getByName(multicastIp);
            this.multicastSocket.joinGroup(group);

            this.unicastSocket = new DatagramSocket(unicastPort);

            new Thread(this::receiveMulticastMessages).start(); // receives multicast messages
            new Thread(this::receiveUnicastMessages).start(); // receives unicast messages

            sendMulticast("join:" + this.ip + ":" + this.unicastPort); // notify entrance

            // start a timer to wait for "info" messages (id discovery)
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(this::assignClientId, 1, TimeUnit.SECONDS);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMulticastMessages() {
        byte[] buffer = new byte[1000];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                multicastSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                handleMulticast(msg, packet.getAddress(), packet.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveUnicastMessages() {
        byte[] buffer = new byte[1000];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                unicastSocket.receive(packet);
                ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
                Message message = (Message) is.readObject();
                is.close();

                // deposit the message in the buffer
                synchronized (messageBuffer) {
                    messageBuffer.add(message);
                }

                // update the local matrix
                updateLamportMatrix(message.getLamportVector(), message.getSenderId());

                // deliver the message to the client
                client.deliver(message.getMessage());

                // check for stable messages and discard them
                discardStableMessages();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMulticast(String msg, InetAddress address, int port) {
        if (msg.startsWith("join:")) { // someone joined
            String[] parts = msg.split(":");
            String memberIp = parts[1];
            int memberPort = Integer.parseInt(parts[2]);
            if (memberIp.equals(this.ip) && memberPort == this.unicastPort) {
                return; // ignore messages from itself
            }
            InetSocketAddress member = new InetSocketAddress(memberIp, memberPort);
            if (!members.contains(member)) {
                members.add(member);
            }
            sendMulticast("info:" + this.ip + ":" + this.unicastPort); // notify entrance and id
        } else if (msg.startsWith("info:")) { // existing member id
            String[] parts = msg.split(":");
            String memberIp = parts[1];
            int memberPort = Integer.parseInt(parts[2]);
            if (memberIp.equals(this.ip) && memberPort == this.unicastPort) {
                return; // ignore messages from itself
            }
            InetSocketAddress member = new InetSocketAddress(memberIp, memberPort);
            if (!members.contains(member)) {
                members.add(member);
            }
            synchronized (infoMessagesReceived) {
                infoMessagesReceived.add(member);
            }
        } else if (msg.startsWith("leave:")) { // someone is leaving
            String[] parts = msg.split(":");
            String memberIp = parts[1];
            int memberPort = Integer.parseInt(parts[2]);
            if (memberIp.equals(this.ip) && memberPort == this.unicastPort) {
                return; // ignore messages from itself
            }
            InetSocketAddress member = new InetSocketAddress(memberIp, memberPort);
            members.remove(member);
        }
    }

    private void sendMulticast(String msg) {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);

        try {
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUnicast(Message message, InetSocketAddress member) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            os.flush();
            os.writeObject(message);
            os.flush();
            byte[] sendBuf = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, member.getAddress(), member.getPort());

            unicastSocket.send(packet);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assignClientId() {
        synchronized (infoMessagesReceived) {
            this.clientId = infoMessagesReceived.size();
            System.out.println("Assigned client ID: " + this.clientId);
        }
    }

    public void msend(String msg) {
        System.out.println("...Sending message..."); // debugging
        Scanner sc = new Scanner(System.in); // debugging
        // construct the vector timestamp for the message
        int[] vectorTimestamp = new int[N_CLIENTS];
        synchronized (lamport) {
            System.arraycopy(lamport[clientId], 0, vectorTimestamp, 0, N_CLIENTS);
            lamport[clientId][clientId]++;
        }

        // create the message with the vector timestamp
        Message message = new Message(msg, vectorTimestamp, this.clientId);

        // deposit
        synchronized (messageBuffer) {
            messageBuffer.add(message);
        }

        // send the message to all known members via unicast
        for (InetSocketAddress member : members) {
            System.out.print("Press Enter to send each message!"); // debugging
            sc.nextLine(); // debugging
            sendUnicast(message, member);
        }

    }

    public void leaveGroup() {
        try {
            // notify others of leave
            sendMulticast("leave:" + this.ip + ":" + this.unicastPort);
            this.multicastSocket.leaveGroup(group);
            this.multicastSocket.close();
            this.unicastSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getClientId() {
        return this.clientId;
    }

    public List<Message> getMessageBuffer() {
        synchronized (messageBuffer) {
            return new ArrayList<>(messageBuffer);
        }
    }

    private void updateLamportMatrix(int[] receivedVectorClock, int senderId) {
        synchronized (lamport) {
            System.arraycopy(receivedVectorClock, 0, lamport[senderId], 0, N_CLIENTS);
            lamport[clientId][senderId]++;
        }
    }

    private void discardStableMessages() {
        synchronized (messageBuffer) {
            Iterator<Message> iterator = messageBuffer.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                int senderId = message.getSenderId();
                int messageSeqNum = message.getLamportVector()[senderId];
                boolean isStable = true;

                for (int i = 0; i < N_CLIENTS; i++) {
                    if (lamport[i][senderId] <= messageSeqNum) {
                        isStable = false;
                        break;
                    }
                }

                if (isStable) {
                    iterator.remove();
                }
            }
        }
    }

    public int[][] getCurrentLamportMatrix() {
        synchronized (lamport) {
            int[][] copy = new int[N_CLIENTS][N_CLIENTS];
            for (int i = 0; i < N_CLIENTS; i++) {
                System.arraycopy(lamport[i], 0, copy[i], 0, N_CLIENTS);
            }
            return copy;
        }
    }
}
