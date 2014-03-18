
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
    HashMap<Color, Integer> refColors;
    double threshold;

    public RepColors() {
        threshold = 5; //The default threshold
    }

    /**
     * Loads reference colors from a comma separated file, one color per line.
     * @param filename A comma delimited file specifying the reference colors to match to.
     */
    public void loadRefColors(String filename) {
        refColors = new HashMap<>();
        if (debug) System.out.println("Loading reference colors...");
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

    public LinkedList<ColorCount> processImage(BufferedImage image) {
        HashSet<Color>  pixels = new HashSet<>();
        LinkedList<ColorCount> colors;

        //Add the pixels to the HashSet
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                pixels.add(new Color(image.getRGB(x, y)));
            }
        }

        if (debug) System.out.println("Number of unique pixels: " + pixels.size());

        //Pull the pixels out again, and compute the distance
        for (Color temp : pixels) {
            for (Color ref : refColors.keySet()) {
                if (colorDistance(temp, ref) < threshold) refColors.put(ref, refColors.get(ref) + 1);
            }
        }

        if (debug) System.out.println("Sorting results.");
        //Sort the colors by the most common.
        colors = new LinkedList<>();
        for (Color ref : refColors.keySet()) {
            colors.add(new ColorCount(refColors.get(ref), ref));
        }
        Collections.sort(colors, Collections.reverseOrder());
        return colors;
    }

    /**
     * Writes numColors of the top colors in this image to outFile.
     * @param outFile The file to which the most top colors should be written.
     * @param numColors The number of colors to write
     */
    public void writeColors(String outFile, int numColors, LinkedList<ColorCount> colors) {
        //TODO: Write out match strength
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

        col.loadRefColors(colorRef);
        //Connect to the database
        ImageProc proc = new ImageProc("localhost", "VTMaster", "testuser", "test", "Textile_img", col.debug);
        //Pull images from the database
        proc.fetchImages("IMG_detail");

        int i = 0;
        while (proc.hasNextImage()) {
            col.writeColors(String.format("image%d.csv", i), numColors, col.processImage(proc.nextImage()));
            i++;
        }

        if (colorRef.length() == 0) {
            System.out.println("usage: java RepColors -i image.jpg -r reference_colors.csv -o outfile.csv [-d -n number]");
            System.out.println("\t-d enables debugging.\n-n number specifies number of images to parse. Default is 10.\nArguments can occur in any order.");
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
