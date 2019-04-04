package ccait.ccweb.utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;

public final class ImageUtils {

    private static final Logger log = LogManager.getLogger(ImageUtils.class);

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
            height = Float.valueOf(Float.valueOf(im.getWidth()) * rote).intValue();
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

    /**
     * 给图片添加水印文字、可设置水印文字的旋转角度
     */
    public static BufferedImage watermark(BufferedImage srcImg, String waterMarkContent,Color markContentColor,Font font) {
        try {

            int srcImgWidth = srcImg.getWidth();//获取图片的宽
            int srcImgHeight = srcImg.getHeight();//获取图片的高
            // 加水印

            /*新生成结果图片*/
            BufferedImage result = new BufferedImage(srcImgWidth, srcImgHeight, BufferedImage.TYPE_INT_RGB);

            Graphics g = result.getGraphics();
            g.drawImage(srcImg.getScaledInstance(srcImgWidth, srcImgHeight, Image.SCALE_SMOOTH), 0, 0, null);
            g.setColor(markContentColor); //根据图片的背景设置水印颜色
            g.setFont(font);              //设置字体

            //设置水印的坐标
            int x = srcImgWidth - 2* getWatermarkLength(waterMarkContent, g);
            int y = srcImgHeight - 2
                    * getWatermarkLength(waterMarkContent, g);
            g.drawString(waterMarkContent, x, y);  //画出水印
            g.dispose();
            // 输出图片
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return srcImg;
        }
    }

    private static int getWatermarkLength(String waterMarkContent, Graphics g) {
        return g.getFontMetrics(g.getFont()).charsWidth(waterMarkContent.toCharArray(), 0, waterMarkContent.length());
    }
}
