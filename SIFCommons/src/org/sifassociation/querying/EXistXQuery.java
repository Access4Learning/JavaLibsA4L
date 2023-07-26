/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.querying;

import java.lang.reflect.InvocationTargetException;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;
import org.exist.xmldb.EXistResource;

/**
 * Simple class to hold the boilerplate code for making XQueries (of eXist).
 * Goal: Generic enough that an XQuery interface is simply derived from it.
 * 
 * Almost immutable (see the iterator);
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class EXistXQuery implements IXQuery {

    private String target;  // So we can confirm what collection we are targeting.
    private XQueryService xQueryService;  // So we have to reuse (helps scale).
    private ResourceIterator iterator;  // So we leverage builtin scalability.
    
    /**
     * 
     * @param target (the URL to the collection to be queried).
     * @throws ClassNotFoundException (when the driver cannot be located).
     * @throws InstantiationException (when the DB instance cannot be created).
     * @throws IllegalAccessException (if you are not allowed to create the DB).
     * @throws XMLDBException (anytime a database operation fails).
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.reflect.InvocationTargetException
     * @since 3.0
     */
    public EXistXQuery(String target) throws ClassNotFoundException, InstantiationException, IllegalAccessException, XMLDBException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        // So we can confirm what collection we are targeting.
        this.target = target;
        
        // So we have an eXist database connection to the target.
        Class driver = Class.forName("org.exist.xmldb.DatabaseImpl");
        Database database = Database.class.getDeclaredConstructor(driver).newInstance();  // To Do:  Test and see if I have this right!
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);
        Collection collection = DatabaseManager.getCollection(
                this.target.toString());
        this.xQueryService = (XQueryService)collection.getService(
                "XQueryService", "1.0");
        this.xQueryService.setProperty("indent", "yes");
        
        // So all our member variables are initialized.
        this.iterator = null;
        
        // So we cleanup where need be.
        if(collection != null) {
            collection.close();
        }
    }
    
    /**
     * Runs the specified query.
     * 
     * Note:  This return type does not scale to large result sets.
     * 
     * @param query  (XQuery to run against the classes collection.)
     * @see getNext()
     * @since 3.0
     */
    @Override
    public void run(String query) throws XMLDBException {
        ResourceSet results = this.xQueryService.query(query);
        this.iterator = results.getIterator();
    }    

    /**
     * So we can process query results one at a time.
     * 
     * Note:  This approach is used in order to leverage the built in 
     *        efficiencies (primarily RAM usage) of the employed engine.
     * 
     * @return The next query result from the previously run query, else empty.
     * @throws XMLDBException 
     * @since 3.0
     */
    @Override
    public String getNext() throws XMLDBException {
        String result = "";
        
        Resource resource = null;
        if(iterator.hasMoreResources()) {
            try {
                resource = iterator.nextResource();
                result = (String)resource.getContent();
            } finally {
                ((EXistResource)resource).freeResources();
            }
        }
        
        return result;
    }
    
    /**
     * So we know when we are done.
     *
     * Note:  This approach is used in order to leverage the built in
     *        efficiencies (primarily RAM usage) of the employed engine.
     *
     * @return True if the collection contains more records, otherwise false.
     * @throws XMLDBException
     * @since 3.0
     */
    @Override
    public boolean hasNext() throws XMLDBException {
        return iterator.hasMoreResources();
    }
    
    /**
     * Quickly check if two instances target the same collection.
     * 
     * Note:  So that UNIX paths may be used comparison is case sensitive!
     * 
     * @param o (Object to compare.)
     * @return True if the two objects targets match, otherwise false.
     * @since 3.0
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EXistXQuery))
            return false;
        return o.toString().equals(target);
    }
    
    /**
     * So we support hash collections properly.
     * 
     * @return The hash of the target collection.
     * @since 3.0
     */
    @Override
    public int hashCode() {
        return target.hashCode();
    }
    
    /**
     * So the collection targeted by this object can be expressed to the user.
     * 
     * @return The target path to the collection.
     * @see getNext()
     * @since 3.0
     */
    @Override
    public String toString() {
        return this.target;
    }
}
