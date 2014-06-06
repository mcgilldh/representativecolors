/**
 *
 * This is a utility class for loading images and reference colors from the database.
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
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
    DebugUtil debugUtil;

    boolean debug;

    public ImageProc(String server, String database, String user, String password, String imageField, boolean debug, DebugUtil debugUtil) {
        this.debug = debug;
        this.debugUtil = debugUtil;
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
        debugUtil.debug("Connecting to the database.", debug);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://"+server+"/"+database+"?user="+user+"&password="+password);
            statement = connect.createStatement();
            insertColorStatement = connect.createStatement();
        }
        catch (ClassNotFoundException er ) {
            debugUtil.error("Couldn't load mysql jdbc driver. Is it installed?", debug, er);
        }
        catch(SQLException er) {
            debugUtil.error("Could not connect to database " + database, debug, er);
        }
    }

    /**
     * Launches the query to fetch all images from the database.
     * @param table The table in which the images are stored.
     */
    public void fetchImages(String table, String startID, String endID) {
        debugUtil.debug("Fetching images.", debug);
        try {
            //Where Textile_imd_id > startingImageId
            if (startID.length() == 0 || endID.length() == 0)
                resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE Img_type_cd='Full Light' ORDER BY Textile_img_id");
            else
                resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE Img_type_cd='Full Light' " +
                        "AND VT_tracking BETWEEN CAST('"+startID+"' as char(30)) AND CAST('"+endID+"' as char(30)) ORDER BY Textile_img_id");
            resultSet.first();
        }
        catch (SQLException er ) {
            debugUtil.error("Could not issue statements to the database.", debug, er);
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
            debugUtil.debug("Loaded " + map.keySet().size() + " colors from the database.", debug);
            return map;
        }
        catch(SQLException er) {
            debugUtil.error("Failed to load color table.", debug, er);
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
    public ImageItem readImageFromFile(String filename) {
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            String tracking = filename.substring(0,filename.indexOf("."));
            return new ImageItem(getTextileImgID(tracking), tracking, image);
        }
        catch(IOException er ) {
            debugUtil.error("Unable to read image file.", debug, er);
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
                ImageItem retval = new ImageItem(resultSet.getInt("Textile_img_id"),resultSet.getString("VT_tracking"),ImageIO.read(imageBlob.getBinaryStream(1, imageBlob.length())));
                resultSet.next();
                return retval;
            }
            catch (SQLException er) {
                debugUtil.error("Could not fetch next image from query results. Did you fetch the images first?", debug, er);
            }
            catch(IOException er) {
                debugUtil.error("Could not read image blob. Did you select the right field?", debug, er);
            }
        }
        else {
            debugUtil.error("Images were not fetched from the database before attempting to fetch the next image. Please contact developer.");
        }
        return null;
    }

    public void insertColorMatch(ImageItem imageItem, ColorItem colorItem, BufferedWriter out) {
        insertColorMatch(imageItem.textile_img_id, colorItem, out);
    }
    public void insertColorMatch(int textile_img_id, ColorItem colorItem, BufferedWriter out) {
        try {
            ResultSet temp = insertColorStatement.executeQuery("SELECT Textile_inst_ID FROM VTMaster.IMG_hdr,VTMaster.IMG_detail where Textile_img_id = "+ textile_img_id + " and Textile_img_hdr_id = IMG_hdr_id");
            temp.first();
            int textile_inst_id = temp.getInt("Textile_inst_id");

            if (out == null) {
                insertColorStatement.execute("INSERT INTO Textile_color_detail VALUES (" + textile_inst_id + ",NULL, NULL, NULL, NULL," + colorItem.color_detail_id + ")");
            }
            else {
                try {
                    out.write("INSERT INTO Textile_color_detail VALUES (" + textile_inst_id + ",NULL, NULL, NULL, NULL," + colorItem.color_detail_id + ");\n");
                }
                catch (IOException er ) {
                    debugUtil.error("Could not write to SQL script file.", debug, er);
                }
            }
        }
        catch (SQLException er) {
            debugUtil.error("Failed to insert color values into Textile Color Detail table.", debug, er);
        }
    }

    public int getTextileImgID(String vtTracking) {
        try {
            ResultSet temp = statement.executeQuery("SELECT Textile_img_id FROM VTMaster.IMG_detail WHERE VTTracking='" + vtTracking + "'");
            temp.first();
            return temp.getInt("Textile_inst_id");
        }
        catch (SQLException er) {
            debugUtil.error("Could not locate VTTracking id: "+ vtTracking, debug, er);
        }
        return -1;
    }
}

class ColorItem {
    Color color;
    int color_detail_id;

    public ColorItem(int color_detail_id, Color color) {
        this.color = color;
        this.color_detail_id = color_detail_id;
    }

    public String toString() {
        return "(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }
}
