#!/bin/bash
# change to directory of the script file
cd "$( cd "$(dirname "$0")" ; pwd -P )"

export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

THING_PROPERTIES_URI=file://./config/TemperatureDashboardProperties.json
FILTER=floor1
RUNNING_TIME=${1:-60}

python -u temperature_dashboard.py $THING_PROPERTIES_URI $RUNNING_TIME
