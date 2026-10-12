@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer is required. Install a JDK and try again.
  pause
  exit /b 1
)
if not exist "target\plantcausal-core-0.2.0.jar" (
  call mvn --batch-mode --no-transfer-progress package
  if errorlevel 1 (
    echo Build failed. Open pom.xml in IntelliJ IDEA and run Main instead.
    pause
    exit /b 1
  )
)
java -jar "target\plantcausal-core-0.2.0.jar"
if errorlevel 1 pause
