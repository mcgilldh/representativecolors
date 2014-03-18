/**
 *
 * This is a utility class for loading images and reference colors from the database.
 */
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.*;
import java.util.Date;

public class ImageProc {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private String imageField;

    boolean debug;

    public ImageProc(String server, String database, String user, String password, String imageField, boolean debug) {
        this.debug = debug;
        connect(server, database, user, password);
        this.imageField = imageField;
    }

    /**
     * Connect to a database using the MySQL JDBC driver.
     * @param server
     * @param database
     * @param user
     * @param password
     */
    void connect(String server, String database, String user, String password) {
        if (debug) System.out.println("Connecting to the database.");
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://"+server+"/"+database+"?user="+user+"&password="+password);
        }
        catch (ClassNotFoundException er ) {
            System.err.println("[ERROR] Couldn't load mysql jdbc driver. Is it installed?");
            if (debug) er.printStackTrace();
        }
        catch(SQLException er) {
            System.err.println("[ERROR] Could not connect to database " + database);
            if (debug) er.printStackTrace();
        }
    }

    /**
     * Launches the query to fetch all images from the database.
     * @param table The table in which the images are stored.
     */
    public void fetchImages(String table) {
        if (debug) System.out.println("Fetching images.");
        try{
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM " + table);
            resultSet.first();
        }
        catch (SQLException er ) {
            System.err.println("[ERROR] Could not issue statements to the database.");
            if (debug) er.printStackTrace();
        }
    }

    public boolean hasNextImage() {
        try {
            return !resultSet.isLast();
        }
        catch (SQLException er) {
            if (debug) er.printStackTrace();
        }
        return false;
    }

    /**
     * Returns the next image from the resultSet as a BufferedImage for processing.
     * @return
     */
    public BufferedImage nextImage() {
        if (resultSet != null) {
            try {
                resultSet.next();
                Blob imageBlob = resultSet.getBlob(imageField);
                return ImageIO.read(imageBlob.getBinaryStream(1, imageBlob.length()));
            }
            catch (SQLException er) {
                System.err.println("[ERROR] Could not fetch next image from query results. Did you fetch the images first?");
                if (debug) er.printStackTrace();
            }
            catch(IOException er) {
                System.err.println("[ERROR] Could not read image blob. Did you select the right field?");
                if (debug) er.printStackTrace();
            }
        }
        else {
            System.err.println("[ERROR] Images were not fetched from the database before attempting to fetch the next image.");
        }
        return null;
    }
}
