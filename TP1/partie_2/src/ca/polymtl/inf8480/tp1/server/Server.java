package ca.polymtl.inf8480.tp1.server;

import ca.polymtl.inf8480.tp1.shared.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.*;

/**
 * Class representing the file server.
 */
public class Server implements ServerInterface {

    private static final String ALPHA_NUMERIC_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final String SERVER_DIR_NAME = "server/";
    private final String LOGIN_FILE = "legit_users";
    private final String GROUP_FILE = "groups";
    private final String GROUP_LOCK = ".groups.lock";
    private final String EMAIL_META_FILE = ".emails.dat";

    private final int EMAIL_SUCCESS = 0;
    private final int EMAIL_META_ERROR = 2;
    private final int EMAIL_CONTENT_ERROR = 4;

    private HashMap<String, String> logins = new HashMap<>(); // keeps the logins and passwords
    private HashMap<String, Group> groups = new HashMap<>(); // holds the group name and its members
    private HashMap<String, String> users = new HashMap<>(); // holds connexion token and the associated login

    private FileManager serverFileManager;

    public Server() {
        super();
        this.serverFileManager = new FileManager(this.SERVER_DIR_NAME);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    private void readLoginFiles() {
        // <login>:<password>

        try (BufferedReader br = this.serverFileManager.newBufferedReader(LOGIN_FILE)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(":");
                this.logins.put(split[0], split[1]);
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire le fichier " + LOGIN_FILE);
        }
    }

    private void writeLoginFiles() {
        // <login>:<password>

        try (BufferedWriter bw = this.serverFileManager.newBufferedWriter
                (LOGIN_FILE, StandardOpenOption.TRUNCATE_EXISTING)) {
            this.logins.forEach((k, v) -> {
                StringBuilder sb = new StringBuilder();
                sb.append(k).append(":").append(v);
                try {
                    bw.write(sb.toString());
                    bw.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void readGroupFiles() {
        // <nom_du_groupe>:user1,user2,user3

        try (BufferedReader br = this.serverFileManager.newBufferedReader(GROUP_FILE)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(":");
                Group g = new Group(split[0]);

                for (String user : split[1].split(",")) {
                    g.addMember(user);
                }

                this.groups.put(split[0], g);
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire le fichier " + GROUP_FILE);
        }
    }

    private synchronized void writeGroupFiles() {
        // <nom_du_groupe>:user1,user2,user3

        try (BufferedWriter bw = this.serverFileManager.newBufferedWriter
                (GROUP_FILE, StandardOpenOption.TRUNCATE_EXISTING)) {
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

    /**
     * Run the file server.
     */
    private void run() {

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            ServerInterface stub = (ServerInterface) UnicastRemoteObject
                    .exportObject(this, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("server", stub);
            System.out.println("Server ready.");
        } catch (ConnectException e) {
            System.err
                    .println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
            System.err.println();
            System.err.println("Erreur: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        this.readLoginFiles();
        this.readGroupFiles();
    }

    @Override
    public ServerResponse<String> openSession(String login, String password) {
        ServerResponse<String> response = new ServerResponse<>();
        if ((this.logins.containsKey(login)) && (this.logins.get(login).equals(password))) {

            String token = "";
            if (this.users.containsValue(login)) {
                for (Map.Entry<String, String> entry : this.users.entrySet()) {
                    if (entry.getValue().equals(login))
                        token = entry.getKey();
                }
            } else {
                token = this.generateToken();
                this.users.put(token, login);
            }
            response.setSuccessful(true);
            response.setData(token);
        } else {
            response.setSuccessful(false);
            response.setErrorMessage("La combinaison login/password est non valide");
        }

        return response;
    }

    @Override
    public ServerResponse<List<Group>> getGroupList(String md5, String token) {
        ServerResponse<List<Group>> response = new ServerResponse<>();
        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }
        // Generate GROUP_FILE md5 and compare it with md5
        if (this.serverFileManager.md5Checksum(GROUP_FILE).equals(md5)) {
            response.setSuccessful(false);
            response.setErrorMessage("Votre liste de groupe est a jour");
            return response;
        }

        this.readGroupFiles();

        response.setSuccessful(true);
        response.setData((List<Group>) this.groups.values());

        return response;
    }

    @Override
    public synchronized ServerResponse<String> lockGroupList(String token) {
        ServerResponse<String> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        if (this.serverFileManager.isReadable(GROUP_LOCK)) {
            String s = this.serverFileManager.readFile(GROUP_LOCK);
            response.setSuccessful(false);
            response.setErrorMessage("La liste de groupes globale est déjà verrouillée par " + s);
            return response;
        }

        try (BufferedWriter bw = this.serverFileManager.newBufferedWriter(
                GROUP_LOCK, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(this.users.get(token));
        } catch (IOException e) {
            e.printStackTrace();
        }

        response.setSuccessful(true);
        response.setData("La liste de groupes globale est verrouillée avec succès.");
        return response;
    }

    @Override
    public synchronized ServerResponse<String> pushGroupList(List<Group> groups, String token) {
        ServerResponse<String> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        if (!this.serverFileManager.isReadable(GROUP_LOCK)) {
            response.setSuccessful(false);
            response.setErrorMessage("La liste de groupes globale n'a pas été verrouillée. " +
                    "Veuillez le faire avant d'executer cette commande.");
            return response;
        } else {
            String s = this.serverFileManager.readFile(GROUP_LOCK);
            if (!s.equals(this.users.get(token))) {
                response.setSuccessful(false);
                response.setErrorMessage("La liste de groupes globale est déjà verrouillée par " + s);
                return response;
            }
        }

        this.groups = new HashMap<>();
        for (Group g :
                groups) {
            this.groups.put(g.getName(), g);
        }

        this.writeGroupFiles();

        // Enlever le verrou
        try {
            this.serverFileManager.deleteFile(GROUP_LOCK);
        } catch (IOException e) {
            e.printStackTrace();
        }

        response.setSuccessful(true);
        return response;
    }

    @Override
    public ServerResponse<String> sendEmail(String to, String subj, String content, String token) {
        ServerResponse<String> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        boolean to_user = this.logins.containsValue(to);
        boolean to_group = this.groups.containsKey(to);

        String sender = this.users.get(token);

        // verifier que le destinataire existe (login ou nom de groupe)
        if ((!to_user) && (!to_group)) {
            response.setSuccessful(false);
            response.setErrorMessage("Destinataire ou groupe de destinataire inconnu...");
            return response;
        }


        // Ecrire le contenue du courriel dans le dossier du destinataire;
        // Si groupe, ecrire dans le dossier de chaque membre.
        StringBuilder sb = new StringBuilder();
        if (to_user) {
            if (this.createEmail(sender, to, subj, content) != EMAIL_SUCCESS) {
                response.setSuccessful(false);
                sb.append("Le courriel n'as pas ete achemine a ").append(to).append(". Reessayez plus tard.");
            }
        }

        if (to_group) {
            sb.append("Envoye du courriel ").append(subj).append(" au groupe ").append(to).append(":");
            for (String u : this.groups.get(to).getMembers()) {
                if (this.createEmail(sender, u, subj, content) != EMAIL_SUCCESS) {
                    response.setSuccessful(false);
                    sb.append("Le courriel n'as pas ete achemine a ").append(u).append(". Reessayez plus tard.").append('\n');
                }
            }
        }

        if (!response.isSuccessful())
            response.setErrorMessage(sb.toString());
        return response;
    }

    @Override
    public ServerResponse<List<EmailMetadata>> listMails(boolean justUnread, String token) {
        ServerResponse<List<EmailMetadata>> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        String user = this.users.get(token);
        FileManager fm = new FileManager(SERVER_DIR_NAME + user);

        List<EmailMetadata> datas = new ArrayList<>();

        try (BufferedReader br = fm.newBufferedReader(EMAIL_META_FILE)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equals("")) continue;
                EmailMetadata meta = EmailMetadata.fromString(line);
                if (meta.isRead() && justUnread) continue;
                datas.add(meta);
            }
            response.setSuccessful(true);
            response.setData(datas);
        } catch (IOException e) {
            e.printStackTrace();
            response.setSuccessful(false);
            response.setErrorMessage("Une erreur s'est produite lors de la collection de la liste de vos courriels.\n" +
                    "Veuillez contacter une personne competente.");
        }

        return response;
    }

    @Override
    public ServerResponse<String> readMail(EmailMetadata metadata, String token) {
        ServerResponse<String> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        String cp = metadata.getContentPath();
        String user = this.users.get(token);

        FileManager fm = new FileManager(SERVER_DIR_NAME + user);

        String s = fm.readFile(cp);

        if (s == null) {
            response.setSuccessful(false);
            response.setErrorMessage("Impossible de lire le contenu du courriel selectionné");
        } else {
            response.setData(s);
        }

        metadata.setRead(true);

        try {
            HashMap<String, EmailMetadata> mets = this.readMetadata(user);
            mets.put(metadata.getContentPath(), metadata);
            this.writeMetadata(mets, user);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la lecture du fichier de metadata");
        }

        response.setSuccessful(true);
        return response;
    }

    @Override
    public synchronized ServerResponse<String> deleteMail(EmailMetadata metadata, String token) {
        ServerResponse<String> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        String user = this.users.get(token);

        FileManager fm = new FileManager(SERVER_DIR_NAME + user);

        // remove the actual file
        try {
            fm.deleteFile(metadata.getContentPath());
        } catch (IOException e) {
            e.printStackTrace();
            response.setSuccessful(false);
            response.setErrorMessage("Un probleme est survenu lors de la suppression du courriel.");
            return response;
        }

        try {
            HashMap<String, EmailMetadata> mets = this.readMetadata(user);
            mets.remove(metadata.getContentPath());
            this.writeMetadata(mets, user);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la lecture du fichier de metadata");
        }

        return response;
    }

    @Override
    public ServerResponse<List<EmailMetadata>> searchMail(String[] keywords, String token) {
        ServerResponse<List<EmailMetadata>> response = new ServerResponse<>();

        if (!this.users.containsKey(token)) {
            response.setSuccessful(false);
            response.setErrorMessage("Vous n'etes pas connecté");
            return response;
        }

        String user = this.users.get(token);

        FileManager fm = new FileManager(SERVER_DIR_NAME + user);

        List<EmailMetadata> data = new ArrayList<>();

        try {
            HashMap<String, EmailMetadata> mets = this.readMetadata(user);
            for (String key : mets.keySet()) {
                String s = fm.readFile(key);
                for (String kwd : keywords) {
                    if (s.contains(kwd)) {
                        data.add(mets.get(key));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        response.setData(data);
        return response;
    }


    private String generateToken() {

        StringBuilder sb = new StringBuilder();
        int count = 16;
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            sb.append(ALPHA_NUMERIC_STRING.charAt(character));

        }
        return sb.toString();
    }

    private int createEmail(String from, String to, String subj, String content) {
        FileManager fm = new FileManager(SERVER_DIR_NAME + to);

        EmailMetadata email = new EmailMetadata(from, subj, Date.from(Instant.now()).toString());

        StringBuilder sb = new StringBuilder();
        sb.append(from).append('_').append(this.generateToken());

        try (BufferedWriter br = fm.newBufferedWriter(sb.toString())) {
            br.write(content);
            br.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            return EMAIL_CONTENT_ERROR;
        }

        email.setContentPath(sb.toString());

        try (BufferedWriter br = fm.newBufferedWriter(EMAIL_META_FILE)) {
            br.write(email.toString());
            br.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            return EMAIL_META_ERROR;
        }

        return EMAIL_SUCCESS;
    }

    private HashMap<String, EmailMetadata> readMetadata(String user) throws IOException {
        HashMap<String, EmailMetadata> datas = new HashMap<>();
        FileManager fm = new FileManager(SERVER_DIR_NAME + user);

        BufferedReader br = fm.newBufferedReader(EMAIL_META_FILE);

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().equals("")) continue;
            EmailMetadata meta = EmailMetadata.fromString(line);
            datas.put(meta.getContentPath(), meta);
        }

        br.close();
        return datas;
    }

    private void writeMetadata(HashMap<String, EmailMetadata> metas, String user) throws IOException {
        FileManager fm = new FileManager(SERVER_DIR_NAME + user);
        BufferedWriter bw = fm.newBufferedWriter(EMAIL_META_FILE, StandardOpenOption.TRUNCATE_EXISTING);

        for (EmailMetadata meta : metas.values()) {
            bw.write(meta.toString());
            bw.newLine();
        }

        bw.close();
    }
}
