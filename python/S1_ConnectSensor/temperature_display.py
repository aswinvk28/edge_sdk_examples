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
This code is part of example scenario 1 'Connect a Sensor' of the
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

READ_SAMPLE_DELAY = 0.1

'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))

class TemperatureDisplay(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._dr = None
        self._thing = None
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._dr = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Temperature Display started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._dr is not None:
            self._dr.close()
        print('Temperature Display stopped')
        
    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        tgr = JSonTagGroupRegistry()
        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json'))
        self._dr.add_tag_group_registry(tgr) 
 
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example/TemperatureDisplayThingClass.json'))
        self._dr.add_thing_class_registry(tcr)
 
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return self._dr.create_thing(tp)
    
    def run(self, running_time) :
        start = time.time()
        elapsed_seconds = 0
        while True:
            # Read all data for input 'temperature'
            msgs = self._thing.read_iot_nvp('temperature', int((running_time - elapsed_seconds) * 1000))
            
            for msg in msgs:
                flow_state = msg.flow_state
                if flow_state == FlowState.ALIVE:
                    data_sample = msg.data
                    temperature = 0.0
                    
                    try:
                        for nvp in data_sample:
                            if nvp.name == 'temperature':
                                temperature = nvp.value.float32
                    except Exception as e:
                        print('An unexpected error occured while processing data-sample ' + str(e))
                        continue
                    
                    print('Sensor data received: {:5.2f}'.format(temperature))

            # Wait for some time before reading next samples
            time.sleep(READ_SAMPLE_DELAY)
  
            elapsed_seconds = time.time() - start
            
            if elapsed_seconds >= float(running_time):
                break

def main():
    # Get thing properties URI from command line parameter
    parser = argparse.ArgumentParser()    
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/TemperatureDisplayProperties.json')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
    args = parser.parse_args()
    
    try:
        temp_display = TemperatureDisplay(args.thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with temp_display as tdisplay:
            tdisplay.run(args.running_time)
    except Exception as e:
        print('Display: An unexpected error occurred: {}'.format(e))
    

if __name__ == '__main__':
    main()
