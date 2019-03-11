package ca.polymtl.inf8480.tp2.ns;

import ca.polymtl.inf8480.tp2.shared.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type: Serveur RMI
 *  En tant que tel, s'enregistre sur le Registre RMI
 *
 * Fourni au serveur un moyen de verifier l'integrite d'un repartiteur de charge
 *
 * Fourni aux repartiteurs la liste des serveurs de calcul
 */
public class NameServer implements INameServer {
    private HashMap<IServer, ServerInfo> servers = new HashMap<>();
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

        // Ping chaque serveur toutes les 5 secondes
        Thread ping = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    for (IServer s : servers.keySet()) {
                        if (s == null) break;
                        try {
                            s.isAlive();
                        } catch (RemoteException e) {
                            servers.remove(s);
                            System.out.println("Serveur supprime:" + s.toString());
                        }
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        ping.start();
    }

    @Override
    public ServerResponse<List<ServerInfo>> getOnlineServers() throws RemoteException {
        ServerResponse<List<ServerInfo>> ret = new ServerResponse<>();
        ret.setData(new ArrayList<>(this.servers.values()));
        return ret;
    }

    @Override
    public synchronized void addServer(ServerInfo server) throws RemoteException {
        IServer s = (IServer) RMIUtils.getStub(server.getIp(), server.getPort(), server.getName());
        this.servers.put(s, server);
        System.out.println("Serveur ajoute: " + server.getName());
    }

    @Override
    public void addLoadbalancer(String name, String password) throws RemoteException {
        this.lbs.put(name, password);
        System.out.println("Repartiteur ajoute: " + name);
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
