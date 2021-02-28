@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

REM save working diretory
set "CWD=%CD%"
REM change to parent directory of script. Yes, ..\.. (get ride of file name, and then directory)
cd %0\..\..

SET EXEC_CMD=GeneratorA.exe ^
    -f file://./config/GeneratorA/FuelLevelSensorProperties.json ^
    -s file://./config/GeneratorA/RotationalSpeedSensorProperties.json ^
    -t file://./config/GeneratorA/TemperatureSensorProperties.json ^
    -r %RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "GeneratorA" cmd /K "%EXEC_CMD%"
)

cd "%CWD%"
