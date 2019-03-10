package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer extends Remote {
    ServerResponse<Boolean> register(String l, String p) throws RemoteException;
    ServerResponse<Integer> execute(Task task) throws RemoteException;

    ServerResponse<Boolean> isAlive() throws RemoteException;
}
