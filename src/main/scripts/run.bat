@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=standalone.bat"
)

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

pushd "%DIRNAME%.."
set "RESOLVED_DASH_HOME=%CD%"
popd

if "x%DASH_HOME%" == "x" (
  set "DASH_HOME=%RESOLVED_DASH_HOME%"
)

pushd "%DASH_HOME%"
set "SANITIZED_DASH_HOME=%CD%"
popd

if "x%RUN_CONF%" == "x" (
   set "RUN_CONF=%DIRNAME%run.bat.conf"
)
if exist "%RUN_CONF%" (
   echo Calling "%RUN_CONF%"
   call "%RUN_CONF%" %*
) else (
   echo Config file not found "%RUN_CONF%"
)

if "%DEBUG_MODE%" == "true" (
   echo "%JAVA_OPTS%" | findstr /I "\-agentlib:jdwp" > nul
  if errorlevel == 1 (
     set "JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%DEBUG_PORT%,server=y,suspend=n"
  ) else (
     echo Debug already enabled in JAVA_OPTS, ignoring --debug argument
  )
)

set DIRNAME=

rem Set the dash log dir
if "x%DASH_LOG_DIR%" == "x" (
  set  "DASH_LOG_DIR=%DASH_HOME%\log"
)

rem Set the dash configuration dir
if "x%DASH_CONF_DIR%" == "x" (
  set  "DASH_CONF_DIR=%DASH_HOME%\conf"
)

rem Set the dash configuration dir
if "x%DASH_LIBS_DIR%" == "x" (
  set  "DASH_LIBS_DIR=%DASH_HOME%\lib"
)

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  if not exist "%JAVA_HOME%" (
    echo JAVA_HOME "%JAVA_HOME%" path doesn't exist
    goto END
  ) else (
    echo Setting JAVA property to "%JAVA_HOME%\bin\java"
    set "JAVA=%JAVA_HOME%\bin\java"
  )
)

echo =========================================================================
echo .
echo   Application Environment
echo .
echo   APPLICATION_HOME_DIR: "%DASH_HOME%"
echo .
echo   JAVA: "%JAVA_HOME%"
echo .
echo   JAVA_OPTS: "%JAVA_OPTS%"
echo .
echo   CLASSPATH: "%CLASSPATH%"
echo .
echo =========================================================================
echo .

"%JAVA%" %JAVA_OPTS% ^
    "-Dapplication.home.dir=file:%DASH_HOME%" ^
    "-Dapplication.conf.dir=file:%DASH_CONF_DIR%" ^
    "-Dlog.dir=file:%DASH_LOG_DIR%" ^
    "-Dlogback.configurationFile=file:%DASH_CONF_DIR%/logback.xml" ^
    com.nuodb.field.Main ^
    %SERVER_OPTS%
