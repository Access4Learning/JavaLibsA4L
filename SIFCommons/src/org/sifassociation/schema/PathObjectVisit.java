/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaUse;

/**
 * Builds XPaths to the objects from the collections.
 * Designed for use with a SIF Collection XSD.
 * Supports objects with no collection (since 3.2).
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.2
 */
public class PathObjectVisit implements IElementVisit {

    private final List<String> crumbs;
    private final List<Boolean> mandatories;
    private final List<XPathPlus> paths;
    
    public PathObjectVisit() {
        crumbs = new ArrayList<>();
        mandatories = new ArrayList<>();
        paths = new ArrayList<>();
    } 
    
    private String getCurrentXPath()  {
        StringBuilder xpath = new StringBuilder();
        for(String current : crumbs) {
            xpath.append('/');
            xpath.append(current);
        }
        return xpath.toString();
    }
    
    // So we can know if ANY OBJECT part of the XPath allows for the element or 
    // attribute to be optional.
    private boolean isMandatory() {
        // So the collection doesn't make everything optional.
        int depth = 0;
        for(Boolean mandatory : mandatories) {
            if(false == mandatory && 2 <= depth) {
                return false;
            }
            depth++;
        }
        return true;
    }
    
    @Override
    public void head(XmlSchemaObject object) {
        // So we only go one level deep.
        if(object instanceof XmlSchemaElement) { 
            XmlSchemaElement element = (XmlSchemaElement)object;
            // So we know who our parents are.
            String name = element.getName();
            if(null != name) {
                crumbs.add(name);
            }
            mandatories.add(0 < element.getMinOccurs());
        }
        if(object instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            crumbs.add("@" + attribute.getName());
            mandatories.add(XmlSchemaUse.REQUIRED == attribute.getUse());
        }
    }
    
    @Override
    public void tail(XmlSchemaObject object) { 
        // So we have the root to add when no collection is found.
        if(1 == crumbs.size()) {
            XPathPlus root = new XPathPlus(
                    getCurrentXPath(), isMandatory(), getAnnotation(object));
            root.setType(getType(object));
            if(object instanceof XmlSchemaElement) {  // To Do:  Consider this insteady of "paths.remove(paths.size()-1);" below!!!
                int index = mandatories.size() - 1;
                if(0 <= index) {
                    root.setMandatory(mandatories.get(index));
                }
                if(object instanceof XmlSchemaElement) {
                    XmlSchemaElement element = (XmlSchemaElement)object; 
                    QName name = element.getQName();
                    if(null != name) {
                        root.setNamespace(name.getNamespaceURI());
                    }
                }
                if(!paths.isEmpty()) {
                    // So we drop all children when each is not the object.
                    // So we only add the root when there is no collection.
                    XPathPlus child = paths.get(paths.size()-1);
                    String[] split = child.getPath().split("/");
                    if(3 == split.length && 
                            0 != split[1].compareTo(split[2] + 's')) {
                        String start = root.getPath() + '/';
                        // So we don't modify the list while looping through it.
                        List<XPathPlus> nonObjects = new ArrayList<>();
                        for(XPathPlus path : paths) {
                            if(path.getPath().startsWith(start)) {
                                nonObjects.add(path);
                            }
                        }
                        for(XPathPlus nonObject : nonObjects) {
                            paths.remove(nonObject);
                        }
                        paths.add(root);
                    }
                }
            }
        }    
        // So we generate all our XPaths when we reach the bottom of the tree.
        if(2 == crumbs.size()) {
            XPathPlus current = new XPathPlus(
                    getCurrentXPath(), isMandatory(), getAnnotation(object));
            current.setType(getType(object));
            if(object instanceof XmlSchemaElement) {
                int index = mandatories.size() - 1;
                if(0 <= index) {
                    current.setMandatory(mandatories.get(index));
                }
                if(object instanceof XmlSchemaElement) {
                    XmlSchemaElement element = (XmlSchemaElement)object; 
                    QName name = element.getQName();
                    if(null != name) {
                        current.setNamespace(name.getNamespaceURI());
                    }
                }
                paths.add(current);
            }
        }
        // So we forget our parents other children.
        if(object instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            int crumbIndex = crumbs.size()-1;
            if(0 < crumbs.size() && null != attribute.getName()) {
                crumbs.remove(crumbIndex);
            }
            int mandatoryIndex = mandatories.size()-1;
            if(0 < mandatories.size()) {
                mandatories.remove(mandatoryIndex);
            }
        }
        else if(object instanceof XmlSchemaElement) { 
            XmlSchemaElement element = (XmlSchemaElement)object;
            int crumbIndex = crumbs.size()-1;
            if(0 < crumbs.size() && null != element.getName()) {
                crumbs.remove(crumbIndex);
            }
            int mandatoryIndex = mandatories.size()-1;
            if(0 < mandatories.size()) {
                mandatories.remove(mandatoryIndex);
            }
            //System.out.println("\t" + this.getCurrentXPath());  // Debug
        }
    }

    // So we get any annotations consistently.
    private static XmlSchemaAnnotation getAnnotation(XmlSchemaObject object) {
        if(object instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)object;
            if(null != element.getSchemaType() && 
                    null != element.getSchemaType().getAnnotation()) {
                XmlSchemaAnnotation present = 
                        element.getSchemaType().getAnnotation();
                return present;
            }
        }           
        else if(object instanceof XmlSchemaAttribute) {    
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            if(null != attribute.getSchemaType() && 
                    null != attribute.getSchemaType().getAnnotation()) {
                XmlSchemaAnnotation present = 
                        attribute.getSchemaType().getAnnotation();
                return present;
            }
        }
        return null;
    }
    
    // So we get the type consistently.
    private static QName getType(XmlSchemaObject object) {
        if(object instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)object;
            return element.getSchemaTypeName();
        }           
        else if(object instanceof XmlSchemaAttribute) {    
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            return attribute.getSchemaTypeName();
        }
        return null;        
    }
    
    // So we can get the results.
    public List<XPathPlus> getPaths() {
        return paths;
    }
    
}
