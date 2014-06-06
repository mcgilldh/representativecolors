import java.awt.image.BufferedImage;

/**
 * Created by petdav on 2014-05-02.
 */
public class ImageItem {
    BufferedImage image;
    int textile_img_id = -1;
    String vt_tracking;

    public ImageItem(int textile_img_id, String vt_tracking, BufferedImage image) {
        this.image = image;
        this.textile_img_id = textile_img_id;
        this.vt_tracking = vt_tracking;
    }
}
