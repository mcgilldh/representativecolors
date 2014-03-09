import javafx.scene.layout.Priority;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;


/**
 * Created by petdav on 3/7/2014.
 */
public class RepColors {
    HashSet<Color> pixels;
    boolean debug = false;
    HashMap<Color, Integer> refColors;
    double threshold;
    LinkedList<ColorCount> colors;

    public RepColors() {
        threshold = 5; //The default threshold
    }

    /**
     * Loads reference colors from a comma separated file, one color per line.
     * @param filename
     */
    public void loadRefColors(String filename) {
        refColors = new HashMap<Color, Integer>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            String[] fields;
            while ((line = in.readLine()) != null ) {
                fields = line.split(",");
                int red = Integer.parseInt(fields[0]);
                int green = Integer.parseInt(fields[1]);
                int blue = Integer.parseInt(fields[2]);
                refColors.put(new Color(red, green, blue), 0);
            }
        }
        catch (IOException er) {
            er.printStackTrace();
        }
    }

    public void processImage(String inputFile) {
        pixels = new HashSet<Color>();

        BufferedImage image;
        try {
            image = ImageIO.read(new File(inputFile));

            //Add the pixels to the HashSet
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pixels.add(new Color(image.getRGB(x, y)));
                }
            }

            if (debug) System.out.println("Number of unique pixels: " + pixels.size());

            //Pull the pixels out again, and compute the distance
            Iterator<Color> it = pixels.iterator();
            while (it.hasNext()) {
                Color temp = it.next();
                for (Color ref : refColors.keySet()) {
                    if (colorDistance(temp, ref) < threshold) refColors.put(ref, refColors.get(ref) + 1);
                }
            }

            if (debug) System.out.println("Sorting results.");
            //Sort the colors by the most common.
            colors = new LinkedList<ColorCount>();
            for (Color ref : refColors.keySet()) {
                colors.add(new ColorCount(refColors.get(ref), ref));
            }
            Collections.sort(colors, Collections.reverseOrder());
        }
        catch (IOException er) {
            er.printStackTrace();
        }
    }

    /**
     * Writes numColors of the top colors in this image to outFile.
     * @param outFile
     * @param numColors
     */
    public void writeColors(String outFile, int numColors) {
        if (debug) System.out.println("Writing Results to " + outFile);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
            for (int i = 0; i < numColors; i ++) {
                ColorCount tempcount = colors.pop();
                Color temp = tempcount.c;
                if (debug) System.out.println("Color " + i + " has score " + tempcount.i);
                out.write(String.format("%03d,%03d,%03d\n", temp.getRed(), temp.getGreen(), temp.getBlue()));
            }
            out.close();
            if (debug) System.out.println("Finished.");
        }
        catch(IOException er) {
            er.printStackTrace();
        }

    }

    public static double colorDistance(Color a, Color b) {
        return Math.sqrt(Math.pow(a.getRed()-b.getRed(), 2) + Math.pow(a.getGreen()-b.getGreen(), 2) + Math.pow(a.getBlue()-b.getBlue(),2));
    }

    public static void main(String[] args) {

        if (args.length >= 3) {
            RepColors col = new RepColors();
            if (args.length >= 4 && args[3].equals("-d")) col.debug = true;
            col.loadRefColors(args[0]);
            col.processImage(args[1]);
            col.writeColors(args[2], 10);

        }
        else {
            System.err.println("usage: RepColors reference_colors.csv image.jpg outfile.txt");
            System.exit(1);
        }
    }

}

/**
 * A simple class for storing color matches in a sort-able way.
 */
class ColorCount implements Comparable<ColorCount>{
    int i;
    Color c;
    public ColorCount(int i, Color c) {
        this.i = i;
        this.c = c;
    }

    public int compareTo(ColorCount other) {
        return i-other.i;
    }
}
