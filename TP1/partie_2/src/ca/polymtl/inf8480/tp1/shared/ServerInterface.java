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

    ServerResponse<List<Group>> getGroupList(String md5, String token) throws RemoteException;

    ServerResponse<String> pushGroupList(List<Group> groups, String token) throws RemoteException;

    ServerResponse<String> lockGroupList(String token) throws RemoteException;

    ServerResponse<String> sendEmail(String to, String subj, String content, String token) throws RemoteException;

    ServerResponse<List<Email>> listMails(boolean justUnread, String token) throws RemoteException;

    ServerResponse<String> readMail(int id, String token) throws RemoteException;

    ServerResponse<String> deleteMail(int id, String token) throws RemoteException;

    ServerResponse<List<Email>> searchMail(String[] keywords, String token) throws RemoteException;
}

