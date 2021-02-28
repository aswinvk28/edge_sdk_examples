#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml
RUNNING_TIME=${1:-60}

function clean_up {
    kill $CAMERA_PID
    kill $LIGHTSENSOR_PID
    exit
}

trap clean_up SIGHUP SIGINT SIGTERM

java -jar S4_CameraLauncher.jar \
    --thing file://./config/Station1/Camera1Properties.json \
    --barcodes ./barcodes1.txt \
    --running-time $RUNNING_TIME &
CAMERA_PID=$!

java -jar S4_LightSensorLauncher.jar \
    -t file://./config/Station1/LightSensorProperties.json \
    -r $RUNNING_TIME &
LIGHTSENSOR_PID=$!

wait $CAMERA_PID
wait $LIGHTSENSOR_PID
