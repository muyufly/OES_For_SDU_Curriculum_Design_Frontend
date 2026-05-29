@echo off
cd /d "%~dp0"
echo Starting OES JavaFX client via Maven...
call mvnw.cmd javafx:run
