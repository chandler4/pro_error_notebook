@REM Maven Wrapper (Windows cmd)
@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven Wrapper jar ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$url = (Select-String -Path '%WRAPPER_PROPERTIES%' -Pattern '^wrapperUrl=(.+)$').Matches[0].Groups[1].Value.Trim();" ^
    "Invoke-WebRequest -Uri $url -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 (
    echo ERROR: failed to download wrapper jar >&2
    exit /b 1
  )
)

if exist "%JAVA_HOME%\bin\java.exe" (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVACMD=java"
)

"%JAVACMD%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR:~0,-1%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
