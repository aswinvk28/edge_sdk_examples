@echo off
SET ADLINK_DATARIVER_URI=file://%EDGE_SDK_HOME%/etc/config/default_datariver_config_v1.7.xml
SET RUNNING_TIME=%1
IF "%1"=="" (
    SET RUNNING_TIME=60
)

REM save working diretory
set "CWD=%CD%"
REM change to parent directory of script. Yes, ..\.. (get ride of file name, and then directory)
cd %0\..\..

set FLAG=/K
IF "%2"=="-fg" (
    set FLAG=/C
)

start "Station 1 - Camera" cmd %FLAG% "Camera.exe" ^
    -u file://./config/Station1/Camera1Properties.json ^
    -b barcodes/barcodes1.txt ^
    -r %RUNNING_TIME%

start "Station 1 - Light Sensor" cmd %FLAG% "LightSensor.exe" ^
    -u file://./config/Station1/LightSensorProperties.json ^
    -r %RUNNING_TIME%

cd "%CWD%"