package ca.polymtl.inf8480.tp2.shared;

/**
 * Created by marak on 19-03-12.
 */
public class Debug {
    private static final boolean DEBUG = false;

    public static void print(String m) {
        if (DEBUG)
            System.err.println("\033[31;1;4m[DBG] - " + m + "\033[0m");
    }

}
