/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaPatternFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Builds XPaths and whether they are mandatory.
 * Designed for use with a SIF Collection XSD.
 * Useful when you want only the avaliable data fields (elements and attributes).
 * 
 * @author jlovell
 * @since 3.0
 */
public class PathMandatoryVisit implements IElementVisit {

    private List<String> crumbs;
    private List<Boolean> mandatories;
    private List<XmlSchemaAnnotation> annotations;
    private List<XPathPlus> paths;
    private List<QName> types;
    
    public PathMandatoryVisit() {
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
        // So we include wildcards.
        if(object instanceof XmlSchemaAny) {
            XmlSchemaAnnotation annotation = this.getCurrentAnnotation();
            Map<String, String> appInfos = SIFXmlSchemaUtil.getAppInfos(annotation);
            String documentation = SIFXmlSchemaUtil.getDocumentation(annotation);
            XPathPlus current = new XPathPlus(
                    getCurrentXPath(), isMandatory(), annotation);
                        current.setAppInfos(appInfos);
            current.setDocumentation(documentation);
            current.setType(new QName("any"));
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
            // So restricted paths keep there outer most requirments.
            int i = paths.indexOf(current);
            if(-1 == i) {
                paths.add(current);
            }
            //System.out.println("\t" + this.getCurrentXPath());  // Debug
        }         

        // So we generate all our XPaths when we reach the bottom of the tree.
        if(object instanceof XmlSchemaSimpleType)
        {
            // So we include the best annotation/documentation possible.
            // Note:  Must be set and used wihtout impacting the XML tree.
            XmlSchemaAnnotation annotation = this.getCurrentAnnotation();   
            Map<String, String> appInfos = SIFXmlSchemaUtil.getAppInfos(annotation);
            String documentation = SIFXmlSchemaUtil.getDocumentation(annotation);
            if(null != appInfos) {
                String last = appInfos.getOrDefault("useLastAnnotation", "false");
                if("true".equals(last)) {
                    for(int i = annotations.size()-2; 0 <= i; i--) {
                        XmlSchemaAnnotation lastAnnotation = annotations.get(i);
                        if(null != lastAnnotation && !lastAnnotation.getItems().isEmpty()) {
                            // appinfos
                            Map<String, String> temp = new HashMap<>();
                            Map<String, String> lastAppInfos = 
                                    SIFXmlSchemaUtil.getAppInfos(lastAnnotation);
                            if(null != lastAppInfos) {
                                temp.putAll(lastAppInfos);
                                temp.putAll(appInfos);
                                appInfos = temp;
                            }
                            // documentation
                            if(documentation.isEmpty()) {
                                documentation = SIFXmlSchemaUtil.getDocumentation(lastAnnotation);
                            }
                            // So we only copy the last annotation.
                            break;
                        }
                    }
                }
            }
            
            XPathPlus current = new XPathPlus(
                    getCurrentXPath(), isMandatory(), annotation);
            current.setAppInfos(appInfos);
            current.setDocumentation(documentation);
            current.setType(this.getCurrentType());
            
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
            // So restricted paths keep there outer most requirments.
            int i = paths.indexOf(current);
            if(-1 == i) {
                paths.add(current);
            }
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
