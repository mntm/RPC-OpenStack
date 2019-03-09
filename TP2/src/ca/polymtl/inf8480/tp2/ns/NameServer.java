package ca.polymtl.inf8480.tp2.ns;

import ca.polymtl.inf8480.tp2.shared.INameServer;
import ca.polymtl.inf8480.tp2.shared.RMIUtils;
import ca.polymtl.inf8480.tp2.shared.ServerInfo;
import ca.polymtl.inf8480.tp2.shared.ServerResponse;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type: Serveur RMI
 * En tant que tel, s'enregistre sur le Registre RMI
 * <p>
 * Fourni au serveur un moyen de verifier l'integrite d'un distributeur de charge
 * <p>
 * Fourni aux distributeurs la liste des serveurs de calcul
 */
public class NameServer implements INameServer {
    private List<ServerInfo> servers = new ArrayList<>();
    private Map<String, String> lbs = new HashMap<>();

    private static void usage(String message) {
        System.out.println(message);
        System.out.println("server.sh <nom> <adress:port local>");
        System.exit(-1);
    }

    public static void main(String[] args) {
        RMIUtils.initSecurityManager();
        if (args.length < 2) {
            usage("Parametres insuffiants. Veuillez fournir toutes les valeurs necessaire.");
        }

        NameServer ns = new NameServer();
        ns.run(args);
    }

    private void run(String[] args) {
        String[] split = args[1].split(":");
        RMIUtils.register(split[0], Integer.parseInt(split[1]), this, args[0]);
    }

    @Override
    public ServerResponse<List<ServerInfo>> getOnlineServers() throws RemoteException {
        ServerResponse<List<ServerInfo>> ret = new ServerResponse<>();
        ret.setData(this.servers);
        return ret;
    }

    @Override
    public void addServer(ServerInfo server) throws RemoteException {
        this.servers.add(server);
    }

    @Override
    public void addLoadbalancer(String name, String password) throws RemoteException {
        this.lbs.put(name, password);
    }

    @Override
    public ServerResponse<Boolean> isLegitLoadbalancer(String l, String p) throws RemoteException {
        ServerResponse<Boolean> ret = new ServerResponse<>();
        if (this.lbs.containsKey(l) && this.lbs.get(l).equals(p))
            ret.setData(true);
        else
            ret.setData(false);

        return ret;
    }
}
