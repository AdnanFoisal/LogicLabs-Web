@echo off
REM Launches Logic Labs using Skia's software renderer.
REM Use this only if an older Windows 10 graphics driver causes display glitches.
start "" "%~dp0LogicLabs.exe" --software
