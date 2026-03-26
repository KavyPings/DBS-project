@echo off
echo ==============================================
echo Mini Course Management System Builder
echo ==============================================

if not exist lib\sqlite-jdbc.jar (
    echo [1/3] Downloading JDBC SQLite driver...
    mkdir lib 2>nul
    curl -L "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.2.0/sqlite-jdbc-3.45.2.0.jar" -o lib\sqlite-jdbc.jar
    curl -L "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar" -o lib\slf4j-api.jar
    curl -L "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar" -o lib\slf4j-simple.jar
) else (
    echo [1/3] Dependencies found in \lib.
)

echo [2/3] Compiling Project...
mkdir bin 2>nul
javac -d bin -cp "lib\*" src\*.java

if %errorlevel% neq 0 (
    echo Compilation failed. Check the errors above.
    pause
    exit /b %errorlevel%
)

echo [3/3] Launching App...
java -cp "bin;lib\*" MainDashboard

pause
