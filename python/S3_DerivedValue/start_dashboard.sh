#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

THING_PROPERTIES_URI=file://./config/DashboardProperties.json
RUNNING_TIME=${1:-60}

python dashboard.py $THING_PROPERTIES_URI $RUNNING_TIME
