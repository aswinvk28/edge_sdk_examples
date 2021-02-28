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
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, FlowState, ThingEx, DataAvailableListenerEx
from adlinktech.datariver import Dispatcher, IotNvpDataAvailableListener, TimeoutError
from definitions.DistanceTagGroup_pb2 import Distance
from definitions.LocationTagGroup_pb2 import Location
from definitions.DistanceTagGroup_dr import Distance_register_with_datariver
from definitions.LocationTagGroup_dr import Location_register_with_datariver

'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))


class GpsSensorDataListener(DataAvailableListenerEx):
    
    def __init__(self, distance_service_thing):
        DataAvailableListenerEx.__init__(self)
        self._distance_service_thing = distance_service_thing

    def calculate_distance(self, truck_location_lat, truck_location_lng):
        return math.sqrt(math.pow(truck_location_lat - self._distance_service_thing.warehouse_lat, 2)
            + math.pow(truck_location_lng - self._distance_service_thing.warehouse_lng, 2))
    
    def notify_data_available(self, data):
        for location_message in data:
            my_location_flow_id = location_message.flow_id
            if location_message.flow_state == FlowState.ALIVE:
                # Get location data from sample
                location_data = location_message.get(Location)
                truck_location_lat = location_data.location.latitude
                truck_location_lng = location_data.location.longitude
                timestamp = location_data.timestampUtc

                # Calculate distance to the warehouse
                distance = self.calculate_distance(truck_location_lat, truck_location_lng)

                # This example uses a fixed multiplier for ETA. In a real-world
                # scenario this would be calculated based on e.g. real-time traffic information
                eta = distance * 5.12345

                self._distance_service_thing.write_distance(my_location_flow_id, distance, eta, timestamp)

class DistanceServiceThing(object):
    
    # Initializing
    def __init__(self, thing_properties_uri, warehouse_lat, warehouse_lng):
        self._thing_properties_uri = thing_properties_uri
        self._warehouse_lat = warehouse_lat
        self._warehouse_lng = warehouse_lng
        
        self._datariver = None
        self._thing = None

    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Distance Service started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Distance Service stopped')
        
    def create_thing(self):
        #  Register the protobuf TagGroups
        Location_register_with_datariver(self._datariver)
        Distance_register_with_datariver(self._datariver)
 
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example.protobuf/DistanceServiceThingClass.json'))
        self._datariver.add_thing_class_registry(tcr)
 
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._datariver.create_thing(tp))

    @property
    def warehouse_lat(self):
        return self._warehouse_lat

    @property
    def warehouse_lng(self):
        return self._warehouse_lng
    
    def write_distance(self, my_location_flow_id, distance, eta, timestamp):
        distance_data = Distance()
        distance_data.distance = distance
        distance_data.eta = eta
        distance_data.timestampUtc = timestamp
        
        # Write distance to DataRiver using flow ID from incoming location sample
        self._thing.write('distance', my_location_flow_id, distance_data)
        
    def run(self, running_time):
        # Use custom dispatcher for processing events
        dispatcher = Dispatcher()
        
        # Add listener for new GPS sensor Things using our custom dispatcher
        gps_data_received_listener = GpsSensorDataListener(self)
        self._thing.add_listener(gps_data_received_listener, dispatcher)

        # Process events with our dispatcher
        start = time.time()
        elapsed_seconds = 0
        while True:
            try:
                # block the call for 1000ms
                dispatcher.process_events(1000)
            except TimeoutError as e:
                # Ignore.
                pass
                
            elapsed_seconds = time.time() - start
            
            if elapsed_seconds >= running_time:
                break

        # Remove listener
        self._thing.remove_listener(gps_data_received_listener, dispatcher)

def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK Edge SDK Example Derived value service')
    requiredNamed = parser.add_argument_group('required named arguments')
    requiredNamed.add_argument('--thing', type=str, nargs='?',
                               help='Thing properties URI',
                               default='file://./config/DistanceServiceProperties.json')
    requiredNamed.add_argument('--lat', type=float, nargs='?',
                               help='Warehouse location latitude',
                               default=52.057313)
    requiredNamed.add_argument('--lng', type=float, nargs='?',
                               help='Warehouse location longitude',
                               default=4.130987)
    requiredNamed.add_argument('--running-time', type=int, nargs='?',
                               help='Running Time (in seconds)',
                               default=60)
    args = parser.parse_args()
    
    return args.thing, args.lat, args.lng, args.running_time

def main():
    # Get command line parameters
    thing_properties_uri, warehouse_lat, warehouse_lng, running_time = get_command_line_parameters()
    
    try:
        distance_service_thing = DistanceServiceThing(thing_properties_uri, warehouse_lat, warehouse_lng)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with distance_service_thing as dst:
            dst.run(running_time)
    except Exception as e:
        print('DistanceService: An unexpected error occurred: ' + str(e))
    

if __name__ == '__main__':
    main()

