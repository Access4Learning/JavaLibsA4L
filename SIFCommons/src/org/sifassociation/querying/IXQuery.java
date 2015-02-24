/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.querying;

import org.xmldb.api.base.XMLDBException;

/**
 * Interface intended to structure the boilerplate code around for XQueries.
 * 
 * @author jlovell
 */
public interface IXQuery {
    
    /**
     * Runs the specified query.
     *
     * Note:  This return type does not scale to large result sets.
     *
     * @param query  (XQuery to run against the classes collection.)
     * @see getNext()
     * @since 3.0
     */
    void run(String query) throws Exception;
    
    /**
     * So we can process query results one at a time.
     *
     * Note:  This approach is used in order to leverage the built in
     *        efficiencies (primarily RAM usage) of the employed engine.
     *
     * @return The next query result from the previously run query, else empty.
     * @throws Exception
     * @since 3.0
     */
    String getNext() throws Exception;    
    
    /**
     * So we know when we are done.
     *
     * Note:  This approach is used in order to leverage the built in
     *        efficiencies (primarily RAM usage) of the employed engine.
     *
     * @return True if the collection contains more records, otherwise false.
     * @throws Exception
     * @since 3.0
     */
    boolean hasNext() throws Exception;
    
    /**
     * Quickly check if two instances target the same collection.
     *
     * Note:  So that UNIX paths may be used, comparison is case sensitive!
     *
     * @param o (Object to compare.)
     * @return True if the two objects targets match, otherwise false.
     * @since 3.0
     */
    @Override
    boolean equals(Object o);

    /**
     * So we support hash collections properly.
     *
     * @return The hash of the target collection.
     * @since 3.0
     */
    @Override
    int hashCode();

    /**
     * So the collection targeted by this object can be expressed to the user.
     *
     * @return The target path to the collection.
     * @see getNext()
     * @since 3.0
     */
    @Override
    String toString();
    
}
