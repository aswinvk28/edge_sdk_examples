#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

THING_PROPERTIES_URI=file://./config/TemperatureSensorProperties.json
RUNNING_TIME=${1:-60}

# change to parent directory of the script file
cd "$( cd "$(dirname "$0")" ; pwd -P )/.."

node lib/temperature_sensor.js -t $THING_PROPERTIES_URI -r $RUNNING_TIME
