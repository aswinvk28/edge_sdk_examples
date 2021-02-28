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
This code is part of example scenario 4 'Gateway Service' of the
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
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, FlowState, ThingEx
from adlinktech.datariver import ThingDiscoveredListener
from definitions.CameraStateTagGroup_pb2 import CameraState
from definitions.IlluminanceAlarmTagGroup_pb2 import IlluminanceAlarm
from definitions.IlluminanceTagGroup_pb2 import Illuminance
from definitions.ObservationTagGroup_pb2 import Observation

from definitions.CameraStateTagGroup_dr import CameraState_register_with_datariver
from definitions.IlluminanceAlarmTagGroup_dr import IlluminanceAlarm_register_with_datariver
from definitions.IlluminanceTagGroup_dr import Illuminance_register_with_datariver
from definitions.ObservationTagGroup_dr import Observation_register_with_datariver

READ_DELAY = 500

if sys.platform.lower() == 'win32':
    os.system('color')

NO_COLOR = '\33[0m'
COLOR_RED = '\33[31m'
COLOR_GREEN = '\33[32m'
COLOR_MAGENTA = '\33[35m'
COLOR_GREY = '\33[90m'
CLEAR_SCREEN = '\033[2J'
MOVE_CURSOR_TO_ORIGIN = '\033[0;0H'

GATEWAY_INITIAL_DELAY = 2000
READ_DELAY = 10
DISPLAY_REFRESH_RATE = 10.0
TOTAL_HEADER_LINES = 2
TOTAL_FOOTER_MESSAGE_LINES = 1


# Convert python returned current time expressed in seconds(as floating point number) to milliseconds
current_milli_time = lambda: int(round(time.time() * 1000))

def truncate(in_str, width):
    if len(in_str) > (width - 3):
        return in_str[0, width - 3] + '...'

    return in_str

 
'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))

class DataFlowValue(object):
    sample_count = 0
    flow_state = FlowState.ALIVE

thing_context = {}

class DataFlowKey(object):
        
    def __init__(self, data_sample):
        self._tag_group = data_sample.tag_group
        self._source_thing_class_id = data_sample.source_class
        self._source_thing_id = data_sample.source_id
        self._flow_id = data_sample.flow_id
    
    def __hash__(self):
        return hash((self.tag_group_name, self.source_thing_class_id, self.source_thing_id, self.flow_id))
    
    def __eq__(self, other):
        return ((self.tag_group_name, self.source_thing_class_id, self.source_thing_id, self.flow_id) 
                == (other.tag_group_name, other.source_thing_class_id, other.source_thing_id, other.flow_id))

    @property
    def source_thing_class_id(self):
        return self._source_thing_class_id

    @property
    def source_thing_id(self):
        return self._source_thing_id

    @property
    def source_thing_context(self):
        if self._source_thing_id in thing_context:
            context = thing_context[self._source_thing_id]
        else:
            context = '<unknown>'

        return context

    @property
    def tag_group_name(self):
        return self._tag_group.name

    @property
    def tag_group_qos(self):
        return self._tag_group.qos_profile

    @property
    def flow_id(self):
        return self._flow_id

class NewThingDiscoveredListener(ThingDiscoveredListener):
    
    def notify_thing_discovered(self, thing):
        thing_context[thing.id] = thing.context_id


