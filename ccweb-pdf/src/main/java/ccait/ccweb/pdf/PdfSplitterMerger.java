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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

public class PdfSplitterMerger
{
    OutputStream target;
    int pos = 15;
    private int number = 3;
    private ArrayList<Integer> pageNumbers, xrefs;
    public PdfSplitterMerger(OutputStream outputStream) throws IOException {
        this.xrefs = new ArrayList<Integer>();
        this.pageNumbers = new ArrayList<Integer>();
        this.target = outputStream;
        OutputStreamWriter sw = new OutputStreamWriter(this.target);
        sw.write("%PDF-1.4\r");
        sw.flush();
        byte[] buffer = new byte[7];
        buffer[0] = (byte) 0x25;
        buffer[1] = (byte) 0xE2;
        buffer[2] = (byte) 0xE3;
        buffer[3] = (byte) 0xCF;
        buffer[4] = (byte) 0xD3;
        buffer[5] = (byte) 0x0D;
        buffer[6] = (byte) 0x0A;
        this.target.write(buffer, 0, buffer.length);
        this.target.flush();
    }
    public Integer[] getPageNumByLen(int len)
    {
        ArrayList ps = new ArrayList();
        for (int index = 0; index < len; index++)
        {
            ps.add(index);
        }
        return (Integer[]) ps.toArray(new Integer[]{});
    }

    public Integer[] getPageNumByContent(String content)
    {
        String pages=content;
        ArrayList ps = new ArrayList();
        if (pages != null || pages.length() != 0)
        {
            String[] ss = pages.split("(,| |;)");
            for (String s : ss) {
                if (Pattern.matches("\\d+-\\d+", s)) {
                    String[] arr =s.split("-");
                    int start = Integer.parseInt(arr[0]);
                    int end = Integer.parseInt(arr[1]);
                    if (start > end)
                        return new Integer[]{0};
                    while (start <= end) {
                        ps.add(start - 1);
                        start++;
                    }
                } else {
                    ps.add(Integer.parseInt(s) - 1);
                }
            }
        }
        return (Integer[]) ps.toArray(new Integer[]{});
    }

    public Boolean add(InputStream PdfInputStream, Integer[] PageNumbers)
    {
        try
        {
            PdfFile pf = new PdfFile(PdfInputStream);
            pf.load();
            PdfSplitLoader ps = new PdfSplitLoader();
            ps.load(pf, PageNumbers, this.number);
            this.add(ps);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    public Boolean add(PdfFile PdfInput)
    {
        try
        {
            PdfFile pf = PdfInput;
            pf.load();
            Integer[] PageNumbers=PdfInput.getPages();
            PdfSplitLoader ps = new PdfSplitLoader();
            ps.load(pf, PageNumbers, this.number);
            this.add(ps);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    private Boolean add(PdfSplitLoader PdfSplitter)
    {
        try
        {
            for (int pageNumber : PdfSplitter.pageNumbers)
            {
                this.pageNumbers.add(PdfSplitter.transHash.get(pageNumber));
            }
            ArrayList<PdfFileObject> sortedObjects = new ArrayList<PdfFileObject>();
            for (Object pfo : PdfSplitter.sObjects.values())
            sortedObjects.add((PdfFileObject)pfo);
            sortedObjects.sort(new Comparator<PdfFileObject>() {
                @Override
                public int compare(PdfFileObject o1, PdfFileObject o2) {
                    return o1.number.compareTo(o2.number);
                }
            });

            for (PdfFileObject pfo : sortedObjects)
            {
                this.xrefs.add(pos);
                this.pos += pfo.writeToOutputStream(this.target);
                this.number++;
            }
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    public void finish() throws IOException {
        OutputStreamWriter sw = new OutputStreamWriter(this.target);

        String root = "";
        root = "1 0 obj\r";
        root += "<< \r/Type /Catalog \r";
        root += "/Pages 2 0 R \r";
        root += ">> \r";
        root += "endobj\r";

        xrefs.add(0, pos);
        pos += root.length();
        sw.write(root);

        String pages = "";
        pages+= "2 0 obj \r";
        pages += "<< \r";
        pages += "/Type /Pages \r";
        pages += "/Count " + pageNumbers.size() + " \r";
        pages += "/Kids [ ";
        for (int pageIndex : pageNumbers)
        {
            pages += pageIndex + " 0 R ";
        }
        pages += "] \r";
        pages += ">> \r";
        pages += "endobj\r";

        xrefs.add(1, pos);
        pos += pages.length();
        sw.write(pages);


        sw.write("xref\r");
        sw.write("0 " + (this.number) + " \r");
        sw.write("0000000000 65535 f \r");

        DecimalFormat g1=new DecimalFormat("0000000000");
        for (Integer xref : this.xrefs)
        sw.write(g1.format(xref+1) + " 00000 n \r");
        sw.write("trailer\r");
        sw.write("<<\r");
        sw.write("/Size " + (this.number) + "\r");
        sw.write("/Root 1 0 R \r");
        sw.write(">>\r");
        sw.write("startxref\r");
        sw.write((pos+1) + "\r");
        sw.write("%%EOF\r");
        sw.flush();
        sw.close();
    }
}
