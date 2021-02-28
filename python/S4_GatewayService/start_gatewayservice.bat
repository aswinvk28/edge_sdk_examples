@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/GatewayServiceProperties.json
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET LINES=%2
IF "%2"=="" (
    SET LINES=45
)
SET EXEC_CMD=python gatewayservice.py %THING_PROPERTIES_URI% %RUNNING_TIME%
SET EXEC_FINAL_CMD="mode con lines=%LINES% && %EXEC_CMD%"

IF "%3"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Gateway Service" cmd /K %EXEC_FINAL_CMD%
)
