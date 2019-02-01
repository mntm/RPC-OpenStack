package ca.polymtl.inf8480.tp1.client;
import ca.polymtl.inf8480.tp1.shared.*;

import ca.polymtl.inf8480.tp1.shared.FileManager;
import ca.polymtl.inf8480.tp1.shared.ServerInterface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class Client implements Runnable {

    // Attributes
    private ServerInterface localServerStub = null;
    private FileManager clientFileManager = new FileManager("client_files");
    private List<Group> groups = null;
    private String[] args = null;

    public Client() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            //{{ Initializes connection to the auth server and the file server
            this.localServerStub = (ServerInterface) loadServerStub("127.0.0.1", "server");
            //}}
            //groups = localServerStub.getGroupList();
        } catch (Exception e) {
        }

        if (this.localServerStub == null) {
            System.err.println("Impossible de joindre le serveur de fichier!");
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        if (args.length >= 1) {
            client.set(args);
        }

        new Thread(client).start();
    }

    /**
     * Print program usage.
     *
     * @param msg String to print before the usage is printed.
     */
    private static void printUsage(final String msg) {
        System.out.println(msg);
        System.out.println("USAGE:");
        System.out.println("    send <filename>");
        System.out.println("    list");
        System.out.println("    syncLocalDirectory");
        System.out.println("    get <filename>");
        System.out.println("    lock <filename>");
        System.out.println("    push <filename>");
    }

    public void set(final String[] args) {
        this.args = args;
    }

    @Override
    public void run() {
        this.run(this.args);
    }

    /**
     * Parse les arguments et execute la commande appropriee
     *
     * @param args String []
     */
    private void run(final String[] args) {
        try {
            switch (args[0]) {
                case "send":
                    if (args.length != 2) {
                        Client.printUsage("The `send` command requires 2 argument.");
                        return;
                    }
//                    this.send();
                    break;
                case "read":
//                    this.read();
                    break;
                case "delete":
//                    this.delete();
                    break;
                case "search":
//                    this.search();
                    break;
                case "create-group":
//                    this.createGroup();
                    break;
                case "join-group":
//                    this.joinGroup();
                    break;
                case "publish-group-list":
                    this.publishGroupList();
                    break;

                case "lock-group-list":
                    this.lockGroupList();
                    break;

                case "help":
                    Client.printUsage("Invalid command");
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

    private String readMD5(String fileName) {
        // Lire le fichier md5 si il existe, sinon, retourner null
        String md5 = null;
        try (BufferedReader br = this.clientFileManager.newBufferedReader("." + fileName + ".md5")) {
            md5 = br.readLine();
        } catch (IOException e) {
            System.err.println("Could not read MD5 of file: " + fileName);
        }

        return md5;
    }

    private String generateAndWriteMD5(String fileName) {
        String md5 = null;
        try (BufferedWriter fmd5 = this.clientFileManager.newBufferedWriter("." + fileName + ".md5")) {
            md5 = this.clientFileManager.md5Checksum(fileName);
            fmd5.write(md5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return md5;
    }

    public void send(String to, String subject, String content) {
        //localServerStub.sendEmail(to, subject, content);
    }


    public void read(int id) {
        //localServerStub.readMail(id);
    }


    public void delete(int id) {
        //localServerStub.deleteMail(id);
    }


    public void search(String[] kwds) {
        //localServerStub.searchMail(kwds);
    }


    public void createGroup(String grpName) {
        Group group = new Group(grpName);
        groups.add(group);
    }


    public void joinGroup(String grpName, String user) {
        Group group = new Group(grpName);
        group.addMember(user);
        groups.add(group);
    }


    public void publishGroupList() throws RemoteException {
        lockGroupList();
        //localServerStub.pushGroupList(groups);
    }


    public void lockGroupList() throws RemoteException {
        //localServerStub.lockGroupList();
    }
}
