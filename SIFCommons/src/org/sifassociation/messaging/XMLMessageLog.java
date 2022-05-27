/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.*;
import org.sifassociation.util.SIFEXistUtil;
import org.sifassociation.util.SIFXOMUtil;

/**
 * Production implementation of IMessageLog (using eXist).
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.0
 */
public class XMLMessageLog implements IMessageLog {    
    private List<String> listing = null;
    private String connectorId = "";
    private SIFEXistUtil rest = null;
    private final Object sync = new Object();
    private Map<String, Integer> last = null;
    
    /**
     * So we know what we have in RAM.
     * So what we have is on disk.
     * 
     * @param id
     * @param url
     * @param user
     * @param password 
     */
    public XMLMessageLog(String id, String url, String user, String password) {
        this.listing = Collections.synchronizedList(new ArrayList<String>(100));
        this.connectorId = id;
        this.rest = new SIFEXistUtil(url, user, password);
        last = Collections.synchronizedMap(new HashMap<String, Integer>(5));
    }
    
    private String getRelativePath(String name) {
        return "/" + this.connectorId + "/" + name + ".xml";
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
            String identification = current.getIdentification();
            listing.add(identification);
            String path = this.getRelativePath(identification);
            try {
                this.rest.createResource(path, current.toString());
            } catch (MalformedURLException ex) {
                Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException | ParsingException ex) {
                Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        String entry = "";
        
        String path = this.getRelativePath(entryId);
        try {
            entry = rest.readResource(path);
        } catch (MalformedURLException ex) {
            Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ParsingException ex) {
            Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return entry;
    }
    
    /**
     * Manages retrieval of last indexes.
     * 
     * @see setLastIndex
     * 
     * @param webSessionID
     * @return lastIndex or zero
     */
    protected int getLastIndex(String webSessionID) {
        // So we get where to start updating the log for this web session.
        Integer index;
        index = last.get(webSessionID);
        
        // So a new websession gets initalized.
        if(null == index) {
            index = 0;
            setLastIndex(webSessionID, index);
        }
        
        return index;
    }
    
    /**
     * Manages setting of last indexes.
     * 
     * @see getLastIndex
     * 
     * @param webSessionID
     * @param index 
     */
    protected void setLastIndex(String webSessionID, int index) {
        last.put(webSessionID, index);
    }
    
    /**
     * Helps page through the log.
     * 
     * Note:  Returns the listing in reverse chronological (newest first) order.
     * Note:  Changes the state of last.
     * 
     * @param webSessionID
     * @param begin
     * @param end
     * @return XML of specified log listings.
     * 
     * @see getLatestListing
     * @since 3.0
     */
    protected String getEntriesListing(String webSessionID, int begin, int end) {
        // So we stay within the bounds of current entires.
        if(0 > begin) {
            begin = 0;
        }
        if(listing.size() < end) {
            end = listing.size();
        }
        
        // So we know where we left off.
        setLastIndex(webSessionID, end);
        
        //System.out.println(end);  // Debug

        // So we have all the entries.
        Element root = new Element("listing");
        if(begin < end) {
            synchronized(sync) {
                String path;
                ListIterator<String> position = listing.listIterator(end);
                do {
                    path = this.getRelativePath(position.previous());
                    LogEntry current = new LogEntry();
                    try {
                        current.parse(this.rest.readResource(path));
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException | ParsingException ex) {
                        Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
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
     * @param webSessionID
     * @return XML of specified log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getLatestListing(String webSessionID) {
        
        //System.out.println(last);  // Debug
        
        int begin = getLastIndex(webSessionID);
        int end = this.listing.size();
        
        return getEntriesListing(webSessionID, begin, end);
    }
    

    /**
     * So we can easily get the desired page of log entries.
     * 
     * @param webSessionID
     * @param page
     * @param pageSize
     * @return XML of specified log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getPageListing(String webSessionID, int page, int pageSize) {
        int min = page * pageSize;
        int max = (page + 1) * pageSize;
        int end = (this.listing.size()) - min;
        int begin = (this.listing.size()) - max;
        
        return getEntriesListing(webSessionID, begin, end);
    }
    
    /**
     * So we can always get the first page of log entries.
     * 
     * @param webSessionID
     * @param pageSize
     * @return XML of specified log listings.
     * 
     * @since 3.0
     */
    @Override
    public String getNewestListing(String webSessionID, int pageSize) {
        return getPageListing(webSessionID, 0, pageSize);
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
        int wholePages = this.listing.size() / pageSize;
        int remainingEntries = this.listing.size() % pageSize;
        
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
    // To Do:  Use eXists XQuery mechinism to make this more efficient.
    @Override
    public String getMatchListing(String xpath, String namespaceT, 
            String namespaceP) {
        // So we check each entry for a match.
        Element root = new Element("listing");
        if(0 < xpath.length()) {
            synchronized(sync) {
                String path;
                ListIterator<String> position = 
                        listing.listIterator(listing.size());
                do {
                    path = this.getRelativePath(position.previous());
                    LogEntry current = new LogEntry();
                    try {
                        current.parse(this.rest.readResource(path));
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException | ParsingException ex) {
                        Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
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
        String id = this.listing.get(index);
        return this.getEntry(id);
    }
    
    @Override
    public String toString() {
        Element root = new Element("logEntries");
        synchronized(sync) {
            String path;
            for(String id : listing) {
                path = this.getRelativePath(id);
                LogEntry entry = new LogEntry();
                try {
                    entry.parse(this.rest.readResource(path));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException | ParsingException ex) {
                    Logger.getLogger(XMLMessageLog.class.getName()).log(Level.SEVERE, null, ex);
                }            

                root.appendChild(entry.toXOM(false));
            }
        }
        
        return SIFXOMUtil.pretty(root);
    }
    
}
