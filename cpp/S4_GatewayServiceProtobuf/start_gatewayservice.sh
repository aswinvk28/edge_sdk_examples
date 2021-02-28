#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

THING_PROPERTIES_URI=file://./config/GatewayServiceProperties.json
RUNNING_TIME=${1:-60}

# Get terminal height passed from unitttest
if [ "$#" -ge 2 ]
then
    TERMINAL_HEIGHT_IN_LINES=$2
elif [ ! -z "${LINES}" ]
then
    TERMINAL_HEIGHT_IN_LINES=${LINES}
else
    TERMINAL_HEIGHT_IN_LINES=$(tput lines)
fi

LINES=$TERMINAL_HEIGHT_IN_LINES ./gatewayservice $THING_PROPERTIES_URI $RUNNING_TIME

