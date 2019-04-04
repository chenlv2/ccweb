/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.filter;

import ccait.ccweb.utils.FastJsonUtils;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestWrapper extends HttpServletRequestWrapper implements MultipartHttpServletRequest {

    private static final String FILE_CHARSET = "ISO-8859-1";
    private byte[] requestBody;
    private static Charset charSet;
    private String postString;
    private HttpServletRequest req;

    private static final Logger log = LogManager.getLogger(RequestWrapper.class);

    public RequestWrapper(HttpServletRequest request, Map newParams)
    {
        super(request);
        this.params = newParams;
        this.postString = FastJsonUtils.convertObjectToJSON(newParams);
    }

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        req = request;

        //缓存请求body
        try {
            requestBody = readBody(request);
            if(requestBody == null) {
                requestBody = new byte[0];
            }
            postString = new String(requestBody, FILE_CHARSET);

            Map<String, Object> map = new HashMap<String, Object>();
            List<String> list = StringUtils.splitString2List(postString, "(\\-\\-)+\\-*[\\d\\w]+");
            for(String content : list) {
                Pattern regex = Pattern.compile("Content-Disposition:\\s*form-data;\\s*name=\"([^\"]+)\"(;\\s*filename=\"([^\"]+)\")?\\s*(Content-Type:\\s*([^/]+/[^\\s]+)\\s*)?\\s*?([\\w\\W]+)", Pattern.CASE_INSENSITIVE);
                Matcher m = regex.matcher(content);
                while (m.find()) {
                    String key = m.group(1);
                    Object value = m.group(6);
                    if(m.group(5) != null && Pattern.matches("[^/]+/.+", m.group(5))) {

                        //返回字节数组，fastjson序列化时会进行Base64编码
                        value = m.group(6).getBytes(FILE_CHARSET);
                        map.put(String.format("%s_upload_filename", key), m.group(3));
                    }

                    map.put(key, value);
                }
            }

            if(map.size() > 0) {
                postString = FastJsonUtils.convertObjectToJSON(map);
                this.params = map;
            }
            else {
                if(Pattern.matches("\\s*^\\[[^\\[\\]]+\\]$\\s*", postString)) {
                    this.params = FastJsonUtils.convertJsonToObject(postString, List.class);
                }
                else {
                    this.params = FastJsonUtils.convertJsonToObject(postString, Map.class);
                }
            }

        } catch (Exception e) {
            log.error(e);
        }
    }

    private Object params;

    public Object getParameters()
    {
        return params;
    }

    public  String getRequestPostString()
    {
        return postString;
    }

    public static String getRequestPostString(HttpServletRequest request)
            throws IOException {
        String charSetStr = request.getCharacterEncoding();
        if (charSetStr == null) {
            charSetStr = "UTF-8";
        }
        charSet = Charset.forName(charSetStr);

        return StreamUtils.copyToString(request.getInputStream(), charSet);
    }

    /**
     * 重写 getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() {
        if (requestBody == null) {
            requestBody = new byte[0];
        }

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    /**
     * 重写 getReader()
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public void setPostParameter(Object parameter) {
        this.params = parameter;
        this.postString = FastJsonUtils.convertObjectToJSON(parameter);
    }


    @Override
    public HttpMethod getRequestMethod() {
        return ((MultipartHttpServletRequest)req).getRequestMethod();
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return ((MultipartHttpServletRequest)req).getRequestHeaders();
    }

    @Override
    public HttpHeaders getMultipartHeaders(String s) {
        return ((MultipartHttpServletRequest)req).getMultipartHeaders(s);
    }

    @Override
    public Iterator<String> getFileNames() {
        return ((MultipartHttpServletRequest)req).getFileNames();
    }

    @Override
    public MultipartFile getFile(String s) {
        return ((MultipartHttpServletRequest)req).getFile(s);
    }

    @Override
    public List<MultipartFile> getFiles(String s) {
        return ((MultipartHttpServletRequest)req).getFiles(s);
    }

    @Override
    public Map<String, MultipartFile> getFileMap() {
        return ((MultipartHttpServletRequest)req).getFileMap();
    }

    @Override
    public MultiValueMap<String, MultipartFile> getMultiFileMap() {
        return ((MultipartHttpServletRequest)req).getMultiFileMap();
    }

    @Override
    public String getMultipartContentType(String s) {
        return ((MultipartHttpServletRequest)req).getMultipartContentType(s);
    }

    public static byte[] readBody(HttpServletRequest request)
            throws IOException{
        int formDataLength = request.getContentLength();
        DataInputStream dataStream = new DataInputStream(request.getInputStream());
        byte body[] = new byte[formDataLength];
        int totalBytes = 0;
        while (totalBytes < formDataLength) {
            int bytes = dataStream.read(body, totalBytes, formDataLength);
            totalBytes += bytes;
        }
        return body;
    }
}
