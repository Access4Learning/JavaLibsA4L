/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;
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
    
    public XPathPlus(
            String path, 
            boolean compleatlyMandatory, 
            XmlSchemaAnnotation annotation) {
        this.path = path;
        this.compleatlyMandatory = compleatlyMandatory;
        this.annotation = annotation;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        String name = "";
        int last = path.lastIndexOf('/');
        if(last > 0) {
            name = path.substring(last+1, path.length());
        }
        return name;
    }
    
    public boolean isCompleatlyMandatory() {
        return compleatlyMandatory;
    }

    public XmlSchemaAnnotation getAnnotation() {
        return annotation;
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
