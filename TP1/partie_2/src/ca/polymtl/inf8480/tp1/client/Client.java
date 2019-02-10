package ca.polymtl.inf8480.tp1.client;

import ca.polymtl.inf8480.tp1.shared.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;


public class Client {

    // Attributes
    private ServerInterface localServerStub = null;
    private FileManager clientFileManager = new FileManager("client");
    private HashMap<String, Group> groups = new HashMap<>(); // holds the group name and its members
    private String[] args = null;
    private String token = null;

    public Client() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            //{{ Initializes connection to the server
            this.localServerStub = (ServerInterface) loadServerStub("127.0.0.1", "server");
            FileManager tokenFile = new FileManager("client");
            if (tokenFile.isReadable("tokenFile")) {
                getToken();
            }
            //}}
        } catch (Exception e) {
        }

        if (this.localServerStub == null) {
            System.err.println("Impossible de joindre le serveur de fichier!");
            System.exit(-1);
        }

        this.getGroupFromLocalFile();
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run(args);
    }

    /**
     * Print program usage.
     *
     * @param msg String to print before the usage is printed.
     */
    private static void printUsage(final String msg) {
        System.out.println(msg);
        System.out.println("USAGE:");
        System.out.println("    opensession -u <UserName> -p <Password>");
        System.out.println("    send -s \"<subject>\" <emailAdress>");
        System.out.println("    read");
        System.out.println("    delete");
        System.out.println("    list [-ur]");
        System.out.println("    search <words>");
        System.out.println("    lock-group-list");
        System.out.println("    get-group-list");
        System.out.println("    create-group <groupName> --descr <groupDescription>");
        System.out.println("    join-group <groupName> -u <userName>");
        System.out.println("    publish-group-list");
    }

    /**
     * Parse les arguments et execute la commande appropriee
     *
     * @param args String []
     */
    public void run(final String[] args) {
        try {
            switch (args[0]) {
                case "opensession":
                    if (args.length < 4) {
                        Client.printUsage("The `send` command requires 4 argument. type help for more infos");
                        return;
                    }
                    String userName = args[2];
                    String password = args[4];
                    openSession(userName, password);
                    break;
                case "send":
                    if (args.length < 4) {
                        Client.printUsage("The `send` command requires 4 argument. type help for more infos");
                        return;
                    }
                    String subject = args[2];
                    String addrDest = args[3];
                    Scanner scanner = new Scanner(System.in);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while (!(line = scanner.nextLine()).trim().equals(".")) {
                        sb.append(line).append('\n');
                    }
                    scanner.close();
                    send(subject, addrDest, sb.toString());
                    break;
                case "read":
                    if (args.length > 1) {
                        Client.printUsage("The `read` command requires 0 argument. type help for more infos");
                        return;
                    }
                    this.read();
                    break;
                case "delete":
                    if (args.length > 1) {
                        Client.printUsage("The `delete` command requires 0 or 1 argument. type help for more infos");
                        return;
                    }
                    this.delete();
                    break;
                case "list":
                    boolean unread = ((args.length > 1) && (args[1].equals("-ur")));
                    this.list(unread);
                    break;
                case "search":
                    if (args.length < 2) {
                        Client.printUsage("The `search` command requires at least 1 argument. type help for more infos");
                        return;
                    }
                    String[] keywords = new String[args.length - 1];
                    System.arraycopy(args, 1, keywords, 0, args.length - 1);
                    search(keywords);
                    break;
                case "create-group":
                    if (args.length != 2) {
                        Client.printUsage("The `create-group` command requires 1 argument. type help for more infos");
                        return;
                    }
                    String groupName = args[1];
                    this.createGroup(groupName);
                    break;
                case "join-group":
                    if (args.length != 4) {
                        Client.printUsage("The `join-group` command requires 3 argument. type help for more infos");
                        return;
                    }
                    String joinGroupName = args[1];
                    String joinUserName = args[3];
                    this.joinGroup(joinGroupName, joinUserName);
                    break;
                case "publish-group-list":
                    if (args.length > 1) {
                        Client.printUsage("The `publish-group-list` command requires 0 argument. type help for more infos");
                        return;
                    }
                    this.publishGroupList();
                    break;

                case "lock-group-list":
                    if (args.length > 1) {
                        Client.printUsage("The `lock-group-list` command requires 0 argument. type help for more infos");
                        return;
                    }
                    this.lockGroupList();
                    break;

                case "get-group-list":
                    this.getGroupList();
                    break;

                case "help":
                    Client.printUsage("help");
                    break;
                default:
                    Client.printUsage("Invalid command");
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private Remote loadServerStub(String hostname, String sname) {
        Remote stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = registry.lookup(sname);
        } catch (NotBoundException e) {
            System.out.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (AccessException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return stub;
    }

    private void getToken() {
        try (BufferedReader pw = clientFileManager.newBufferedReader("tokenFile")) {
            token = pw.readLine();
        } catch (IOException e) {
        }
    }

    private void openSession(String userName, String password) throws RemoteException {
        ServerResponse response = localServerStub.openSession(userName, password);
        if (!response.isSuccessful()) {
            System.err.println(response.getErrorMessage());
        } else {
            token = (String) response.getData();
            try (BufferedWriter pw = clientFileManager.newBufferedWriter("tokenFile")) {
                pw.write(token);
            } catch (IOException e) {
            }
        }
    }

    private void send(String subject, String addrDest, String content) throws RemoteException {
        ServerResponse response = null;
        if (this.groups.containsKey(addrDest)) {
            /*
                On envoie a tous les membres du groupes au niveau du client parce qu'il se peut que le
                groupe existe sur le client et pas sur le serveur. Ou que les membres sont differents.
             */
            for (String to : this.groups.get(addrDest).getMembers()) {
                response = localServerStub.sendEmail(to, subject, content, token);
            }
        } else {
            response = localServerStub.sendEmail(addrDest, subject, content, token);
        }
        if (!response.isSuccessful()) {
            System.err.println(response.getErrorMessage());
        }
    }

    private void read() throws RemoteException {
        ServerResponse<List<EmailMetadata>> list = localServerStub.listMails(false, token);
        if (!list.isSuccessful()) {
            System.err.println(list.getErrorMessage());
            return;
        }

        int i = this.printEmailList(list.getData(), true);

        int id = this.getLineNumber("Lire le courrier numero: ", i);


        ServerResponse<String> rm = localServerStub.readMail(list.getData().get(id - 1), token);
        if (!rm.isSuccessful()) {
            System.err.println(list.getErrorMessage());
            return;
        }

        String data = rm.getData();
        System.out.println("\n" + data);
    }

    private void delete() throws RemoteException {
        ServerResponse<List<EmailMetadata>> list = localServerStub.listMails(false, token);
        if (!list.isSuccessful()) {
            System.err.println(list.getErrorMessage());
            return;
        }

        int i = this.printEmailList(list.getData(), true);

        int id = this.getLineNumber("Supprimer le courrier numero: ", i);


        ServerResponse<String> rm = localServerStub.deleteMail(list.getData().get(id - 1), token);
        if (!rm.isSuccessful()) {
            System.err.println(list.getErrorMessage());
        }
    }

    private void search(String[] keywords) throws RemoteException {
        ServerResponse response = localServerStub.searchMail(keywords, token);
        if (!response.isSuccessful()) {
            System.err.println(response.getErrorMessage());
        } else {
            this.printEmailList((List<EmailMetadata>) response.getData(), false);
        }
    }

    private void list(boolean justUnread) throws RemoteException {
        ServerResponse response = localServerStub.listMails(justUnread, token);
        if (!response.isSuccessful()) {
            System.err.println(response.getErrorMessage());
        } else {
            printEmailList((List<EmailMetadata>) response.getData(), false);
        }
    }

    private void createGroup(String groupName) {
        Group group = new Group(groupName);
        this.groups.putIfAbsent(groupName, group);
        writeGroupFile();
    }

    private void joinGroup(String groupName, String user) {

        if (!this.groups.containsKey(groupName)) {
            System.out.println("Le groupe `" + groupName + "' n'existe pas.");
            return;
        }

        this.groups.get(groupName).addMember(user);
        this.writeGroupFile();
    }

    private void publishGroupList() throws RemoteException {
        List<Group> list = new ArrayList<>();
        list.addAll(this.groups.values());

        ServerResponse response = localServerStub.pushGroupList(list, token);
        if (!response.isSuccessful()) {
            System.err.println(response.getErrorMessage());
        }
    }

    private void lockGroupList() throws RemoteException {
        ServerResponse response = localServerStub.lockGroupList(token);
        if (!response.isSuccessful()) {
            System.err.println(response.getErrorMessage());
        } else {
            System.err.println((String) response.getData());
        }
    }

    private void getGroupList() throws RemoteException {
        // Generer le MD5 du fichier des groupes
        String md5 = this.clientFileManager.md5Checksum("groups");
        // Envoyer la requete au serveur
        ServerResponse<List<Group>> groupList = this.localServerStub.getGroupList(md5, token);
        if (!groupList.isSuccessful()) {
            System.out.println(groupList.getErrorMessage());
            return;
        }
        // Ecraser les groupes existantes
        this.groups = new HashMap<>();
        for (Group g : groupList.getData()) {
            this.groups.put(g.getName(), g);
        }
        // Ecrire la liste sur le fichier
        this.writeGroupFile();
    }

    private int printEmailList(List<EmailMetadata> list, boolean ret) {
        int index = 1;
        int nr = 0;
        StringBuilder sb = new StringBuilder();
        for (EmailMetadata data : list) {
            if (ret) sb.append(index).append("\t");
            sb.append((data.isRead()) ? "-" : "N").append("\t");
            sb.append(data.getFrom()).append("\t");
            sb.append(data.getDate()).append("\t");
            sb.append(data.getSubject()).append("\n");
            index++;
            if (!data.isRead()) nr++;
        }

        System.out.println(index - 1 + " courriers dont " + nr + " non-lus.");
        System.out.println(sb.toString());

        return index;
    }

    private int getLineNumber(String message, int up) {
        System.out.print("\n" + message);
        Scanner scanner = new Scanner(System.in);
        int ret = -1;
        try {
            ret = scanner.nextInt();
            if (ret < 0 || ret > up) ret = getLineNumber(message, up);
        } catch (Exception e) {
            ret = getLineNumber(message, up);
        }
        return ret;
    }

    private void writeGroupFile() {
        if (this.clientFileManager.isReadable("groups")) {
            try {
                this.clientFileManager.deleteFile("groups");
            } catch (IOException e) {
            }
        }
        this.groups.forEach((String k, Group v) -> {
            try {
                this.clientFileManager.writeSerializeableObject("groups", v, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void getGroupFromLocalFile() {
        try {

            Collection<Object> objects = this.clientFileManager.readSerializeableObjects("groups");

            objects.forEach((o) -> {
                Group g = (Group) o;
                this.groups.put(g.getName(), g);
            });

        } catch (IOException e) {
        }
    }
}
