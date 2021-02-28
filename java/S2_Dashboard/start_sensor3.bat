@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/TemperatureSensor3Properties.json
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=java -jar S2_TemperatureSensorLauncher.jar -t %THING_PROPERTIES_URI% -r %RUNNING_TIME%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Temperature Sensor 3" cmd /K "%EXEC_CMD%"
)