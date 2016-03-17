/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import java.util.ArrayList;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaUse;

/**
 * Builds XPaths and whether they are mandatory.
 * Designed for use with a SIF Collection XSD.
 * 
 * @author jlovell
 * @since 3.0
 */
public class PathMandatoryVisit implements IElementVisit {

    private List<String> crumbs;
    private List<Boolean> mandatories;
    private XmlSchemaAnnotation annotation;
    private List<XPathPlus> paths;
    
    public PathMandatoryVisit() {
        crumbs = new ArrayList<String>();
        mandatories = new ArrayList<Boolean>();
        annotation = null;
        paths = new ArrayList<XPathPlus>();
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
        if(object instanceof XmlSchemaElement) { 
            XmlSchemaElement element = (XmlSchemaElement)object;
            // So we know who our parents are.
            String name = element.getName();
            if(null != name) {
                crumbs.add(name);
            }
            mandatories.add(0 < element.getMinOccurs());
            annotation = element.getAnnotation();
        }
        if(object instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            crumbs.add("@" + attribute.getName());
            mandatories.add(XmlSchemaUse.REQUIRED == attribute.getUse());
            annotation = attribute.getAnnotation();
        }
        
    }
    
    @Override
    public void tail(XmlSchemaObject object) { 
        // So we generate all our XPaths when we reach the bottom of the tree.
        if(object instanceof XmlSchemaSimpleType)
        {
            paths.add((new XPathPlus(
                    getCurrentXPath(), isMandatory(), annotation)));
        }
        // So we forget our parents other children.
        else if(object instanceof XmlSchemaAttribute) {
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

    // So we can get the results.
    public List<XPathPlus> getPaths() {
        return paths;
    }
    
}
