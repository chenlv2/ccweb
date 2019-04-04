package ccait.ccweb.utils;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

public final class ImageUtils {


    /**
     * @param im            原始图像
     * @param width         固定宽度
     * @return              返回处理后的图像
     */
    public static BufferedImage resizeImage(BufferedImage im, Integer width) {

        float rote = 0;
        int height = im.getHeight();
        if(width < im.getWidth()) {
            rote = width.floatValue() / Float.valueOf(im.getWidth());
            height = Float.valueOf(Float.valueOf(im.getWidth()) / rote).intValue();
        }

        else if(width > im.getWidth()) {
            rote = Float.valueOf(im.getWidth()) / width.floatValue();
            height = Float.valueOf(Float.valueOf(im.getHeight()) / rote).intValue();
        }

        else {
            return im;
        }

        /*新生成结果图片*/
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        result.getGraphics().drawImage(im.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return result;
    }

    /**
     * @param im            原始图像
     * @param resizeTimes   百分比,比如50就是缩小一半
     * @return              返回处理后的图像
     */
    public static BufferedImage zoomImage(BufferedImage im, Integer resizeTimes) {
        /*原始图像的宽度和高度*/
        int width = im.getWidth();
        int height = im.getHeight();

        /*调整后的图片的宽度和高度*/
        int toWidth = (int) (Float.parseFloat(String.valueOf(width)) * BigDecimal.valueOf(resizeTimes).divide(BigDecimal.valueOf(100)).floatValue());
        int toHeight = (int) (Float.parseFloat(String.valueOf(height)) * BigDecimal.valueOf(resizeTimes).divide(BigDecimal.valueOf(100)).floatValue());

        /*新生成结果图片*/
        BufferedImage result = new BufferedImage(toWidth, toHeight, BufferedImage.TYPE_INT_RGB);

        result.getGraphics().drawImage(im.getScaledInstance(toWidth, toHeight, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return result;
    }

    public static BufferedImage getImage(byte[] data) throws IOException {

        InputStream input = new ByteArrayInputStream(data);
        return ImageIO.read(input);
    }

    public static byte[] toBytes(BufferedImage image, String format) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);

        return out.toByteArray();
    }
}
