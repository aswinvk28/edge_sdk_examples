#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml
RUNNING_TIME=${1:-60}

function clean_up {
    kill $CAMERA1_PID
    kill $CAMERA2_PID
    kill $LIGHTSENSOR_PID
    exit
}

trap clean_up SIGHUP SIGINT SIGTERM

# change to parent directory of the script file
cd "$( cd "$(dirname "$0")" ; pwd -P )/.."

./Camera \
    -u file://./config/Station2/Camera1Properties.json \
    -b barcodes/barcodes2.txt \
    -r $RUNNING_TIME &
CAMERA1_PID=$!

./Camera \
    -u file://./config/Station2/Camera2Properties.json \
    -b barcodes/barcodes2.txt \
    -r $RUNNING_TIME &
CAMERA2_PID=$!

./LightSensor \
    -u file://./config/Station2/LightSensorProperties.json \
    -r $RUNNING_TIME &
LIGHTSENSOR_PID=$!

wait $CAMERA1_PID $CAMERA2_PID $LIGHTSENSOR_PID
