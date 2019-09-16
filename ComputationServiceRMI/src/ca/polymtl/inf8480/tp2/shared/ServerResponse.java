package ca.polymtl.inf8480.tp2.shared;

import java.io.Serializable;

/**
 * Object returned by the Server RMI calls.
 * Contains a a boolean that represents if the call was successful
 * and a string containing an error message if the call failed.
 * If everything went wall, the data can be accessed with `getData()`.
 *
 * @param <T>
 */
public class ServerResponse<T> implements Serializable {
    private boolean isSuccessful = true;
    private String errorMessage = null;
    private T data = null;

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public ServerResponse<T> setSuccessful(boolean successful) {
        isSuccessful = successful;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ServerResponse<T> setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public T getData() {
        return data;
    }

    public ServerResponse<T> setData(T data) {
        this.data = data;
        return this;
    }
}
