@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/GatewayServiceProperties.json
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

REM setting variable LINES (represents terminal height)
SET LINES=%2
IF "%2"=="" (
    SET LINES=45
)

SET EXEC_CMD=java -jar S4_GatewayServiceLauncher.jar -t %THING_PROPERTIES_URI% -r %RUNNING_TIME%
SET EXEC_FINAL_CMD="mode con lines=%LINES% && %EXEC_CMD%"

IF "%3"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Gateway Service" cmd /K %EXEC_FINAL_CMD%
)