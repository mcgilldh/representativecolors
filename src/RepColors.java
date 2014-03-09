import javafx.scene.layout.Priority;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Created by petdav on 3/7/2014.
 */
public class RepColors {
    HashMap<Color, PriorityQueue<Double>> pixels;
    boolean debug = true;
    LinkedList<Color> refColors;

    public RepColors() {
    }

    /**
     * Loads reference colors from a comma separated file, one color per line.
     * @param filename
     */
    public void loadRefColors(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            String[] fields;
            while ((line = in.readLine()) != null ) {
                fields = line.split(",");
                int red = Integer.parseInt(fields[0]);
                int green = Integer.parseInt(fields[1]);
                int blue = Integer.parseInt(fields[2]);
                refColors.add(new Color(red, green, blue));
            }
        }
        catch (IOException er) {
            er.printStackTrace();
        }
    }

    public void processImage(String filename) {
        pixels = new HashMap<Color, PriorityQueue<Double>>();

        BufferedImage image;
        try {
            image = ImageIO.read(new File(filename));

            //Add the pixels to the HashSet
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pixels.put(new Color(image.getRGB(x, y)), new PriorityQueue<Double>(20, Collections.reverseOrder()));
                }
            }

            if (debug) System.out.println("Number of unique pixels: " + pixels.size());

            //Pull the pixels out again, and compute the distance
            Iterator<Color> it = pixels.keySet().iterator();
            while (it.hasNext()) {
                Color temp = it.next();
                for (Color ref : refColors) {
                    //Store the distance in a max heap.
                    pixels.get(temp).add(colorDistance(temp, ref));
                }
            }

            //Take the
        }
        catch (IOException er) {
            er.printStackTrace();
        }
    }

    public static double colorDistance(Color a, Color b) {
        return Math.sqrt(Math.pow(a.getRed()-b.getRed(), 2) + Math.pow(a.getGreen()-b.getGreen(), 2) + Math.pow(a.getBlue()-b.getBlue(),2));
    }

}