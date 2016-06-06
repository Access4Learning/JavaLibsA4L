/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;

/**
 * So we can know both an XPath and its usage information.
 * 
 * @author jlovell
 * @since 3.0
 */
public class XPathPlus {
    private String path;
    private boolean compleatlyMandatory;
    private XmlSchemaAnnotation annotation;
    private boolean mandatory;
    private String namespace;
    private QName type;
    private String enumerations;
    
    public XPathPlus(
            String path, 
            boolean compleatlyMandatory, 
            XmlSchemaAnnotation annotation) {
        this.path = path;
        this.compleatlyMandatory = compleatlyMandatory;
        this.annotation = annotation;
        this.mandatory = false;
        this.namespace = "";
        this.type = new QName("", "", "");
        this.enumerations = "";
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        String name = "";
        int last = path.lastIndexOf('/');
        if(last >= 0) {
            name = path.substring(last+1, path.length());
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

    public QName getType() {
        if(null != type) {
            return type;
        }
        
        return new QName("http://www.w3.org/2001/XMLSchema", "string", "xs");
    }

    public void setType(QName type) {
        this.type = type;
    }

    public String getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
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
