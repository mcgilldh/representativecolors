import javax.swing.*;

/**
 * Created by petdav on 06/06/14.
 */
public class DebugUtil {

    JTextArea output;
    public DebugUtil(JTextArea outputArea) {
        output = outputArea;
    }

    public void debug(String message, boolean debug) {
        if (debug) {
            System.out.println("[DEBUG] " + message);
            output.append("[DEBUG] " + message + "\n");
        }
    }

    public void output(String message) {
        System.out.println(message);
        output.append(message + "\n");
    }

    public void error(String message) {
        System.out.println("[ERROR] " + message);
        output.append("[ERROR] "+ message + "\n");
    }

    public void error(String message, boolean debug, Exception er) {
        error(message);
        if (debug) {
            er.printStackTrace();
            output.append(er.getStackTrace().toString()+"\n"); //TODO: Test to make sure this works as expected
        }
    }
}
