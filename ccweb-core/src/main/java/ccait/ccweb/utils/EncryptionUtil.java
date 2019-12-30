/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.utils;


import entity.tool.util.StringUtils;
import org.codehaus.plexus.util.Base64;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;

public final class EncryptionUtil {

    /*******AES BEGIN******/

	/*
	 * 加密用的Key 可以用26个字母和数字组成 此处使用AES-128-CBC加密模式，key需要为16位。
	 */
    private final static String ivParameter = "0123456789abcdef";

    public static ByteBuffer convert(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length);
        byteBuffer.put(data);
        byteBuffer.flip();

        return byteBuffer;
    }

    public static byte[] convert(ByteBuffer byteBuffer){
        int len = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[len];

        if(byteBuffer.isReadOnly()){
            return null;
        }else {
            byteBuffer.get(bytes);
        }
        return bytes;
    }

    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    /**
     * Convert hex string to byte[]
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }
    /**
     * Convert char to byte
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /***
     * AES加密
     * @param text
     * @return
     */
    public static String encryptByAES(String text, String publicKey){
        return encryptByAES(text, publicKey, "UTF-8");
    }
    public static String encryptByAES(String text, String publicKey, String encoding){
        String result = "";

        try {
            String privateKey = genPrivateKey(publicKey, encoding);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] raw = privateKey.getBytes(encoding);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes(encoding));// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(text.getBytes(encoding));
            result = bytesToHexString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }

    /***
     * AES解密
     * @param text
     * @return
     */
    public static String decryptByAES(String text, String publicKey){
        return decryptByAES(text, publicKey, "UTF-8");
    }
    public static String decryptByAES(String text, String publicKey, String encoding){
        try {
            String privateKey = genPrivateKey(publicKey, encoding);
            byte[] raw = privateKey.getBytes(encoding);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = hexStringToBytes(text);
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, encoding);
            return originalString;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static String genPrivateKey(String publicKey, String encoding) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        publicKey = md5(publicKey, encoding).substring(8, 24);
        String privateKey = "";
        for(int i=0; i<publicKey.length(); i++) {
            int code = (int)publicKey.charAt(i);
            code = code % 26 + 65;
            privateKey+= (char)code;
        }

        return privateKey;
    }
    /*******AES END******/


    /*******BASE64 BEGIN******/
    /**
     * Base64编码
     * @param data 待编码数据
     * @return String 编码数据
     * @throws Exception
     */
    public static String base64Encode(String data, String encoding) throws Exception {

        // 执行编码
        byte[] b = Base64.encodeBase64(data.getBytes(encoding));

        return new String(b, encoding);
    }

    /**
     * Base64安全编码
     * 遵循RFC 2045实现
     * @param data 待编码数据
     * @return String 编码数据
     *
     * @throws Exception
     */
    public static String base64EncodeSafe(String data, String encoding) throws Exception {

        // 执行编码
        byte[] b = Base64.encodeBase64(data.getBytes(encoding), true);

        return new String(b, encoding);
    }

    /**
     * Base64解码
     *
     * @param data 待解码数据
     * @return String 解码数据
     * @throws Exception
     */
    public static String base64Decode(String data, String encoding) throws Exception {

        // 执行解码
        byte[] b = Base64.decodeBase64(data.getBytes(encoding));
        for (int i = 0; i < b.length; ++i) {
            if (b[i] < 0) {// 调整异常数据
                b[i] += 256;
            }
        }
        return new String(b, encoding);
    }
    /*******BASE64 END******/


    /*******MD5 BEGIN******/
    public static String md5(String str, String encoding) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return md5(str, encoding, encoding);
    }

    public static String md5(String str, String md5PublicKey, String encoding) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        if(StringUtils.isEmpty(str)) {
            return "";
        }
        if(StringUtils.isNotEmpty(md5PublicKey)) {
            str = md5PublicKey + str;
        }

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        md5.update((str).getBytes(encoding));
        byte b[] = md5.digest();

        int i;
        StringBuffer buf = new StringBuffer("");

        for(int offset=0; offset<b.length; offset++){
            i = b[offset];
            if(i<0){
                i+=256;
            }
            if(i<16){
                buf.append("0");
            }
            buf.append(Integer.toHexString(i));
        }

        String result = buf.toString();

        return result;
    }
    /*******MD5 END******/

    /*******SHA BEGIN*******/
    public static final String KEY_SHA = "SHA";

    public static String sha(String inputStr) throws NoSuchAlgorithmException {
        BigInteger sha =null;
        byte[] inputData = inputStr.getBytes();

        MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA);
        messageDigest.update(inputData);
        sha = new BigInteger(messageDigest.digest());

        return sha.toString(32);
    }
    /*******SHA END*******/

    /*******MAC BEGIN******/
    public static final String KEY_MAC = "HmacMD5";

    /**
     * 初始化HMAC密钥
     *
     * @return
     * @throws Exception
     */
    public static String initMacKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_MAC);
        SecretKey secretKey = keyGenerator.generateKey();
        return  (new BASE64Encoder()).encodeBuffer(secretKey.getEncoded());
    }

    /**
     * HMAC加密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static String mac(byte[] data, String key) throws Exception {

        SecretKey secretKey = new SecretKeySpec((new BASE64Decoder()).decodeBuffer(key), KEY_MAC);
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        return new String(mac.doFinal(data));

    }

    /*******MAC END******/
}
