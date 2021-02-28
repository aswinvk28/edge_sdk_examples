#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

THING_PROPERTIES_URI=file://./config/TemperatureDashboardProperties.json
FILTER=floor1
RUNNING_TIME=${1:-60}

java -jar S2B_TemperatureDashboardLauncher.jar -t $THING_PROPERTIES_URI -f $FILTER -r $RUNNING_TIME
