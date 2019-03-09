package ca.polymtl.inf8480.tp2.shared;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILoadBalancer extends Remote {
    ServerResponse<Integer> execute(Task task) throws RemoteException;
}
