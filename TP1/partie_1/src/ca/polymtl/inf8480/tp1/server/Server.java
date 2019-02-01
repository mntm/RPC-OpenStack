package ca.polymtl.inf8480.tp1.server;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server implements ServerInterface {

    public Server() {
        super();
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            ServerInterface stub = (ServerInterface) UnicastRemoteObject
                    .exportObject(this, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("server", stub);
            System.out.println("Server ready.");
        } catch (ConnectException e) {
            System.err
                    .println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
            System.err.println();
            System.err.println("Erreur: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    /*
     * Méthode accessible par RMI. Additionne les deux nombres passés en
     * paramètre.
     */
    @Override
    public int execute(byte[] arg) {
        return arg.length;
    }
}
