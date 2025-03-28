import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON Schema Reference Resolver
 * 
 * A standalone utility to resolve JSON Schema references, including:
 * - Local references within a schema (#/definitions/...)
 * - References to external files (other.json)
 * - References to external files with fragments (other.json#/definitions/...)
 * - References to URLs (http://example.com/schema.json)
 * - Handling circular references to prevent infinite recursion
 * 
 * This is a self-contained implementation that can be used independently 
 * or integrated with SIFJsonSchemaUtil.
 * 
 * Usage:
 *   java JsonSchemaReferenceResolver [options] <schema-path>
 * 
 * Options:
 *   -o, --output <path>    Path to save the resolved schema
 *   -a, --analyze          Only analyze references without resolving
 *   -d, --debug            Enable debug logging
 *   -h, --help             Show this help message
 */
public class JsonSchemaReferenceResolver {
    private static final Logger LOG = Logger.getLogger(JsonSchemaReferenceResolver.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private static boolean debugMode = false;
    
    /**
     * Main method for command-line usage
     */
    public static void main(String[] args) {
        // Parse command line arguments
        String schemaPath = null;
        String outputPath = null;
        boolean analyzeOnly = false;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if ("-h".equals(arg) || "--help".equals(arg)) {
                printHelp();
                return;
            } else if ("-o".equals(arg) || "--output".equals(arg)) {
                if (i + 1 < args.length) {
                    outputPath = args[++i];
                } else {
                    System.err.println("Error: Missing output path");
                    printHelp();
                    return;
                }
            } else if ("-a".equals(arg) || "--analyze".equals(arg)) {
                analyzeOnly = true;
            } else if ("-d".equals(arg) || "--debug".equals(arg)) {
                debugMode = true;
            } else if (!arg.startsWith("-")) {
                schemaPath = arg;
            } else {
                System.err.println("Error: Unknown option: " + arg);
                printHelp();
                return;
            }
        }
        
        if (schemaPath == null) {
            System.err.println("Error: No schema path provided");
            printHelp();
            return;
        }
        
        System.out.println("JSON Schema Reference Resolver");
        System.out.println("=============================");
        System.out.println("Input schema: " + schemaPath);
        
        if (analyzeOnly) {
            // Analyze the schema
            System.out.println("\nAnalyzing schema references...");
            
            JsonNode schema = loadSchema(schemaPath);
            if (schema == null) {
                System.err.println("Failed to load schema: " + schemaPath);
                return;
            }
            
            Set<String> refs = findAllReferences(schema);
            System.out.println("\nFound " + refs.size() + " references:");
            for (String ref : refs) {
                System.out.println("  " + ref);
            }
        } else {
            // Resolve all references
            System.out.println("Output path: " + (outputPath != null ? outputPath : "auto-generated"));
            System.out.println("\nResolving all references...");
            
            boolean success = resolveAndSave(schemaPath, outputPath);
            
            if (success) {
                System.out.println("\n✅ Successfully resolved schema references");
            } else {
                System.err.println("\n❌ Failed to resolve schema references");
            }
        }
    }
    
    /**
     * Print usage help
     */
    private static void printHelp() {
        System.out.println("Usage: java JsonSchemaReferenceResolver [options] <schema-path>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output <path>    Path to save the resolved schema");
        System.out.println("  -a, --analyze          Only analyze references without resolving");
        System.out.println("  -d, --debug            Enable debug logging");
        System.out.println("  -h, --help             Show this help message");
    }
    
    /**
     * Helper method for logging
     */
    private static void log(String message) {
        System.out.println(message);
        if (debugMode) {
            LOG.info(message);
        }
    }
    
    /**
     * Helper method for debug logging
     */
    private static void debug(String message) {
        if (debugMode) {
            System.out.println("[DEBUG] " + message);
            LOG.fine(message);
        }
    }
    
    /**
     * Context class for the reference resolution process
     */
    static class RefContext {
        private final Set<String> visited = new HashSet<>();
        private final Map<String, JsonNode> externalSchemas = new HashMap<>();
        private final String basePath;
        private final JsonNode rootSchema;
        
        public RefContext(String basePath, JsonNode rootSchema) {
            this.basePath = basePath;
            this.rootSchema = rootSchema;
        }
        
        public String getBasePath() {
            return basePath;
        }
        
        public JsonNode getRootSchema() {
            return rootSchema;
        }
        
        public boolean isVisited(String ref) {
            return visited.contains(ref);
        }
        
        public void addVisited(String ref) {
            visited.add(ref);
        }
        
        public Set<String> getVisited() {
            return visited;
        }
        
        public boolean hasExternalSchema(String path) {
            return externalSchemas.containsKey(path);
        }
        
        public JsonNode getExternalSchema(String path) {
            return externalSchemas.get(path);
        }
        
        public void addExternalSchema(String path, JsonNode schema) {
            externalSchemas.put(path, schema);
        }
        
        public Map<String, JsonNode> getExternalSchemas() {
            return externalSchemas;
        }
    }
    
    /**
     * Resolve all references in a JSON Schema file
     * 
     * @param schemaPath Path to the JSON Schema file
     * @return The resolved schema
     */
    public static JsonNode resolveReferences(String schemaPath) {
        // Load the schema
        JsonNode schema = loadSchema(schemaPath);
        if (schema == null) {
            return null;
        }
        
        // Create a context for reference resolution
        RefContext context = new RefContext(schemaPath, schema);
        
        // Resolve all references
        return resolveAllReferences(schema, context);
    }
    
    /**
     * Resolve and save a JSON Schema file
     * 
     * @param inputPath Path to the input JSON Schema file
     * @param outputPath Path to save the resolved schema (null for default)
     * @return true if successful, false otherwise
     */
    public static boolean resolveAndSave(String inputPath, String outputPath) {
        if (outputPath == null) {
            outputPath = inputPath.replace(".json", "_resolved.json");
        }
        
        // Resolve all references
        JsonNode resolvedSchema = resolveReferences(inputPath);
        if (resolvedSchema == null) {
            return false;
        }
        
        // Save the resolved schema
        return saveSchema(resolvedSchema, outputPath);
    }
    
    /**
     * Recursively resolve all references in a schema node
     */
    private static JsonNode resolveAllReferences(JsonNode node, RefContext context) {
        // Special case for null nodes
        if (node == null) {
            return null;
        }
        
        // If this is a reference, resolve it
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            
            // Detect circular references
            if (context.isVisited(ref)) {
                log("⚠️ Circular reference detected: " + ref);
                // Return a node that indicates the circular reference
                ObjectNode refNode = mapper.createObjectNode();
                refNode.put("$ref", ref);
                refNode.put("description", "Circular reference detected");
                refNode.put("circularReference", true);
                
                // Copy all properties from the original node except $ref
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (!"$ref".equals(fieldName)) {
                        refNode.set(fieldName, node.get(fieldName));
                    }
                }
                
                return refNode;
            }
            
            // Mark this reference as visited
            context.addVisited(ref);
            debug("Added reference to visited set: " + ref);
            
            // Resolve the reference
            JsonNode resolved = resolveReference(ref, context);
            if (resolved != null) {
                // Create a new node with properties from both nodes
                ObjectNode result;
                if (resolved.isObject()) {
                    result = resolved.deepCopy();
                    debug("Created deep copy of resolved object for: " + ref);
                } else {
                    // Wrap non-object values
                    result = mapper.createObjectNode();
                    result.set("value", resolved);
                    debug("Wrapped non-object value in object for: " + ref);
                }
                
                // Copy all properties from the original node except $ref
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (!"$ref".equals(fieldName)) {
                        result.set(fieldName, node.get(fieldName));
                    }
                }
                
                // Recursively resolve any nested references
                debug("Recursively resolving nested references for: " + ref);
                return resolveAllReferences(result, context);
            } else {
                log("❌ Failed to resolve reference: " + ref);
                // Add a property to indicate the resolution failure
                ObjectNode failedNode = node.deepCopy();
                failedNode.put("resolutionFailed", true);
                return failedNode; // Keep the original reference with indicator
            }
        }
        
        // For object nodes, process all fields
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                // Resolve references in the field value
                result.set(fieldName, resolveAllReferences(fieldValue, context));
            }
            
            return result;
        }
        
        // For array nodes, process all elements
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            
            for (JsonNode element : node) {
                result.add(resolveAllReferences(element, context));
            }
            
            return result;
        }
        
        // For primitive values, return as is
        return node;
    }
    
    /**
     * Resolve a single reference
     */
    private static JsonNode resolveReference(String ref, RefContext context) {
        log("  Resolving reference: " + ref);
        
        // Special case for just "#" (reference to the root)
        if ("#".equals(ref)) {
            debug("    Returning root schema for '#' reference");
            return context.getRootSchema();
        }
        
        // Handle local references within the document
        if (ref.startsWith("#/")) {
            String path = ref.substring(2); // Remove "#/"
            String[] parts = path.split("/");
            
            debug("    Local reference with " + parts.length + " path components");
            
            JsonNode current = context.getRootSchema();
            for (String part : parts) {
                // Handle JSON pointer escaping
                part = part.replace("~1", "/").replace("~0", "~");
                debug("    Processing path component: " + part);
                
                if (current.has(part)) {
                    current = current.get(part);
                } else {
                    log("    ❌ Local reference part not found: " + part);
                    LOG.warning("Local reference part not found: " + part + " in ref: " + ref);
                    return null;
                }
            }
            
            debug("    Successfully resolved local reference: " + ref);
            return current;
        }
        
        // Handle external references
        if (ref.contains("#")) {
            // Split into file path and fragment
            String[] parts = ref.split("#", 2);
            String filePath = parts[0];
            String fragment = parts.length > 1 ? "#" + parts[1] : "#";
            
            debug("    External reference with file: '" + filePath + "' and fragment: '" + fragment + "'");
            
            // If it's just a fragment reference to the current document
            if (filePath.isEmpty()) {
                debug("    Fragment reference to current document");
                return resolveReference(fragment, context);
            }
            
            // Load the external schema if needed
            JsonNode externalSchema;
            if (context.hasExternalSchema(filePath)) {
                debug("    Using cached external schema for: " + filePath);
                externalSchema = context.getExternalSchema(filePath);
            } else {
                // Get the absolute path to the referenced file
                String absolutePath = getAbsolutePath(filePath, context.getBasePath());
                debug("    Loading external schema from: " + absolutePath);
                
                // Load the schema
                externalSchema = loadSchema(absolutePath);
                if (externalSchema == null) {
                    log("    ❌ Failed to load external schema: " + absolutePath);
                    LOG.warning("Failed to load external schema: " + absolutePath);
                    return null;
                }
                
                // Cache the schema
                debug("    Caching external schema for: " + filePath);
                context.addExternalSchema(filePath, externalSchema);
            }
            
            // Resolve the fragment within the external schema
            if (fragment.equals("#")) {
                debug("    Empty fragment, returning whole external schema");
                return externalSchema;
            } else {
                debug("    Resolving fragment within external schema: " + fragment);
                
                // Create a new context for the external schema
                RefContext externalContext = new RefContext(
                        getAbsolutePath(filePath, context.getBasePath()),
                        externalSchema);
                
                // Share the visited refs and external schemas to avoid circular references
                for (String visitedRef : context.getVisited()) {
                    externalContext.addVisited(visitedRef);
                }
                
                for (Map.Entry<String, JsonNode> entry : context.getExternalSchemas().entrySet()) {
                    externalContext.addExternalSchema(entry.getKey(), entry.getValue());
                }
                
                return resolveReference(fragment, externalContext);
            }
        } else if (!ref.isEmpty()) {
            // Reference to an entire file
            String filePath = ref;
            debug("    Reference to entire file: " + filePath);
            
            // Check if we've already loaded this schema
            if (context.hasExternalSchema(filePath)) {
                debug("    Using cached schema for: " + filePath);
                return context.getExternalSchema(filePath);
            }
            
            // Get the absolute path to the referenced file
            String absolutePath = getAbsolutePath(filePath, context.getBasePath());
            debug("    Resolving absolute path: " + absolutePath);
            
            // Load the schema
            JsonNode externalSchema = loadSchema(absolutePath);
            if (externalSchema == null) {
                log("    ❌ Failed to load external schema: " + absolutePath);
                LOG.warning("Failed to load external schema: " + absolutePath);
                return null;
            }
            
            // Cache the schema
            debug("    Caching schema for: " + filePath);
            context.addExternalSchema(filePath, externalSchema);
            return externalSchema;
        }
        
        log("    ❌ Unrecognized reference format: " + ref);
        LOG.warning("Unrecognized reference format: " + ref);
        return null;
    }
    
    /**
     * Get the absolute path for a reference
     */
    private static String getAbsolutePath(String refPath, String basePath) {
        // Handle absolute paths and URLs
        if (refPath.startsWith("/") || refPath.startsWith("http://") || refPath.startsWith("https://")) {
            return refPath;
        }
        
        // Handle relative paths
        File baseFile = new File(basePath);
        File baseDir = baseFile.getParentFile();
        
        // Fix for references that don't include the current directory
        // If the referenced file is not found in the current directory,
        // check if it exists in ../base or ../communication directories
        File refFile = new File(baseDir, refPath);
        if (!refFile.exists() && !refPath.startsWith("../")) {
            // Try common directories for schema types
            String[] commonDirs = {
                "../base/",
                "../communication/",
                "../codelist/",
                "../person/",
                "../meta/",
                "../organization/"
            };
            
            for (String dir : commonDirs) {
                File altFile = new File(baseDir, dir + refPath);
                if (altFile.exists()) {
                    debug("    Found alternate path for " + refPath + " at " + altFile.getAbsolutePath());
                    return altFile.getAbsolutePath();
                }
            }
        }
        
        return refFile.getAbsolutePath();
    }
    
    /**
     * Load a JSON Schema from a file or URL
     */
    private static JsonNode loadSchema(String path) {
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return loadSchemaFromUrl(path);
            } else {
                return loadSchemaFromFile(path);
            }
        } catch (Exception e) {
            System.out.println("❌ Error loading schema: " + e.getMessage());
            LOG.log(Level.WARNING, "Error loading schema: " + path, e);
            return null;
        }
    }
    
    /**
     * Load a JSON Schema from a file
     */
    private static JsonNode loadSchemaFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("❌ Schema file not found: " + filePath);
            return null;
        }
        
        JsonNode schema = mapper.readTree(file);
        System.out.println("  Schema loaded from file: " + filePath);
        return schema;
    }
    
    /**
     * Load a JSON Schema from a URL
     */
    private static JsonNode loadSchemaFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000); // 10 seconds timeout
        connection.setReadTimeout(30000);    // 30 seconds timeout
        
        try {
            connection.connect();
            
            if (connection.getResponseCode() != 200) {
                log("❌ HTTP error: " + connection.getResponseCode() + " - " + connection.getResponseMessage() + " for " + urlString);
                LOG.warning("HTTP error: " + connection.getResponseCode() + " - " + connection.getResponseMessage() + " for " + urlString);
                return null;
            }
            
            try (InputStream inputStream = connection.getInputStream()) {
                JsonNode schema = mapper.readTree(inputStream);
                log("  Schema loaded from URL: " + urlString);
                return schema;
            }
        } catch (IOException e) {
            log("❌ Connection error: " + e.getMessage() + " for " + urlString);
            LOG.log(Level.WARNING, "Connection error for URL: " + urlString, e);
            return null;
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Save a JSON Schema to a file
     */
    private static boolean saveSchema(JsonNode schema, String filePath) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), schema);
            System.out.println("  Schema saved: " + filePath);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error saving schema: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find all references in a schema
     */
    public static Set<String> findAllReferences(JsonNode schema) {
        Set<String> refs = new HashSet<>();
        collectReferences(schema, refs);
        return refs;
    }
    
    /**
     * Recursively collect all references in a schema
     */
    private static void collectReferences(JsonNode node, Set<String> refs) {
        if (node == null) return;
        
        if (node.isObject()) {
            // Check if this node has a $ref
            if (node.has("$ref")) {
                refs.add(node.get("$ref").asText());
            }
            
            // Check all fields recursively
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                collectReferences(node.get(fieldName), refs);
            }
        } else if (node.isArray()) {
            // Check all array elements
            for (JsonNode element : node) {
                collectReferences(element, refs);
            }
        }
    }
}