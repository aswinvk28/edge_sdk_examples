@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=generator_b.exe ^
    --speed-sensor=file://./config/GeneratorB/RotationalSpeedSensorProperties.json ^
    --temp-sensor=file://./config/GeneratorB/TemperatureSensorProperties.json ^
    --running-time=%RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Generator B" cmd /K "%EXEC_CMD%"
)