/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaPatternFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
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
    private QName type;
    
    public PathMandatoryVisit() {
        crumbs = new ArrayList<String>();
        mandatories = new ArrayList<Boolean>();
        annotation = null;
        paths = new ArrayList<XPathPlus>();
        type = new QName("");
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
            type = element.getSchemaTypeName();
        }
        if(object instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            crumbs.add("@" + attribute.getName());
            mandatories.add(XmlSchemaUse.REQUIRED == attribute.getUse());
            annotation = attribute.getAnnotation();
            type = attribute.getSchemaTypeName();
        }

    }
    
    @Override
    public void tail(XmlSchemaObject object) { 
        // So we generate all our XPaths when we reach the bottom of the tree.
        if(object instanceof XmlSchemaSimpleType)
        {
            XPathPlus current = new XPathPlus(
                    getCurrentXPath(), isMandatory(), annotation);
            current.setType(type);
            
            // So we can share enumerations & patterns in the documentation.
            try {
                XmlSchemaSimpleTypeContent content = 
                        ((XmlSchemaSimpleType)object).getContent();
                XmlSchemaSimpleTypeRestriction restriction = null;
                if(content instanceof XmlSchemaSimpleTypeRestriction) {
                    restriction = (XmlSchemaSimpleTypeRestriction)content;
                }
                List<XmlSchemaFacet> facets = restriction.getFacets();
                List<String> enums = new ArrayList<>();
                List<String> patts = new ArrayList<>();
                for (XmlSchemaFacet facet : facets) {
                    // Enumerations
                    if(facet instanceof XmlSchemaEnumerationFacet) {
                        XmlSchemaEnumerationFacet enumFacet = 
                                (XmlSchemaEnumerationFacet)facet;
                        enums.add(enumFacet.getValue().toString());
                    }
                    // Patterns
                    if(facet instanceof XmlSchemaPatternFacet) {
                        XmlSchemaPatternFacet pattFacet = 
                                (XmlSchemaPatternFacet)facet;
                        patts.add(pattFacet.getValue().toString());
                    }                    
                }
                current.setEnumerations(formatList(enums));
                current.setPatterns(formatList(patts));
            } catch (NullPointerException ex) {
                // It is okay if there are no facets.
            }
            
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
    
    // So we can condense lists of strings consistently.
    public static String formatList(List<String> strings) {
        String combined = "";
        for (Iterator<String> it = strings.iterator(); it.hasNext();) {
            String individual = it.next();
            if(combined.isEmpty()) {
                combined = combined + individual;
            }
            else {
                combined = combined + ", " + individual;
            }
        }
        return combined;
    }
}
