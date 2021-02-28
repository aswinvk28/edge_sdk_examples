#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

RUNNING_TIME=${1:-60}

python distance_service.py \
    --thing=file://./config/DistanceServiceProperties.json \
    --lat=52.057313 \
    --lng=4.130987 \
    --running-time=$RUNNING_TIME
