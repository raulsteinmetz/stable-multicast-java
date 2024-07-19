package StableMulticast;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class StableMulticast {
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

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.unicastPort = port;
        this.ip = ip;
        this.multicastPort = 4446; // defaulted for now
        this.multicastIp = "230.0.0.0";
        this.client = client;
        this.members = new HashSet<>();

        this.lamport = new int[4][4]; // defaulted for 4 members in the group

        try {
            this.multicastSocket = new MulticastSocket(multicastPort);
            this.group = InetAddress.getByName(multicastIp);
            this.multicastSocket.joinGroup(group);

            this.unicastSocket = new DatagramSocket(unicastPort);

            new Thread(this::receiveMulticastMessages).start(); // receives multicast messages
            new Thread(this::receiveUnicastMessages).start(); // receives unicast messages

            sendMulticast("join:" + this.ip + ":" + this.unicastPort); // notify entrance and id

        } catch (IOException e) {
            //
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
                //
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

                client.deliver(message.getMessage()); 
            } catch (IOException | ClassNotFoundException e) {
                //
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
            //
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
            //
        }
    }

    public void msend(String msg) {
        Message message = new Message(msg, lamport);
        // send the message to all known members via unicast
        for (InetSocketAddress member : members) {
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
            //
        }
    }
}
