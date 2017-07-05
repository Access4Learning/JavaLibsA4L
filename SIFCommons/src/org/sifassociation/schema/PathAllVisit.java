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
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaUse;

/**
 * Builds ALL XPaths and whether they are mandatory.
 * Designed for use with a SIF Collection XSD.
 * Useful when you want a complete view of an object not just its elements.
 * 
 * @author jlovell
 * @since 3.0
 */
public class PathAllVisit implements IElementVisit {

    private List<String> crumbs;
    private List<Boolean> mandatories;
    private List<XmlSchemaAnnotation> annotations;
    private List<XPathPlus> paths;
    private List<QName> types;
    
    public PathAllVisit() {
        crumbs = new ArrayList<String>();
        mandatories = new ArrayList<Boolean>();
        annotations = new ArrayList<XmlSchemaAnnotation>();
        paths = new ArrayList<XPathPlus>();
        types = new ArrayList<QName>();
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
    
    private XmlSchemaAnnotation getCurrentAnnotation() {
        int i = annotations.size()-1;
        return annotations.get(i);
    }
    
    private QName getCurrentType() {
        int i = types.size()-1;
        return types.get(i);
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
            annotations.add(element.getAnnotation());
            types.add(element.getSchemaTypeName());
        }
        if(object instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)object;
            crumbs.add("@" + attribute.getName());
            mandatories.add(XmlSchemaUse.REQUIRED == attribute.getUse());
            annotations.add(attribute.getAnnotation());
            types.add(attribute.getSchemaTypeName());
        }
    }
    
    @Override
    public void tail(XmlSchemaObject object) { 
        // So we generate all our XPaths.
        if( object instanceof XmlSchemaAttribute || object instanceof XmlSchemaElement)
        {
            XPathPlus current = new XPathPlus(
                    getCurrentXPath(), isMandatory(), this.getCurrentAnnotation());
            current.setType(this.getCurrentType());
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
                else {
                    // So we don't let elements without names mess us up.
                    // Instead add the parents stuff to the child.
                    current = paths.get(paths.size()-1);
                }
            }
            if(object instanceof XmlSchemaParticle) {
                XmlSchemaParticle particle = (XmlSchemaParticle)object;
                if(particle.getMaxOccurs() != XmlSchemaParticle.DEFAULT_MAX_OCCURS) {
                    current.setRepeatable(true);
                    //System.out.println("Set Repeatable:  " + current.getPath());  // Debug
                }
            }
            // So a single path can be built up from multiple schema elements.
            int i = paths.indexOf(current);
            if(-1 == i) {
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
            int annotationIndex = annotations.size()-1;
            if(0 < annotations.size() && null != attribute.getName()) {
                annotations.remove(annotationIndex);
            }
            int typeIndex = types.size()-1;
            if(0 < types.size()) {
                types.remove(typeIndex);
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
            int annotationIndex = annotations.size()-1;
            if(0 < annotations.size() && null != element.getName()) {
                annotations.remove(annotationIndex);
            }
            int typeIndex = types.size()-1;
            if(0 < types.size()) {
                types.remove(typeIndex);
            }       
            //System.out.println("\t" + this.getCurrentXPath());  // Debug
        }
    }

    // So we can get the results.
    public List<XPathPlus> getPaths() {
        return paths;
    }
    
}