class GatewayService(object):
     
    # Initializing
    def __init__(self, thing_properties_uri, screen_height_in_lines):
        self._thing_properties_uri = thing_properties_uri
        self._datariver = None
        self._thing = None
        
        self._sample_count = dict()
        self._screen_height_in_lines = screen_height_in_lines
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Gateway Service started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Gateway Service stopped')
         
    def create_thing(self):
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example.protobuf/GatewayServiceThingClass.json'))
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._datariver.create_thing(tp))
    

    def display_status(self):
        # Move cursor position to the origin (0,0) of the console
        sys.stdout.write(MOVE_CURSOR_TO_ORIGIN)

        # Add header row for table
        self.display_header()

        # Write new data to console
        line_count = 0
        for key in self._sample_count.keys():
            value = self._sample_count.get(key)

            # Set grey color for purged flows
            alive = value.flow_state == FlowState.ALIVE
            COLOR1 = NO_COLOR
            COLOR2 = NO_COLOR
            flow_state = ' <purged>'
            if alive:
                COLOR1 = COLOR_GREEN
                COLOR2 = COLOR_MAGENTA
                flow_state = ''
            
            print(COLOR1 + '{:<32}'.format(truncate(key.source_thing_context + flow_state, 32)) + NO_COLOR +
                  COLOR2 + '{:<30}'.format(truncate(key.flow_id, 30)) + NO_COLOR +
                  COLOR2 + '{:<20}'.format(truncate(key.tag_group_name, 20)) + NO_COLOR +
                  COLOR_GREY + '{:<12}'.format(truncate(key.tag_group_qos, 12)) + NO_COLOR +
                  COLOR1 + '{:>8}'.format(value.sample_count) + NO_COLOR)
            
            line_count += 1
            
            if (line_count < len(self._sample_count) and 
                    line_count >= (self._screen_height_in_lines - TOTAL_HEADER_LINES - TOTAL_FOOTER_MESSAGE_LINES - 1)):
                print(
                    '... {} more lines available. '.format(len(self._sample_count) - line_count) +
                    'Set terminal height to {}. '.format(len(self._sample_count) + TOTAL_HEADER_LINES + TOTAL_FOOTER_MESSAGE_LINES + 1) +
                    'See the README file for more instructions.'
                    )
                break
            

    def display_header(self):
        print(
              '{:<32}'.format('Thing\'s ContextId') +
              '{:<30}'.format('Flow Id') +
              '{:<20}'.format('TagGroup Name') +
              '{:<12}'.format('QoS') +
              '{:>8}'.format('Samples') +
              os.linesep
              )

    def read_things_from_registry(self):
        discovered_things_registry = self._datariver.discovered_thing_registry
        things = discovered_things_registry.discovered_things
        for thing in things:
            thing_context[thing.id] = thing.context_id

    def run(self, running_time):
        start_timestamp = current_milli_time()
        display_updated_timestamp = start_timestamp
        elapsed_time = 0

        # Add listener for discovering new Things
        new_thing_discovered_listener = NewThingDiscoveredListener()
        self._datariver.add_listener(new_thing_discovered_listener)

        # Get meta-data (contextId) for Things in discovered things registry
        self.read_things_from_registry()
        
        # Clear console screen before printing samples
        sys.stdout.write(CLEAR_SCREEN)

        while True:
            # Read data
            msgs = self._thing.read_next('dynamicInput', int((running_time * 1000) - elapsed_time))

            # Loop received samples and update counters
            for msg in msgs:
                flow_state = msg.flow_state

                key = DataFlowKey(msg)                        
                
                if key not in self._sample_count:
                    self._sample_count[key] = DataFlowValue()

                # Store state in value for this flow
                self._sample_count[key].flow_state = flow_state

                # In case flow is alive or if flow is purged but sample
                # contains data: increase sample count
                sample_contains_data = (flow_state == FlowState.ALIVE) or msg.has_data

                if sample_contains_data:
                    self._sample_count[key].sample_count += 1

                    # In a real-world use-case you would have additional processing
                    # of the data received by msg.getData()

            # Update console output
            self.display_status()

            # Sleep before reading next samples
            time.sleep(float(READ_DELAY) / 1000.0)

            # Get elapsed time
            now = current_milli_time()
            elapsed_time = now - start_timestamp
            
            if (elapsed_time / 1000) >= running_time:
                break

        # Remove listener
        self._datariver.remove_listener(new_thing_discovered_listener)

def _terminal_lines():
    import shutil
    return shutil.get_terminal_size(fallback=(80,1000)).lines

def main():
    # Get thing properties URI from command line parameter
    parser = argparse.ArgumentParser()
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/GatewayServiceProperties.json')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
    args = parser.parse_args()
    
    try:
        with GatewayService(args.thing_properties_uri, _terminal_lines()) as gws:
            gws.run(args.running_time)
    except Exception as e:
        print('GatewayService: An unexpected error occurred: {}'.format(e)) 

if __name__ == '__main__':
    main()

