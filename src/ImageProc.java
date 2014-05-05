/**
 *
 * This is a utility class for loading images and reference colors from the database.
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;

public class ImageProc {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private ResultSet colorSet = null;
    private Statement insertColorStatement;
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
            statement = connect.createStatement();
            insertColorStatement = connect.createStatement();
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
        try {
            //Where Textile_imd_id > startingImageId
            resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE Img_type_cd='Full Light' ORDER BY Textile_img_id");
            resultSet.first();
        }
        catch (SQLException er ) {
            System.err.println("[ERROR] Could not issue statements to the database.");
            if (debug) er.printStackTrace();
        }
    }

    public HashMap<ColorItem, Integer> loadRefColors(String table) {
        try {
            colorSet = statement.executeQuery("SELECT * FROM " + table);
            colorSet.first();

            HashMap<ColorItem, Integer> map = new HashMap<>();
            while (!colorSet.isAfterLast()) {
                int red = colorSet.getInt("RGB_R");
                int green = colorSet.getInt("RGB_G");
                int blue = colorSet.getInt("RGB_B");
                map.put(new ColorItem(colorSet.getInt("Color_detail_id"), new Color(red, green, blue)), 0);
                colorSet.next();
            }
            if (debug) System.out.println("Loaded " + map.keySet().size() + " colors from the database.");
            return map;
        }
        catch(SQLException er) {
            System.err.println("[ERROR] Failed to load color table.");
            if (debug) er.printStackTrace();
        }
        return null;
    }

    public boolean hasNextImage() {
        try {
            return !resultSet.isAfterLast();
        }
        catch (SQLException er) {
            if (debug) er.printStackTrace();
        }
        return false;
    }

    /**
     * Reads a single image from a file and creates an ImageItem.
     * @return
     */
    public static ImageItem readImageFromFile(String filename) {
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            return new ImageItem(filename.substring(0, filename.length()-4), image);
        }
        catch(IOException er ) {
            System.err.println("[ERROR] Unable to read image file.");
        }
        return null;
    }

    /**
     * Returns the next image from the resultSet as a BufferedImage for processing.
     * @return
     */
    public ImageItem nextImage() {
        if (resultSet != null) {
            try {
                Blob imageBlob = resultSet.getBlob(imageField);
                ImageItem retval = new ImageItem(resultSet.getInt("Textile_img_id"),ImageIO.read(imageBlob.getBinaryStream(1, imageBlob.length())));
                resultSet.next();
                return retval;
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

    public void insertColorMatch(ImageItem imageItem, ColorItem colorItem) {
        try {
            ResultSet temp = insertColorStatement.executeQuery("SELECT Textile_inst_ID FROM VTMaster.IMG_hdr,VTMaster.IMG_detail where Textile_img_id = "+ imageItem.textile_img_id + " and Textile_img_hdr_id = IMG_hdr_id");
            temp.first();
            int textile_inst_id = temp.getInt("Textile_inst_id");
            insertColorStatement.execute("INSERT INTO Textile_color_detail VALUES (" + textile_inst_id + "," + colorItem.color_detail_id + ", 'Active')");
        }
        catch (SQLException er) {
            System.err.println("[ERROR] Failed to insert color values into Textile Color Detail table.");
            if (debug) er.printStackTrace();
        }
    }
}

class ColorItem {
    Color color;
    int color_detail_id;

    public ColorItem(int color_detail_id, Color color) {
        this.color = color;
        this.color_detail_id = color_detail_id;
    }
}
