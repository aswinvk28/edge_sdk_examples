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
This code is part of example scenario 3 'Derived Value Service' of the
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
import math
import random
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, ThingEx
from definitions.DistanceTagGroup_pb2 import Distance
from definitions.LocationTagGroup_pb2 import Location
from definitions.DistanceTagGroup_dr import Distance_register_with_datariver
from definitions.LocationTagGroup_dr import Location_register_with_datariver

MIN_SAMPLE_DELAY_MS = 1500

'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))

class GpsSensor(object):
    
    # Initializing
    def __init__(self, thing_properties_uri, truck_lat, truck_lng):
        self._thing_properties_uri = thing_properties_uri
        self._truck_lat = truck_lat
        self._truck_lng = truck_lng
        
        self._datariver = None
        self._thing = None
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('GPS Sensor started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('GPS Sensor stopped')
        
    def create_thing(self):
        #  Register the protobuf TagGroups
        Location_register_with_datariver(self._datariver)
        Distance_register_with_datariver(self._datariver)
 
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example.protobuf/GpsSensorThingClass.json'))
        self._datariver.add_thing_class_registry(tcr)
 
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._datariver.create_thing(tp))
    
    def write_sample(self, location_lat, location_lng, timestamp):
        sensor_data = Location()
        sensor_data.location.latitude = location_lat
        sensor_data.location.longitude = location_lng
        sensor_data.timestampUtc = timestamp
        
        # Write data to DataRiver
        self._thing.write('location', sensor_data)
        
    def run(self, running_time):
        random.seed()
        start_timestamp = time.time()
        elapsed_time = 0

        while True:
            # Simulate location change
            self._truck_lat += float(random.randrange(1000)) / 100000.0
            self._truck_lng += float(random.randrange(1000)) / 100000.0

            self.write_sample(self._truck_lat, self._truck_lng, int(time.time()))

            # Wait for random interval
            time.sleep(float(MIN_SAMPLE_DELAY_MS + random.randrange(3000)) / 1000.0)

            # Get elapsed time
            elapsed_time = time.time() - start_timestamp
            
            if elapsed_time >= running_time:
                break

def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK Edge SDK Example GPS Sensor')
    requiredNamed = parser.add_argument_group('required named arguments')
    requiredNamed.add_argument('--thing', type=str, nargs='?',
                               help='Thing properties URI',
                               default='file://./config/GpsSensor1Properties.json')
    requiredNamed.add_argument('--lat', type=float, nargs='?',
                               help='Truck start location latitude',
                               default=51.900000)
    requiredNamed.add_argument('--lng', type=float, nargs='?',
                               help='Truck start location longitude',
                               default=4.000000)
    requiredNamed.add_argument('--running-time', type=int, nargs='?',
                               help='Running Time (in seconds)',
                               default=60)
    args = parser.parse_args()
    
    return args.thing, args.lat, args.lng, args.running_time

def main():
    # Get command line parameters
    thing_properties_uri, truck_lat, truck_lng, running_time = get_command_line_parameters()
    
    try:
        gps_sensor = GpsSensor(thing_properties_uri, truck_lat, truck_lng)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with gps_sensor as gs:
            gs.run(running_time)
    except Exception as e:
        print('GpsSensor: An unexpected error occurred: ' + str(e))
    

if __name__ == '__main__':
    main()

