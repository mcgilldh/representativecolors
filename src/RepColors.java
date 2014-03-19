
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;


/**
 * Processes images and finds representative colors.
 */
public class RepColors {
    boolean debug = false;
    HashMap<ColorItem, Integer> refColors;
    double threshold;
    ImageItem currentImage;

    public RepColors() {
        threshold = 5; //The default threshold
    }

    public LinkedList<ColorCount> processImage(ImageItem imageItem) {
        HashSet<Color>  pixels = new HashSet<>();
        LinkedList<ColorCount> colors;
        currentImage = imageItem;

        //Add the pixels to the HashSet
        for (int x = 0; x < imageItem.image.getWidth(); x++) {
            for (int y = 0; y < imageItem.image.getHeight(); y++) {
                pixels.add(new Color(imageItem.image.getRGB(x, y)));
            }
        }

        if (debug) System.out.println("Number of unique pixels: " + pixels.size());

        //Pull the pixels out again, and compute the distance
        for (Color temp : pixels) {
            for (ColorItem ref : refColors.keySet()) {
                if (colorDistance(temp, ref.color) < threshold) refColors.put(ref, refColors.get(ref) + 1);
            }
        }

        if (debug) System.out.println("Sorting results.");
        //Sort the colors by the most common.
        colors = new LinkedList<>();
        for (ColorItem ref : refColors.keySet()) {
            colors.add(new ColorCount(refColors.get(ref), ref));
        }
        Collections.sort(colors, Collections.reverseOrder());
        return colors;
    }

    /**
     * Writes numColors of the top colors in this image to outFile.
     * @param numColors The number of colors to write
     */
    public void writeColors(ImageProc proc, int numColors, LinkedList<ColorCount> colors) {
        for (int i = 0; i < numColors; i ++) {
            ColorCount tempcount = colors.pop();
            ColorItem temp = tempcount.c;
            if (debug) System.out.println("Color " + i + " has score " + tempcount.i);
            proc.insertColorMatch(currentImage, temp);
        }

        if (debug) System.out.println("Finished.");
    }

    public static double colorDistance(Color a, Color b) {
        return Math.sqrt(Math.pow(a.getRed()-b.getRed(), 2) + Math.pow(a.getGreen()-b.getGreen(), 2) + Math.pow(a.getBlue()-b.getBlue(),2));
    }

    public static void main(String[] args) {
        RepColors col = new RepColors();
        String colorRef = "";
        String imageFile = "";
        String outFile = "";
        int numColors = 10;

        for (int i = 0; i < args.length; i++) {
            switch(args[i]) { //We officially REQUIRE JDK 1.7 or higher now
                case "-r":
                    i++;
                    colorRef = args[i];
                    break;
                case "-o":
                    i++;
                    outFile = args[i];
                    break;
                case "-i":
                    i++;
                    imageFile = args[i];
                    break;
                case "-n":
                    i++;
                    numColors = Integer.parseInt(args[i]);
                    break;
                case "-d":
                    col.debug = true;
                    break;
            }
        }

        //Connect to the database
        ImageProc proc = new ImageProc("localhost", "VTMaster", "testuser", "test", "Textile_img", col.debug);
        //Load representative colors from the database.
        col.refColors = proc.loadRefColors("Color_detail");
        //Pull images from the database
        proc.fetchImages("IMG_detail");

        int i = 0;
        while (proc.hasNextImage()) {
            col.writeColors(proc, numColors, col.processImage(proc.nextImage()));
            i++;
        }

    }

}

/**
 * A simple class for storing color matches in a sort-able way.
 */
class ColorCount implements Comparable<ColorCount>{
    int i;
    ColorItem c;
    public ColorCount(int i, ColorItem c) {
        this.i = i;
        this.c = c;
    }

    public int compareTo(ColorCount other) {
        return i-other.i;
    }
}
