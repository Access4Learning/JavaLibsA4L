/*
 * JSON Schema path and metadata container
 */
package org.sifassociation.schema;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * So we can know both a JSON path and its usage information.
 * This is the JSON Schema equivalent of XPathPlus.
 * 
 * @author claude
 * @since 3.0
 */
public class JsonPathPlus implements IPathPlus {
    private String path;
    private boolean mandatory;
    private String documentation;
    private String title;
    private QName type;
    private String jsonType;
    private String enumerations;
    private String patterns;
    private boolean repeatable;
    private String format;
    private String minLength;
    private String maxLength;
    private String minimum;
    private String maximum;
    private Map<String, String> metadata;
    
    /**
     * Default constructor
     */
    public JsonPathPlus() {
        this.path = "";
        this.mandatory = false;
        this.documentation = "";
        this.title = "";
        this.type = new QName("http://json-schema.org", "string", "json");
        this.jsonType = "string";
        this.enumerations = "";
        this.patterns = "";
        this.repeatable = false;
        this.format = "";
        this.minLength = "";
        this.maxLength = "";
        this.minimum = "";
        this.maximum = "";
        this.metadata = new HashMap<>();
    }
    
    /**
     * Constructor with path
     * 
     * @param path JSON path expression
     */
    public JsonPathPlus(String path) {
        this();
        this.path = path;
    }
    
    /**
     * Get the JSON path expression
     * Equivalent to getPath() in XPathPlus
     * 
     * @return JSON path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Set the JSON path expression
     * 
     * @param path JSON path expression 
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Get the name of the property represented by this path
     * Equivalent to getName() in XPathPlus
     * 
     * @return Property name
     */
    public String getName() {
        String name = "";
        int lastDot = path.lastIndexOf('.');
        int lastBracket = path.lastIndexOf("[]");
        
        if (lastDot >= 0 && (lastBracket < 0 || lastDot > lastBracket)) {
            name = path.substring(lastDot + 1);
            // Remove array notation if present
            if (name.endsWith("[]")) {
                name = name.substring(0, name.length() - 2);
            }
        } else if (lastBracket >= 0) {
            name = "items";
        }
        
        return name;
    }
    
    /**
     * Get the parent path
     * Equivalent to getParentPath() in XPathPlus
     * 
     * @return Parent JSON path
     */
    public String getParentPath() {
        String parent = "";
        int lastDot = path.lastIndexOf('.');
        
        if (lastDot > 0) {
            parent = path.substring(0, lastDot);
        }
        
        return parent;
    }
    
    /**
     * Check if this field is mandatory
     * Equivalent to isMandatory() in XPathPlus
     * 
     * @return true if field is required
     */
    public boolean isMandatory() {
        return mandatory;
    }
    
    /**
     * Set mandatory status
     * 
     * @param mandatory true if field is required
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
    
    /**
     * Get field documentation
     * Equivalent to getDocumentation() in XPathPlus
     * 
     * @return Documentation string
     */
    public String getDocumentation() {
        return documentation;
    }
    
    /**
     * Set field documentation
     * 
     * @param documentation Documentation string
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
    
    /**
     * Get field title
     * JSON Schema specific - no direct XPathPlus equivalent
     * 
     * @return Title string
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set field title
     * 
     * @param title Title string
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get field type as QName
     * Equivalent to getType() in XPathPlus
     * 
     * @return Field type as QName
     */
    public QName getType() {
        if (type != null) {
            return type;
        }
        
        return new QName("http://json-schema.org", "string", "json");
    }
    
    /**
     * Set field type as QName
     * 
     * @param type Field type 
     */
    public void setType(QName type) {
        this.type = type;
    }
    
    /**
     * Get the JSON type name
     * JSON Schema specific - simpler than QName for JSON types
     * 
     * @return JSON type name (string, number, integer, object, array, boolean, null)
     */
    public String getJsonType() {
        return jsonType;
    }
    
