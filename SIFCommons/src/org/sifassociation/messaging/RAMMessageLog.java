/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import nu.xom.*;
import org.sifassociation.util.SIFXOMUtil;

/**
 * Reference implementation of IMessageLog (using RAM).
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.0
 */
public class RAMMessageLog implements IMessageLog {
    private List<String> listing = null;
    private Map<String, LogEntry> entries = null;
    private final Object sync = new Object();
    private int last = 0;

    /**
     * So our collections are marked as thread safe.
     * 
     * @since 3.0
     */
    public RAMMessageLog() {
        listing = Collections.synchronizedList(new ArrayList<String>(100));
        entries = Collections.synchronizedMap(new HashMap<String, LogEntry>(100));
    }
    
    /**
     * So we can add to our log.
     * 
     * @param current 
     * 
     * @since 3.0
     */
    @Override
    public void addEntry(LogEntry current) {
        synchronized(sync) {
            listing.add(current.getIdentification());
            entries.put(current.getIdentification(), current);
        }
    }
    
    /**
     * So we can retrieve the full log entry.
     * 
     * @param entryId
     * @return 
     * 
     * @since 3.0
     */
    @Override
    public String getEntry(String entryId) {
        String entry;
        synchronized(sync) {
            entry = entries.get(entryId).toString();
        }
        return entry;
    }
    
    /**
     * Helps page through the log.
     * 
     * Note:  Returns the listing in reverse chronological (newest first) order.
     * Note:  Changes the state of last.
     * 
     * @param begin
     * @param end
     * @return XML of specified log listings.
     * 
     * @see getLatestListing
     * @since 3.0
     */
    protected String getEntriesListing(int begin, int end) {
        // So we stay within the bounds of current entires.
        if(0 > begin) {
            begin = 0;
        }
        if(listing.size() < end) {
            end = listing.size();
        }
        
        // So we know where we left off.
        last = end;
        
        System.out.println(last);  // Debug

        // So we have all the entries.
        Element root = new Element("listing");
        if(begin < end) {
            synchronized(sync) {
                ListIterator<String> position = listing.listIterator(end);
                do {
                    LogEntry current = 
                            entries.get(position.previous().toString());
                    
                    root.appendChild(current.toXOM(true));

                } while(position.hasPrevious() && 
                        position.previousIndex() >= begin);
            }
        }
        
        // So we have the results.
        return root.toXML();
    }
            
    /**
     * So we can update the users view of the log from where we last left off.
     * 
     * Note:  Depends on the state of last.
     * 
     * @return XML of specified log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getLatestListing() {
        
        System.out.println(last);  // Debug
        
        int begin = last;
        int end = entries.size();
        
        return getEntriesListing(begin, end);
    }
    

    /**
     * So we can easily get the desired page of log entries.
     * 
     * @param page
     * @param pageSize
     * @return XML of specified log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getPageListing(int page, int pageSize) {
        int min = page * pageSize;
        int max = (page + 1) * pageSize;
        int end = (entries.size()) - min;
        int begin = (entries.size()) - max;
        
        return getEntriesListing(begin, end);
    }
    
    /**
     * So we can always get the first page of log entries.
     * 
     * @param pageSize
     * @return XML of specified log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getNewestListing(int pageSize) {
        return getPageListing(0, pageSize);
    }
    
    
    /**
     * So we can let the user know how many pages there are to inspect.
     * 
     * @param pageSize
     * @return Number of pages.
     * 
     * @since 3.0
     */
    @Override
    public int getPageCount(int pageSize) {
        int wholePages = entries.size() / pageSize;
        int remainingEntries = entries.size() % pageSize;
        
        // So we take into account a partial last page.
        if(0 < remainingEntries) {
            return wholePages + 1;
        }
        
        return wholePages;
    }

    /**
     * So we can get a listing of log entries matching the given XPath.
     * 
     * @param xpath
     * @param namespaceT "transport"
     * @param namespaceP "payload"
     * @return XML of matching log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getMatchListing(String xpath, String namespaceT, 
            String namespaceP) {
        // So we check each entry for a match.
        Element root = new Element("listing");
        if(0 < xpath.length()) {
            synchronized(sync) {
                ListIterator<String> position = 
                        listing.listIterator(listing.size());
                do {
                    LogEntry current = 
                            entries.get(position.previous().toString());
                    
                    if(current.isMatch(xpath, namespaceT, namespaceP)) {
                        root.appendChild(current.toXOM(true));
                    }

                } while(position.hasPrevious());
            }
        }
        
        // So we have the results.
        return root.toXML();
    }

    @Override
    public int getEntryCount() {
        return this.listing.size();
    }

    @Override
    public String getEntry(int index) {
        return this.entries.get(this.listing.get(index)).toString();
    }
    
    @Override
    public String toString() {
        Element root = new Element("logEntries");
        for(LogEntry entry : entries.values()) {
            root.appendChild(entry.toXOM(false));
        }
        
        return SIFXOMUtil.pretty(root);
    }
}
