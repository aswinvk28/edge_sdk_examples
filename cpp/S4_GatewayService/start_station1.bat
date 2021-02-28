@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

set FLAG=/K
IF "%2"=="-fg" (
    set FLAG=/C
)

start "Station 1 - Camera" cmd %FLAG% "camera.exe" ^
    --thing=file://./config/Station1/Camera1Properties.json ^
    --barcodes=barcodes1.txt ^
    --running-time=%RUNNING_TIME%

start "Station 1 - Light Sensor" cmd %FLAG% "lightsensor.exe" ^
    file://./config/Station1/LightSensorProperties.json ^
    %RUNNING_TIME%
