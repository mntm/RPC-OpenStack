package ca.polymtl.inf8480.tp1.client;

import ca.polymtl.inf8480.tp1.shared.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class Client implements Runnable {

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
            //{{ Initializes connection to the auth server and the file server
            this.localServerStub = (ServerInterface) loadServerStub("127.0.0.1", "server");
            FileManager tokenFile = new FileManager("client");
            if(tokenFile.isReadable("tokenFile")){
                getToken();
            }
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
        System.out.println("    read");
        System.out.println("    delete");
        System.out.println("    list [-ur]");
		System.out.println("    search <words>");
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
                    opensession(userName,password);
                    break;
                case "send":
                    if (args.length < 4) {
                        Client.printUsage("The `send` command requires 4 argument. type help for more infos");
                        return;
                    }
					String subject = args[2];
					String addrDest = args[3];
					String content = args[4];
                    send(subject, addrDest, content);
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
                    for (int i = 1; i < args.length; i++) { 
                        keywords[i-1] = args[i]; 
                    } 
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

    public void getToken(){
        try(BufferedReader pw = clientFileManager.newBufferedReader("tokenFile")){
            token = pw.readLine();
        }catch(IOException e){} 
    }

    public void opensession(String userName, String password) throws RemoteException {
        ServerResponse response = localServerStub.openSession(userName, password);
        if(!response.isSuccessful()){ System.err.println(response.getErrorMessage());}
        else{
            token = (String)response.getData();
            try(BufferedWriter pw = clientFileManager.newBufferedWriter("tokenFile")){
                pw.write(token);
            }catch(IOException e){} 
        }
    }

    public void send(String subject, String addrDest, String content) throws RemoteException {
        ServerResponse response = localServerStub.sendEmail(addrDest, subject, content, token);
        if(!response.isSuccessful()){ System.err.println(response.getErrorMessage());}
    }


    public void read() throws RemoteException {
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


    public void delete() throws RemoteException {
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


    public void search(String[] keywords) throws RemoteException {
        ServerResponse response = localServerStub.searchMail(keywords, token);
        if(!response.isSuccessful()){ System.err.println(response.getErrorMessage());}
        else{
            this.printEmailList((List<EmailMetadata>) response.getData(), false);
        }
    }
	
	public void list(boolean justUnread) throws RemoteException {
        ServerResponse response = localServerStub.listMails(justUnread, token);
        if(!response.isSuccessful()){ System.err.println(response.getErrorMessage());}
        else{
            printEmailList((List<EmailMetadata>) response.getData(), false);
        }
    }

    public void createGroup(String groupName) throws RemoteException {
        Group group = new Group(groupName);
        this.groups.put(groupName,group);
        writeGroupFiles();
    }

    public void getGroupFromLocalFile(){
        try (BufferedReader reader = clientFileManager.newBufferedReader("groups");) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(":");
                Group g = new Group(split[0]);

                for (String user : split[1].split(",")) {
                    g.addMember(user);
                }
                this.groups.put(split[0], g);
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire le fichier ");
        }
    }

    public void joinGroup(String groupName, String user) throws RemoteException {
        try {
            BufferedReader reader = clientFileManager.newBufferedReader("groups");
            int lineNum = 0;
            String line;
            List<String> lines = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                if(line.contains(groupName)) { 
                    line = line + "," + user;
                }
                lines.add(line);
            }
            try(BufferedWriter pw = clientFileManager.newBufferedWriter("groups")){
                for(int i = 0; i < lines.size();i++){
                    pw.write(lines.get(i));
                }
            }catch(IOException e){}
        } catch(Exception e) { 
            System.out.println("Erreur");
        }
    }

    public void publishGroupList() throws RemoteException {
        getGroupFromLocalFile();
        List<Group> groupList = new ArrayList<Group>(groups.values());
        ServerResponse response = localServerStub.pushGroupList(groupList, token);
        if(!response.isSuccessful()){ System.err.println(response.getErrorMessage());}
    }


    public void lockGroupList() throws RemoteException {
        ServerResponse response = localServerStub.lockGroupList(token);
        if(!response.isSuccessful()){ System.err.println(response.getErrorMessage());}
        else{ System.err.println((String)response.getData());}
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

        System.out.println(index - 1 + "courriers dont " + nr + "non-lus.");
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

    private synchronized void writeGroupFiles() {
        // <nom_du_groupe>:user1,user2,user3

        try (BufferedWriter bw = clientFileManager.newBufferedWriter("groups")) {
            this.groups.forEach((String k, Group v) -> {
                try {
                    bw.write(v.toString());
                    bw.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
