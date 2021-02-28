#!/bin/bash
# change to directory of the script file
cd "$( cd "$(dirname "$0")" ; pwd -P )"

export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

SENSOR_NUMBER=$1
THING_PROPERTIES_URI=file://./config/TemperatureSensor${SENSOR_NUMBER}Properties.json
RUNNING_TIME=${2:-60}

./temperaturesensor $THING_PROPERTIES_URI $RUNNING_TIME
