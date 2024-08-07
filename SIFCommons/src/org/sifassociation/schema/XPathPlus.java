/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;

/**
 * So we can know both an XPath and its usage information.
 * Note:  The the various visitors set different sets of these variables.
 * 
 * @author jlovell
 * @since 3.0
 */
public class XPathPlus {
    private String path;
    private boolean compleatlyMandatory;
    private Map<String, String> appInfos;
    private XmlSchemaAnnotation annotation;
    private String documentation;
    private boolean mandatory;
    private String namespace;
    private QName type;
    private String typeDocumentation;
    private String enumerations;
    private String patterns;
    private boolean repeatable;
    private boolean complexExtension;
    private String length;
    private String maxLength;
    private String minLength;

    XPathPlus() {
        this.path = "";
        this.compleatlyMandatory = true;
        this.annotation = null;
        this.appInfos = null;
        this.documentation = "";
        this.mandatory = false;
        this.namespace = "";
        this.type = new QName("", "", "");
        this.typeDocumentation = "";
        this.enumerations = "";
        this.patterns = "";
        this.repeatable = false;
        this.complexExtension = false;
        this.length = "";
        this.maxLength = "";
        this.minLength = "";
    }
    
    public XPathPlus(
            String path, 
            boolean compleatlyMandatory, 
            XmlSchemaAnnotation annotation) {
        this.path = path;
        this.compleatlyMandatory = compleatlyMandatory;
        this.annotation = annotation;
        this.appInfos = null;
        this.documentation = "";
        this.mandatory = false;
        this.namespace = "";
        this.type = new QName("", "", "");
        this.typeDocumentation = "";
        this.enumerations = "";
        this.patterns = "";
        this.repeatable = false;
        this.complexExtension = false;
        this.length = "";
        this.maxLength = "";
        this.minLength = "";        
    }

    public String getPath() {
        return path;
    }

    public void setPath(String xPath) {
        path = xPath;
    }
    
    public String getName() {
        String name = "";
        int last = path.lastIndexOf('/');
        if(last >= 0) {
            name = path.substring(last+1, path.length());
        }
        return name;
    }
    
    // Removes collection and object root before return the xpath.
    public String getSIF2Name() {
        String name = "";
        String[] split = path.split("/");
        if(null != split && split.length > 2) {
            for(int i = 3; i < split.length; i++) {
                if(!name.isEmpty()) {
                    name = name + "/";
                }
                name = name + split[i];
            }
        }
        return name;
    }
    
    public String getParentPath() {
        String parent = "";
        int last = path.lastIndexOf('/');
        if(last > 0) {
            parent = path.substring(0, last);
        }
        return parent;        
    }
    
    public boolean isCompleatlyMandatory() {
        return compleatlyMandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public XmlSchemaAnnotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(XmlSchemaAnnotation annotation) {
        this.annotation = annotation;
    }
    
    /* So we can work with the documentation in the annotations without messing up the XML tree. */
    
    public Map<String, String> getAppInfos() {
        return appInfos;
    }

    public void setAppInfos(Map<String, String> appInfos) {
        this.appInfos = appInfos;
    }
    
    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
    
    /* End */
    
    public QName getType() {
        if(null != type) {
            return type;
        }
        
        return new QName("http://www.w3.org/2001/XMLSchema", "string", "xs");
    }

    public void setType(QName type) {
        this.type = type;
    }

    public String getTypeDocumentation() {
        return typeDocumentation;
    }

    public void setTypeDocumentation(String typeDocumentation) {
        this.typeDocumentation = typeDocumentation;
    }

    public String getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
    }

    /**
     * So we know when a valid value is restricted by a regular expression
     * Note:  This works, if set by the visitor.
     * 
     * @return The regular expression limiting this particular node.
     */
    public String getPatterns() {
        return patterns;
    }

    public void setPatterns(String patterns) {
        this.patterns = patterns;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean isComplexExtension() {
        return complexExtension;
    }

    public void setComplexExtension() {
        this.complexExtension = true;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(String maxLength) {
        this.maxLength = maxLength;
    }

    public String getMinLength() {
        return minLength;
    }

    public void setMinLength(String minLength) {
        this.minLength = minLength;
    }
    
    @Override
    public String toString() {
        return compleatlyMandatory + "\t" + path;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final XPathPlus other = (XPathPlus) obj;
        if ((this.path == null) ? (other.path != null) : !this.path.equals(other.path)) {
            return false;
        }
        return true;
    }
    
}
