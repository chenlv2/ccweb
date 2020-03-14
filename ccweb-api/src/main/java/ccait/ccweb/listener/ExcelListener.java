package ccait.ccweb.listener;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.enums.EncryptMode;
import ccait.ccweb.model.SheetHeaderModel;
import ccait.ccweb.utils.EncryptionUtil;
import com.alibaba.excel.read.metadata.ReadSheet;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

/**
 * 模板的读取类
 */
// 有个很重要的点 HashMapListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
public class ExcelListener extends AnalysisEventListener<Object> {
    private static final Logger logger = LoggerFactory.getLogger(ExcelListener.class);
    /**
     * 每隔500条存储数据库，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 500;
    private final List<SheetHeaderModel> headerList;
    private final Object entity;
    private final String md5Fields;
    private final String md5PublicKey;
    private final String base64Fields;
    private final String macFields;
    private final String shaFields;
    private final String macPublicKey;
    private final String aesFields;
    private final String aesPublicKey;
    private final String encoding;

    List<Object> list = new ArrayList<Object>();
    String tablename;

    /**
     * 如果使用了spring,请使用这个构造方法。每次创建Listener的时候需要把spring管理的类传进来
     */
    public ExcelListener(String tablename, Object entity, List<SheetHeaderModel> headerList) {
        this.entity = entity;
        this.headerList = headerList;
        this.tablename = tablename;
        md5Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.MD5.fields}");
        md5PublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.MD5.publicKey}");
        base64Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.BASE64.fields}");
        macFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.MAC.fields}");
        shaFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.SHA.fields}");
        macPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.MAC.publicKey}");
        aesFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.fields}");
        aesPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.publicKey}");
        encoding = ApplicationConfig.getInstance().get("${entity.encoding}");
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data
     *            one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(Object data, AnalysisContext context) {

        ReadSheet readSheet = new ReadSheet();
        context.currentSheet(readSheet);
        if("schema".equals(readSheet.getSheetName())){
            return;
        }

        list.add(data);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (list.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            list.clear();
        }
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        logger.info("easyexcel: {}条数据，开始导入数据库！", list.size());
        for(Object obj : list) {

            try {
                Map<String, Object> postData = new HashMap<String, Object>();
                for(SheetHeaderModel headerModel : headerList) {
                    Object value = ReflectionUtils.getFieldValue(obj, getFieldName(headerModel.getField()));
                    postData.put(headerModel.getField(), value);
                }

                encrypt(postData);

                BaseController.fillData(postData, entity, true);

                ((Queryable) entity).insert();

            } catch (Exception e) {
                logger.error("插入数据失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
        logger.info("导入数据库成功！");
    }

    private String getFieldName(String field) {
        List<String> list = StringUtils.splitString2List(field, "_");
        for(int i=0; i<list.size(); i++) {
            String value = list.get(i);
            if(i==0) {
                list.set(i, value.substring(0, 1).toLowerCase() + value.substring(1));
                continue;
            }

            list.set(i, value.substring(0, 1).toUpperCase() + value.substring(1));
        }

        String result = StringUtils.join("", list);

        return result;
    }

    protected void encrypt(Map<String, Object> data) {

        if(data == null) {
            return;
        }

        if(StringUtils.isNotEmpty(md5Fields)) {

            List<String> fieldList = StringUtils.splitString2List(md5Fields, ",");

            encrypt(data, fieldList, EncryptMode.MD5);
        }

        if(StringUtils.isNotEmpty(macFields)) {

            List<String> fieldList = StringUtils.splitString2List(macFields, ",");

            encrypt(data, fieldList, EncryptMode.AES);
        }

        if(StringUtils.isNotEmpty(shaFields)) {

            List<String> fieldList = StringUtils.splitString2List(shaFields, ",");

            encrypt(data, fieldList, EncryptMode.AES);
        }

        if(StringUtils.isNotEmpty(base64Fields)) {

            List<String> fieldList = StringUtils.splitString2List(base64Fields, ",");

            encrypt(data, fieldList, EncryptMode.BASE64);
        }

        if(StringUtils.isNotEmpty(aesFields)) {

            List<String> fieldList = StringUtils.splitString2List(aesFields, ",");

            encrypt(data, fieldList, EncryptMode.AES);
        }
    }

    protected void encrypt(Map<String, Object> data, List<String> fieldList, EncryptMode encryptMode) {

        if(fieldList == null || fieldList.size() < 1) {
            return;
        }

        data.keySet().stream().filter(a -> fieldList.contains(a) || fieldList.contains(String.join(".", tablename, a)))
                .forEach(key -> {
                    if(data.get(key) instanceof String) {
                        switch (encryptMode) {
                            case MD5:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, md5PublicKey, encoding));
                                break;
                            case MAC:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, macPublicKey, encoding));
                                break;
                            case SHA:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode));
                                break;
                            case BASE64:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, encoding));
                                break;
                            case AES:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, aesPublicKey));
                                break;
                        }
                    }
                });
    }

    public static String encrypt(String value, EncryptMode encryptMode, String... encryptArgs) {
        try {
            switch (encryptMode) {
                case MD5:
                    if(encryptArgs == null || encryptArgs.length != 2) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.md5(value, encryptArgs[0], encryptArgs[1]);
                    break;
                case MAC:
                    if(encryptArgs == null || encryptArgs.length != 2) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.mac(value.getBytes(encryptArgs[1]), encryptArgs[0]);
                    break;
                case SHA:
                    value = EncryptionUtil.sha(value);
                    break;
                case BASE64:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.base64Encode(value, encryptArgs[0]);
                    break;
                case AES:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.encryptByAES(value, encryptArgs[0]);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return value;
    }
}
