/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import org.apache.ws.commons.schema.XmlSchemaObject;

/**
 * So we can reuse our XmlSchema tree traversal code.
 * Assumes traversal will hand us all encountered types in tree order.
 * Assumes traversal will ensure all paths end is a simpleType.
 * Therefore implementer can decide how much action to take.
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.3
 */
public interface IElementVisit {
    
    /**
     * So we can work with the element (and its scope) before its children.
     * 
     * @param object  The current XML Schema object.
     */
    void head(XmlSchemaObject object);

    /**
     * So we can work with the element (and its scope) after its children.
     * 
     * @param object  The current XML Schema object.
     * @param amassed  The amassed scope from the element's parents.
     */
    void tail(XmlSchemaObject object);
}
