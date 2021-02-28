@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=python gps_sensor.py ^
    --thing=file://./config/GpsSensor1Properties.json ^
    --lat=51.900000 ^
    --lng=4.000000 ^
    --running-time=%RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "GPS Sensor 1" cmd /K "%EXEC_CMD%"
)
