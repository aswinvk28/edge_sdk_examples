@echo off

REM save working diretory
set "CWD=%CD%"
REM change to directory of script. The \.. gets rid of file name.
cd %0\..

SET SENSOR_NUMBER=%1
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/TemperatureSensor%SENSOR_NUMBER%Properties.json
SET RUNNING_TIME=%2
IF "%2"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=python -u temperature_sensor.py %THING_PROPERTIES_URI% %RUNNING_TIME%

IF "%3"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Temperature Sensor %SENSOR_NUMBER%" cmd /K "%EXEC_CMD%"
)

REM restore the working directory
cd "%CWD%"
