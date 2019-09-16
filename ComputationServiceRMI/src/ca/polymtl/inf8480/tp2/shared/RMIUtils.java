package ca.polymtl.inf8480.tp2.shared;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Helper class to do various RMI tasks.
 */
public class RMIUtils {
    public static void initSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    /**
     * @param hostname IP address where to find rmiregistry (usually the local ip address)
     * @param port     Port on which rmiregistry is listening
     * @param name     The name of the shared object to retrieve
     * @return A stub to the requested object
     */
    public static Remote getStub(String hostname, int port, String name) {
        Remote stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            stub = registry.lookup(name);
        } catch (NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        return stub;
    }

    /**
     * Register a Remote object on the registry
     *
     * @param hostname IP address where to find rmiregistry (usually the local ip address)
     * @param port     Port on which rmiregistry is listening
     * @param server   The remote object to be exported
     * @param name     The name to be associated to this server
     */
    public static void register(String hostname, int port, Remote server, String name) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        int xPort = Integer.parseInt(System.getProperty("rmi.object.active-port"));

        try {
            Remote stub = UnicastRemoteObject
                    .exportObject(server, xPort);

            Registry registry = LocateRegistry.getRegistry(hostname, port);

            registry.rebind(name, stub);
        } catch (ConnectException e) {
            System.err
                    .println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
            System.err.println();
            System.err.println("Erreur: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
}
