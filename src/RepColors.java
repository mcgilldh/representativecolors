
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * Processes images and finds representative colors.
 */
public class RepColors {
    boolean debug = false;
    HashMap<ColorItem, Integer> refColors;
    double threshold;
    ImageItem currentImage;
    DebugUtil debugUtil;

    public RepColors(DebugUtil debugUtil) {
        threshold = 5; //The default threshold
        this.debugUtil = debugUtil;
    }

    /**
     * Finds all the unique colors in an image, and then returns a LinkedList containing them.
     * @param imageItem
     * @return
     */
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

        debugUtil.debug("Number of unique pixels: " + pixels.size(), debug);

        //Pull the pixels out again, and compute the distance
        for (Color temp : pixels) {
            for (ColorItem ref : refColors.keySet()) {
                if (colorDistance(temp, ref.color) < threshold) refColors.put(ref, refColors.get(ref) + 1);
            }
        }

        debugUtil.debug("Sorting results.", debug);
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
    public void insertColors(ImageProc proc, int numColors, LinkedList<ColorCount> colors) {
        for (int i = 0; i < numColors; i ++) {
            ColorCount tempcount = colors.pop();
            ColorItem temp = tempcount.c;
            debugUtil.debug("Color " + i + " has score " + tempcount.i, debug);
            proc.insertColorMatch(currentImage, temp, null);
        }
    }

    public void writeColors(ImageProc proc, int numColors, LinkedList<ColorCount> colors, BufferedWriter out) {
        for (int i = 0; i < numColors; i ++) {
            ColorCount tempcount = colors.pop();
            ColorItem temp = tempcount.c;
            debugUtil.debug("Color " + i + " has score " + tempcount.i, debug);
            proc.insertColorMatch(currentImage, temp, out);
        }
    }

    public static double colorDistance(Color a, Color b) {
        return Math.sqrt(Math.pow(a.getRed()-b.getRed(), 2) + Math.pow(a.getGreen()-b.getGreen(), 2) + Math.pow(a.getBlue()-b.getBlue(),2));
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

    public String toString() {
        return c.toString();
    }
}
