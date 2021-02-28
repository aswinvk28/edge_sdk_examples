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
import argparse
import sys
import six
import time
import os
from enum import Enum
from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties, FlowState
from adlinktech.datariver import IotValue, IotNvp, IotNvpSeq, BLOCKING_TIME_INFINITE

US_IN_ONE_SEC = 1000000
BYTES_PER_SEC_TO_MEGABITS_PER_SEC = 125000

# Convert python returned current time expressed in seconds(as floating point number) to microseconds
# Note: On Windows, time.time() does not provide time with a better precision than 1 second
if six.PY2:
    current_time_microseconds = lambda: int(round(time.time() * 1000000))
else:
    current_time_microseconds = lambda: int(round(time.perf_counter() * 1000000))

sample_count = 0
current_time = current_time_microseconds()
start_time = current_time_microseconds()
bytes_received = 0
out_of_order_count = 0
batch_count = 0
batch_max_size = 0

stop = False

def show_summary():
    # Output totals and averages
    global sample_count, current_time, start_time, bytes_received, out_of_order_count, batch_count, batch_max_size
    if batch_count > 0:
        delta_time = (current_time - start_time) / US_IN_ONE_SEC
        if delta_time > 0:
            print(os.linesep + 'Total received: {} samples, {} bytes'.format(sample_count, bytes_received) + os.linesep +
                  'Out of order: {} samples'.format(out_of_order_count) + os.linesep +
                  'Average transfer rate: {:.0f} samples/s, {:.2f} Mbit/s'
                  .format(float(sample_count) / delta_time, (float(bytes_received) / BYTES_PER_SEC_TO_MEGABITS_PER_SEC) / delta_time)
                   + os.linesep +
                   'Average sample-count per batch: {}, maximum batch-size: {}'.format(int(sample_count / batch_count), batch_max_size))
        else:
            print(os.linesep + 'Total received: {} samples, {} bytes'.format(sample_count, bytes_received) + os.linesep +
                  'Out of order: {} samples'.format(out_of_order_count) + os.linesep +
                  'Average transfer rate: {:.0f} samples/s, {:.2f} Mbit/s'
                  .format(0.0, 0.0)
                   + os.linesep +
                   'Average sample-count per batch: {}, maximum batch-size: {}'.format(int(sample_count / batch_count), batch_max_size))


class ThroughputReader(object):
     
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._datariver = None
        self._thing = None

    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Throughput reader started')
        
        return self

    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Throughput reader stopped')

    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        tgr = JSonTagGroupRegistry()
        tgr.register_tag_groups_from_uri('file://definitions/TagGroup/com.adlinktech.example/ThroughputTagGroup.json')
        self._datariver.add_tag_group_registry(tgr) 
  
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri('file://definitions/ThingClass/com.adlinktech.example/ThroughputReaderThingClass.json')
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        
        return self._datariver.create_thing(tp)
    
    
    
    def run(self, polling_delay, running_time):
        prev_count = 0
        prev_received = 0
        samples_in_batch = 0
        payload_size = 0
        delta_received = None
        delta_time = None
        first_time = True
        prev_time = current_time_microseconds()
        received_sequence_number = None
        last_received_sequence_number = None
        received_data = None
        seq_nr_found = False
        first_sample = True
        
        print('Waiting for samples...')
        
        # Loop through until the runningTime has been reached (0 = infinite)
        # each cycle is 1 second
        cycles = 0
        
        global stop, sample_count, current_time, start_time, bytes_received, out_of_order_count, batch_count, batch_max_size
        
        while not stop and (running_time == 0 or cycles < running_time):
            if polling_delay > 0:
                time.sleep(float(polling_delay)/1000.0)
            
            # Take samples and iterate through them
            samples = self._thing.read_iot_nvp('ThroughputInput', BLOCKING_TIME_INFINITE)
            
            # New batch
            batch_count += 1
            samples_in_batch = sample_count
            
            for sample in samples:
                if sample.flow_state == FlowState.ALIVE:
                    data = sample.data
                    
                    # find the message, stored in the name-value-pair with name 'name':
                    seq_nr_found = False
                    for nvp in data:
                        if nvp.name == 'sequencenumber':
                            received_sequence_number = nvp.value.uint64
                            seq_nr_found = True
                        elif nvp.name == 'sequencedata':
                            # Add the sample payload size to the total received
                            received_data = nvp.value.byte_seq
                            payload_size = received_data.size()
                            bytes_received += payload_size + 8 # add 8 bytes for sequence number field
                    
                    if seq_nr_found:
                        # Increase sample count
                        sample_count += 1
                        
                        if first_sample:
                            last_received_sequence_number = received_sequence_number - 1
                            first_sample = False
                        
                        # Check that the sample is the next one expected
                        if received_sequence_number != last_received_sequence_number + 1:
                            out_of_order_count += (received_sequence_number - (last_received_sequence_number + 1))
                        
                        # Keep track of last received seq nr
                        last_received_sequence_number = received_sequence_number
                else:
                    print('Writer flow purged, stop reader')
                    stop = True
            
            if not stop:
                current_time = current_time_microseconds()
                if (current_time - prev_time) > US_IN_ONE_SEC:
                    # If not the first iteration
                    if not first_time:
                        # Calculate the samples and bytes received and the time passed since the  last iteration and output
                        delta_received = bytes_received - prev_received
                        delta_time = (current_time - prev_time) / US_IN_ONE_SEC
                        
                        print('Payload size: {} | '.format(payload_size) +
                              'Total: {:>9} samples, '.format(sample_count) +
                              '{:>12} bytes | '.format(bytes_received) +
                              'Out of order: {:>6} samples | '.format(out_of_order_count) +
                              'Transfer rate: {:>7.0f} samples/s, '.format(float(sample_count - prev_count) / delta_time) +
                              '{:>9.2f} Mbit/s'.format((float(delta_received) / BYTES_PER_SEC_TO_MEGABITS_PER_SEC) / delta_time))
                        
                        cycles += 1
                    else:
                        # Set the start time if it is the first iteration
                        first_time = False
                        start_time = current_time
                    
                    # Update the previous values for next iteration
                    prev_received = bytes_received
                    prev_count = sample_count
                    prev_time = current_time
                
                # Update max samples per batch
                samples_in_batch = sample_count - samples_in_batch
                if samples_in_batch > batch_max_size:
                    batch_max_size = samples_in_batch
                
        show_summary()
        
        return 0
    
def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK ThingSDK ThroughputReader')
    parser.add_argument('-p', '--polling-delay', type=int,
                        help='Polling delay (milliseconds)', default=0)
    parser.add_argument('-r', '--running-time', type=int,
                        help='Running time (seconds, 0 = infinite)',
                        default=0)
    
    args = parser.parse_args()
    
    return args.polling_delay, args.running_time

def main():
    # Get command line parameters
    polling_delay, running_time = get_command_line_parameters()
    
    try:
        throughput_rd = ThroughputReader('file://./config/ThroughputReaderProperties.json')
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with throughput_rd as thp_rd:
            thp_rd.run(polling_delay, running_time)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))
    except KeyboardInterrupt:
        show_summary()
        sys.exit(1)

if __name__ == '__main__':
    main()


