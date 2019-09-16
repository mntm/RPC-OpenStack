package ca.polymtl.inf8480.tp1.shared;

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

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
