@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/TemperatureSensorProperties.json
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

REM save working diretory
set "CWD=%CD%"
REM change to parent directory of script. Yes, ..\.. (get ride of file name, and then directory)
cd %0\..\..

SET EXEC_CMD=temperaturesensor.exe %THING_PROPERTIES_URI% %RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Temperature Sensor" cmd /K "%EXEC_CMD%"
)

cd "%CWD%"
