#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

RUNNING_TIME=${1:-60}

java -jar S3_GpsSensorLauncher.jar \
    --thing file://./config/GpsSensor2Properties.json \
    --lat 51.700000 \
    --lng 4.100000 \
    --running-time $RUNNING_TIME
