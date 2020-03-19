/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.model;

import java.io.Serializable;


public class PageInfo implements Serializable {

    private static final long serialVersionUID = 101393572507513115L;
    public int getPageCount() {
        return pageCount;
    }
    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
    public void setPageCount() {
        if(this.getPageSize() > 0 && this.getTotalRecords() > 0) {

            this.setPageCount(Double
                    .valueOf( Math.ceil( Double.valueOf( this.getTotalRecords() ) / Double.valueOf( this.getPageSize() ) ) )
                    .intValue());
        }
    }
    public int getPageIndex() {
        return pageIndex;
    }
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }
    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    public long getTotalRecords() {
        return totalRecords;
    }
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    private int pageCount;
    private int pageIndex;
    private int pageSize;
    private long totalRecords;
}
