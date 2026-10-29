@echo off
rem Builds the model and logic implementation jars into ..\artifacts and runs the tester.
rem Usage: build.cmd            (build + test)
rem        build.cmd notest     (build only)
setlocal
cd /d "%~dp0"

set OUT=out
set ARTIFACTS=..\artifacts
set API=model-1.0.jar;logic-1.0.jar

if exist %OUT% rmdir /s /q %OUT%
mkdir %OUT%\model %OUT%\logic
if not exist %ARTIFACTS% mkdir %ARTIFACTS%

echo === Compiling model
call :sources model\src %OUT%\model-sources.txt
javac --release 25 -d %OUT%\model -cp model-1.0.jar @%OUT%\model-sources.txt || goto :error

echo === Compiling logic
call :sources logic\src %OUT%\logic-sources.txt
javac --release 25 -d %OUT%\logic -cp "%API%;%OUT%\model" @%OUT%\logic-sources.txt || goto :error

echo === Packaging
jar --create --file %ARTIFACTS%\workshops-model-impl.jar -C %OUT%\model . || goto :error
jar --create --file %ARTIFACTS%\workshops-logic-impl.jar -C %OUT%\logic . || goto :error
echo Jars written to %ARTIFACTS%

if /i "%1"=="notest" goto :eof

echo === Running tester
java -cp "workshop-tester.jar;%API%;%ARTIFACTS%\workshops-model-impl.jar;%ARTIFACTS%\workshops-logic-impl.jar" patmal.workshops.main.Main
exit /b %errorlevel%

:error
echo BUILD FAILED
exit /b 1

rem Writes every .java file under %1 to the argfile %2, quoted and with forward slashes
rem (the project path contains spaces, and javac argfiles treat backslash as an escape).
:sources
setlocal enabledelayedexpansion
(for /r %1 %%f in (*.java) do (
    set "p=%%f"
    echo "!p:\=/!"
)) > %2
endlocal
exit /b 0
