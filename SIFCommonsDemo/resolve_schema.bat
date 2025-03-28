@echo off
REM Script to resolve JSON Schema references
REM Usage: resolve_schema.bat [options] <schema-path>

SET SCRIPT_DIR=%~dp0
cd %SCRIPT_DIR%

REM Set the classpath with all required libraries
SET CLASSPATH=dist\JsonSchemaResolver.jar;dist\SIFCommonsDemo.jar

FOR %%F IN (lib\*.jar) DO (
  CALL :AddToClasspath %%F
)

FOR %%F IN (lib\*\*.jar) DO (
  CALL :AddToClasspath %%F
)

FOR %%F IN (lib\*\*\*.jar) DO (
  CALL :AddToClasspath %%F
)

REM Run the resolver
java -cp "%CLASSPATH%" JsonSchemaReferenceResolver %*
goto :EOF

:AddToClasspath
SET CLASSPATH=%CLASSPATH%;%1
goto :EOF