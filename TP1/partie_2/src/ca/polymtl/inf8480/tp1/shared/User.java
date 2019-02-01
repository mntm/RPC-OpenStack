package ca.polymtl.inf8480.tp1.shared;

import java.io.Serializable;

/**
 * Created by marak on 19-01-30.
 */
public class User implements Serializable {
    private String token;
    private String login;

    public User(String token, String login) {
        this.token = token;
        this.login = login;
    }

    public String getToken() {
        return token;
    }

    public String getLogin() {
        return login;
    }
}
