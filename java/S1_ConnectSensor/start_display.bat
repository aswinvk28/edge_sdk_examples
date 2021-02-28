@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET THING_PROPERTIES_URI=file://./config/TemperatureDisplayProperties.json
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

SET EXEC_CMD=java -jar S1_TemperatureDisplayLauncher.jar -r %RUNNING_TIME% -t %THING_PROPERTIES_URI%

IF "%2"=="-fg" (
    %EXEC_CMD%
) ELSE (
    start "Temperature Display" cmd /K "%EXEC_CMD%"
)