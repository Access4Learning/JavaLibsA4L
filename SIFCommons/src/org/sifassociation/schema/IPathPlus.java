/*
 * Common interface for JsonPathPlus and XPathPlus
 */
package org.sifassociation.schema;

import java.util.Map;
import javax.xml.namespace.QName;

/**
 * Common interface for path objects representing both JSON and XML paths
 * with their associated metadata.
 * 
 * @author claude
 * @since 3.0
 */
public interface IPathPlus {
    /**
     * Get the path expression
     * 
     * @return path expression
     */
    String getPath();
    
    /**
     * Set the path expression
     * 
     * @param path path expression
     */
    void setPath(String path);
    
    /**
     * Get the name of the element or property
     * 
     * @return element or property name
     */
    String getName();
    
    /**
     * Get the parent path
     * 
     * @return parent path
     */
    String getParentPath();
    
    /**
     * Check if element or property is mandatory
     * 
     * @return true if mandatory
     */
    boolean isMandatory();
    
    /**
     * Set mandatory status
     * 
     * @param mandatory true if mandatory
     */
    void setMandatory(boolean mandatory);
    
    /**
     * Get documentation
     * 
     * @return documentation string
     */
    String getDocumentation();
    
    /**
     * Set documentation
     * 
     * @param documentation documentation string
     */
    void setDocumentation(String documentation);
    
    /**
     * Get type
     * 
     * @return type as QName
     */
    QName getType();
    
    /**
     * Set type
     * 
     * @param type type as QName
     */
    void setType(QName type);
    
    /**
     * Get enumeration values
     * 
     * @return enumeration values
     */
    String getEnumerations();
    
    /**
     * Set enumeration values
     * 
     * @param enumerations enumeration values
     */
    void setEnumerations(String enumerations);
    
    /**
     * Get pattern constraints
     * 
     * @return pattern constraints
     */
    String getPatterns();
    
    /**
     * Set pattern constraints
     * 
     * @param patterns pattern constraints
     */
    void setPatterns(String patterns);
    
    /**
     * Check if element or property is repeatable
     * 
     * @return true if repeatable
     */
    boolean isRepeatable();
    
    /**
     * Set repeatable status
     * 
     * @param repeatable true if repeatable
     */
    void setRepeatable(boolean repeatable);
    
    /**
     * Get minimum length constraint
     * 
     * @return minimum length
     */
    String getMinLength();
    
    /**
     * Set minimum length constraint
     * 
     * @param minLength minimum length
     */
    void setMinLength(String minLength);
    
    /**
     * Get maximum length constraint
     * 
     * @return maximum length
     */
    String getMaxLength();
    
    /**
     * Set maximum length constraint
     * 
     * @param maxLength maximum length
     */
    void setMaxLength(String maxLength);
    
    /**
     * Get metadata map
     * 
     * @return metadata map
     */
    Map<String, String> getMetadata();
    
    /**
     * Set metadata map
     * 
     * @param metadata metadata map
     */
    void setMetadata(Map<String, String> metadata);
}