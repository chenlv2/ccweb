package ccait.ccweb.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

public final class VideoUtils {

    private static final int THUMB_FRAME = 5;

    private VideoUtils() { }

    /**
     * 获取视频指定帧
     */
    public static BufferedImage getFrame (File file)
            throws IOException, JCodecException {

        return getFrame(file, THUMB_FRAME);
    }

    /**
     * 获取视频指定帧
     */
    public static BufferedImage getFrame (File file, int frameNumber)
            throws IOException, JCodecException {

        Picture picture = FrameGrab.getFrameFromFile(file, frameNumber);

        BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);

        return bufferedImage;
    }

    /**
     * 获取缩略图
     * @return
     */
    public static byte[] getThumbnail(File file) throws IOException, JCodecException {

        return getThumbnail(file, 33);
    }

    /**
     * 获取缩略图
     * @return
     */
    public static byte[] getThumbnail(File file, int scalRatio) throws IOException, JCodecException {

        BufferedImage frameBi = getFrame(file);

        frameBi = ImageUtils.zoomImage(frameBi, scalRatio);

        return ImageUtils.toBytes(frameBi);
    }
}
