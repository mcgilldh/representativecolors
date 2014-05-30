import java.awt.image.BufferedImage;

/**
 * Created by petdav on 2014-05-02.
 */
public class ImageItem {
    BufferedImage image;
    int textile_img_id = -1;
    String vt_tracking;

    public ImageItem(int textile_img_id, BufferedImage image) {
        this.image = image;
        this.textile_img_id = textile_img_id;
    }
}
