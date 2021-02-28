#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml
RUNNING_TIME=${1:-60}

function clean_up {
    kill $CAMERA_PID
    kill $LIGHTSENSOR_PID
    exit
}

trap clean_up SIGHUP SIGINT SIGTERM

# change to parent directory of the script file
cd "$( cd "$(dirname "$0")" ; pwd -P )/.."

# check whether this scrpt is running in the background
case $(ps -o stat= -p $$) in
  *+*) in_background=0 ;;
  *) in_background=1 ;;
esac

./Camera \
    -u file://./config/Station1/Camera1Properties.json \
    -b barcodes/barcodes1.txt \
    -r $RUNNING_TIME &
CAMERA_PID=$!

# check whether this scrpt is running in the background
case $(ps -o stat= -p $$) in
  *+*) in_background=0 ;;
  *) in_background=1 ;;
esac

./LightSensor \
    -u file://./config/Station1/LightSensorProperties.json \
    -r $RUNNING_TIME &
LIGHTSENSOR_PID=$!

wait $CAMERA_PID $LIGHTSENSOR_PID
