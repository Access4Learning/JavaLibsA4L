# JSON Schema Reference Resolver

A standalone utility for resolving references in JSON Schema files. This tool can handle:

- Local references within a schema (`#/definitions/...`)
- References to external files (`other.json`)
- References to external files with fragments (`other.json#/definitions/...`)
- References to URLs (`http://example.com/schema.json`)
- Circular references (detected and properly handled)

## Features

- Recursively resolves all references in a JSON Schema
- Handles circular references gracefully (prevents infinite recursion)
- Supports both file system and HTTP/HTTPS schema loading
- Preserves properties from the original reference objects
- Detailed debug output with the `-d` option
- Analysis mode to list all references without resolving them

## Usage

### On Unix/Mac:

```bash
./resolve_schema.sh [options] <schema-path>
```

### On Windows:

```batch
resolve_schema.bat [options] <schema-path>
```

### Options:

- `-o, --output <path>`: Path to save the resolved schema
- `-a, --analyze`: Only analyze references without resolving
- `-d, --debug`: Enable debug logging
- `-h, --help`: Show help message

## Examples

### Resolve a schema with all references:

```bash
./resolve_schema.sh resources/json/organization/OrganizationType.json
```

### Save the resolved schema to a specific file:

```bash
./resolve_schema.sh -o resolved-schema.json resources/json/organization/OrganizationType.json
```

### Only analyze references without resolving:

```bash
./resolve_schema.sh -a resources/json/organization/OrganizationType.json
```

### Enable debug output:

```bash
./resolve_schema.sh -d resources/json/organization/OrganizationType.json
```

## How It Works

1. The tool loads the input schema
2. It identifies all `$ref` references in the schema
3. For each reference:
   - Local references (`#/definitions/Type`) are resolved within the same schema
   - External references are resolved by loading the target schema
   - External references with fragments are resolved by navigating to the fragment within the external schema
   - Circular references are detected and handled with special markers
4. The fully resolved schema is saved to the output file

## Integration

This utility is self-contained but can also be used programmatically:

```java
// Resolve all references
JsonNode resolvedSchema = JsonSchemaReferenceResolver.resolveReferences(schemaPath);

// Resolve and save
JsonSchemaReferenceResolver.resolveAndSave(inputPath, outputPath);
```

## Requirements

- Java 8 or higher
- Jackson JSON library (included in the classpath)