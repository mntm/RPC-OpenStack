package ca.polymtl.inf8480.tp2.shared;

import java.io.Serializable;

public class ServerInfo implements Serializable {
    private String name;
    private String ip;
    private int port;
    private int capacity;

    public ServerInfo(String name, String ip, int port, int capacity) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getCapacity() {
        return capacity;
    }
}
