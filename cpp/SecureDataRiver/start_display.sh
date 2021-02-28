#!/bin/bash
export ADLINK_DATARIVER_URI=file://./config/generated/display/display_datariver_config.xml

THING_PROPERTIES_URI=file://./config/TemperatureDisplayProperties.json
RUNNING_TIME=${1:-60}

./temperaturedisplay $THING_PROPERTIES_URI $RUNNING_TIME
