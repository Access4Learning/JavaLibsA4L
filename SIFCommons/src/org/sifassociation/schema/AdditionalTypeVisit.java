/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;
import static org.sifassociation.schema.PathMandatoryVisit.formatList;

/**
 * Gets (simple) types that may elude PathAllVisit.
 * 
 * @author jlovell
 */
public class AdditionalTypeVisit implements IElementVisit {

    private List<XPathPlus> types;
    private List<String> knownNS;

    public AdditionalTypeVisit() {
        types = new ArrayList();
        knownNS = new ArrayList();
        knownNS.add("http://www.w3.org/2001/XMLSchema");
        knownNS.add("http://www.w3.org/2001/XMLSchema-instance");
        knownNS.add("http://www.w3.org/2001/XInclude");
        knownNS.add("http://www.w3.org/1999/xhtml");
        knownNS.add("http://json.org/");
    }
    
    @Override
    public void head(XmlSchemaObject object) {
        
    }

    @Override
    public void tail(XmlSchemaObject object) {
        // So we only work with what we don't in PathAllVisit.
        if(!(object instanceof XmlSchemaAttribute || object instanceof XmlSchemaElement)) {
            // So we only work with simple types (complex ones can be derived).
            if(object instanceof XmlSchemaSimpleType) {
                XmlSchemaSimpleType simple = (XmlSchemaSimpleType)object;
                // So we don't redefine known simple types.
                QName qName = simple.getQName();
                if(null != knownNS && null != qName &&
                        knownNS.contains(qName.getNamespaceURI())) {
                    return;
                }
                XmlSchemaAnnotation annotation = simple.getAnnotation();
                Map<String, String> appInfos = SIFXmlSchemaUtil.getAppInfos(annotation);
                String documentation = SIFXmlSchemaUtil.getDocumentation(annotation);            
                XPathPlus current = new XPathPlus("/" + simple.getName(), false, 
                        annotation);
                current.setAppInfos(appInfos);
                current.setDocumentation(documentation);                            
                try {
                    XmlSchemaSimpleTypeContent content = 
                            ((XmlSchemaSimpleType)object).getContent();
                    current.setTypeDocumentation(
                            SIFXmlSchemaUtil.getDocumentation(
                                    content.getAnnotation()));
                    XmlSchemaSimpleTypeRestriction restriction = null;
                    if(content instanceof XmlSchemaSimpleTypeRestriction) {
                        restriction = (XmlSchemaSimpleTypeRestriction)content;
                    }
                    if(null != restriction) {
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
                        current.setType(restriction.getBaseTypeName());
                    }
                    XmlSchemaSimpleTypeUnion union = null;
                    if(union instanceof XmlSchemaSimpleTypeUnion) {
                        union = (XmlSchemaSimpleTypeUnion)content;
                    }
                    if(null != union) {
                        System.out.println("Error: Unsupported union detected (by AdditinalTypeVisit).");
                    }                    
                } catch (NullPointerException ex) {
                    // It is okay if there are no facets.
                }                
            
                types.add(current);
            }
        }
        
    }

    public List<XPathPlus> getTypes() {
        return types;
    }
    
}
