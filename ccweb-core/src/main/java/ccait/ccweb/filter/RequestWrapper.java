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
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RequestWrapper extends HttpServletRequestWrapper {
    private byte[] requestBody;
    private static Charset charSet;
    private String postString;
    private HttpServletRequest req;

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        req = request;
        //缓存请求body
        try {
            postString = RequestWrapper.getRequestPostString(request);
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

        } catch (IOException e) {}
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

    /**
     * 重写 getReader()
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public void setPostParameter(Object parameter) {
        this.params = parameter;
        this.postString = JsonUtils.toJson(parameter); //这里不允许使用fastjson，否则日期格式转为map用作查询会有问题
    }
}
