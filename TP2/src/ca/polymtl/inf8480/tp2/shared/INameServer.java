package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface INameServer extends Remote {
    ServerResponse<List<ServerInfo>> getOnlineServers() throws RemoteException;

    ServerResponse<Boolean> addServer(ServerInfo server) throws RemoteException;

    ServerResponse<Boolean> addLoadbalancer(String name, String password) throws RemoteException;

    ServerResponse<Boolean> isLegitLoadbalancer(String l, String p) throws RemoteException;
}
