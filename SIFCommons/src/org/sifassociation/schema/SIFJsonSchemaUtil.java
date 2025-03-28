/*
 * JSON Schema utility class to parallel the XML Schema utility functionality
 */
package org.sifassociation.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

/**
 * So we can traverse JSON Schema instances consistently.
 * 
 * This class serves as a parallel to SIFXmlSchemaUtil but for JSON Schemas.
 * It follows JSON Schema patterns to extract structural information, types, 
 * paths, and metadata from JSON Schema definitions.
 * 
 * Design: Follow the JSON Schema structure, processing types, properties,
 * required fields, references, etc.
 * 
 * Supports common JSON Schema patterns:
 * - Object properties
 * - Type definitions
 * - References ($ref)
 * - Arrays
 * - Enumerations
 * - Pattern constraints
 * - Required field markers
 * - Descriptions and other metadata
 * 
 * @author claude
 * @since 3.0
 */
public class SIFJsonSchemaUtil {
    private static final Logger LOG = Logger.getLogger(SIFJsonSchemaUtil.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Represents a JSON path and value with visitor context
     * Equivalent to the head/tail approach in XML traversal
     */
    public interface IJsonVisit {
        void visit(JsonNode node, String path, JsonSchemaContext context);
    }
    
    /**
     * Context object for traversal state
     * Equivalent to the visitor state in XML traversal
     */
    public static class JsonSchemaContext {
        private final Map<String, JsonNode> definitions = new HashMap<>();
        private Set<String> visited = new HashSet<>();
        private final List<JsonPathPlus> paths = new ArrayList<>();
        private final JsonNode rootSchema;
        private Map<String, JsonNode> externalSchemas = new HashMap<>();
        private final String basePath;
        
        public JsonSchemaContext(JsonNode rootSchema, String basePath) {
            this.rootSchema = rootSchema;
            this.basePath = basePath != null ? basePath : "";
            
            // Load definitions from the schema for future reference resolution
            if (rootSchema != null) {
                if (rootSchema.has("definitions")) {
                    JsonNode defs = rootSchema.get("definitions");
                    Iterator<String> fieldNames = defs.fieldNames();
                    while (fieldNames.hasNext()) {
                        String name = fieldNames.next();
                        definitions.put(name, defs.get(name));
                    }
                }
                // JSON Schema 7+ uses $defs instead of definitions
                if (rootSchema.has("$defs")) {
                    JsonNode defs = rootSchema.get("$defs");
                    Iterator<String> fieldNames = defs.fieldNames();
                    while (fieldNames.hasNext()) {
                        String name = fieldNames.next();
                        definitions.put(name, defs.get(name));
                    }
                }
            }
        }
        
        public JsonSchemaContext(JsonNode rootSchema) {
            this(rootSchema, null);
        }
        
        public Map<String, JsonNode> getDefinitions() {
            return definitions;
        }
        
        public Set<String> getVisited() {
            return visited;
        }
        
        public void setVisited(Set<String> visitedRefs) {
            this.visited = visitedRefs;
        }
        
        public List<JsonPathPlus> getPaths() {
            return paths;
        }
        
        public JsonNode getRootSchema() {
            return rootSchema;
        }
        
        public void addPath(JsonPathPlus path) {
            paths.add(path);
        }
        
        public String getBasePath() {
            return basePath;
        }
        
        public void addExternalSchema(String uri, JsonNode schema) {
            externalSchemas.put(uri, schema);
        }
        
        public JsonNode getExternalSchema(String uri) {
            return externalSchemas.get(uri);
        }
        
        public boolean hasExternalSchema(String uri) {
            return externalSchemas.containsKey(uri);
        }
        
        public Map<String, JsonNode> getExternalSchemas() {
            return externalSchemas;
        }
        
        public void setExternalSchemas(Map<String, JsonNode> schemas) {
            this.externalSchemas = schemas;
        }
    }
    
    /**
     * Traverse a JSON Schema starting from the root
     * Equivalent to traverse() in SIFXmlSchemaUtil
     */
    public static void traverse(JsonNode schema, IJsonVisit visitor) {
        // Create initial context
        JsonSchemaContext context = new JsonSchemaContext(schema);
        
        // Visit the root node first
        visitor.visit(schema, "$", context);
        
        // Create a JsonPathPlus for the root schema if it's a schema definition
        if (isSchemaDefinition(schema)) {
            JsonPathPlus rootInfo = createJsonPathPlus("$", schema, false);
            context.addPath(rootInfo);
        }
        
        // Handle root properties if this is an object schema
        if (isObjectSchema(schema)) {
            // Process properties at root level
            traverseObjectProperties(schema, "$", visitor, context);
        }
        // Handle root array if this is an array schema
        else if (isArraySchema(schema)) {
            traverseArray(schema, "$", visitor, context);
        }
    }
    
    /**
     * Traverse object properties in a schema
     * Equivalent to handleSequence/handleAll/handleChoice in SIFXmlSchemaUtil
     */
    private static void traverseObjectProperties(JsonNode node, String path, 
                                               IJsonVisit visitor, 
                                               JsonSchemaContext context) {
        // Visit the current node first
        visitor.visit(node, path, context);
        
        // If this is a reference, resolve it first
        if (node.has("$ref")) {
            JsonNode resolved = resolveReference(node.get("$ref").asText(), context);
            if (resolved != null) {
                // Avoid circular references
                String refPath = node.get("$ref").asText();
                if (!context.getVisited().contains(refPath)) {
                    context.getVisited().add(refPath);
                    traverseObjectProperties(resolved, path, visitor, context);
                }
            }
            return;
        }
        
        // Get required properties to track mandatory fields
        Set<String> requiredProperties = new HashSet<>();
        if (node.has("required") && node.get("required").isArray()) {
            ArrayNode required = (ArrayNode) node.get("required");
            for (JsonNode req : required) {
                requiredProperties.add(req.asText());
            }
        }
        
        // Process all properties of this object
        if (node.has("properties")) {
            JsonNode properties = node.get("properties");
            Iterator<String> fieldNames = properties.fieldNames();
            
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldSchema = properties.get(fieldName);
                String fieldPath = path.equals("$") ? "$." + fieldName : path + "." + fieldName;
                
                // Create a JsonPathPlus for this field
                JsonPathPlus fieldInfo = createJsonPathPlus(fieldPath, fieldSchema, requiredProperties.contains(fieldName));
                context.addPath(fieldInfo);
                
                // Process field based on its type
                if (isObjectSchema(fieldSchema)) {
                    traverseObjectProperties(fieldSchema, fieldPath, visitor, context);
                } else if (isArraySchema(fieldSchema)) {
                    traverseArray(fieldSchema, fieldPath, visitor, context);
                } else {
                    // For simple types, just visit
                    visitor.visit(fieldSchema, fieldPath, context);
                }
            }
        }
        
        // Handle additional properties
        if (node.has("additionalProperties")) {
            JsonNode additionalProps = node.get("additionalProperties");
            
            // Only process if additionalProperties is an object schema, not just boolean
            if (additionalProps.isObject()) {
                String addlPath = path + ".*";
                
                // Visit additional properties schema
                visitor.visit(additionalProps, addlPath, context);
                
                // Add a JsonPathPlus for the additionalProperties
                JsonPathPlus addlInfo = createJsonPathPlus(addlPath, additionalProps, false);
                context.addPath(addlInfo);
                
                // Process additional properties based on type
                if (isObjectSchema(additionalProps)) {
                    traverseObjectProperties(additionalProps, addlPath, visitor, context);
                } else if (isArraySchema(additionalProps)) {
                    traverseArray(additionalProps, addlPath, visitor, context);
                }
            }
        }
        
        // Handle allOf - equivalent to extension in XML schema
        if (node.has("allOf") && node.get("allOf").isArray()) {
            ArrayNode allOf = (ArrayNode) node.get("allOf");
            for (JsonNode component : allOf) {
                traverseObjectProperties(component, path, visitor, context);
            }
        }
        
        // Handle oneOf and anyOf - equivalent to choice in XML schema
        for (String field : new String[]{"oneOf", "anyOf"}) {
            if (node.has(field) && node.get(field).isArray()) {
                ArrayNode options = (ArrayNode) node.get(field);
                for (JsonNode option : options) {
                    // Process each option
                    if (isObjectSchema(option)) {
                        traverseObjectProperties(option, path, visitor, context);
                    } else if (isArraySchema(option)) {
                        traverseArray(option, path, visitor, context);
                    } else {
                        visitor.visit(option, path, context);
                    }
                }
            }
        }
        
        // If this is a schema with patternProperties, process them
        if (node.has("patternProperties") && node.get("patternProperties").isObject()) {
            JsonNode patternProps = node.get("patternProperties");
            Iterator<String> patternNames = patternProps.fieldNames();
            
            while (patternNames.hasNext()) {
                String pattern = patternNames.next();
                JsonNode patternSchema = patternProps.get(pattern);
                String patternPath = path + ".[" + pattern + "]";
                
                // Create a JsonPathPlus for this pattern property
                JsonPathPlus patternInfo = createJsonPathPlus(patternPath, patternSchema, false);
                patternInfo.setPatterns(pattern);
                context.addPath(patternInfo);
                
                // Process pattern property based on its type
                if (isObjectSchema(patternSchema)) {
                    traverseObjectProperties(patternSchema, patternPath, visitor, context);
                } else if (isArraySchema(patternSchema)) {
                    traverseArray(patternSchema, patternPath, visitor, context);
                } else {
                    // For simple types, just visit
                    visitor.visit(patternSchema, patternPath, context);
                }
            }
        }
    }
    
