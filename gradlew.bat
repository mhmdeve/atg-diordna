@echo off
setlocal
set VERSION=8.13
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set CACHE=%GRADLE_USER_HOME%\wrapper\dists\gradle-%VERSION%-bin\manual
set DIST=%CACHE%\gradle-%VERSION%
set ZIP=%CACHE%\gradle-%VERSION%-bin.zip
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%CACHE%" mkdir "%CACHE%"
  if not exist "%ZIP%" (
    echo Downloading Gradle %VERSION% distribution...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%ZIP%'"
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%CACHE%'"
)
call "%DIST%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
