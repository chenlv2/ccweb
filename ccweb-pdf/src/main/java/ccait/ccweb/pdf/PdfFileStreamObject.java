/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.pdf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfFileStreamObject extends PdfFileObject
{
        private byte[] streamBuffer;
        private int streamStartOffset, streamLength;
        public PdfFileStreamObject(PdfFileObject obj) throws IOException {
                this.address = obj.address;
                this.length = obj.length;
                this.text = obj.text;
                this.number = obj.number;
                this.PdfFile = obj.PdfFile;
                this.loadStreamBuffer();
        }

        private void loadStreamBuffer() throws IOException {
                Matcher m1 = Pattern.compile("stream\\s*").matcher(this.text);
                this.streamStartOffset = m1.start() + m1.group().length();
                this.streamLength = this.length - this.streamStartOffset;
                this.streamBuffer = new byte[this.streamLength];
                this.PdfFile.memory.mark(this.address+this.streamStartOffset);
                this.PdfFile.memory.read(this.streamBuffer, 0,this.streamLength);

                this.PdfFile.memory.mark(this.address);
                InputStreamReader sr = new InputStreamReader(this.PdfFile.memory);
                char[] startChars = new char[this.streamStartOffset];
                sr.read(startChars, 0, this.streamStartOffset);
                StringBuilder sb = new StringBuilder();
                sb.append(startChars);
                this.text = sb.toString();
        }

        @Override
        public void transform(Hashtable TransformationHash)
        {
                super.transform(TransformationHash);
        }

        @Override
        public int writeToOutputStream(OutputStream outputStream) throws IOException {
                OutputStreamWriter sw = new OutputStreamWriter(outputStream);
                sw.write(this.text);
                sw.flush();
                sw.write(new String(this.streamBuffer));
                sw.flush();
                return this.streamLength+this.text.length();
        }
}
