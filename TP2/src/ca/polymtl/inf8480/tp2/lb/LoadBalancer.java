package ca.polymtl.inf8480.tp2.lb;


import ca.polymtl.inf8480.tp2.shared.*;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
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
    private ConcurrentMap<String, Map.Entry<IServer, ServerInfo>> servers = new ConcurrentHashMap<>();
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
        ns.addLoadbalancer(this.name, this.password);

        // Start a thread to update the list of servers
        Thread pullServers = new ServerPullerThread();
        pullServers.start();
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public ServerResponse<Integer> execute(Task task) throws RemoteException {
        ServerResponse<Integer> ret = new ServerResponse<>();

        if (this.servers.size() == 0)
            return ret.setSuccessful(false).setErrorMessage("Aucun serveur n'est en ligne pour effectuer la tache");

        if (isSecure()) {
            ret = secureExecution(task);
        } else {
            ret = insecureExecution(task);
        }

        return ret;
    }

    /**
     * Envoyer aux serveur le maximum d'operation qu'ils peuvent supporter,
     * sans verifier le resultat. Les operations sont places dans une file que
     * chaque thread d'execution partage.
     * <p>
     * Si est des serveur tombe, on tente de reprendre les operations destinee a ce serveur
     *
     * @param task
     * @return
     */
    private ServerResponse<Integer> secureExecution(Task task) {
        ServerResponse<Integer> ret = new ServerResponse<>();

        Queue<TaskElement> queue = new ConcurrentLinkedQueue<>(task.getOperations());
        Queue<AtomicInteger> results = new ConcurrentLinkedQueue<>();
        Queue<Map.Entry<IServer, ServerInfo>> nodes = new ConcurrentLinkedQueue<>(this.servers.values());

        ExecutorService pool = Executors.newFixedThreadPool(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            pool.execute(new SecureExecutionWorker(queue, results, nodes));
        }
        pool.shutdown();

        try {
            pool.awaitTermination(300, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return ret.setErrorMessage("La requete a pris trop de temps pour s'executer (>300s)").setSuccessful(false);
        }

        if (queue.size() > 0) {
            return ret.setErrorMessage("Une erreur s'est produite lors de l'execution de la requete").setSuccessful(false);
        }

        int data = 0;
        while (results.peek() != null) {
            data = (data + results.poll().get()) % Const.OP_MODULUS;
        }

        return ret.setData(data);
    }

    /**
     * Envoyer le meme sous-ensemble de tache a un sous-ensemble de serveur.
     *
     * Si un des serveurs tombe, l'operation est annulee, le systeme renvoie un message d'erreur.
     *
     * @param task
     * @return
     */
    private ServerResponse<Integer> insecureExecution(Task task) {
        Debug.print("Execution insecure operation");
        ServerResponse<Integer> ret = new ServerResponse<>();

        Queue<Map.Entry<IServer, ServerInfo>> nodes = new ConcurrentLinkedQueue<>(this.servers.values());

        int nServer = nodes.size();
        Debug.print("Nodes size: " + nServer);

        if ((nServer % 3) != 0) {
            return ret.setSuccessful(false)
                    .setErrorMessage("En mode non-securise, assurez-vous d'avoir un nombre de serveur multiple de 3");
        }

        int nPart = nServer / 3;
        int nOps = (int) Math.ceil(Math.ceil(task.getSize()) / nPart);

        Debug.print("Number of part:" + String.valueOf(nPart));
        Debug.print("Number of ops:" + String.valueOf(nOps));

        ArrayList<TaskElement> elements = task.getOperations();
        ExecutorService pool = Executors.newFixedThreadPool(nodes.size());

        AtomicIntegerArray results = new AtomicIntegerArray(nPart);


        int from = 0;
        ArrayList<AtomicBoolean> commonBreaks = new ArrayList<>();
        for (int i = 0; i < nPart; i++) {
            AtomicBoolean commonBreak = new AtomicBoolean(false);
            int to = ((from + nOps) < elements.size()) ? from + nOps : elements.size();

            Task t = new Task(new ArrayList<>(elements.subList(from, to)));

            int min = Integer.MAX_VALUE;

            List<Map.Entry<IServer, ServerInfo>> s = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                Map.Entry<IServer, ServerInfo> poll = nodes.poll();
                Debug.print("Server #" + j + ": " + poll.getValue().getName());
                min = Integer.min(min, poll.getValue().getCapacity());
                s.add(poll);
            }

            ConcurrentHashMap<Integer, Integer> sharedBuff = new ConcurrentHashMap<>();
            for (int j = 0; j < 3; j++) {
                pool.execute(new NonSecureExecutionWorker(i, j, s, min, t, sharedBuff, results, commonBreak));
            }

            commonBreaks.add(commonBreak);
            from = to;
        }

        pool.shutdown();

        boolean termination = false;
        try {
            termination = pool.awaitTermination(300, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return ret.setErrorMessage("La requete a ete interrompu avant d'etre complete").setSuccessful(false);
        }

        if (!termination) {
            commonBreaks.forEach((v) -> {
                v.set(true);
            });
            return ret.setErrorMessage("La requete a pris trop de temps pour s'executer (>300s)")
                    .setSuccessful(false);
        }

        int data = 0;
        for (int i = 0; i < nPart; i++) {
            int v = results.get(i);

            if (v == Const.ERR_EXEC_ABORTED) {
                return ret.setErrorMessage("Erreur serveur: La requete ne s'est pas termine correctement!").setSuccessful(false);
            }

            data = (data + v) % Const.OP_MODULUS;
        }

        return ret.setData(data);
    }

    private class SecureExecutionWorker implements Runnable {
        private Queue<TaskElement> input;
        private Queue<AtomicInteger> output;
        private Queue<Map.Entry<IServer, ServerInfo>> nodes;
        private IServer server;
        private ServerInfo info;


        SecureExecutionWorker(Queue<TaskElement> input, Queue<AtomicInteger> output, Queue<Map.Entry<IServer, ServerInfo>> nodes) {
            this.input = input;
            this.output = output;
            this.nodes = nodes;
        }

        @Override
        public void run() {
            do { // S'arrete lorsqu'il n'y a plus de tache ou de serveur disponible
                Map.Entry<IServer, ServerInfo> node = nodes.poll();
                if (node == null) {
                    System.out.println("Aucun serveur de disponible!");
                    break;
                }

                this.server = node.getKey();
                this.info = node.getValue();
                this.nodes.offer(node);

                Task task = this.createTask();
                if (task == null) break;

                ServerResponse<Integer> execute = new ServerResponse<>();

                try {
                    execute = this.server.execute(task);
                } catch (Exception e) {
                    execute.setSuccessful(false).setErrorMessage(e.getMessage());
                    this.nodes.remove(node);
                }

                if (!execute.isSuccessful()) {
                    System.out.println(execute.getErrorMessage());
                    reinject(task);
                    try {
                        if (execute.getData() == Const.ERR_NOT_REGISTERED) this.nodes.remove(node);
                    } catch (Exception e) {
                    }
                } else {
                    this.output.offer(new AtomicInteger(execute.getData()));
                }
            } while (true);
        }

        private void reinject(Task task) {
            task.getOperations().forEach((v) -> {
                input.offer(v);
            });
        }

        private boolean isEmpty() {
            return (this.input.peek() == null);
        }

        private Task createTask() {
            if (this.isEmpty()) return null;
            Task ret = new Task();
            for (int i = 0; (i < this.info.getCapacity()) && (!this.isEmpty()); i++) {
                ret.addOperation(this.input.poll());
            }
            return ret;
        }
    }

    private class NonSecureExecutionWorker implements Runnable {

        private final int threadGroup;
        private final int index;
        private final List<Map.Entry<IServer, ServerInfo>> servers;
        private final int min;
        private final Task task;
        private ConcurrentHashMap<Integer, Integer> sharedBuff;
        private AtomicIntegerArray results;
        private AtomicBoolean commonBreak;

        private boolean valid = false;

        public NonSecureExecutionWorker(int threadGroup, final int index, final List<Map.Entry<IServer, ServerInfo>> servers,
                                        final int min, final Task task, ConcurrentHashMap<Integer, Integer> sharedBuff,
                                        AtomicIntegerArray results, AtomicBoolean commonBreak) {

            this.threadGroup = threadGroup;
            this.index = index;
            this.servers = servers;
            this.min = min;
            this.task = task;
            this.sharedBuff = sharedBuff;
            this.results = results;
            this.commonBreak = commonBreak;
        }

        @Override
        public void run() {

            int data = 0;
            while (!valid) {
                Task t;
                int from = 0;
                while ((t = createTask(from, min)).getSize() != 0) {
                    ServerResponse<Integer> execute = new ServerResponse<>();
                    try {
                        execute = this.servers.get(index).getKey().execute(t);
                    } catch (Exception e) {
                        execute.setSuccessful(false).setErrorMessage(e.getMessage());
                        StringBuilder sb = new StringBuilder();
                        sb.append("Thread Group: #").append(this.threadGroup);
                        sb.append(" - Serveur: #").append(index).append(";\t");
                        sb.append("name: ").append(this.servers.get(index).getValue().getName()).append(";\t");
                        sb.append("address: ").append(this.servers.get(index).getValue().getIp()).append(";\t");
                        sb.append("est inaccessible.");
                        System.err.println(sb.toString());

                        this.results.set(this.threadGroup, Const.ERR_EXEC_ABORTED);

                        this.commonBreak.set(true);
                        return;
                    }

                    if (execute.isSuccessful()) {
                        data = (data + execute.getData()) % Const.OP_MODULUS;
                    }

                    from += min;
                    if (this.commonBreak.get()) return;
                }
                Debug.print(Thread.currentThread().getName() + " - data: " + data);
                this.sharedBuff.put(index, data);
                valid = verify(data);
            }
        }

        private Task createTask(int from, int capacity) {
            int to = ((from + capacity) < this.task.getSize()) ? from + capacity : this.task.getSize();

            ArrayList<TaskElement> taskElements;

            try {
                taskElements = new ArrayList<>(this.task.getOperations().subList(from, to));
            } catch (Exception e) {
                return new Task();
            }

            return new Task(taskElements);
        }

        private boolean verify(Integer data) {

            boolean ret = false;

            while (this.sharedBuff.size() != 3) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (Integer k : this.sharedBuff.keySet()) {
                if (k == index) {
                    Debug.print("Data from thread " + Thread.currentThread().getName() + " -- " +
                            "TG:" + threadGroup + " index:" + index + ": " + this.sharedBuff.get(index));
                    continue;
                }
                if (data.equals(this.sharedBuff.get(k))) ret = true;
            }

            if (ret) {
                this.results.set(this.threadGroup, data);
                commonBreak.set(true);
            }

            return ret;
        }
    }

    /**
     * Met a jour la liste des serveurs. Si le serveur de nom est inaccessible: attendre 2s puis reessayer.
     * <p>
     * Fait une pause de 5.5s entre chaque requete
     */
    private class ServerPullerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                // Get server list form NS
                ServerResponse<List<ServerInfo>> ret = new ServerResponse<>();
                try {
                    ret = ns.getOnlineServers();
                } catch (RemoteException e) {
                    ret.setSuccessful(false).setErrorMessage(e.getMessage());
                }

                List<ServerInfo> serverInfos = new ArrayList<>();
                if (ret.isSuccessful()) {
                    serverInfos = ret.getData();
                    System.out.print("Nombre de serveur en ligne: " + serverInfos.size() + " --");
                    serverInfos.forEach((v) -> {
                        System.out.print("\t" + v.getName());
                    });
                    System.out.println();
                } else {
                    System.err.println(ret.getErrorMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    continue;
                }

                // register on each server
                ConcurrentMap<String, Map.Entry<IServer, ServerInfo>> tmp = new ConcurrentHashMap<>();
                for (ServerInfo info : serverInfos) {
                    IServer s = (IServer) RMIUtils.getStub(info.getIp(), info.getPort(), info.getName());
                    ServerResponse<Boolean> register = new ServerResponse<>();
                    try {
                        register = s.register(name, password);
                    } catch (Exception e) {
                        register.setSuccessful(false).setErrorMessage(e.getMessage());
                    }
                    if (register.isSuccessful() && register.getData())
                        tmp.put(info.getName(), new AbstractMap.SimpleEntry<>(s, info));
                }

                servers = tmp;

                try {
                    Thread.sleep(5500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
