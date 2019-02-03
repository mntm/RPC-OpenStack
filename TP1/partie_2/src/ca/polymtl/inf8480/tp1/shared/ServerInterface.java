package ca.polymtl.inf8480.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface representing the server API.
 */
public interface ServerInterface extends Remote {

    /**
     * Open a session for the user
     *
     * @param login
     * @param password
     * @return
     */
    ServerResponse<String> openSession(String login, String password) throws RemoteException;

    /**
     * Retourne un objet ServerResponse contenant la liste des groupes global
     *
     * @param md5   Hash de la liste de groupe local a l'utilisateur; utilise pour savoir si la liste globale est differente de la sienne
     * @param token jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<List<Group>> getGroupList(String md5, String token) throws RemoteException;

    /**
     * Envoi la liste de groupe local vers le serveur.
     * L'appel de cette fonction ecrase la version presente sur le serveur.
     * <p>
     * La methode lockGroupList doit etre appelee avant d'appeler cette methode.
     *
     * @param groups liste des groupes local
     * @param token  jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<String> pushGroupList(List<Group> groups, String token) throws RemoteException;

    /**
     * Verouille la liste des groupes globale;
     * <p>
     * Echoue si la liste est deja verrouillee par un autre utilisateur
     *
     * @param token jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<String> lockGroupList(String token) throws RemoteException;

    /**
     * Envoie un courriel vers un utilisateur ou groupe d'utilisateur
     *
     * @param to      destinataire
     * @param subj    objet du courriel
     * @param content contenu du courriel
     * @param token   jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<String> sendEmail(String to, String subj, String content, String token) throws RemoteException;

    /**
     * Retourne un ServerResponse contenant la liste des courriels present dans la boite de reception de l'utilisateur
     *
     * @param justUnread pour ne renvoyer que les courriels non lus
     * @param token      jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<List<EmailMetadata>> listMails(boolean justUnread, String token) throws RemoteException;

    /**
     * Lis le contenu d'un courriel
     *
     * @param metadata meta-donnees obtenu par l'appel de listMails ou searchMail
     * @param token    jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<String> readMail(EmailMetadata metadata, String token) throws RemoteException;

    /**
     * Supprime un courriel. Il sera impossible de le recuperer apres suppression
     *
     * @param metadata meta-donnees obtenu par l'appel de listMails ou searchMail
     * @param token    jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<String> deleteMail(EmailMetadata metadata, String token) throws RemoteException;

    /**
     * Retourne la liste des courriels qui contient un ou plusieurs mots cles
     *
     * @param keywords
     * @param token    jeton de connexion obtenue lors de l'appel de openSession
     * @return
     * @throws RemoteException
     */
    ServerResponse<List<EmailMetadata>> searchMail(String[] keywords, String token) throws RemoteException;
}

