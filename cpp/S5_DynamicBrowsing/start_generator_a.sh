#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

RUNNING_TIME=${1:-60}

./generator_a \
    --fuel-sensor=file://./config/GeneratorA/FuelLevelSensorProperties.json \
    --speed-sensor=file://./config/GeneratorA/RotationalSpeedSensorProperties.json \
    --temp-sensor=file://./config/GeneratorA/TemperatureSensorProperties.json \
    --running-time=$RUNNING_TIME
