#!/usr/bin/env python

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
This is a simple throughput application measuring obtainable throughput using the thingSDK
'''

from __future__ import print_function
import sys
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, FlowState, ThingEx
from definitions.PongTagGroup_pb2 import Pong as PongData
from definitions.PongTagGroup_dr import Pong_register_with_datariver
from definitions.PingTagGroup_pb2 import Ping
from definitions.PingTagGroup_dr import Ping_register_with_datariver

 
class Pong(object):
     
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._datariver = None
        self._thing = None

    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Pong started')
        
        return self

    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Pong stopped')

    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        Pong_register_with_datariver(self._datariver)
        Ping_register_with_datariver(self._datariver)

  
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri('file://definitions/ThingClass/com.adlinktech.example.protobuf/PongThingClass.json')
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        
        return ThingEx(self._datariver.create_thing(tp))
    
    def run(self):
        terminate = False
        print('Waiting for samples from ping to send back...')

        while not terminate:
            samples = self._thing.read('Ping')

            for sample in samples:
                if sample.flow_state == FlowState.PURGED:
                    print('Received termination request. Terminating.')
                    terminate = True
                    break
                else:
                    ping_data = sample.get(Ping) 
                    pong_data = PongData()
                    pong_data.payload = ping_data.payload
                    self._thing.write('Pong', pong_data)


def main():
    try:
        pong = Pong("file://./config/PongProperties.json")
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with pong as p:
            p.run()
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))
    except KeyboardInterrupt:
        print('Ctrl-c detected .. exiting')
        sys.exit(1)

if __name__ == '__main__':
    main()





