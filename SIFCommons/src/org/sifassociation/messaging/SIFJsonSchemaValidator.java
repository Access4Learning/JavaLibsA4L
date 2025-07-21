package org.sifassociation.messaging;

import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaResolver;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.Problem;
import org.leadpony.justify.api.ProblemHandler;
import org.leadpony.justify.api.SpecVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.stream.JsonParser;

/**
 * Utility class for validating JSON payloads against JSON Schema using Justify 3.1.0.
 * 
 * This class provides methods to validate JSON strings against JSON Schema documents,
 * with support for both file-based and URL-based schema loading.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class SIFJsonSchemaValidator {
    
    private static final Logger logger = Logger.getLogger(SIFJsonSchemaValidator.class.getName());
    private static final JsonValidationService service = JsonValidationService.newInstance();
    
    // Cache for resolved schemas to avoid redundant file operations
    private static final ConcurrentHashMap<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();
    
    // Thread-local storage for schema directory context
    private static final ThreadLocal<Path> currentSchemaDirectory = new ThreadLocal<>();
    
    // Schema resolver to handle external schema references
    private static final JsonSchemaResolver schemaResolver = new JsonSchemaResolver() {
        @Override
        public JsonSchema resolveSchema(URI uri) {
            String uriString = uri.toString();
            
            // Check cache first
            JsonSchema cachedSchema = schemaCache.get(uriString);
            if (cachedSchema != null) {
                logger.fine("Using cached schema for: " + uriString);
                return cachedSchema;
            }
            
            logger.fine("Resolving schema URI: " + uriString);
            
            // Extract just the filename from the URI
            String fileName = uriString;
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            if (fileName.contains("#")) {
                fileName = fileName.substring(0, fileName.indexOf("#"));
            }
            
            JsonSchema resolvedSchema = null;
            
            // First try to load from the schema directory (if available)
            Path schemaDir = currentSchemaDirectory.get();
            if (schemaDir != null) {
                try {
                    Path refPath = schemaDir.resolve(fileName);
                    if (Files.exists(refPath)) {
                        logger.fine("Found schema in schema directory: " + refPath);
                        try (InputStream fileStream = Files.newInputStream(refPath)) {
                            resolvedSchema = service.readSchema(fileStream);
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Error accessing schema directory for: " + fileName + " - " + e.getMessage());
                }
            }
            
            // Fallback: try to load from classpath resources
            InputStream stream = SIFJsonSchemaValidator.class.getClassLoader().getResourceAsStream(fileName);
            if (stream != null) {
                logger.fine("Found schema in classpath: " + fileName);
                try {
                    resolvedSchema = service.readSchema(stream);
                } catch (Exception e) {
                    logger.warning("Error loading schema from classpath: " + fileName + " - " + e.getMessage());
                }
            }
            
            // Try loading from examples directory in resources
            if (resolvedSchema == null) {
                stream = SIFJsonSchemaValidator.class.getClassLoader().getResourceAsStream("examples/" + fileName);
                if (stream != null) {
                    logger.fine("Found schema in examples directory: " + fileName);
                    try {
                        resolvedSchema = service.readSchema(stream);
                    } catch (Exception e) {
                        logger.warning("Error loading schema from examples directory: " + fileName + " - " + e.getMessage());
                    }
                }
            }
            
            // Try loading from file system relative to current working directory  
            if (resolvedSchema == null) {
                try {
                    Path filePath = Paths.get(fileName);
                    if (Files.exists(filePath)) {
                        logger.fine("Found schema in file system: " + fileName);
                        try (InputStream fileStream = Files.newInputStream(filePath)) {
                            resolvedSchema = service.readSchema(fileStream);
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Error accessing file system for: " + fileName + " - " + e.getMessage());
                }
            }
            
            // Try loading from resources/examples directory (for demo usage)
            if (resolvedSchema == null) {
                try {
                    Path resourcePath = Paths.get("resources", "examples", fileName);
                    if (Files.exists(resourcePath)) {
                        logger.info("Schema resolver loaded: " + fileName);
                        try (InputStream fileStream = Files.newInputStream(resourcePath)) {
                            resolvedSchema = service.readSchema(fileStream);
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Error accessing resources/examples for: " + fileName + " - " + e.getMessage());
                }
            }
            
            if (resolvedSchema != null) {
                // Cache the successfully resolved schema
                schemaCache.put(uriString, resolvedSchema);
                logger.fine("Cached schema: " + fileName);
            } else {
                logger.warning("Could not resolve schema: " + uriString);
            }
            
            return resolvedSchema;
        }
    };
    
    /**
     * Validation result containing success status and any validation problems.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getErrorSummary() {
            if (errors.isEmpty()) {
                return "No validation errors";
            }
            return "Validation errors: " + String.join("; ", errors);
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from a file path.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaPath The file path to the JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    public static ValidationResult validatePayload(String jsonPayload, String schemaPath) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON payload cannot be null or empty"));
        }
        
        try {
            Path path = Paths.get(schemaPath);
            if (!Files.exists(path)) {
                return new ValidationResult(false, List.of("Schema file not found: " + schemaPath));
            }
            
            return validatePayloadWithPath(jsonPayload, path);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error reading schema file: " + schemaPath, e);
            return new ValidationResult(false, List.of("Error reading schema file: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from a URL.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaUrl The URL to the JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    public static ValidationResult validatePayload(String jsonPayload, URL schemaUrl) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON payload cannot be null or empty"));
        }
        
        try (InputStream schemaStream = schemaUrl.openStream()) {
            return validatePayload(jsonPayload, schemaStream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading schema from URL: " + schemaUrl, e);
            return new ValidationResult(false, List.of("Error reading schema from URL: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema provided as a string.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaString The JSON Schema as a string
     * @return ValidationResult containing success status and any errors
     */
    public static ValidationResult validatePayloadWithSchemaString(String jsonPayload, String schemaString) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON payload cannot be null or empty"));
        }
        
        if (schemaString == null || schemaString.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON schema cannot be null or empty"));
        }
        
        try (StringReader schemaReader = new StringReader(schemaString)) {
            // Create a schema reader factory with the resolver
            var readerFactory = service.createSchemaReaderFactoryBuilder()
                .withSchemaResolver(schemaResolver)
                .build();
            var reader = readerFactory.createSchemaReader(schemaReader);
            JsonSchema schema = reader.read();
            return validateWithSchema(jsonPayload, schema);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing schema string", e);
            return new ValidationResult(false, List.of("Error parsing schema: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from a file path.
     * This method preserves file context for resolving relative schema references.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaPath The Path to the JSON Schema file
     * @return ValidationResult containing success status and any errors
     */
    private static ValidationResult validatePayloadWithPath(String jsonPayload, Path schemaPath) {
        try {
            // Store the schema directory for the resolver to use
            currentSchemaDirectory.set(schemaPath.getParent());
            
            try (InputStream schemaStream = Files.newInputStream(schemaPath)) {
                return validatePayload(jsonPayload, schemaStream);
            } finally {
                // Clean up thread local
                currentSchemaDirectory.remove();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading schema file: " + schemaPath, e);
            return new ValidationResult(false, List.of("Error reading schema file: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from an InputStream.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaStream The InputStream containing the JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    private static ValidationResult validatePayload(String jsonPayload, InputStream schemaStream) {
        try {
            // Create a schema reader factory with the resolver
            var readerFactory = service.createSchemaReaderFactoryBuilder()
                .withSchemaResolver(schemaResolver)
                .build();
            var reader = readerFactory.createSchemaReader(schemaStream);
            JsonSchema schema = reader.read();
            return validateWithSchema(jsonPayload, schema);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error reading or parsing schema", e);
            return new ValidationResult(false, List.of("Error reading or parsing schema: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a compiled JSON Schema.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schema The compiled JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    private static ValidationResult validateWithSchema(String jsonPayload, JsonSchema schema) {
        List<String> errors = new ArrayList<>();
        
        // Create a problem handler to collect validation errors
        ProblemHandler problemHandler = problems -> {
            for (Problem problem : problems) {
                errors.add(problem.getMessage());
            }
        };
        
        try {
            // Create a validating parser
            try (JsonParser parser = service.createParser(new StringReader(jsonPayload), schema, problemHandler)) {
                // Force full parse + validation
                while (parser.hasNext()) {
                    parser.next();
                }
            }
            
            // Check if validation succeeded (no errors collected)
            return new ValidationResult(errors.isEmpty(), errors);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during JSON validation", e);
            errors.add("Error during JSON validation: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
    }
    
    /**
     * Convenience method to check if a JSON payload is valid against a schema file.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaPath The file path to the JSON Schema
     * @return true if the payload is valid, false otherwise
     */
    public static boolean isValid(String jsonPayload, String schemaPath) {
        return validatePayload(jsonPayload, schemaPath).isValid();
    }
    
    /**
     * Convenience method to check if a JSON payload is valid against a schema URL.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaUrl The URL to the JSON Schema
     * @return true if the payload is valid, false otherwise
     */
    public static boolean isValid(String jsonPayload, URL schemaUrl) {
        return validatePayload(jsonPayload, schemaUrl).isValid();
    }
    
    /**
     * Convenience method to check if a JSON payload is valid against a schema string.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaString The JSON Schema as a string
     * @return true if the payload is valid, false otherwise
     */
    public static boolean isValidWithSchemaString(String jsonPayload, String schemaString) {
        return validatePayloadWithSchemaString(jsonPayload, schemaString).isValid();
    }
}