    /**
     * Traverse array items in a schema
     * Equivalent to handling repeatable elements in SIFXmlSchemaUtil
     */
    private static void traverseArray(JsonNode node, String path, 
                                     IJsonVisit visitor, 
                                     JsonSchemaContext context) {
        // Visit the array node itself
        visitor.visit(node, path, context);
        
        // If this is a reference, resolve it first
        if (node.has("$ref")) {
            JsonNode resolved = resolveReference(node.get("$ref").asText(), context);
            if (resolved != null) {
                // Avoid circular references
                String refPath = node.get("$ref").asText();
                if (!context.getVisited().contains(refPath)) {
                    context.getVisited().add(refPath);
                    traverseArray(resolved, path, visitor, context);
                }
            }
            return;
        }
        
        // Process items schema (for array elements)
        if (node.has("items")) {
            JsonNode items = node.get("items");
            String itemsPath = path + "[]";
            
            // Create JsonPathPlus for array items
            JsonPathPlus itemInfo = createJsonPathPlus(itemsPath, items, false);
            itemInfo.setRepeatable(true);
            context.addPath(itemInfo);
            
            // Process based on item type
            if (isObjectSchema(items)) {
                traverseObjectProperties(items, itemsPath, visitor, context);
            } else if (isArraySchema(items)) {
                // Nested arrays
                traverseArray(items, itemsPath, visitor, context);
            } else {
                // Simple type array
                visitor.visit(items, itemsPath, context);
            }
        }
    }
    
