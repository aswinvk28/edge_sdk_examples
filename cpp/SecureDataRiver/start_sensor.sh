#!/bin/bash
export ADLINK_DATARIVER_URI=file://./config/generated/sensor/sensor_datariver_config.xml

THING_PROPERTIES_URI=file://./config/TemperatureSensorProperties.json
RUNNING_TIME=${1:-60}

./temperaturesensor $THING_PROPERTIES_URI $RUNNING_TIME
