package StableMulticast;

import java.io.IOException;
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

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.unicastPort = port;
        this.ip = ip;
        this.multicastPort = 4446; // defaulted for now
        this.multicastIp = "230.0.0.0";
        this.client = client;
        this.members = new HashSet<>();
        
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
                String msg = new String(packet.getData(), 0, packet.getLength());
                client.deliver(msg);
            } catch (IOException e) {
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

    private void sendUnicast(String msg, InetSocketAddress member) {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, member.getAddress(), member.getPort());

        try {
            unicastSocket.send(packet);
        } catch (IOException e) {
            //
        }
    }

    public void msend(String msg) {
        // send the message to all known members via unicast
        for (InetSocketAddress member : members) {
            sendUnicast(msg, member);
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
