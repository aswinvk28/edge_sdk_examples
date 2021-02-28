# About the Gen1 Application

The Gen1 application is a minimally changed copy of the S2A_Dashboard example. Modifications were made for the following reasons:

* simply filtering so that values from all sensors are displayed. The original S2A display data for on 'floor1' (sensors 1 & 2) and ignored values from 'floor2' (sensor 3)
* simplified the launch scripts so that sensors from different generations could be easily launched in combination

# Changes made to S2A_Dashboard

## File: config/TemperatureDashboardProperties.json

MOD1. Modified the flow ID filter to match all floors:

        {
          "id": "8838c6bf-43dd-4534-9be6-47f190537b23",
          "classId": "TemperatureDashboard:com.adlinktech.example:v1.0",
          "contextId": "floor1.dashboard1",
          "description": "Edge SDK example dashboard Thing that shows temperature data from sensors on floor 1",
          "inputSettings" : [
             {
                "name" : "temperature",
                "filters" : {
                  "flowIdFilters": ["floor?.*"]
                }
             }
          ]
        }

## Files: start_sensor.sh, start_sensor.bat

MOD2. Combined three variants (start_sensor1, start_sensor2, start_sensor3) into a single version with a sensor number argument.

MOD3. Modified scripts to calculate their directory so they can be run from any directory and always behave the same.
