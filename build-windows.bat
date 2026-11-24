@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo   Logic Labs - Windows 10/11 Installer (.exe / .msi) Build
echo ============================================================
echo.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] JDK 21 is required on PATH to build the Windows installer.
    echo Download Eclipse Temurin 21 JDK from https://adoptium.net/
    exit /b 1
)

echo [1/3] Running full 9-module test suite...
call gradlew.bat test --no-daemon
if %errorlevel% neq 0 (
    echo [ERROR] Tests failed. Aborting build.
    exit /b 1
)

echo.
echo [2/3] Building Windows portable fat JAR...
call gradlew.bat :desktop-app:windowsFatJar --no-daemon
if %errorlevel% neq 0 (
    echo [ERROR] windowsFatJar failed.
    exit /b 1
)

echo.
echo [3/3] Packaging native Windows .exe and .msi installers via jpackage...
call gradlew.bat :desktop-app:packageExe :desktop-app:packageMsi --no-daemon
if %errorlevel% neq 0 (
    echo [WARN] jpackage requires WiX Toolset 3.x installed on Windows.
    echo You can still build a standalone app image with:
    echo   gradlew.bat :desktop-app:createDistributable --no-daemon
    exit /b 1
)

echo.
echo ============================================================
echo   BUILD COMPLETE!
echo   Installers are located in:
echo     desktop-app\build\compose\binaries\main\exe\
echo     desktop-app\build\compose\binaries\main\msi\
echo ============================================================
endlocal
