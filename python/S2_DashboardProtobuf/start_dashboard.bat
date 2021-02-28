@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/TemperatureDashboardProperties.json
SET FILTER=floor1
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=python temperature_dashboard.py %THING_PROPERTIES_URI% %FILTER% %RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Temperature Dashboard" cmd /K "%EXEC_CMD%"
)
