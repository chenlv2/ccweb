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


import ccait.ccweb.model.ResponseData;
import ccait.ccweb.utils.FastJsonUtils;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;

public class CCWebResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream buffer = null;

    private ServletOutputStream out = null;

    private PrintWriter writer = null;

    private HttpServletResponse res;

    private String body;

    private static final Logger log = LogManager.getLogger( CCWebResponseWrapper.class );

    public CCWebResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);

        res = response;
        buffer = new ByteArrayOutputStream();
        out = new WapperedOutputStream(buffer);
        writer = new PrintWriter(new OutputStreamWriter(buffer, "UTF-8"));
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return out;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {

        try {
            if(StringUtils.isEmpty(body)) {
                body = "";
            }
            PrintWriter out = res.getWriter();
            out.write(body);
            out.flush();
            out.close();
        }
        catch (Exception e){}
    }

    @Override
    public void reset() {
        buffer.reset();
        super.reset();
    }

    public String getResponseBody() throws IOException {
        if(StringUtils.isEmpty(body)) {
            if(buffer.size() < 1) {
                return null;
            }

            byte[] bytes = buffer.toByteArray();
            try {
                body = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);
            }

            if (out != null) {
                out.flush();
            }

            if (writer != null) {
                writer.flush();
            }

        }

        return body;
    }

    public ByteArrayOutputStream getBuffer()
    {
        return buffer;
    }

    public void writeBuffer(ResponseData responseData) throws IOException {

        if(responseData == null) {
            return;
        }
        getBuffer().write(FastJsonUtils.convertObjectToJSON(responseData).getBytes());
    }

    //内部类，对ServletOutputStream进行包装，指定输出流的输出端
    private class WapperedOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream bos = null;

        public WapperedOutputStream(ByteArrayOutputStream stream) throws IOException {
            bos = stream;
        }

        //将指定字节写入输出流bos
        @Override
        public void write(int b) throws IOException {
            bos.write(b);
        }

        @Override
        public boolean isReady() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            // TODO Auto-generated method stub

        }
    }
}
