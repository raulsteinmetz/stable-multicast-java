package StableMulticast;

public class StableMulticast {
    private IStableMulticast client;
    private String ip;
    private int port;

    
    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.ip = ip;
        this.port = port;
        this.client = client;
    }

    public void msend(String msg) {
        // pass
    }
}