package ccait.ccweb.model;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class EventInfoSet {

    private class EventComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            EventInfo eventInfo1 = (EventInfo)o1;
            EventInfo eventInfo2 = (EventInfo)o2;

            return eventInfo1.getOrder().compareTo(eventInfo2.getOrder());
        }
    }

    private Set<EventInfo> eventSet;

    public EventInfoSet() {
        eventSet = new TreeSet<EventInfo>(new EventComparator());
    }

    public Set<EventInfo> getEventSet() {
        return eventSet;
    }

    public void setEventSet(Set<EventInfo> eventSet) {
        this.eventSet = eventSet;
    }
}
