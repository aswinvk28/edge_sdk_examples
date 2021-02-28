@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=java -jar S3_DistanceServiceLauncher.jar ^
    --thing file://./config/DistanceServiceProperties.json ^
    --lat 52.057313 ^
    --lng 4.130987 ^
    --running-time %RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Distance Service" cmd /K "%EXEC_CMD%"
)