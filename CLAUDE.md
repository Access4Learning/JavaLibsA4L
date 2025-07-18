# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaLibsA4L is a comprehensive Java library for implementing the Schools Interoperability Framework (SIF) specifications. The project supports both SIF 2.x and SIF 3.x protocols, providing XML-JSON conversion, messaging, schema processing, and XML database integration capabilities.

## Build Commands

The project uses Apache Ant for building:

- **Clean and build**: `ant clean build`
- **Run tests**: `ant test`
- **Run single test**: `ant test-single -Dtest.includes=org/sifassociation/messaging/SIFVersionTest.java`
- **Generate javadocs**: `ant javadoc`

Build from either the SIFCommons or SIFCommonsDemo directories. The SIFCommons project is the core library, while SIFCommonsDemo provides usage examples and JSON schema analysis tools.

## JSON Schema Analysis Tools

The SIFCommonsDemo project includes standalone JSON schema analysis utilities:

- **Resolve schema references**: `./resolve_schema.sh [options] <schema-path>`
- **Analyze schema structure**: Run `JsonSchemaAnalyzer` class for detailed schema analysis
- **Options**: `-o` (output path), `-a` (analyze only), `-d` (debug), `-h` (help)

## Core Architecture

### SIF Version Handling

The library implements a **version-specific strategy pattern** with clean separation between protocols:

- **SIF2MessageXML**: Complex implementation for SIF 2.x with HTTP/SOAP transport, extensive header management, and built-in security (authentication/encryption levels 0-4)
- **SIF3Message**: Simplified implementation for SIF 3.x with REST transport, modern JSON/XML format flexibility, and delegated security

Both implement the `ISIFMessageXML` interface for consistent message parsing, transport handling, and payload validation.

### XML-JSON Conversion

The XMLJSON package provides multiple conversion strategies via the `IXmlJson` interface:

- **GoessnerNative**: Primary implementation following Goessner notation standards
- **JacksonNative**: Advanced implementation with better type preservation and round-trip support
- **PESCNative**: Support for Postsecondary Electronic Standards Council JSON notation

SIF 3.x messages automatically detect and convert between XML and JSON formats seamlessly.

### Message Handling Patterns

- **Immutable message design**: Once parsed, messages maintain original state
- **Transport agnostic**: Core message logic separated from transport specifics
- **Factory pattern**: `SIF2Payloads` and `SIF3Payloads` provide static factory methods for payload creation
- **Template-based**: SIF 3.x uses XML templates from `resources/payloads/` directory

### Schema Processing

The schema package provides XML schema traversal and analysis using visitor patterns:

- **PathConverter**: Converts between XPath and JSON Path representations
- **Annotation processing**: Handles SIF-specific schema annotations and metadata
- **Visitor implementations**: `PathAllVisit`, `PathMandatoryVisit`, `PathObjectVisit` for different exploration needs

## Key Dependencies

- **XOM**: Primary XML library chosen for tree API, efficient SAX parsing, namespace support, and UTF-8 handling
- **Jackson**: JSON processing for schema analysis and conversion
- **eXist**: XML database integration via both XML:DB API and REST interfaces
- **Apache Commons**: HTTP client and utilities

## Development Notes

- The library uses **telescoping constructor pattern** for building messages
- **Namespace management** is sophisticated across versions with automatic prefix handling
- **Error handling** is comprehensive with detailed missing component tracking
- **Security**: SIF 2.x has built-in authentication; SIF 3.x delegates to transport layer
- **Thread safety**: SIFVersion and SIFRefId classes are designed as immutable objects

## Testing

Tests are located in the `test/` directories. The library includes unit tests for core components like SIFVersion, SIFRefId, and SIF2MessageXML. Run tests with the ant commands above.

## Resource Files

- **SIFCommons/resources/**: Contains message templates, schemas, objects, and JavaScript utilities
- **SIFCommonsDemo/resources/**: Contains JSON schemas, examples, and analysis tools
- **Build integration**: The ant build copies necessary resources to `build/classes/` during compilation