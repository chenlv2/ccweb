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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfFileObject
{
    public Integer address;
    public Integer number,length;
    public String text;
    public PdfFile PdfFile;
    public static PdfFileObject create(PdfFile PdfFile, int number, int address) throws IOException {
        PdfFileObject pfo = new PdfFileObject();
        pfo.PdfFile = PdfFile;
        pfo.number = number;
        pfo.address = address;
        pfo.length(PdfFile);
        pfo.loadText();
        if (PdfObjectType.Stream.equals(pfo.getType()))
        {
            pfo = new PdfFileStreamObject(pfo);
        }
        return pfo;
    }

    private void loadText() throws IOException {
        this.PdfFile.memory.mark(this.address);
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < this.length; index++)
        {
            sb.append((char)this.PdfFile.memory.read());
        }
        this.text = sb.toString();
    }
    private void length(PdfFile pdfFile) throws IOException {
        InputStream stream = pdfFile.memory;
        stream.mark(this.address);

        Matcher m = Pattern.compile("endobj\\s*").matcher("");
        int b = 0;
        this.length = 0;
        String word = "";
        while (b != -1)
        {
            b = stream.read();
            this.length++;
            if (b > 97 && b < 112)
            {
                char c = (char)b;
                word += c;
                if (word == "endobj")
                    b = -1;
            }
            else
            {
                word = "";
            }
        }
        Character c2 = (char)stream.read();
        while (Pattern.compile("\\s").matcher(c2.toString()).find())
        {
            this.length++;
            c2 = (char)stream.read();
        }
    }

    protected PdfObjectType type;
    public PdfObjectType getType()
    {
        if (this.type==PdfObjectType.UnKnown)
        {
            if (Pattern.compile("/Page").matcher(this.text).find() &
                    !Pattern.compile("/Pages").matcher(this.text).find())
            {
                this.type = PdfObjectType.Page;
                return this.type;
            }
            if (Pattern.compile("stream").matcher(this.text).find())
            {
                this.type = PdfObjectType.Stream;
                return this.type;
            }
            this.type = PdfObjectType.Other;
        }
        return this.type;
    }


    public Integer[] getArrayNumbers(String arrayName)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        String pattern = "/" + arrayName + "\\s*\\[(\\s*(?'id'\\d+) 0 R\\s*)*";
        Matcher m = Pattern.compile(this.text).matcher(pattern);
        while(m.find()) {
            String value = m.group("id");
            ids.add(Integer.parseInt(value));
        }
        return ids.toArray(new Integer[0]);
    }
    public ArrayList getKids()
    {
        ArrayList kids = new ArrayList();
        for (int id : this.getArrayNumbers("Kids"))
        {
            PdfFileObject pfo = PdfFile.loadObject(id);
            if (PdfObjectType.Page.equals(pfo.getType()))
            {
                kids.add(pfo);
            }
            else
            {
                kids.addAll(pfo.getKids());
            }
        }
        return kids;
    }

    public void populateRelatedObjects(PdfFile pdfFile, Hashtable container)
    {
        if (!container.containsKey(this.number))
        {
            container.put(this.number, this);
            Matcher m = Pattern.compile("((/Parent)*)\\s*(\\d+) 0 R[^G]").matcher(this.text);
            while (m.find())
            {
                int num = Integer.parseInt(m.group(2));
                Boolean notparent = m.group(1).length() == 0;
                if (notparent & !container.contains(num))
                {
                    PdfFileObject pfo = pdfFile.loadObject(num);
                    if (pfo != null & !container.contains(pfo.number))
                    {
                        pfo.populateRelatedObjects(pdfFile, container);
                    }
                }
            }
        }
    }

    private Hashtable TransformationHash;
    private String filterEval(Matcher m)
    {
        int id = Integer.parseInt(m.group(1));
        String end = m.group(4);
        if (this.TransformationHash.containsKey(id))
        {
            String rest = m.group(2);
            return (int)TransformationHash.get(id) + rest+end;
        }
        return end;
    }
    public PdfFileObject getParent()
    {
        return this.PdfFile.loadObject(this.text,"Parent");
    }
    public String getMediaBoxText()
    {
        String pattern="/MediaBox\\s*\\[\\s*(\\+|-)?\\d+(.\\d+)?\\s+(\\+|-)?\\d+(.\\d+)?\\s+(\\+|-)?\\d+(.\\d+)?\\s+(\\+|-)?\\d+(.\\d+)?\\s*]";
        return Pattern.compile(pattern).matcher(this.text).group();
    }
    public  void transform(Hashtable TransformationHash) {
        if (PdfObjectType.Page.equals(this.getType()) && "".equals(this.getMediaBoxText()))
        {
            PdfFileObject parent=this.getParent();
            while (parent!=null)
            {
                String mb=parent.getMediaBoxText();
                if (mb=="")
                {
                    parent=parent.getParent();
                }
                else
                {
                    this.text = Pattern.compile("/Type\\s*/Page").matcher(this.text).replaceAll("/Type /Page\\r"+mb);
                    parent=null;
                }
            }
        }
        this.TransformationHash = TransformationHash;
        Matcher m = Pattern.compile("(\\d+)( 0 (obj|R))([^G])").matcher(this.text);
        String temp = "";
        while (m.find())
        {
            temp += filterEval(m);
        }
        this.text = temp;
        this.text = Pattern.compile("/Parent\\s+(\\d+ 0 R)*").matcher(this.text).replaceAll("/Parent 2 0 R \\n");
    }

    public  int writeToOutputStream(OutputStream InputStream) throws IOException {
        OutputStreamWriter sw = new OutputStreamWriter(InputStream, Charset.forName("ASCII"));
        sw.write(this.text);
        sw.flush();
        return this.text.length();
    }

}
