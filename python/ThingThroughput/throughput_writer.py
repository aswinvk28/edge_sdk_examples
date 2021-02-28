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

Updated the default running-time to 60 seconds as KeyboardInterrupt does not work with the OpenSplice on Windows.
'''

from __future__ import print_function
import sys
import six
import argparse
import time
from enum import Enum
from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties, FlowState
from adlinktech.datariver import IotValue, IotNvp, IotNvpSeq

# Convert python returned current time expressed in seconds(as floating point number) to milliseconds
# Note: On Windows, time.time() does not provide time with a better precision than 1 second
if six.PY2:
    current_time_milliseconds = lambda: int(round(time.time() * 1000))
else:
    current_time_milliseconds = lambda: int(round(time.perf_counter() * 1000))

'''
Writer mode:
    standard = use default write function
    outputHandler = use output handler for writing
    outputHandlerNotThreadSafe = use non-thread-safe write method for output handler
'''
class WriterMode(Enum):
    STANDARD = 1
    OUTPUT_HANDLER = 2
    OUTPUT_HANDLER_NOT_THREAD_SAFE = 3
 
class ThroughputWriter(object):
     
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._datariver = None
        self._thing = None
        self._sample = None

    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Throughput writer started')
        
        return self

    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Throughput writer stopped')

    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        tgr = JSonTagGroupRegistry()
        tgr.register_tag_groups_from_uri('file://definitions/TagGroup/com.adlinktech.example/ThroughputTagGroup.json')
        self._datariver.add_tag_group_registry(tgr) 
  
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri('file://definitions/ThingClass/com.adlinktech.example/ThroughputWriterThingClass.json')
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        
        return self._datariver.create_thing(tp)
    
    
    def setup_message(self, payload_size):
        self._sample = IotNvpSeq(2)
        
        self._sample[0].name = 'sequencenumber'
        self._sample[0].value.uint64 = 0
        
        self._sample[1].name = 'sequencedata'
        # Note: bytearray call is only necessary in Python 2.7
        self._sample[1].value.byte_seq = bytearray(payload_size * b'a')
    
    
    def wait_for_reader(self):
        try:
            # wait for throughputreader to appear by discovering its thing_id and thing_class
            print('Waiting for Throughput reader.. ')
            
            discovered_thing_registry = self._datariver.discovered_thing_registry
            reader_found = False
            
            while not reader_found:
                try:
                    thing = discovered_thing_registry.find_discovered_thing('*', 'ThroughputReader:com.adlinktech.example:v1.0')
                    reader_found = True
                except:
                    # Thing not available yet
                    pass
                # sleep for 100 milliseconds = 0.01 seconds
                time.sleep(0.01)
            
            print('Throughput reader found')
                
        except KeyboardInterrupt:
            print('Terminated')
            sys.exit(1)
    
    
    def write(self, burst_interval, burst_size, running_time, mode):
        try:
            burst_count = 0
            count = 0
            timed_out = False
            
            pub_start = current_time_milliseconds()
            burst_start = current_time_milliseconds()
            current_time = current_time_milliseconds()
            
            output_handler = self._thing.get_output_handler('ThroughputOutput')
            internal_sequencenumber_v = IotValue()
            
            if mode == WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE:
                output_handler.non_reentrant_flow_id = self._thing.context_id
                internal_nvp_seq = output_handler.setup_non_reentrant_nvp_seq(self._sample)
                
                internal_sequencenumber_nvp = internal_nvp_seq[0]
                internal_sequencenumber_v = internal_sequencenumber_nvp.value
            
            while not timed_out:
                # Write data until burst size has been reached
                if burst_count < burst_size:
                    burst_count += 1
                    
                    if mode == WriterMode.OUTPUT_HANDLER:
                        # Fill the nvp_seq with updated sequencenr
                        self._sample[0].value.uint64 = count
                        count += 1
                        
                        # Write the data using output handler
                        output_handler.write(self._sample)
                    elif mode == WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE:
                        # Fill the nvp_seq with updated sequencenr
                        internal_sequencenumber_v.uint64 = count
                        count += 1
                        
                        # Write the data using non-reentrant write on output handler
                        output_handler.write_non_reentrant()
                    else:
                        # Fill the nvp_seq with updated sequencenr
                        self._sample[0].value.uint64 = count
                        count += 1
                        
                        # Write the data
                        self._thing.write('ThroughputOutput', self._sample)
                elif burst_interval != 0:
                    # Sleep until burst interval has passed
                    current_time = current_time_milliseconds()
                    
                    delta_time = current_time - burst_start
                    if delta_time < burst_interval:
                        time.sleep(float(burst_interval - delta_time)/1000.0)
                    
                    burst_start = current_time_milliseconds()
                    burst_count = 0
                else:
                    burst_count = 0
                
                # Check of timeout
                if running_time != 0:
                    current_time = current_time_milliseconds()
                    if float(current_time - pub_start)/1000.0  > running_time:
                        timed_out = True
            
            print('Timed out: {} samples written'.format(count))
        except KeyboardInterrupt:
            print('Terminated: {} samples written'.format(count))
            sys.exit(1)
            
    
    
    def run(self, payload_size, burst_interval, burst_size, running_time, writer_mode):
        writer_mode_str = 'standard'
        if writer_mode == WriterMode.OUTPUT_HANDLER:
            writer_mode_str = 'outputHandler'
        elif writer_mode == WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE:
            writer_mode_str = 'outputHandlerNotThreadSafe'
        
        print('payloadSize: {} | burstInterval: {} | burstSize: {} | runningTime: {} | writer-mode: {}'.
              format(payload_size, burst_interval, burst_size, running_time, writer_mode_str))
        
        # Wait for reader to be discovered
        self.wait_for_reader()
        
        # Create the message that is sent
        self.setup_message(payload_size)
        
        # Write data
        self.write(burst_interval, burst_size, running_time, writer_mode)
        
        # Give middleware some time to finish writing samples
        time.sleep(2)
        
        return 0
    

def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK ThingSDK ThroughputWriter')
    parser.add_argument('-p', '--payload-size', type=int,
                        help='Payload size', default=4096)
    parser.add_argument('-b', '--burst-interval', type=int,
                        help='Burst interval in milliseconds',
                        default=0)
    parser.add_argument('-s', '--burst-size', type=int,
                        help='Burst size',
                        default=1)
    parser.add_argument('-r', '--running-time', type=int,
                        help='Running Time in seconds (0 is infinite)',
                        default=0)
    
    parser.add_argument('-w', '--writer-mode', type=str.lower,
                        choices=['standard','outputhandler','outputhandlernotthreadsafe'],
                        help='Writer mode (standard, outputHandler, outputHandlerNotThreadSafe)',
                        default='standard')
    
    args = parser.parse_args()
    
    writer_mode = WriterMode.STANDARD
    
    if args.writer_mode == 'outputhandler':
        writer_mode = WriterMode.OUTPUT_HANDLER
    elif args.writer_mode == 'outputhandlernotthreadsafe':
        writer_mode = WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE
    elif args.writer_mode == 'standard':
        writer_mode = WriterMode.STANDARD
    
    return args.payload_size, args.burst_interval, args.burst_size, args.running_time, writer_mode


def main():
    # Get command line parameters
    payload_size, burst_interval, burst_size, running_time, writer_mode = get_command_line_parameters()
    
    try:
        throughput_wr = ThroughputWriter("file://./config/ThroughputWriterProperties.json")
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with throughput_wr as thp_wr:
            thp_wr.run(payload_size, burst_interval, burst_size, running_time, writer_mode)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))

if __name__ == '__main__':
    main()





