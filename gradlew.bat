@echo off
set DIR=%~dp0
java -Dfile.encoding=UTF-8 -jar "%DIR%gradle\wrapper\gradle-wrapper.jar" %*
