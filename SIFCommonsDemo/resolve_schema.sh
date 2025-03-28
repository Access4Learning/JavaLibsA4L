#!/bin/bash
# Script to resolve JSON Schema references
# Usage: ./resolve_schema.sh [options] <schema-path>

cd $(dirname "$0")

# Set the classpath with all required libraries
CLASSPATH="dist/JsonSchemaResolver.jar:dist/SIFCommonsDemo.jar"
for jar in lib/*.jar lib/*/*.jar lib/*/*/*.jar; do
  if [ -f "$jar" ]; then
    CLASSPATH="$CLASSPATH:$jar"
  fi
done

# Run the resolver
java -cp "$CLASSPATH" JsonSchemaReferenceResolver "$@"