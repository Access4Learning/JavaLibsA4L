/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import nu.xom.Element;

/**
 * So we can reuse our XOM tree traversal code.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public interface IElementAction {
    
    /**
     * What to do (to the element when we encounter it).
     * 
     * @param node The current element in the tree.
     * @param indicator The modification to be made.
     * @since 3.0
     */
    void action(Element node, String indicator);

}
