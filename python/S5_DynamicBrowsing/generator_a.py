#
#                           ADLINK Edge SDK
#  
#     This software and documentation are Copyright 2018 to 2020 ADLINK
#     Technology Limited, its affiliated companies and licensors. All rights
#     reserved.
#  
#     Licensed under the Apache License, Version 2.0 (the "License");
#     you may not use this file except in compliance with the License.
#     You may obtain a copy of the License at
#  
#         http://www.apache.org/licenses/LICENSE-2.0
#  
#     Unless required by applicable law or agreed to in writing, software
#     distributed under the License is distributed on an "AS IS" BASIS,
#     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#     See the License for the specific language governing permissions and
#     limitations under the License.
#

'''
This code is part of example scenario 5 'Dynamic Browsing' of the
ADLINK Edge SDK. For a description of this scenario see the
'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.

For instructions on running the example see the README
file in the Edge SDK installation directory.
'''

from __future__ import print_function
import argparse
import os
from threading import Thread
from sensors.fuel_level_sensor import FuelLevelSensor
from sensors.rotational_speed_sensor import RotationalSpeedSensor
from sensors.temperature_sensor import TemperatureSensor


def fuel_level_sensor_task(thing_properties_uri, running_time):
    try:
        fuel_sensor = FuelLevelSensor(thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with fuel_sensor as fs:
            fs.run(running_time)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))

def temperature_sensor_task(thing_properties_uri, running_time):
    try:
        temp_sensor = TemperatureSensor(thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with temp_sensor as ts:
            ts.run(running_time)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))

def speed_sensor_task(thing_properties_uri, running_time):
    try:
        speed_sensor = RotationalSpeedSensor(thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with speed_sensor as ss:
            ss.run(running_time)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))


def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK ThingSDK Example Generator A')
    parser.add_argument('-f', '--fuel-sensor', type=str,
                        help='Fuel Level Sensor Thing properties URI',
                        default='file://./config/GeneratorA/FuelLevelSensorProperties.json')
    parser.add_argument('-s', '--speed-sensor', type=str,
                        help='Rotational Speed Sensor Thing properties URI',
                        default='file://./config/GeneratorA/RotationalSpeedSensorProperties.json')
    parser.add_argument('-t', '--temp-sensor', type=str,
                        help='Temperature Sensor Thing properties URI',
                        default='file://./config/GeneratorA/TemperatureSensorProperties.json')
    parser.add_argument('-r', '--running-time', type=int,
                        help='Running Time in seconds',
                        default=60)
    
    args = parser.parse_args()
    
    return args.fuel_sensor, args.speed_sensor, args.temp_sensor, args.running_time


def main():
    # Get command line parameters
    (fuel_level_sensor_thing_properties_uri, 
     speed_sensor_thing_properties_uri, 
     temperature_sensor_thing_properties_uri, 
     running_time) = get_command_line_parameters()
    
    # Create threads for Sensors
    t_fuel_level_sensor = Thread(target=fuel_level_sensor_task,
                 args=(fuel_level_sensor_thing_properties_uri, running_time))
    t_fuel_level_sensor.start()
    
    t_temperature_sensor = Thread(target=temperature_sensor_task,
                 args=(temperature_sensor_thing_properties_uri, running_time))
    t_temperature_sensor.start()
    
    t_speed_sensor = Thread(target=speed_sensor_task,
                 args=(speed_sensor_thing_properties_uri, running_time))
    t_speed_sensor.start()
    
    t_fuel_level_sensor.join()
    t_temperature_sensor.join()
    t_speed_sensor.join()

if __name__ == '__main__':
    main()


