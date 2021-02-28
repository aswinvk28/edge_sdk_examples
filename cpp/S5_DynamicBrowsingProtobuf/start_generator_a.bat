@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=generator_a.exe ^
    --fuel-sensor=file://./config/GeneratorA/FuelLevelSensorProperties.json ^
    --speed-sensor=file://./config/GeneratorA/RotationalSpeedSensorProperties.json ^
    --temp-sensor=file://./config/GeneratorA/TemperatureSensorProperties.json ^
    --running-time=%RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Generator A" cmd /K "%EXEC_CMD%"
)