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
This code is part of example scenario 2 'Connect a Dashboard' of the 
ADLINK Edge SDK. For a description of this scenario see the 
'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.

For instructions on running the example see the README
file in the Edge SDK installation directory.
'''

from __future__ import print_function
import argparse
import sys
import os
import time
import random
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, ThingEx
from definitions.TemperatureTagGroup_pb2 import Temperature
import definitions.TemperatureTagGroup_dr as tag_groups

SAMPLE_DELAY_MS = 5000

'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))

class TemperatureSensor(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._dr = None
        self._thing = None

    # Enter the runtime context related to the object
    def __enter__(self):
        self._dr = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Temperature Sensor started')
        
        return self

    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._dr is not None:
            self._dr.close()
        print('Temperature Sensor stopped')
        
    def create_thing(self):
        # Register the protobuf TagGroup
        tag_groups.Temperature_register_with_datariver(self._dr)
 
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example.protobuf/TemperatureSensorThingClass.json'))
        self._dr.add_thing_class_registry(tcr)
 
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._dr.create_thing(tp))
    
    def write_sample(self, temperature):
        sensor_data = Temperature()
        sensor_data.temperature = temperature
 
        self._thing.write('temperature', sensor_data)
        
    def run(self, running_time):
        random.seed()
        sample_count = (float(running_time) * 1000.0) / SAMPLE_DELAY_MS
        actual_temperature = 21.5
 
        while sample_count > 0:
            # Simulate temperature change
            actual_temperature += float(random.randrange(10) - 5) / 5.0
 
            self.write_sample(actual_temperature)
            
            time.sleep(SAMPLE_DELAY_MS/1000.0)
            sample_count = sample_count - 1.0

def main():
    # Get thing properties URI from command line parameter
    parser = argparse.ArgumentParser()    
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/TemperatureSensor1Properties.json')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
    args = parser.parse_args()
      
    # Create Thing
    try:
        temp_sensor = TemperatureSensor(args.thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with temp_sensor as sensor:
            sensor.run(args.running_time)
    except Exception as e:
        print('Sensor: An unexpected error occurred: {}'.format(e))
    

if __name__ == '__main__':
    main()








