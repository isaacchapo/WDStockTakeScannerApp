@echo off
cd /d F:\StockT\StockApp
call gradlew.bat installDebug
exit /b %ERRORLEVEL%
