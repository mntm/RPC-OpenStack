package ca.polymtl.inf8480.tp2.server;

import ca.polymtl.inf8480.tp2.shared.*;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Type: Serveur RMI
 * En tant que tel, s'enregistre sur le Registre RMI
 * <p>
 * Contact le service de nom pour:
 * s'enregistrer
 * verifier qu'un repartiteur de tache est legit
 */
public class Server implements IServer {

    private INameServer ns;
    private int capacity = 2;
    private int maxOpAllowed = 12;
    private AtomicInteger currentOpCount = new AtomicInteger(0);
    private boolean faulty = false;
    private int malicious_rate = 0;
    private ArrayList<String> allowed = new ArrayList<>();

    public static void main(String[] args) {
        RMIUtils.initSecurityManager();
        if (args.length < 4) usage("Parametre insuffiant. Veuillez fournir toutes les valeurs necessaire.");

        Server server = new Server();
        for (int i = 4; i < args.length; i++) {
            try {

                switch (args[i]) {
                    case "-m":
                        server.setMaliciousRate(Integer.parseInt(args[++i]));
                        break;
                    case "-q":
                        server.setCapacity(Integer.parseInt(args[++i]));
                        break;
                    case "-f":
                        server.setFaulty(true);
                        break;
                    default:
                        break;
                }

            } catch (Exception e) {
                usage("Parametre(s) invalide: " + args[i]);
            }
        }

        try {
            server.run(args);
        } catch (Exception e) {
            e.printStackTrace();
            usage("");
        }
    }

    private static void usage(String message) {
        System.out.println(message);
        System.out.println("server.sh <nom> <adress:port local> <nom repertoire> <adresse:port repertoire> [-m <taux de malice>] [-q capacite] [-f]");
        System.out.println("\t-m\tpar defaut 0\n\t-q\tpar defaut 2\n\t-f\tle serveur a " + Const.FAULTY_RATE + "% de chance de tomber");
        System.exit(-1);
    }

    public boolean isFaulty() {
        return faulty;
    }

    public void setFaulty(boolean faulty) {
        this.faulty = faulty;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.maxOpAllowed = 6 * capacity;
    }

    public void setMaliciousRate(int rate) {
        this.malicious_rate = rate;
    }

    private void run(String[] args) throws Exception {
        System.out.println("Parametres: ");
        StringBuilder sb = new StringBuilder();

        String[] split = args[1].split(":");
        ServerInfo info = new ServerInfo(args[0], split[0], Integer.parseInt(split[1]), this.capacity);
        sb.append("\tNOM:").append(info.getName()).append("\t");
        sb.append("IP:").append(info.getIp()).append("\t");
        sb.append("PORT:").append(info.getPort()).append("\t");
        sb.append("MALICE:").append(this.malicious_rate).append("%\t");
        sb.append("DEFECTUEUX:").append(this.isFaulty() ? "OUI" : "NON").append("\t");
        sb.append("CAPACITE:").append(info.getCapacity()).append("\n");

        RMIUtils.register(info.getIp(), info.getPort(), this, info.getName());
        System.out.println(sb.toString());
        sb = new StringBuilder();

        // Get NS's stub
        split = args[3].split(":");
        ns = (INameServer) RMIUtils.getStub(split[0], Integer.parseInt(split[1]), args[2]);
        sb.append("Repertoire de nom:\n").append("\tNOM:").append(args[2]).append("\tIP:")
                .append(split[0]).append("\tPORT:").append(split[1]).append("\n");

        System.out.println(sb.toString());
        ns.addServer(info);

    }

    @Override
    public ServerResponse<Boolean> register(String l, String p) throws RemoteException {
        ServerResponse<Boolean> ret = ns.isLegitLoadbalancer(l, p);

        try {
            if (ret.getData())
                this.allowed.add(RemoteServer.getClientHost());
        } catch (ServerNotActiveException e) {
            ret.setErrorMessage("Erreur lors de l'enregistrement: Impossible de recuperer l'adresse IP de l'appelant.")
                    .setData(false)
                    .setSuccessful(false);
        }

        return ret;
    }

    @Override
    public ServerResponse<Integer> execute(Task task) throws RemoteException {
        String client = null;

        try {
            client = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        ServerResponse<Integer> ret = new ServerResponse<>();

        if (!this.allowed.contains(client)) {
            return ret.setSuccessful(false)
                    .setErrorMessage("Veuillez vous enregistrer aupres de ce serveur avant de soliciter ce service.").setData(Const.ERR_NOT_REGISTERED);
        }

        // check if we can handle the task
        if ((this.currentOpCount.getAndAdd(0) + task.getSize()) >= this.maxOpAllowed)
            return ret.setErrorMessage("Ce serveur ne peut prendre plus d'operation").setSuccessful(false).setData(Const.ERR_CANNOT_HANDLE);

        if (RandomUtils.probability(task.getSize() - this.capacity, 5 * this.capacity))
            return ret.setErrorMessage("Ce serveur ne peut prendre plus d'operation").setSuccessful(false).setData(Const.ERR_CANNOT_HANDLE);

        this.currentOpCount.addAndGet(task.getSize());

        int data = 0;

        for (TaskElement el : task.getOperations()) {
            if (this.isFaulty() && RandomUtils.probability(Const.FAULTY_RATE))
                System.exit(-3);

            data = (data + el.result()) % Const.OP_MODULUS;
            this.currentOpCount.decrementAndGet();
        }

        ret.setData(alterResult(data));
        return ret;
    }

    @Override
    public ServerResponse<Boolean> isAlive() throws RemoteException {
        ServerResponse<Boolean> ret = new ServerResponse<>();
        ret.setData(true);
        return ret;
    }

    /**
     * Alter the result depending on the malicious rate
     *
     * @param res
     * @return
     */
    private int alterResult(int res) {
        Random r = new Random();
        return (RandomUtils.probability(this.malicious_rate)) ? r.nextInt() : res;
    }
}
