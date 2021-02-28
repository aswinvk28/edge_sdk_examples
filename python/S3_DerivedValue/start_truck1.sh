#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

RUNNING_TIME=${1:-60}

python gps_sensor.py \
    --thing=file://./config/GpsSensor1Properties.json \
    --lat=51.900000 \
    --lng=4.000000 \
    --running-time=$RUNNING_TIME
