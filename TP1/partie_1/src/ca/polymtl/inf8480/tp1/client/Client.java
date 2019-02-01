package ca.polymtl.inf8480.tp1.client;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    private static final int TIME_FIELD = 0;
    private static final int VALUE_FIELD = 1;
    FakeServer localServer = null; // Pour tester la latence d'un appel de
    // fonction normal.
    private ServerInterface localServerStub = null;
    private ServerInterface distantServerStub = null;

    public Client(String distantServerHostname) {
        super();

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        localServer = new FakeServer();
        localServerStub = loadServerStub("127.0.0.1");

        if (distantServerHostname != null) {
            distantServerStub = loadServerStub(distantServerHostname);
        }
    }

    public static void main(String[] args) {
        String distantHostname = null;

        if (args.length > 0) {
            distantHostname = args[0];
        }

        Client client = new Client(distantHostname);
        client.run();
    }

    private void run() {
        for (int i = 1; i <= 7; ++i) {
            byte[] arr = new byte[(int) Math.pow(10, i)];
            StringBuilder sb = new StringBuilder();

            sb.append(Math.pow(10, i)).append(';');
            sb.append(appelNormal(arr)).append(';');

            if (localServerStub != null) {
                sb.append(appelRMILocal(arr)).append(';');
            }

            if (distantServerStub != null) {
                sb.append(appelRMIDistant(arr));
            }
            System.out.println(sb.toString());
        }
    }

    private ServerInterface loadServerStub(String hostname) {
        ServerInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = (ServerInterface) registry.lookup("server");
        } catch (NotBoundException e) {
            System.out.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return stub;
    }

    private long appelNormal(byte[] arr) {
        long start = System.nanoTime();
        localServer.execute(arr);
        long end = System.nanoTime();
        return end - start;
    }

    private long appelRMILocal(byte[] arr) {
        try {
            long start = System.nanoTime();
            localServerStub.execute(arr);
            long end = System.nanoTime();
            return end - start;
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return -1;
    }

    private long appelRMIDistant(byte[] arr) {
        try {
            long start = System.nanoTime();
            distantServerStub.execute(arr);
            long end = System.nanoTime();
            return end - start;
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return -1;
    }
}
