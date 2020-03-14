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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;

public
class PdfSplitLoader
{
    public Hashtable sObjects;
    public ArrayList<Integer> pageNumbers;
    public Hashtable<Integer, Integer> transHash;
    public PdfFile PdfFile;
    public PdfSplitLoader()
    {

    }
    public void load(PdfFile PdfFile, Integer[] PageNumbers, int startNumber)
    {
        this.PdfFile = PdfFile;
        this.pageNumbers = new ArrayList();
        this.sObjects = new Hashtable();
        int part = 0;
        int total = PageNumbers.length;
        for (int PageNumber : PageNumbers)
        {
            PdfFileObject page = (PdfFileObject)PdfFile.getPageList().get(PageNumber);
            page.populateRelatedObjects(PdfFile, this.sObjects);
            this.pageNumbers.add(page.number);
            part++;
        }
        this.transHash = this.calcTransHash(startNumber);
        for (Object obj : this.sObjects.values())
        {
            PdfFileObject pfo = (PdfFileObject) obj;
            pfo.transform(transHash);
        }
    }

    private Hashtable<Integer, Integer> calcTransHash(int startNumber)
    {
        Hashtable<Integer, Integer> ht = new Hashtable<Integer, Integer>();
        ArrayList<PdfFileObject> al = new ArrayList<PdfFileObject>();
        for (Object pfo : this.sObjects.values())
        {
            al.add((PdfFileObject)pfo);
        }
        al.sort(new Comparator<PdfFileObject>() {
            @Override
            public int compare(PdfFileObject o1, PdfFileObject o2) {
                return o1.number.compareTo(o2.number);
            }
        });
        int number = startNumber;
        for (PdfFileObject pfo : al)
        {
            ht.put(pfo.number, number);
            number++;
        }
        return ht;
    }

}
