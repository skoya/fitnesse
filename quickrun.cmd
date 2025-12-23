@echo off
setlocal

if not defined JAVA_HOME (
  set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21"
)

set "SCRIPT_DIR=%~dp0"

if not defined FITNESSE_VERTX_PORT set "FITNESSE_VERTX_PORT=8080"
if not defined FITNESSE_ROOT_PATH set "FITNESSE_ROOT_PATH=%SCRIPT_DIR%"
if not defined FITNESSE_ROOT_DIR set "FITNESSE_ROOT_DIR=FitNesseRoot"
if not defined FITNESSE_AUTH_ENABLED set "FITNESSE_AUTH_ENABLED=false"
if not defined SLIM_PORT set "SLIM_PORT=8099"

call "%SCRIPT_DIR%gradlew.bat" classes copyRuntimeLibs
if errorlevel 1 exit /b 1

set "JAVA_EXE=%JAVA_HOME%\bin\java"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

"%JAVA_EXE%" ^
  -Dprevent.system.exit=false ^
  -Djava.security.manager=allow ^
  -Dslim.port=%SLIM_PORT% ^
  -cp "build\classes\java\main;build\resources\main;lib\*" ^
  fitnesse.vertx.FitNesseVertxMain
