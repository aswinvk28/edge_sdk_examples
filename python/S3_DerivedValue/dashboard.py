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
import os
import time
import sys
from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties, FlowState
from adlinktech.datariver import ThingDiscoveredListener, ThingLostListener

READ_DELAY = 500

if sys.platform.lower() == 'win32':
    os.system('color')
    
CONSOLE_LINE_UP = '\033[F'
NO_COLOR = '\33[0m'
COLOR_GREEN = '\33[32m'
COLOR_MAGENTA = '\33[35m'
COLOR_GREY = '\33[90m'
 
'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))

class TruckDataValue(object):
    lat = float('-inf')
    lng = float('-inf')
    location_update_time = 0
 
    distance = float('-inf')
    eta = float('-inf')
    position_update_time = 0
 
class Dashboard(object):
     
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._datariver = None
        self._thing = None
        
        self._truck_data = dict()
        self._line_count = 0
        self._distance_unit = ''
        self._eta_unit = ''

    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Dashboard started')
        
        return self

    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Dashboard stopped')
         
    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        tgr = JSonTagGroupRegistry()
        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/LocationTagGroup.json'))
        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/DistanceTagGroup.json'))
        self._datariver.add_tag_group_registry(tgr) 
  
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example/LocationDashboardThingClass.json'))
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return self._datariver.create_thing(tp)
    

    def display_header(self):        
        print('{:<20}'.format('Truck Context') +
              '{:<15}'.format('Latitude') +
              '{:<15}'.format('Longitude') +
              '{:<25}'.format('Distance (' + self._distance_unit + ')') +
              '{:<20}'.format('ETA (' + self._eta_unit + ')')
              )
    
    def format_number(self, value, precision):
        result = ''
    
        if value != float('-inf'):
            result = str(round(value, precision))
        else:
            result = '-'
    
        return result
    
    def format_time(self, time_t):
        if time_t != 0: 
            local_time = time.localtime(time_t)
            lt = '{}:{}:{}'.format(local_time.tm_hour, local_time.tm_min, local_time.tm_sec)
            return lt
        else:
            return '-'
    
    def display_status(self):
        # Reset cursor position for previous console update
        for i in range((self._line_count * 2) + 1):
            sys.stdout.write(CONSOLE_LINE_UP)

        # Add header row for table
        self.display_header()

        # Write new data to console
        self._line_count = 0;
        for key in self._truck_data.keys():
            value = self._truck_data.get(key)

            print(COLOR_GREEN + '{:<20}'.format(key) + NO_COLOR +
                  COLOR_MAGENTA + '{:<15}'.format(self.format_number(value.lat, 6)) + NO_COLOR +
                  COLOR_MAGENTA + '{:<15}'.format(self.format_number(value.lng, 6)) + NO_COLOR +
                  COLOR_GREEN + '{:<25}'.format(self.format_number(value.distance, 3)) + NO_COLOR +
                  COLOR_GREEN + '{:<20}'.format(self.format_number(value.eta, 1)) + NO_COLOR)
            print('{:<20}'.format(' ') +
                  COLOR_GREY + '{:<30}'.format('  updated: ' + self.format_time(value.location_update_time)) + NO_COLOR +
                  COLOR_GREY + '{:<45}'.format('  updated: ' + self.format_time(value.position_update_time)) + NO_COLOR)

            self._line_count += 1
    
    def get_location_from_sample(self, sample):
        lat = 0.0
        lng = 0.0
        timestamp = 0
        data = sample.data
        for nvp in data:
            if nvp.name == 'location':
                for location_nvp in nvp.value.nvp_seq:
                    if location_nvp.name == 'latitude':
                        lat = location_nvp.value.float32
                    elif location_nvp.name == 'longitude':
                        lng = location_nvp.value.float32
            elif nvp.name == 'timestampUtc':
                timestamp = nvp.value.uint64
        return lat, lng, timestamp

    def get_distance_from_sample(self, sample):
        distance = 0.0
        eta = 0.0
        timestamp = 0
        data = sample.data
        for nvp in data:
            if nvp.name == 'distance':
                distance = nvp.value.float64
            if nvp.name == 'eta':
                eta = nvp.value.float32
            if nvp.name == 'timestampUtc':
                timestamp = nvp.value.uint64
        return distance, eta, timestamp
    
    
    def process_location_sample(self, data_sample):
        try:
            if data_sample.flow_state == FlowState.ALIVE:
                lat, lng, timestamp = self.get_location_from_sample(data_sample)

                key = str(data_sample.flow_id)
                if key not in self._truck_data:
                    self._truck_data[key] = TruckDataValue()
                self._truck_data[key].lat = lat
                self._truck_data[key].lng = lng
                self._truck_data[key].location_update_time = timestamp
        except Exception as e:
            print('Dashboard: An unexpected error occured while processing data-sample: ' + str(e))

    def process_distance_sample(self, data_sample):
        try:
            if data_sample.flow_state == FlowState.ALIVE:
                distance, eta, timestamp = self.get_distance_from_sample(data_sample)

                key = str(data_sample.flow_id)
                if key not in self._truck_data:
                    self._truck_data[key] = TruckDataValue()
                self._truck_data[key].distance = distance
                self._truck_data[key].eta = eta
                self._truck_data[key].position_update_time = timestamp
        except Exception as e:
            print('Dashboard: An unexpected error occured while processing data-sample: ' + str(e))


    def get_tag_unit_descriptions(self):
        tgr = self._datariver.discovered_tag_group_registry
        distance_tag_group = tgr.find_tag_group('Distance:com.adlinktech.example:v1.0')
        typedefs = distance_tag_group.type_definitions
        for type_d in typedefs:
            for tag in type_d.tags:
                if tag.name == 'distance':
                    self._distance_unit = tag.unit
                if tag.name == 'eta':
                    self._eta_unit = tag.unit

     
    def run(self, running_time) :
        start_timestamp = time.time()
        elapsed_time = 0
         
        while True:
            # Retrieve and process location samples
            location_samples = self._thing.read_iot_nvp('location', 0)
            for sample in location_samples:
                self.process_location_sample(sample)
                
            # Retrieve and process distance samples
            distance_samples = self._thing.read_iot_nvp('distance', 0)
            for sample in distance_samples:
                self.process_distance_sample(sample)
            
            if (self._distance_unit == '') or (self._eta_unit == ''):
                self.get_tag_unit_descriptions()
                
            # Update console output
            self.display_status()
 
            # Sleep before next update
            time.sleep(float(READ_DELAY) / 1000.0)

            # Get elapsed time
            elapsed_time = time.time() - start_timestamp
             
            if elapsed_time >= float(running_time):
                break


def main():
    # Get thing properties URI from command line parameter
    parser = argparse.ArgumentParser()    
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/DashboardProperties.json')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
    args = parser.parse_args()
    
    try:
        dashboard = Dashboard(args.thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with dashboard as dboard:
            dboard.run(args.running_time)
    except Exception as e:
        print('Dashboard: An unexpected error occurred: ' + str(e))
    

if __name__ == '__main__':
    main()
