@echo off
setlocal enabledelayedexpansion
cd /d %~dp0\..
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
xcopy /E /I /Y src\main\resources\* out\ > nul
del sources.txt
set CP=out
if exist lib (
  for %%f in (lib\*.jar) do set CP=!CP!;%%f
) else (
  echo 警告：找不到 lib\*.jar。建議改用 Maven：mvn clean compile exec:java
  echo 若要用此批次檔，請將 sqlite-jdbc jar 放入 lib\ 資料夾。
)
java -cp "%CP%" com.gymapp.App
endlocal
