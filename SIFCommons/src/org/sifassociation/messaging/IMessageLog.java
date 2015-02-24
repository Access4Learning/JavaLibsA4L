/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

/**
 *
 * @author jlovell
 */
public interface IMessageLog {

    /**
     * So we can add to our log.
     *
     * @param current
     *
     * @since 3.0
     */
    void addEntry(LogEntry current);

    /**
     * So we can retrieve the full log entry.
     *
     * @param entryId
     * @return
     *
     * @since 3.0
     */
    String getEntry(String entryId);

    /**
     * So we can update the users view of the log from where we last left off.
     *
     * Note:  Depends on the state of last.
     *
     * @return XML of specified log listings.
     *
     * @since 3.0
     */
    String getLatestListing();

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
    String getMatchListing(String xpath, String namespaceT, String namespaceP);

    /**
     * So we can always get the first page of log entries.
     *
     * @param pageSize
     * @return XML of specified log listings.
     *
     * @since 3.0
     */
    String getNewestListing(int pageSize);

    /**
     * So we can let the user know how many pages there are to inspect.
     *
     * @param pageSize
     * @return Number of pages.
     *
     * @since 3.0
     */
    int getPageCount(int pageSize);

    /**
     * So we can easily get the desired page of log entries.
     *
     * @param page
     * @param pageSize
     * @return XML of specified log listings.
     *
     * @since 3.0
     */
    String getPageListing(int page, int pageSize);
    
    /**
     * So we can stop processing entries when we reach the end.
     * 
     * @return  The total number of entries.
     */
    int getEntryCount();
    
    /**
     * So we can get each entry even as the number grows.
     * 
     * @param index  Place of log from the order it was received.
     * @return  The entry that was added to the log.
     */
    String getEntry(int index);
    
    /**
     * So we can include the log with the results.
     * 
     * @return  All LogEntries
     */
    @Override
    String toString();
}
