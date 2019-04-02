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
    private byte[] requestBody;
    private static Charset charSet;
    private String postString;
    private HttpServletRequest req;

    private static final Logger log = LogManager.getLogger(RequestWrapper.class);

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        req = request;
        //缓存请求body
        try {
            postString = RequestWrapper.getRequestPostString(request);
            Map<String, String> map = new HashMap<String, String>();
            List<String> list = StringUtils.splitString2List(postString, "(\\-\\-)+\\-*\\d{24}");
            for(String content : list) {
                Pattern regex = Pattern.compile("Content-Disposition:\\s*form-data;\\s*name=\"([^\"]+)\"(;\\s*filename=\"[^\"]+\")?\\s*(Content-Type:\\s*image/\\w+\\s*)?\\s*?([\\w\\W]+)");
                Matcher m = regex.matcher(content);
                while (m.find()) {
                    map.put(m.group(1), m.group(4));
                }
            }

            if(map.size() > 0) {
                postString = FastJsonUtils.convertObjectToJSON(map);
                if ( StringUtils.isNotEmpty(postString) ) {
                    requestBody = input2byte(request.getInputStream());
                } else {
                    requestBody = new byte[0];
                }

                this.params = request.getInputStream();
            }
            else {
                if ( StringUtils.isNotEmpty(postString) ) {
                    requestBody = postString.getBytes(charSet);
                } else {
                    requestBody = new byte[0];
                }

                if(Pattern.matches("\\s*^\\[[^\\[\\]]+\\]$\\s*", postString)) {
                    this.params = FastJsonUtils.convertJsonToObject(postString, List.class);
                }
                else {
                    this.params = FastJsonUtils.convertJsonToObject(postString, Map.class);
                }
            }

        } catch (IOException e) {
            log.error(e);
        }
    }

    private Object params;

    public RequestWrapper(HttpServletRequest request, Map newParams)
    {
        super(request);
        this.params = newParams;
        this.postString = FastJsonUtils.convertObjectToJSON(newParams);
    }

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

    public static final InputStream byte2Input(byte[] buf) {
        return new ByteArrayInputStream(buf);
    }

    public static final byte[] input2byte(InputStream inStream)
            throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in2b = swapStream.toByteArray();
        return in2b;
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
}