    /**
     * Resolve a JSON Schema reference
     * Equivalent to type resolution in SIFXmlSchemaUtil
     */
    private static JsonNode resolveReference(String ref, JsonSchemaContext context) {
        LOG.info("Resolving reference: " + ref);
        
        // Special case to prevent infinite recursion
        if (ref.equals("#")) {
            return context.getRootSchema();
        }
        
        // Handle local references
        if (ref.startsWith("#/")) {
            String path = ref.substring(2); // Remove the '#/'
            String[] parts = path.split("/");
            
            JsonNode current = context.getRootSchema();
            for (String part : parts) {
                // Handle JSON pointers with encoded characters
                part = part.replace("~1", "/").replace("~0", "~");
                
                if (current.has(part)) {
                    current = current.get(part);
                } else {
                    LOG.warning("Reference not found: " + ref + ", part: " + part);
                    return null;
                }
            }
            return current;
        }
        
        // Handle definitions directly
        if (ref.startsWith("#/definitions/")) {
            String defName = ref.substring("#/definitions/".length());
            if (context.getDefinitions().containsKey(defName)) {
                return context.getDefinitions().get(defName);
            }
        }
        
        // Handle JSON Schema 7+ $defs
        if (ref.startsWith("#/$defs/")) {
            String defName = ref.substring("#/$defs/".length());
            if (context.getDefinitions().containsKey(defName)) {
                return context.getDefinitions().get(defName);
            }
        }
        
        // Handle external references
        if (ref.contains("#")) {
            // Split into file part and fragment part
            String[] parts = ref.split("#", 2);
            String filePath = parts[0];
            String fragment = parts.length > 1 ? "#" + parts[1] : "#";
            
            // Skip empty filePath and just process fragment
            if (filePath.isEmpty()) {
                return resolveReference(fragment, context);
            }
            
            // Check if we've already loaded this external schema
            if (context.hasExternalSchema(filePath)) {
                JsonNode externalSchema = context.getExternalSchema(filePath);
                
                // If there's a fragment, drill down into the external schema
                if (fragment.equals("#")) {
                    return externalSchema;
                } else {
                    // Recursively resolve the fragment in the external schema
                    // Share visited refs to prevent circular references
                    JsonSchemaContext externalContext = new JsonSchemaContext(externalSchema);
                    externalContext.setVisited(context.getVisited());
                    externalContext.setExternalSchemas(context.getExternalSchemas());
                    return resolveReference(fragment, externalContext);
                }
            } else {
                // Need to load the external schema
                String fullPath;
                if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                    // TODO: Implement HTTP loading if needed
                    LOG.warning("HTTP schema loading not implemented: " + filePath);
                    return null;
                } else {
                    // Determine full file path
                    if (filePath.startsWith("/")) {
                        fullPath = filePath;
                    } else {
                        // Relative path to the current schema
                        String basePath = context.getBasePath();
                        if (basePath.isEmpty()) {
                            fullPath = filePath;
                        } else {
                            File baseDir = new File(basePath).getParentFile();
                            fullPath = new File(baseDir, filePath).getAbsolutePath();
                        }
                    }
                    
                    LOG.info("Loading external schema: " + fullPath);
                    
                    // Load the external schema
                    JsonNode externalSchema = loadSchema(fullPath);
                    if (externalSchema != null) {
                        // Add the external schema to the context
                        context.addExternalSchema(filePath, externalSchema);
                        
                        // If there's a fragment, drill down into the external schema
                        if (fragment.equals("#")) {
                            return externalSchema;
                        } else {
                            // Create a new context for the external schema with the new base path
                            // Share visited refs to prevent circular references
                            JsonSchemaContext externalContext = new JsonSchemaContext(externalSchema, fullPath);
                            externalContext.setVisited(context.getVisited());
                            externalContext.setExternalSchemas(context.getExternalSchemas());
                            return resolveReference(fragment, externalContext);
                        }
                    } else {
                        LOG.warning("Failed to load external schema: " + fullPath);
                    }
                }
            }
        } else if (!ref.contains("#") && !ref.isEmpty()) {
            // Reference to an entire file without a fragment
            String filePath = ref;
            
            // Check if we've already loaded this external schema
            if (context.hasExternalSchema(filePath)) {
                return context.getExternalSchema(filePath);
            } else {
                // Need to load the external schema
                String fullPath;
                if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                    // TODO: Implement HTTP loading if needed
                    LOG.warning("HTTP schema loading not implemented: " + filePath);
                    return null;
                } else {
                    // Determine full file path
                    if (filePath.startsWith("/")) {
                        fullPath = filePath;
                    } else {
                        // Relative path to the current schema
                        String basePath = context.getBasePath();
                        if (basePath.isEmpty()) {
                            fullPath = filePath;
                        } else {
                            File baseDir = new File(basePath).getParentFile();
                            fullPath = new File(baseDir, filePath).getAbsolutePath();
                        }
                    }
                    
                    LOG.info("Loading external schema without fragment: " + fullPath);
                    
                    // Load the external schema
                    JsonNode externalSchema = loadSchema(fullPath);
                    if (externalSchema != null) {
                        // Add the external schema to the context
                        context.addExternalSchema(filePath, externalSchema);
                        return externalSchema;
                    } else {
                        LOG.warning("Failed to load external schema: " + fullPath);
                    }
                }
            }
        }
        
        LOG.warning("Unresolved reference: " + ref);
        return null;
    }
    
    /**
     * Create a JsonPathPlus object from schema information
     * Equivalent to XPathPlus creation in XML schema visitors
     */
    private static JsonPathPlus createJsonPathPlus(String path, JsonNode schema, boolean required) {
        JsonPathPlus info = new JsonPathPlus(path);
        
        // Set mandatory status
        info.setMandatory(required);
        
        // Set documentation
        if (schema.has("description")) {
            info.setDocumentation(schema.get("description").asText());
        }
        
        // Set title if available
        if (schema.has("title")) {
            info.setTitle(schema.get("title").asText());
        }
        
        // Set type information
        if (schema.has("type")) {
            JsonNode typeNode = schema.get("type");
            String type;
            
            if (typeNode.isTextual()) {
                type = typeNode.asText();
            } else if (typeNode.isArray() && typeNode.size() > 0) {
                // For multi-type definitions, use the first non-null type
                type = "object"; // Default to object if no valid type found
                for (JsonNode t : typeNode) {
                    if (t.isTextual() && !t.asText().equals("null")) {
                        type = t.asText();
                        break;
                    }
                }
            } else {
                type = "object"; // Default to object for any other case
            }
            
            info.setType(new QName("http://json-schema.org", type, "json"));
            info.setJsonType(type);
        } else if (isObjectSchema(schema)) {
            // Implicit object type
            info.setType(new QName("http://json-schema.org", "object", "json"));
            info.setJsonType("object");
        } else if (isArraySchema(schema)) {
            // Implicit array type
            info.setType(new QName("http://json-schema.org", "array", "json"));
            info.setJsonType("array");
        }
        
        // Handle enumerations
        if (schema.has("enum") && schema.get("enum").isArray()) {
            ArrayNode enumValues = (ArrayNode) schema.get("enum");
            StringBuilder sb = new StringBuilder();
            for (JsonNode value : enumValues) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(value.toString());
            }
            info.setEnumerations(sb.toString());
        }
        
        // Handle pattern constraints
        if (schema.has("pattern")) {
            info.setPatterns(schema.get("pattern").asText());
        }
        
        // Handle length constraints
        if (schema.has("minLength")) {
            info.setMinLength(schema.get("minLength").asText());
        }
        if (schema.has("maxLength")) {
            info.setMaxLength(schema.get("maxLength").asText());
        }
        
        // Handle numeric constraints
        if (schema.has("minimum")) {
            info.setMinimum(schema.get("minimum").asText());
        }
        if (schema.has("maximum")) {
            info.setMaximum(schema.get("maximum").asText());
        }
        
        // Handle format information
        if (schema.has("format")) {
            info.setFormat(schema.get("format").asText());
        }
        
        // Store all other metadata as custom properties
        Iterator<String> fieldNames = schema.fieldNames();
        Map<String, String> metaProps = new HashMap<>();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            // Skip already processed standard properties
            if (!name.equals("type") && !name.equals("properties") && 
                !name.equals("required") && !name.equals("description") &&
                !name.equals("title") && !name.equals("enum") &&
                !name.equals("pattern") && !name.equals("minLength") &&
                !name.equals("maxLength") && !name.equals("minimum") &&
                !name.equals("maximum") && !name.equals("format") &&
                !name.equals("items") && !name.equals("$ref")) {
                
                JsonNode value = schema.get(name);
                if (value.isTextual()) {
                    metaProps.put(name, value.asText());
                } else if (value.isBoolean() || value.isNumber()) {
                    metaProps.put(name, value.toString());
                }
            }
        }
        info.setMetadata(metaProps);
        
        return info;
    }
    
    /**
     * Check if a schema node represents an object type
     */
    private static boolean isObjectSchema(JsonNode node) {
        // Explicit object type
        if (node.has("type")) {
            JsonNode typeNode = node.get("type");
            if (typeNode.isTextual() && "object".equals(typeNode.asText())) {
                return true;
            } else if (typeNode.isArray()) {
                // Check for object type in array of possible types
                for (JsonNode type : typeNode) {
                    if (type.isTextual() && "object".equals(type.asText())) {
                        return true;
                    }
                }
            }
        }
        
        // Implicit object (has properties or other object schema indicators)
        return node.has("properties") || 
               node.has("additionalProperties") ||
               node.has("patternProperties") ||
               node.has("required") && node.get("required").isArray();
    }
    
    /**
     * Check if a schema node represents an array type
     */
    private static boolean isArraySchema(JsonNode node) {
        // Explicit array type
        if (node.has("type")) {
            JsonNode typeNode = node.get("type");
            if (typeNode.isTextual() && "array".equals(typeNode.asText())) {
                return true;
            } else if (typeNode.isArray()) {
                // Check for array type in array of possible types
                for (JsonNode type : typeNode) {
                    if (type.isTextual() && "array".equals(type.asText())) {
                        return true;
                    }
                }
            }
        }
        
        // Implicit array (has items)
        return node.has("items");
    }
    
    /**
     * Check if a node is a JSON Schema definition (at root level)
     * This is used to identify if we're handling a schema object
     */
    private static boolean isSchemaDefinition(JsonNode node) {
        // A node is considered a schema definition if it has schema-specific properties
        return node.has("$schema") || 
               node.has("title") || 
               node.has("description") && (
                   node.has("type") || 
                   node.has("properties") || 
                   node.has("items")
               );
    }
    
    /**
     * Load a JSON Schema from a file
     * Equivalent to getRootElements in SIFXmlSchemaUtil
     * 
     * @param filePath Path to the JSON Schema file
     * @return The parsed JSON Schema
     */
    public static JsonNode loadSchema(String filePath) {
        try (FileInputStream is = new FileInputStream(filePath)) {
            JsonNode schema = mapper.readTree(is);
            LOG.info("Schema loaded successfully from: " + filePath);
            
            // Validate schema structure
            LOG.info("Schema structure check:");
            if (schema.has("$schema")) LOG.info("  - Has $schema: " + schema.get("$schema").asText());
            if (schema.has("title")) LOG.info("  - Has title: " + schema.get("title").asText());
            if (schema.has("description")) LOG.info("  - Has description: " + schema.get("description").asText());
            if (schema.has("type")) LOG.info("  - Has type: " + schema.get("type").asText());
            if (schema.has("properties")) {
                LOG.info("  - Has properties section");
                JsonNode props = schema.get("properties");
                int propCount = 0;
                StringBuilder propNames = new StringBuilder();
                Iterator<String> propIt = props.fieldNames();
                while (propIt.hasNext()) {
                    propCount++;
                    String fieldName = propIt.next();
                    if (propNames.length() > 0) propNames.append(", ");
                    propNames.append(fieldName);
                }
                LOG.info("  - Properties count: " + propCount);
                LOG.info("  - Property names: " + propNames.toString());
            } else {
                LOG.warning("  - No properties section found!");
            }
            
            return schema;
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, "Schema file not found: " + filePath, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error reading schema file: " + filePath, ex);
        }
        return null;
    }
    
    /**
     * Load a JSON Schema from an input stream
     * 
     * @param is Input stream containing the JSON Schema
     * @return The parsed JSON Schema
     */
    public static JsonNode loadSchema(InputStream is) {
        try {
            return mapper.readTree(is);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error reading schema from input stream", ex);
        }
        return null;
    }
    
    /**
     * Load a JSON Schema from a file with full reference resolution
     * This method loads a schema and recursively resolves all $ref references
     * 
     * @param filePath Path to the JSON Schema file
     * @return The complete schema with all references resolved
     */
    public static JsonNode loadSchemaWithRefs(String filePath) {
        LOG.info("Loading schema with references: " + filePath);
        JsonNode schema = loadSchema(filePath);
        if (schema == null) {
            LOG.warning("Failed to load schema: " + filePath);
            return null;
        }
        
        // Create context with base path for resolving references
        JsonSchemaContext context = new JsonSchemaContext(schema, filePath);
        
        // Recursively resolve all references
        JsonNode resolved = resolveAllReferences(schema, context);
        LOG.info("Successfully resolved schema: " + filePath);
        
        return resolved;
    }
    
    /**
     * Utility method to save a JSON Schema to a file
     * 
     * @param schema The JSON Schema to save
     * @param filePath The file path to save to
     * @return true if successful, false otherwise
     */
    public static boolean saveSchema(JsonNode schema, String filePath) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), schema);
            LOG.info("Schema successfully saved to: " + filePath);
            return true;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error saving schema to: " + filePath, ex);
            return false;
        }
    }
    
    /**
     * Utility method to test reference resolution and save the result
     * 
     * @param inputPath Path to the input JSON Schema
     * @param outputPath Path to save the resolved schema (null to use default)
     * @return true if successful, false otherwise
     */
    public static boolean resolveAndSaveSchema(String inputPath, String outputPath) {
        if (outputPath == null) {
            outputPath = inputPath.replace(".json", "_resolved.json");
        }
        
        JsonNode resolvedSchema = loadSchemaWithRefs(inputPath);
        if (resolvedSchema == null) {
            return false;
        }
        
        return saveSchema(resolvedSchema, outputPath);
    }
    
    /**
     * Recursively resolve all references in a JSON Schema node
     * 
     * @param node The schema node to process
     * @param context The traversal context
     * @return A new node with all references resolved
     */
    private static JsonNode resolveAllReferences(JsonNode node, JsonSchemaContext context) {
        // Special case for null nodes
        if (node == null) {
            return null;
        }
        
        // If this is a reference, resolve it and process its content
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            
            // Avoid circular references
            if (context.getVisited().contains(ref)) {
                LOG.warning("Circular reference detected: " + ref);
                // Return a simplified reference to avoid infinite recursion
                ObjectNode refNode = mapper.createObjectNode();
                refNode.put("$ref", ref);
                refNode.put("description", "Circular reference resolved");
                
                // Copy any other fields from the original reference
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (!"$ref".equals(fieldName)) {
                        refNode.set(fieldName, node.get(fieldName));
                    }
                }
                
                // Don't try to resolve this again
                return refNode;
            }
            
            // Mark this reference as visited to detect circular references
            context.getVisited().add(ref);
            
            // Attempt to resolve the reference
            JsonNode resolved = resolveReference(ref, context);
            
            if (resolved != null) {
                // Deep copy to avoid modifying the original resolved node
                // (which might be shared between multiple references)
                ObjectNode result;
                if (resolved.isObject()) {
                    result = resolved.deepCopy();
                } else {
                    // If not an object, wrap in an object
                    result = mapper.createObjectNode();
                    result.set("value", resolved);
                }
                
                // Copy all additional fields from the original reference
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (!"$ref".equals(fieldName)) {
                        result.set(fieldName, node.get(fieldName));
                    }
                }
                
                // Process the resolved node recursively (but keep the reference marked as visited)
                return resolveAllReferences(result, context);
            } else {
                LOG.warning("Unable to resolve reference: " + ref + " - keeping original reference");
                // Keep the original reference if it couldn't be resolved
                return node;
            }
        }
        
        // Handle different node types for non-reference nodes
        if (node.isObject()) {
            ObjectNode newNode = mapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                try {
                    // Process each field recursively
                    JsonNode resolvedField = resolveAllReferences(fieldValue, context);
                    newNode.set(fieldName, resolvedField);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error resolving field: " + fieldName, e);
                    // Keep the original value if there was an error
                    newNode.set(fieldName, fieldValue);
                }
            }
            
            return newNode;
        } else if (node.isArray()) {
            ArrayNode newNode = mapper.createArrayNode();
            
            for (JsonNode element : node) {
                try {
                    // Process each array element recursively
                    newNode.add(resolveAllReferences(element, context));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error resolving array element", e);
                    // Keep the original element if there was an error
                    newNode.add(element);
                }
            }
            
            return newNode;
        } else {
            // For primitive values, return as is
            return node;
        }
    }
    
    /**
     * Get all JSON paths with their metadata from a schema
     * Equivalent to getAllXPaths in SIFXmlSchemaUtil
     * 
     * @param filePath Path to the JSON Schema file
     * @return List of JsonPathPlus objects with metadata
     */
    public static List<JsonPathPlus> getAllPaths(String filePath) {
        JsonNode schema = loadSchema(filePath);
        if (schema == null) {
            return new ArrayList<>();
        }
        
        return getAllPaths(schema, filePath);
    }
    
    /**
     * Get all JSON paths with their metadata from a schema
     * 
     * @param schema Parsed JSON Schema
     * @return List of JsonPathPlus objects with metadata
     */
    public static List<JsonPathPlus> getAllPaths(JsonNode schema) {
        return getAllPaths(schema, null);
    }
    
    /**
     * Get all JSON paths with their metadata from a schema with base path for reference resolution
     * 
     * @param schema Parsed JSON Schema
     * @param basePath Base path for resolving references
     * @return List of JsonPathPlus objects with metadata
     */
    public static List<JsonPathPlus> getAllPaths(JsonNode schema, String basePath) {
        // Safeguard against null schema
        if (schema == null) {
            LOG.warning("Schema is null, cannot extract paths");
            return new ArrayList<>();
        }
        
        // Direct path extraction (more robust method)
        LOG.info("Starting direct path extraction from schema");
        List<JsonPathPlus> paths = new ArrayList<>();
        extractPaths(schema, "$", paths, new HashSet<>(), false, basePath);
        LOG.info("Path extraction complete. Paths collected: " + paths.size());
        return paths;
    }
    
    /**
     * Extract paths directly from a JSON Schema by analyzing its structure
     * This is more robust than using the visitor pattern
     */
    private static void extractPaths(JsonNode node, String currentPath, List<JsonPathPlus> paths, 
                                    Set<String> visitedRefs, boolean isRequired, String basePath) {
        // Add the current node as a path if it has a type or special property
        if (node.has("type") || node.has("$ref") || isSchemaDefinition(node) || currentPath.equals("$")) {
            JsonPathPlus pathInfo = createJsonPathPlus(currentPath, node, isRequired);
            paths.add(pathInfo);
        }
        
        // Handle $ref nodes
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            
            // Avoid circular references
            if (!visitedRefs.contains(ref)) {
                visitedRefs.add(ref);
                
                // Manually resolve the reference
                JsonNode resolvedNode = null;
                
                if (ref.startsWith("#/")) {
                    // Internal reference, resolve directly
                    String[] parts = ref.substring(2).split("/");
                    JsonNode root = (basePath != null) ? loadSchema(basePath) : node;
                    if (root != null) {
                        JsonNode current = root;
                        for (String part : parts) {
                            part = part.replace("~1", "/").replace("~0", "~");
                            if (current != null && current.has(part)) {
                                current = current.get(part);
                            } else {
                                current = null;
                                break;
                            }
                        }
                        resolvedNode = current;
                    }
                }
                
                if (resolvedNode != null) {
                    extractPaths(resolvedNode, currentPath, paths, visitedRefs, isRequired, basePath);
                }
            }
            
            // Don't process further if this is a ref
            return;
        }
        
        // Process properties for object schemas
        if (node.has("properties") && node.get("properties").isObject()) {
            JsonNode properties = node.get("properties");
            
            // Get required properties list
            Set<String> requiredProps = new HashSet<>();
            if (node.has("required") && node.get("required").isArray()) {
                JsonNode required = node.get("required");
                for (JsonNode req : required) {
                    requiredProps.add(req.asText());
                }
            }
            
            // Process each property
            Iterator<String> fieldNames = properties.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                JsonNode property = properties.get(name);
                
                String propertyPath = currentPath.equals("$") ? "$." + name : currentPath + "." + name;
                boolean propRequired = requiredProps.contains(name);
                
                extractPaths(property, propertyPath, paths, new HashSet<>(visitedRefs), propRequired, basePath);
            }
        }
        
        // Process array items
        if (node.has("items") && !currentPath.endsWith("[]")) {
            JsonNode items = node.get("items");
            String itemsPath = currentPath + "[]";
            
            // Mark the items path as repeatable
            JsonPathPlus itemsInfo = createJsonPathPlus(itemsPath, items, false);
            itemsInfo.setRepeatable(true);
            paths.add(itemsInfo);
            
            extractPaths(items, itemsPath, paths, new HashSet<>(visitedRefs), false, basePath);
        }
        
        // Handle additionalProperties
        if (node.has("additionalProperties") && node.get("additionalProperties").isObject()) {
            JsonNode additionalProps = node.get("additionalProperties");
            String addlPath = currentPath + ".*";
            
            extractPaths(additionalProps, addlPath, paths, new HashSet<>(visitedRefs), false, basePath);
        }
        
        // Handle patternProperties
        if (node.has("patternProperties") && node.get("patternProperties").isObject()) {
            JsonNode patternProps = node.get("patternProperties");
            Iterator<String> patterns = patternProps.fieldNames();
            
            while (patterns.hasNext()) {
                String pattern = patterns.next();
                JsonNode patternSchema = patternProps.get(pattern);
                String patternPath = currentPath + ".[" + pattern + "]";
                
                extractPaths(patternSchema, patternPath, paths, new HashSet<>(visitedRefs), false, basePath);
            }
        }
        
        // Handle allOf, oneOf, anyOf
        for (String keyword : new String[]{"allOf", "oneOf", "anyOf"}) {
            if (node.has(keyword) && node.get(keyword).isArray()) {
                JsonNode options = node.get(keyword);
                
                for (int i = 0; i < options.size(); i++) {
                    JsonNode option = options.get(i);
                    String optionPath = currentPath + "." + keyword + "[" + i + "]";
                    
                    extractPaths(option, optionPath, paths, new HashSet<>(visitedRefs), isRequired, basePath);
                }
            }
        }
    }
    
    /**
     * Get all JSON paths with their metadata from a schema with all references resolved
     * 
     * @param filePath Path to the JSON Schema file
     * @return List of JsonPathPlus objects with metadata from the fully resolved schema
     */
    public static List<JsonPathPlus> getAllPathsWithRefs(String filePath) {
        // Load the schema with all references resolved
        JsonNode schema = loadSchemaWithRefs(filePath);
        if (schema == null) {
            return new ArrayList<>();
        }
        
        return getAllPaths(schema);
    }
    
    /**
     * Get root object definitions from a schema
     * Equivalent to getSIFObjects in SIFXmlSchemaUtil
     * 
     * @param filePath Path to the JSON Schema file
     * @return List of root object definitions as JsonPathPlus
     */
    public static List<JsonPathPlus> getRootObjects(String filePath) {
        JsonNode schema = loadSchema(filePath);
        if (schema == null) {
            return new ArrayList<>();
        }
        
        return getRootObjects(schema, filePath);
    }
    
    /**
     * Get root object definitions from a schema
     * 
     * @param schema Parsed JSON Schema
     * @return List of root object definitions as JsonPathPlus
     */
    public static List<JsonPathPlus> getRootObjects(JsonNode schema) {
        return getRootObjects(schema, null);
    }
    
    /**
     * Get root object definitions from a schema with base path for reference resolution
     * 
     * @param schema Parsed JSON Schema
     * @param basePath Base path for resolving references
     * @return List of root object definitions as JsonPathPlus
     */
    public static List<JsonPathPlus> getRootObjects(JsonNode schema, String basePath) {
        // If schema is null, return empty list
        if (schema == null) {
            LOG.warning("Schema is null, cannot extract root objects");
            return new ArrayList<>();
        }
        
        List<JsonPathPlus> rootObjects = new ArrayList<>();
        
        // First add the schema root
        JsonPathPlus rootInfo = createJsonPathPlus("$", schema, false);
        if (schema.has("title")) {
            String title = schema.get("title").asText();
            LOG.info("Found schema title: " + title);
            rootInfo.setTitle(title);
        }
        if (schema.has("description")) {
            rootInfo.setDocumentation(schema.get("description").asText());
        }
        if (schema.has("type")) {
            JsonNode typeNode = schema.get("type");
            if (typeNode.isTextual()) {
                rootInfo.setJsonType(typeNode.asText());
            } else {
                rootInfo.setJsonType("object"); // Default
            }
        } else if (schema.has("properties")) {
            rootInfo.setJsonType("object");
        }
        rootObjects.add(rootInfo);
        
        // Then add top-level properties
        if (schema.has("properties")) {
            JsonNode properties = schema.get("properties");
            Iterator<String> fieldNames = properties.fieldNames();
            
            // Get required properties
            Set<String> requiredProps = new HashSet<>();
            if (schema.has("required") && schema.get("required").isArray()) {
                ArrayNode required = (ArrayNode) schema.get("required");
                for (JsonNode req : required) {
                    requiredProps.add(req.asText());
                }
            }
            
            // Add each property as a root object
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                JsonNode property = properties.get(name);
                String path = "$." + name;
                
                JsonPathPlus propInfo = createJsonPathPlus(path, property, requiredProps.contains(name));
                rootObjects.add(propInfo);
            }
        }
        
        return rootObjects;
    }
    
    /**
     * Get root object definitions from a schema with all references resolved
     * 
     * @param filePath Path to the JSON Schema file
     * @return List of root object definitions from the fully resolved schema
     */
    public static List<JsonPathPlus> getRootObjectsWithRefs(String filePath) {
        // Load the schema with all references resolved
        JsonNode schema = loadSchemaWithRefs(filePath);
        if (schema == null) {
            return new ArrayList<>();
        }
        
        return getRootObjects(schema);
    }
    
    /**
     * Get all definitions (types) from the schema
     * Equivalent to getAdditionalTypes in SIFXmlSchemaUtil
     * 
     * @param filePath Path to the JSON Schema file
     * @return Map of definition name to JsonNode
     */
    public static Map<String, JsonNode> getDefinitions(String filePath) {
        JsonNode schema = loadSchema(filePath);
        if (schema == null) {
            return new HashMap<>();
        }
        
        return getDefinitions(schema);
    }
    
    /**
     * Get all definitions (types) from the schema
     * 
     * @param schema Parsed JSON Schema
     * @return Map of definition name to JsonNode
     */
    public static Map<String, JsonNode> getDefinitions(JsonNode schema) {
        Map<String, JsonNode> result = new HashMap<>();
        
        // Get definitions/schemas section (JSON Schema Draft 7)
        if (schema.has("definitions")) {
            JsonNode defs = schema.get("definitions");
            Iterator<String> fieldNames = defs.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                result.put(name, defs.get(name));
            }
        }
        
        // Get $defs section (JSON Schema Draft 2019-09/2020-12)
        if (schema.has("$defs")) {
            JsonNode defs = schema.get("$defs");
            Iterator<String> fieldNames = defs.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                result.put(name, defs.get(name));
            }
        }
        
        return result;
    }
    
    /**
     * Get all definitions (types) from the schema with all references resolved
     * 
     * @param filePath Path to the JSON Schema file
     * @return Map of definition name to JsonNode with all references resolved
     */
    public static Map<String, JsonNode> getDefinitionsWithRefs(String filePath) {
        // Load the schema with all references resolved
        JsonNode schema = loadSchemaWithRefs(filePath);
        if (schema == null) {
            return new HashMap<>();
        }
        
        return getDefinitions(schema);
    }
    
    /**
     * Save a fully resolved JSON Schema to a file
     * 
     * @param schema The resolved schema to save
     * @param outputPath The file path to save to
     * @return true if successful, false otherwise
     */
    public static boolean saveResolvedSchema(JsonNode schema, String outputPath) {
        try {
            // Use pretty printer for better readability
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), schema);
            LOG.info("Resolved schema saved to: " + outputPath);
            return true;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error saving resolved schema to: " + outputPath, ex);
            return false;
        }
    }
    
}