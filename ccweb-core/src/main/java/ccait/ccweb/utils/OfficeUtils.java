package ccait.ccweb.utils;

import ccait.ccweb.config.LangConfig;
import ccait.ccweb.model.SheetHeaderModel;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.hslf.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfficeUtils {

    private static final Logger logger = LoggerFactory.getLogger(OfficeUtils.class);
    public static List<SheetHeaderModel> getHeadersByExcel(byte[] fileBytes, List<String> fieldList) throws IOException {
        InputStream is = new ByteArrayInputStream(fileBytes);
        List<SheetHeaderModel> headerList = new ArrayList<SheetHeaderModel>();
        if(FileMagic.valueOf(new ByteArrayInputStream(fileBytes)).name().equalsIgnoreCase("OOXML")) {
            XSSFSheet sheet = new XSSFWorkbook(is).getSheet("schema");
            XSSFRow titleRow = sheet.getRow(0);
            XSSFRow fieldRow = sheet.getRow(1);
            for(int i=0; i<titleRow.getLastCellNum(); i++) {
                SheetHeaderModel headerModel = new SheetHeaderModel();
                String title = titleRow.getCell(i).getStringCellValue();
                String field = fieldRow.getCell(i).getStringCellValue();

                headerModel.setHeader(title);
                headerModel.setField(field);
                headerModel.setIndex(i);

                fieldList.add(field);
                headerList.add(headerModel);
            }
        }

        else if(FileMagic.valueOf(new ByteArrayInputStream(fileBytes)).name().equalsIgnoreCase("OLE2")) {
            HSSFSheet sheet = new HSSFWorkbook(is).getSheet("schema");
            HSSFRow titleRow = sheet.getRow(0);
            HSSFRow fieldRow = sheet.getRow(1);
            for(int i=0; i<titleRow.getLastCellNum(); i++) {
                SheetHeaderModel headerModel = new SheetHeaderModel();
                String title = titleRow.getCell(i).getStringCellValue();
                String field = fieldRow.getCell(i).getStringCellValue();

                headerModel.setHeader(title);
                headerModel.setField(field);
                headerModel.setIndex(i);

                fieldList.add(field);
                headerList.add(headerModel);
            }
        }

        else {
            throw new IOException(LangConfig.getInstance().get("can_not_supported_file_type"));
        }
        return headerList;
    }

    public static String getTextByPPT(byte[] fileBytes) throws IOException {
        InputStream iis = new ByteArrayInputStream(fileBytes);
        PowerPointExtractor extractor=new PowerPointExtractor(iis);
        String text = extractor.getText();
        iis.close();

        return text;
    }

    public static void saveImageByPPT(String table, String currentDatasource, List<Map<String, Object>> resultSet, Map.Entry<String, Object> entry, String filename, byte[] fileBytes) throws IOException {

        String[] arr = filename.split("\\.");

        String suffix = arr[arr.length - 1];

        SlideShow ppt = getSildeShow(suffix, fileBytes);
        Dimension pgsize = ppt.getPageSize();

        for (int i = 0; i < ppt.getSlides().size(); i++) {
            try {
                //防止中文乱码
                setPPTFont(suffix, (Slide) ppt.getSlides().get(i), "宋体");
                BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();
                // clear the drawing area
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

                // render
                drawPPT(suffix, graphics, (Slide) ppt.getSlides().get(i));

                // save the output
                String pptPagePath = String.format("/preview/ppt/%s/%s/%s", currentDatasource, table, entry.getKey());

                String imgName = String.format("%s_%s.png", filename, i);

                String fullpath = UploadUtils.upload(pptPagePath, imgName, ImageUtils.toBytes(img));

                Map<String, Object> result = new HashMap<String, Object>();
                result.put(entry.getKey(), fullpath);
                result.put("page_number", i);
                resultSet.add(result);

            } catch (Exception e) {
                logger.error("第"+i+"张ppt转换出错");
            }
        }
    }

    private static void drawPPT(String suffix, Graphics2D graphics, Slide slide) {
        switch (suffix.toLowerCase()) {
            case "pptx":
                ((XSLFSlide) slide).draw(graphics);
                break;
            case "ppt":
                ((HSLFSlide) slide).draw(graphics);
                break;
        }
    }

    /***
     * 设置PPT字体
     * @param suffix
     * @param iSlide
     * @param fontname
     * @return
     */
    public static void setPPTFont(String suffix, Slide iSlide, String fontname) {
        switch (suffix.toLowerCase()) {
            case "pptx":
                XSLFSlide slide = (XSLFSlide) iSlide;
                for(XSLFShape shape : slide.getShapes()){
                    if(shape instanceof XSLFTextShape) {
                        XSLFTextShape tsh = (XSLFTextShape)shape;
                        for(XSLFTextParagraph p : tsh){
                            for(XSLFTextRun r : p){
                                r.setFontFamily(fontname);
                            }
                        }
                    }
                }
                break;
            case "ppt":
                HSLFSlide slide2 = (HSLFSlide) iSlide;
                for(HSLFShape shape : slide2.getShapes()){
                    if(shape instanceof HSLFTextShape) {
                        HSLFTextShape tsh = (HSLFTextShape)shape;
                        for(HSLFTextParagraph p : tsh){
                            for(HSLFTextRun r : p){
                                r.setFontFamily(fontname);
                            }
                        }
                    }
                }
                break;
        }
    }

    /***
     * 获取ppt对象
     * @param suffix
     * @param fileBytes
     * @return
     * @throws IOException
     */
    public static SlideShow getSildeShow(String suffix, byte[] fileBytes) throws IOException {

        InputStream is = new ByteArrayInputStream(fileBytes);

        switch (suffix.toLowerCase()) {
            case "pptx":
                return new XMLSlideShow(is);
            case "ppt":
                return new HSLFSlideShow(is);
            default:
                return null;
        }
    }
}
