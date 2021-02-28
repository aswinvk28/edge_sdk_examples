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
import os
import time
import sys
from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties, FlowState, ThingEx, IotNvpSeq
from adlinktech.datariver import ThingDiscoveredListener, ThingLostListener
from definitions.TemperatureTagGroup_pb2 import Temperature
import definitions.TemperatureTagGroup_dr as tag_groups

if sys.platform.lower() == 'win32':
    os.system('color')

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
 
class TemperatureSensorDiscoveredListener(ThingDiscoveredListener):
    def notify_thing_discovered(self, thing):
        class_id = thing.class_id
        if class_id.name == 'TemperatureSensor':
            print(COLOR_GREEN + 'New temperature sensor discovered:  {} ({})'.format(thing.description, thing.id) + NO_COLOR)
        else:
            print(COLOR_GREY + 'New incompatible sensor type \'{}\' discovered ({})'.format(class_id.name, thing.id) + NO_COLOR)
 
class TemperatureSensorLostListener(ThingLostListener):
    def notify_thing_lost(self, thing):
        class_id = thing.class_id
        if class_id.name == 'TemperatureSensor':
            print(COLOR_MAGENTA + 'Temperature sensor stopped: {} ({})'.format(thing.description, thing.id) + NO_COLOR)
        else:
            print(COLOR_GREY + 'Other sensor stopped: \'{}\' ({})'.format(class_id.name, thing.id) + NO_COLOR)
 
class TemperatureDashboard(object):
     
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._dr = None
        self._thing = None

    # Enter the runtime context related to the object
    def __enter__(self):
        self._dr = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Temperature Dashboard started')
        
        return self

    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._dr is not None:
            self._dr.close()
        print('Temperature Dashboard stopped')

    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        tgr = JSonTagGroupRegistry()
        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json'))
        self._dr.add_tag_group_registry(tgr) 
        
        # Register Gen2 sensor tag groups
        tag_groups.Temperature_register_with_datariver(self._dr)
  
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json'))
        self._dr.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._dr.create_thing(tp))
     
#   def run(self, floor, running_time) :
    def run(self, running_time) :
        start = time.time()
        elapsed_seconds = 0
         
        # Add listener for new Things
        temperature_sensor_discovered_listener = TemperatureSensorDiscoveredListener()
        self._dr.add_listener(temperature_sensor_discovered_listener)
 
        # Add listener for lost Things
        temperature_sensor_lost_listener = TemperatureSensorLostListener()
        self._dr.add_listener(temperature_sensor_lost_listener)
         
        while True:
            # Read data using selector
            msgs = self._thing.read('temperature', int((running_time - elapsed_seconds) * 1000))
            
            # Process samples
            for msg in msgs:
                flow_state = msg.flow_state
                if flow_state == FlowState.ALIVE:

                    temperature = 0.0
                    sensor_generation = ""
                    
                    if msg.is_compatible(Temperature):
                        sensor_data = msg.get(Temperature)
                        if(sensor_data.humidity == 0.0):
                            sensor_generation = "Gen2"
                        else:
                            sensor_generation = "Gen3"
                    else: 
                        sensor_generation = "Gen1"
                        data_sample = msg.get(IotNvpSeq)
                        try:
                            # Get temperature value from sample
                            for nvp in data_sample:
                                if nvp.name == 'temperature':
                                    temperature = nvp.value.float32
                        except Exception as e:
                            print('An unexpected error occured while processing data-sample: ' + str(e))
                            continue
                     
                    # Show output
                    if(sensor_generation == "Gen1"):
                        print('Temperature data received for flow {}({}): {:5.1f}'.format(str(msg.flow_id), sensor_generation, temperature))
                    elif(sensor_generation == "Gen2"):
                        print('Temperature data received for flow {}({}): {:5.1f}'.format(str(msg.flow_id), sensor_generation, sensor_data.temperature))
                    else:
                        print('Temperature data received for flow {}({}): {:5.1f}, {:5.1f}%'.format(str(msg.flow_id), sensor_generation, sensor_data.temperature, sensor_data.humidity))
 
            elapsed_seconds = time.time() - start
             
            if elapsed_seconds >= float(running_time):
                break
             
        # Remove listeners
        self._dr.remove_listener(temperature_sensor_lost_listener)
        self._dr.remove_listener(temperature_sensor_discovered_listener)

def main():
    #Get thing properties URI and running time from command line parameter
    parser = argparse.ArgumentParser()
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/TemperatureDashboardProperties.json')    
#   parser.add_argument('filter', type=str, nargs='?',
#                       help='Filter for reading filtered sample',
#                       default='floor1')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
   
    args = parser.parse_args()

    try:
        temp_dashboard = TemperatureDashboard(args.thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with temp_dashboard as dashboard:
#           dashboard.run(args.filter, args.running_time)
            dashboard.run(args.running_time)
    except Exception as e:
        print('Dashboard: An unexpected error occurred: {}'.format(e))
    

if __name__ == '__main__':
    main()
