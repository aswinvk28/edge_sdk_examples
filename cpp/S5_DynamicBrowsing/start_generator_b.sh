#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

RUNNING_TIME=${1:-60}

./generator_b \
    --speed-sensor=file://./config/GeneratorB/RotationalSpeedSensorProperties.json \
    --temp-sensor=file://./config/GeneratorB/TemperatureSensorProperties.json \
    --running-time=$RUNNING_TIME
