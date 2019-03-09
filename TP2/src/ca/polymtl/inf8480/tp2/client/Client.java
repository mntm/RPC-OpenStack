package ca.polymtl.inf8480.tp2.client;

import ca.polymtl.inf8480.tp2.shared.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client: Se connecte a un distributeur de charge, lis un fichier en parametre
 * et envoie les operations au distributeur de charge
 */
public class Client {
    // Les operations doivent etres placees dans le dossier client_files
    private FileManager fm = new FileManager("client_files");


    /**
     * Parametres:
     * nom du fichier contenant les operations;
     * adresse ip du distributeur de charge
     * port sur lequel le distributeur ecoute
     * non du distributeur
     *
     * @param args parametres
     */
    public static void main(String[] args) {
        RMIUtils.initSecurityManager();
        Client client = new Client();

        if (args.length < 4) {
            client.usage("");
        }

        client.run(args);
    }

    private void run(String[] args) {
        // Read the file
        if (!this.fm.isReadable(args[0])) {
            usage("Fichier introuvable: " + args[0]);
        }
        Task task = new Task();

        try {
            BufferedReader br = this.fm.newBufferedReader(args[0]);
            String line;
            while ((line = br.readLine()) != null) {
                Pattern pattern = Pattern.compile("(prime|pell)\\s*(\\d+)");
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    OperationType type = (matcher.group(1).equals("pell")) ? OperationType.PELL : OperationType.PRIME;
                    int operand = Integer.parseInt(matcher.group(2));
                    TaskElement element = new TaskElement(type, operand);
                    task.addOperation(element);
                }
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get LB's stub
        ILoadBalancer stub = (ILoadBalancer) RMIUtils.getStub(args[2], Integer.parseInt(args[3]), args[1]);
        if (stub == null) {
            usage("Impossible de contacter le service");
        }
        // Send the tasks
        try {
            ServerResponse<Integer> execute = stub.execute(task);
            if (execute.isSuccessful()) {
                System.out.println("Resultat: " + execute.getData());
            } else {
                System.err.println(execute.getErrorMessage());
            }
        } catch (RemoteException e) {
            usage("Une erreur s'est produite lors de l'envoie de la tache au serveur");
            e.printStackTrace();
        }
    }

    private void usage(String msg) {
        System.out.println(msg);
        System.out.println("client.sh <fichier operation> <nom distributeur> <adresse distributeur> <port distributeur>\n");
        System.exit(-1);
    }
}
