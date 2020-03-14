package main.java.pdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfFile
{
    public String trailer;
    public InputStream memory;
    public Hashtable<Integer, PdfFileObject> objects;
    public String pages;

    public PdfFile(InputStream InputStream)
    {
        this.memory = InputStream;
    }

    public InputStream ToStream()
    {
        return this.memory;
    }

    public void load() throws Exception {
        int startxref = this.getStartXref();
        this.trailer = this.parseTrailer(startxref);
        ArrayList<Integer> adds=this.getAddresses(startxref);
        this.loadHash( adds);
    }
    private void loadHash(ArrayList<Integer> addresses) throws IOException {
        this.objects = new Hashtable();
        int part=0;
        int total=addresses.size();
        for (int add : addresses)
        {
            this.memory.mark(add);
            InputStreamReader isr = new InputStreamReader(this.memory);
            BufferedReader sr = new BufferedReader(isr);
            String line = sr.readLine();
            if (line.length()<2) {
                line = sr.readLine();
            }
            Matcher m = Pattern.compile("(\\d+)( )+0 obj").matcher(line);
            if (m.matches())
            {
                int num = Integer.parseInt(m.group(1));
                if (!objects.containsKey(num))
                {
                    objects.put(num, PdfFileObject.create(this,num,add));
                }
            }
            part++;
        }
    }

    public String getVersion()
    {
        String strRtn = "";
        try
        {
            InputStreamReader isr = new InputStreamReader(this.memory);
            BufferedReader sr = new BufferedReader(isr);
            String strLine = sr.readLine();

            int i = strLine.indexOf("-");
            if (i >= 0)
            {
                try
                {
                    strRtn = strLine.substring(i+1);
                }

                catch(Exception e){}
            }
        }
        catch(Exception e) {}
        return strRtn;
    }

    public PdfFileObject loadObject(String text,String key)
{
    String pattern = "/"+key+" (\\d+)";
    Matcher m = Pattern.compile(pattern).matcher(text);
    if (m.matches())
    {
        return this.loadObject(Integer.parseInt(m.group(1)));
    }
    return null;
}
    public PdfFileObject loadObject(int number)
{
    return this.objects.get(number);
}
    public ArrayList getPageList() {
        PdfFileObject root = this.loadObject(this.trailer, "Root");
        PdfFileObject pages = this.loadObject(root.text, "Pages");
        return pages.getKids();
    }

    public Integer[] getPages() {
        ArrayList ps = new ArrayList();
        if (this.pages == null || pages.length() == 0)
        {
            for (int index = 0; index < this.getPageCount(); index++)
            {
                ps.add(index);
            }
        }
        else
        {
            String[] ss = this.pages.split(",| |;");
            for (String s : ss)
                if (Pattern.matches("\\d+-\\d+", s))
            {
                int start = Integer.parseInt(s.split("-")[0]);
                int end = Integer.parseInt(s.split("-")[1]);
                if (start > end)
                    return new Integer[] { 0 };
                while (start <= end)
                {
                    ps.add(start-1);
                    start++;
                }
            }
						else
            {
                ps.add(Integer.parseInt(s)-1);
            }
        }
        return (Integer[]) ps.toArray(new Integer[]{});
    }

    public int getPageCount() {
        return this.getPageList().size();
    }

    private ArrayList<Integer> getAddresses(int xref) throws IOException {
        this.memory.mark(xref);
        ArrayList<Integer> al = new ArrayList<Integer>();
        InputStreamReader isr = new InputStreamReader(this.memory);
        BufferedReader sr = new BufferedReader(isr);
        String line="";
        String prevPattern = "/Prev \\d+";
        Boolean ok = true;
        while (ok)
        {
            if (Pattern.matches("\\d{10} 00000 n\\s*", line))
            {
                al.add(Integer.parseInt(line.substring(0,10)));
            }

            line = sr.readLine();
            ok = !(line == null || Pattern.matches(">>", line));
            if (line != null)
            {
                Matcher m = Pattern.compile(prevPattern).matcher(line);
                if (m.matches())
                {
                    al.addAll(this.getAddresses(Integer.parseInt(m.group().substring(6))));
                }
            }

        }
        return al;
    }

    private int getStartXref() throws Exception {
        InputStreamReader isr = new InputStreamReader(this.memory);
        BufferedReader sr = new BufferedReader(isr);
        this.memory.mark(this.memory.available() - 100);
        String line="";
        while (!line.startsWith("startxref"))
        {
            line = sr.readLine();
        }
        Integer startxref = Integer.parseInt(sr.readLine());
        if (startxref == -1)
            throw new Exception("Cannot find the startxref");
        return startxref;
    }

    private String parseTrailer(int xref) throws Exception {
        this.memory.mark(xref);
        InputStreamReader isr = new InputStreamReader(this.memory);
        BufferedReader sr = new BufferedReader(isr);
        String line;
        String trailer = "";
        Boolean istrailer = false;
        while ((line = sr.readLine()) != "startxref")
        {
            line = line.trim();
            if (line.startsWith("trailer"))
            {
                trailer = "";
                istrailer = true;
            }
            if (istrailer)
            {
                trailer += line + "\r";
            }
        }
        if (trailer == "")
            throw new Exception("Cannot find trailer");
        return trailer;
    }

}
