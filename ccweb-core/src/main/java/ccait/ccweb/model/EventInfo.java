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


import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.*;

public class EventInfo {

    private class EventComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            Method method1 = (Method)o1;
            Method method2 = (Method)o2;

            Integer sort1 = 0;
            Integer sort2 = 0;

            Order order1 = method1.getAnnotation(Order.class);
            if(order1 != null) {
                sort1 = order1.value();
            }

            Order order2 = method2.getAnnotation(Order.class);
            if(order2 != null) {
                sort2 = order2.value();
            }

            return sort1.compareTo(sort2);
        }
    }

    private Set<Method> onInsertMethodSet;
    private Set<Method> onUpdateMethodSet;
    private Set<Method> onDeleteMethodSet;
    private Set<Method> onListMethodSet;
    private Set<Method> onViewMethodSet;
    private Set<Method> onQueryMethodSet;
    private Set<Method> onResponseMethodSet;
    private Set<Method> onSuccessMethodSet;
    private Set<Method> onErrorMethodSet;
    private Set<Method> onBuildMethodSet;
    private Set<Method> onDownloadMethodSet;
    private Set<Method> onPreviewDocMethodSet;

    private Set<Method> onPlayVideoMethodSet;
    private Set<Method> onUploadMethodSet;

    private Set<Method> onImportMethodSet;
    private Set<Method> onExportMethodSet;
    private String tablename;
    private Class<?> type;
    private int order;

    public EventInfo() {
        onInsertMethodSet = new TreeSet<Method>(new EventComparator());
        onUpdateMethodSet = new TreeSet<Method>(new EventComparator());
        onDeleteMethodSet = new TreeSet<Method>(new EventComparator());
        onListMethodSet = new TreeSet<Method>(new EventComparator());
        onViewMethodSet = new TreeSet<Method>(new EventComparator());
        onQueryMethodSet = new TreeSet<Method>(new EventComparator());
        onResponseMethodSet = new TreeSet<Method>(new EventComparator());
        onSuccessMethodSet = new TreeSet<Method>(new EventComparator());
        onErrorMethodSet = new TreeSet<Method>(new EventComparator());
        onBuildMethodSet = new TreeSet<Method>(new EventComparator());
        onDownloadMethodSet = new TreeSet<Method>(new EventComparator());
        onPreviewDocMethodSet = new TreeSet<Method>(new EventComparator());
        onPlayVideoMethodSet = new TreeSet<Method>(new EventComparator());
        onUploadMethodSet = new TreeSet<Method>(new EventComparator());
        onImportMethodSet = new TreeSet<Method>(new EventComparator());
        onExportMethodSet = new TreeSet<Method>(new EventComparator());
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Set<Method> getOnInsertMethodSet() {
        return onInsertMethodSet;
    }

    public void setOnInsertMethodSet(Set<Method> onInsertMethodSet) {
        this.onInsertMethodSet = onInsertMethodSet;
    }

    public Set<Method> getOnUpdateMethodSet() {
        return onUpdateMethodSet;
    }

    public void setOnUpdateMethodSet(Set<Method> onUpdateMethodSet) {
        this.onUpdateMethodSet = onUpdateMethodSet;
    }

    public Set<Method> getOnDeleteMethodSet() {
        return onDeleteMethodSet;
    }

    public void setOnDeleteMethodSet(Set<Method> onDeleteMethodSet) {
        this.onDeleteMethodSet = onDeleteMethodSet;
    }

    public Set<Method> getOnListMethodSet() {
        return onListMethodSet;
    }

    public void setOnListMethodSet(Set<Method> onListMethodSet) {
        this.onListMethodSet = onListMethodSet;
    }

    public Set<Method> getOnViewMethodSet() {
        return onViewMethodSet;
    }

    public void setOnViewMethodSet(Set<Method> onViewMethodSet) {
        this.onViewMethodSet = onViewMethodSet;
    }

    public Set<Method> getOnQueryMethodSet() {
        return onQueryMethodSet;
    }

    public void setOnQueryMethodSet(Set<Method> onQueryMethodSet) {
        this.onQueryMethodSet = onQueryMethodSet;
    }

    public Set<Method> getOnResponseMethodSet() {
        return onResponseMethodSet;
    }

    public void setOnResponseMethodSet(Set<Method> onResponseMethodSet) {
        this.onResponseMethodSet = onResponseMethodSet;
    }

    public Set<Method> getOnSuccessMethodSet() {
        return onSuccessMethodSet;
    }

    public void setOnSuccessMethodSet(Set<Method> onSuccessMethodSet) {
        this.onSuccessMethodSet = onSuccessMethodSet;
    }

    public Set<Method> getOnErrorMethodSet() {
        return onErrorMethodSet;
    }

    public void setOnErrorMethodSet(Set<Method> onErrorMethodSet) {
        this.onErrorMethodSet = onErrorMethodSet;
    }

    public Set<Method> getOnBuildMethodSet() {
        return onBuildMethodSet;
    }

    public void setOnBuildMethodSet(Set<Method> onBuildMethodSet) {
        this.onBuildMethodSet = onBuildMethodSet;
    }

    public Set<Method> getOnDownloadMethodSet() {
        return onDownloadMethodSet;
    }

    public Set<Method> getOnPreviewDocMethodSet() {
        return onPreviewDocMethodSet;
    }

    public Set<Method> getOnUploadMethodSet() {
        return onUploadMethodSet;
    }

    public Set<Method> getOnImportMethodSet() {
        return onImportMethodSet;
    }

    public void setOnImportMethodSet(Set<Method> onImportMethodSet) {
        this.onImportMethodSet = onImportMethodSet;
    }

    public Set<Method> getOnExportMethodSet() {
        return onExportMethodSet;
    }

    public void setOnExportMethodSet(Set<Method> onExportMethodSet) {
        this.onExportMethodSet = onExportMethodSet;
    }

    public Set<Method> getOnPlayVideoMethodSet() {
        return onPlayVideoMethodSet;
    }

    public void setOnPlayVideoMethodSet(Set<Method> onPlayVideoMethodSet) {
        this.onPlayVideoMethodSet = onPlayVideoMethodSet;
    }

}