    /**
     * Set JSON type name
     * 
     * @param jsonType JSON type name
     */
    public void setJsonType(String jsonType) {
        this.jsonType = jsonType;
    }
    
    /**
     * Get enumeration values as string
     * Equivalent to getEnumerations() in XPathPlus
     * 
     * @return Comma-separated list of enumeration values
     */
    public String getEnumerations() {
        return enumerations;
    }
    
    /**
     * Set enumeration values
     * 
     * @param enumerations Comma-separated list of enumeration values
     */
    public void setEnumerations(String enumerations) {
        this.enumerations = enumerations;
    }
    
    /**
     * Get pattern constraints
     * Equivalent to getPatterns() in XPathPlus
     * 
     * @return Regular expression pattern
     */
    public String getPatterns() {
        return patterns;
    }
    
    /**
     * Set pattern constraints
     * 
     * @param patterns Regular expression pattern
     */
    public void setPatterns(String patterns) {
        this.patterns = patterns;
    }
    
    /**
     * Check if field is repeatable (array)
     * Equivalent to isRepeatable() in XPathPlus
     * 
     * @return true if field is an array
     */
    public boolean isRepeatable() {
        return repeatable;
    }
    
    /**
     * Set repeatable status
     * 
     * @param repeatable true if field is an array
     */
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
    
    /**
     * Get JSON format attribute
     * JSON Schema specific - no direct XPathPlus equivalent
     * 
     * @return Format string (e.g., date-time, email, uri)
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Set JSON format attribute
     * 
     * @param format Format string
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Get minimum length constraint
     * Equivalent to getMinLength() in XPathPlus
     * 
     * @return Minimum length as string
     */
    public String getMinLength() {
        return minLength;
    }
    
    /**
     * Set minimum length constraint
     * 
     * @param minLength Minimum length as string
     */
    public void setMinLength(String minLength) {
        this.minLength = minLength;
    }
    
    /**
     * Get maximum length constraint
     * Equivalent to getMaxLength() in XPathPlus
     * 
     * @return Maximum length as string
     */
    public String getMaxLength() {
        return maxLength;
    }
    
    /**
     * Set maximum length constraint
     * 
     * @param maxLength Maximum length as string
     */
    public void setMaxLength(String maxLength) {
        this.maxLength = maxLength;
    }
    
    /**
     * Get minimum value constraint
     * JSON Schema specific - no direct XPathPlus equivalent
     * 
     * @return Minimum value as string
     */
    public String getMinimum() {
        return minimum;
    }
    
    /**
     * Set minimum value constraint
     * 
     * @param minimum Minimum value as string
     */
    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }
    
    /**
     * Get maximum value constraint
     * JSON Schema specific - no direct XPathPlus equivalent
     * 
     * @return Maximum value as string
     */
    public String getMaximum() {
        return maximum;
    }
    
    /**
     * Set maximum value constraint
     * 
     * @param maximum Maximum value as string
     */
    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }
    
    /**
     * Get additional metadata properties
     * Equivalent to getAppInfos() in XPathPlus
     * 
     * @return Map of metadata properties
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    /**
     * Set additional metadata properties
     * 
     * @param metadata Map of metadata properties
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return (mandatory ? "required" : "optional") + "\t" + path + "\t" + jsonType;
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
        final JsonPathPlus other = (JsonPathPlus) obj;
        return !((this.path == null) ? (other.path != null) : !this.path.equals(other.path));
    }
    
    /**
     * Convert this JsonPathPlus object to an XPathPlus object
     * 
     * @return Equivalent XPathPlus object
     */
    public XPathPlus toXPathPlus() {
        return PathConverter.convertToXPathPlus(this);
    }
    
    /**
     * Create a JsonPathPlus object from an XPathPlus object
     * 
     * @param xpathPlus Source XPathPlus object
     * @return Equivalent JsonPathPlus object
     */
    public static JsonPathPlus fromXPathPlus(XPathPlus xpathPlus) {
        return PathConverter.convertToJsonPathPlus(xpathPlus);
    }
}