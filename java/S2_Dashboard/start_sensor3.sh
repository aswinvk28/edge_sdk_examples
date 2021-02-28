#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

THING_PROPERTIES_URI=file://./config/TemperatureSensor3Properties.json
RUNNING_TIME=${1:-60}

java -jar S2_TemperatureSensorLauncher.jar -t $THING_PROPERTIES_URI -r $RUNNING_TIME
