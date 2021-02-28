# NodeJS S1_ConnectSensor Example

This example connects a sensor for measuring temperature and a display that shows the actual temperature measured by
the sensor.

## Running this example

In order to run this application, follow these steps:

Step 1: Ensure prerequisites are installed. The package requires cmake. On Linux, use:

	sudo apt-get install cmake 

Step 2: Ensure the EdgeSDK has been initialized. On Linux, use:

	source <install-dir>/EdgeSDK/1.7.0/config_env_variables.com

The actual path and version number you supply will depend on the EdgeSDK version you are using, and where your installed it.

Step 3: Copy this directory to a location where you have read access. On Linux, use:

	cp -r $EDGE_SDK_HOME/examples/nodejs/S1_ConnectSensorJsontgc ~/nodejs_s1_connectSensor_tgc

Step 4: Install required packages and then the ThingAPI for NodeJS package into the copied project. One Linux, use:

	cd ~/nodejs_s1_connectSensor_tgc
	npm install
	npm install --save $EDGE_SDK_HOME/nodejs/adlinktech-datariver-1.7.0.tgz
	npm run jsontgc

Step 5: Run the example (for 15 seconds). On Linux:

	node lib/temperature_sensor.js -r 15    # from a command shell
	node lib/temperature_display.js -r 15   # from another command shell

Without parameters, example applications will run with a default running-time of 60 seconds.
