@echo off
if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem Entry point of mypsycho-rrun
rem
rem Environment Variable Prequisites ${undefined}
rem
rem
rem   RRUN_OPTS   (Optional) Java runtime options used when 
rem                   command is executed.
rem
rem   JAVA_HOME       Must point at your Java Development Kit installation
rem                   or your Java Runtime Environment installation
rem
rem
rem ---------------------------------------------------------------------------

rem Identification of installation directory
set RRUN_HOME=%~dp0.\..


rem Get standard environment variables
if exist "%RRUN_HOME%\bin\setenv.bat" call "%RRUN_HOME%\bin\setenv.bat"


rem Make sure prerequisite environment variables are set
if not "%JAVA_EXEC%" == "" goto run
if not "%JAVA_HOME%" == "" goto gotJavaHome
set JAVA_EXEC=java
goto run
:gotJavaHome
set JAVA_EXEC="%JAVA_HOME%\bin\java"
goto run
:run

%JAVA_EXEC% %RRUN_OPTS% -jar "%RRUN_HOME%\lib\org.mypsycho.rrun\rrun-control-1.0.0-SNAPSHOT.jar" %*
if errorlevel 1 pause

if "%OS%" == "Windows_NT" endlocal
