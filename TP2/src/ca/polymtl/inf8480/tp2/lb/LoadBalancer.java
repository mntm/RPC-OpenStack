package ca.polymtl.inf8480.tp2.lb;

import ca.polymtl.inf8480.tp2.shared.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


/**
 * Type: Serveur RMI
 * En tant que tel, s'enregistre sur le Registre RMI
 * <p>
 * Contacte un serveur de nom pour:
 * s'enregistrer;
 * recuperer la liste des serveur de calcul
 * <p>
 * Contacte les serveurs de calcul pour:
 * s'authentifier;
 * soumettre une tache
 * <p>
 * A deux mode de fonctionement: Securise et Non securise
 * <p>
 * Decoupe les taches en fonction de la capacite de chaque serveur
 */
public class LoadBalancer implements ILoadBalancer {
    private INameServer ns = null;
    private List<ServerInfo> servers = new ArrayList<>();
    private String name;
    private String password;
    private boolean secure = true;


    public LoadBalancer(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public static void main(String[] args) {
        RMIUtils.initSecurityManager();

        if (args.length < 5) {
            usage("Parametre insuffiant. Veuillez fournir toutes les valeurs necessaire.");
        }

        boolean b = Arrays.stream(args).anyMatch(Predicate.isEqual("-m"));
        if (b && (args.length != 6))
            usage("Parametre insuffiant. Veuillez fournir toutes les valeurs necessaire.");

        LoadBalancer lb = new LoadBalancer(args[0], args[1]);
        if (b)
            lb.setSecure(false);
        try {
            lb.run(args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void usage(String message) {
        System.out.println(message);
        System.out.println("lb.sh <nom> <unip> <adresse:port repartiteur> <nom repertoire> <adresse:port repertoire > [-m]");
        System.exit(-1);
    }

    private void run(String[] args) throws RemoteException {
        // Register on the registry
        String[] split = args[2].split(":");
        RMIUtils.register(split[0], Integer.parseInt(split[1]), this, this.name);

        // Get NS's Stub
        split = args[4].split(":");
        ns = (INameServer) RMIUtils.getStub(split[0], Integer.parseInt(split[1]), args[3]);

        // Register to NS
        ServerResponse<Boolean> reg = ns.addLoadbalancer(args[0], args[1]);
        if (!reg.isSuccessful()) {
            usage(reg.getErrorMessage());
        }

        // Start a thread to get Server list from NS
        ServerInfoPullThread pull = new ServerInfoPullThread();
        pull.start();

    }

    @Override
    public ServerResponse<Integer> execute(Task task) throws RemoteException {
        ServerResponse<Integer> ret = new ServerResponse<>();
        // TODO handle failure (eg: if a server crashes)


        if (isSecure()) {

        }
        return ret;
    }

    private int secureExecution(Task task) {
        return 0;
    }

    private int insecureExecution(Task task) {
        return 0;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    private class ServerInfoPullThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    ServerResponse<List<ServerInfo>> ret = ns.getOnlineServers();
                    if (ret.isSuccessful()) {
                        servers = ret.getData();
                    } else {
                        System.err.println(ret.getErrorMessage());
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
