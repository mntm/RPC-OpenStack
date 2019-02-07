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
    private String token = null;

    public Client() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            //{{ Initializes connection to the auth server and the file server
            this.localServerStub = (ServerInterface) loadServerStub("127.0.0.1", "server");
            //}}
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
        System.out.println("    opensession -u <UserName> -p <Password>");
        System.out.println("    send -s \"<subject>\" <emailAdress> <Content>");
        System.out.println("    read <idEmail>");
        System.out.println("    delete <idEmail>");
		System.out.println("    list -ur");
		System.out.println("    search <word>");
        System.out.println("    lock-group-list");
        System.out.println("    create-group <groupName> --descr <groupDescription>");
        System.out.println("    join-group <groupName> -u <userName>");
		System.out.println("    publish-group-list");
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
                case "opensession":
                    if (args.length < 4) {
                        Client.printUsage("The `send` command requires 4 argument. type help for more infos");
                        return;
                    }
                    String userName = args[2];
                    String password = args[4];
                    this.opensession(userName,password);
                    break;
                case "send":
                    if (args.length < 4) {
                        Client.printUsage("The `send` command requires 4 argument. type help for more infos");
                        return;
                    }
					String subject = args[2];
					String addrDest = args[3];
					String content = args[4];
//                    this.send(subject, addrDest, content);
                    break;
                case "read":
					if (args.length > 1) {
                        Client.printUsage("The `read` command requires 0 or 1 argument. type help for more infos");
                        return;
                    }
					String idRead = args[1];
//                    this.read(idRead);
                    break;
                case "delete":
					if (args.length > 1) {
                        Client.printUsage("The `delete` command requires 0 or 1 argument. type help for more infos");
                        return;
                    }
					String idDelete = args[1];
//                    this.delete(idDelete);
                    break;
				case "list":
					if (args.length > 1) {
                        Client.printUsage("The `delete` command requires 0 or 1 argument. type help for more infos");
                        return;
                    }
					String justUnread = args[1];
//                    this.list(justUnread);
                    break;
                case "search":
					if (args.length != 1) {
                        Client.printUsage("The `search` command requires 1 argument. type help for more infos");
                        return;
                    }
					String keywords = args[1];
//                    this.search(keywords);
                    break;
                case "create-group":
					if (args.length != 3) {
                        Client.printUsage("The `create-group` command requires 1 argument. type help for more infos");
                        return;
                    }
					String groupName = args[1];
					String groupDescr = args[3];
//                    this.createGroup(groupName, groupDescr);
                    break;
                case "join-group":
					if (args.length != 3) {
                        Client.printUsage("The `join-group` command requires 1 argument. type help for more infos");
                        return;
                    }
					String joinGroupName = args[1];
					String joinUserName = args[3];
//                    this.joinGroup(joinGroupName, joinUserName);
                    break;
                case "publish-group-list":
					if (args.length > 0) {
                        Client.printUsage("The `publish-group-list` command requires 0 argument. type help for more infos");
                        return;
                    }
//                    this.publishGroupList();
                    break;

                case "lock-group-list":
				if (args.length > 0) {
                        Client.printUsage("The `lock-group-list` command requires 0 argument. type help for more infos");
                        return;
                    }
//                   this.lockGroupList();
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

    public void opensession(String userName, String password) throws RemoteException {
        ServerResponse response = localServerStub.openSession(userName, password);
        if(!response.isSuccessful()){
            System.err.println(response.getErrorMessage());
        }
        else{
            token = (String)response.getData();
            FileManager tokenFile = new FileManager("client");
            try(BufferedWriter pw = tokenFile.newBufferedWriter("tokenFile")){
                pw.write(token);
            }catch(IOException e){} 
        }
    }

    public void send(String subject, String addrDest, String content) {
        //localServerStub.sendEmail(to, subject, content);
    }


    public void read(int id) {
        //localServerStub.readMail(id);
    }


    public void delete(int id) {
        //localServerStub.deleteMail(id);
    }


    public void search(String keywords) {
        //localServerStub.searchMail(kwds);
    }
	
	public void list(boolean justUnread) {
        //localServerStub.list(justUnread);
    }

    public void createGroup(String groupName) {
        Group group = new Group(groupName);
        groups.add(group);
    }


    public void joinGroup(String groupName, String user) {
        Group group = new Group(groupName);
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
