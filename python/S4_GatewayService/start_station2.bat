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

start "Station 2 - Camera 1" cmd %FLAG% "python camera.py" ^
    --thing=file://./config/Station2/Camera1Properties.json ^
    --barcodes=barcodes2.txt ^
    --running-time=%RUNNING_TIME%

start "Station 2 - Camera 2" cmd %FLAG% "python camera.py" ^
    --thing=file://./config/Station2/Camera2Properties.json ^
    --barcodes=barcodes2.txt ^
    --running-time=%RUNNING_TIME%

start "Station 2 - Light Sensor" cmd %FLAG% "python light_sensor.py" ^
    file://./config/Station2/LightSensorProperties.json ^
    %RUNNING_TIME%
