
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

    public RepColors() {
        threshold = 5; //The default threshold
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
    public void insertColors(ImageProc proc, int numColors, LinkedList<ColorCount> colors) {
        for (int i = 0; i < numColors; i ++) {
            ColorCount tempcount = colors.pop();
            ColorItem temp = tempcount.c;
            if (debug) System.out.println("Color " + i + " has score " + tempcount.i);
            proc.insertColorMatch(currentImage, temp, null);
        }

        if (debug) System.out.println("Finished.");
    }

    public void writeColors(ImageProc proc, int numColors, LinkedList<ColorCount> colors, File file) {
        for (int i = 0; i < numColors; i ++) {
            ColorCount tempcount = colors.pop();
            ColorItem temp = tempcount.c;
            if (debug) System.out.println("Color " + i + " has score " + tempcount.i);
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                proc.insertColorMatch(currentImage, temp, out);
            }
            catch (IOException er ) {
                System.err.println("[ERROR] Could not open SQL script file for writing.");
            }
        }

        if (debug) System.out.println("Finished.");
    }

    public static double colorDistance(Color a, Color b) {
        return Math.sqrt(Math.pow(a.getRed()-b.getRed(), 2) + Math.pow(a.getGreen()-b.getGreen(), 2) + Math.pow(a.getBlue()-b.getBlue(),2));
    }

    public static void main(String[] args) {
        RepColors col = new RepColors();
        String startingImageID = "";
        int numColors = 10;
        boolean fileMode = false;
        String inFile = "";

        for (int i = 0; i < args.length; i++) {
            switch(args[i]) { //We officially REQUIRE JDK 1.7 or higher now
                case "-s":
                    i++;
                    startingImageID = args[i];
                    break;
                case "-n":
                    i++;
                    numColors = Integer.parseInt(args[i]);
                    break;
                case "-d":
                    col.debug = true;
                    break;
                case "-f":
                    fileMode = true;
                    i++;
                    inFile = args[i];
                    break;
            }
        }

        //If we're processing an individual file, just print this out on the command-line
        if (fileMode) {
            ImageProc proc = new ImageProc("localhost", "VTMaster", "testuser", "test", "Textile_img", col.debug);
            col.refColors = proc.loadRefColors("Color_detail");
            LinkedList<ColorCount> colors = col.processImage(proc.readImageFromFile(inFile));

            for (int i = 0; i < numColors; i++) {
                ColorCount temp = colors.get(i);
                Color color = temp.c.color;
                System.out.println(String.format("Color %d: (%d, %d, %d)\n", i, color.getRed(), color.getBlue(), color.getGreen()));
            }
        }
        else { //Process images in batch and insert them into the database.
            //Connect to the database
            ImageProc proc = new ImageProc("localhost", "VTMaster", "testuser", "test", "Textile_img", col.debug);
            //Load representative colors from the database.
            col.refColors = proc.loadRefColors("Color_detail");
            //Pull images from the database
            proc.fetchImages("IMG_detail");

            int i = 0;
            while (proc.hasNextImage()) {
                //col.insertColors(proc, numColors, col.processImage(proc.nextImage()), );
                i++;
            }
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
