#!/bin/bash
export ADLINK_DATARIVER_URI=file://$EDGE_SDK_HOME/etc/config/default_datariver_config_v1.7.xml

RUNNING_TIME=${1:-60}

# change to parent directory of the script file
cd "$( cd "$(dirname "$0")" ; pwd -P )/.."

# check whether this scrpt is running in the background
case $(ps -o stat= -p $$) in
  *+*) in_background=0 ;;
  *) in_background=1 ;;
esac

if [ ${in_background} -eq 1 ]; then
	# .Net applications and bash hang when background child processes
	# end. Work around this by running the .Net app in the background
	./GPSSensor \
    -u file://./config/GpsSensor1Properties.json \
    -l 51.900000 \
    -o 4.000000 \
    -r $RUNNING_TIME &
    wait
else
	./GPSSensor \
    -u file://./config/GpsSensor1Properties.json \
    -l 51.900000 \
    -o 4.000000 \
    -r $RUNNING_TIME
fi

