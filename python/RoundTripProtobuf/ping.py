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
This is a simple throughput application measuring obtainable throughput using the thingSDK.

Updated the default running-time to 60 seconds as KeyboardInterrupt does not work with the OpenSplice on Windows.
'''

from __future__ import print_function
import argparse
import os
import time
import sys
import six
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, ThingEx
from definitions.PingTagGroup_pb2 import Ping as PingData
from definitions.PingTagGroup_dr import Ping_register_with_datariver
from definitions.PongTagGroup_pb2 import Pong
from definitions.PongTagGroup_dr import Pong_register_with_datariver


if sys.platform.lower() == 'win32':
    os.system('color')

NO_COLOR = '\33[0m'
COLOR_GREEN = '\33[32m'
COLOR_LMAGENTA = '\33[95m'

US_IN_ONE_SEC = 1000000


# Convert python returned current time expressed in seconds(as floating point number) to microseconds
# Note: On Windows, time.time() does not provide time with a better precision than 1 second
if six.PY2:
    current_time_microseconds = lambda: int(round(time.time() * 1000000))
else:
    current_time_microseconds = lambda: int(round(time.perf_counter() * 1000000))

class ExampleTimeStats(object):
    def __init__(self):
        self.values = []
        self.average = 0
        self.min = 0
        self.max = 0
    
    def __hash__(self):
        return hash((self.values, self.average, self.min, self.max))
    
    def __iadd__(self, microseconds):
        '''
            Implementing python += operator
        '''
        self.average = ((len(self.values) * self.average) + microseconds) / (len(self.values) + 1)
        self.values.append(microseconds)
        if (self.min == 0 or microseconds < self.min):
            self.min = microseconds
        if (microseconds > self.max):
            self.max = microseconds
        return self
    
    def example_reset_time_stats(self):
        self.values = []
        self.average = 0
        self.min = 0
        self.max = 0
        return self
    
    def example_get_median_from_time_stats(self):
        if len(self.values) == 0:
            return 0
        
        self.values.sort()
        
        if len(self.values) % 2 == 0:
            return float(self.values[int(round(len(self.values) / 2)) - 1] + self.values[int(round(len(self.values) / 2))]) / 2.0
        
        return float(self.values[int(round(len(self.values) / 2))])
    

round_trip_overall = ExampleTimeStats()
write_access_overall = ExampleTimeStats()
read_access_overall = ExampleTimeStats()

def show_stats(overall, elapsed_seconds, round_trip, write_access, read_access):
    if overall:
        print(os.linesep +
            COLOR_GREEN + '# Overall', end = '')
    else:
        print('{:>9}'.format(elapsed_seconds), end = '')
    
    print('{:>10}'.format(len(round_trip.values)) +
        '{:>9.0f}'.format(round_trip.example_get_median_from_time_stats()) +
        '{:>9.0f}'.format(round_trip.min) +
        '{:>11}'.format(len(write_access.values)) +
        '{:>9.0f}'.format(write_access.example_get_median_from_time_stats()) +
        '{:>9.0f}'.format(write_access.min) +
        '{:>11}'.format(len(read_access.values)) +
        '{:>9.0f}'.format(read_access.example_get_median_from_time_stats()) +
        '{:>9.0f}'.format(read_access.min) + NO_COLOR)


class Ping(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        
        self._datariver = None
        self._thing = None
        self._sample_data = None
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('# Ping started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('# Ping stopped')
    
    
    def send_terminate(self):
        print('# Sending termination request.')
        self._thing.purge('Ping', 'ping')
        time.sleep(1)
        return 0
    
    def wait_for_pong(self):
        # wait for pong to appear by discovering its thingId and thingClass
        print('# Waiting for pong to run...')
        discovered_thing_registry = self._datariver.discovered_thing_registry
        reader_found = False
        
        while not reader_found:
            try:
                # see if we already know pongs's thing class
                thing = discovered_thing_registry.find_discovered_thing('py-pongThing1', 'Pong:com.adlinktech.example.protobuf:v1.0')
                reader_found = True
            except:
                pass
            time.sleep(1)

    
    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        Ping_register_with_datariver(self._datariver)
        Pong_register_with_datariver(self._datariver)
        
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri('file://definitions/ThingClass/com.adlinktech.example.protobuf/PingThingClass.json')
        self._datariver.add_thing_class_registry(tcr)
        
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._datariver.create_thing(tp))
    
    def init_payload(self, payload_size):
        self._sample_data = PingData()
        self._sample_data.payload = payload_size * b'a'
    
    def warm_up(self):
        start_time = time.time()
        wait_timeout = 10000
        
        print('# Warming up 5s to stabilise performance...')
        while (time.time() - start_time) < 5:
            self._thing.write('Ping', self._sample_data)
            self._thing.read('Pong', wait_timeout)
        print('# Warm up complete')
    
    
    def run(self, payload_size, num_samples, running_time):
        start_time = 0
        pre_write_time = 0
        post_write_time = 0
        pre_read_time = 0
        post_read_time = 0
        wait_timeout = 10000
        round_trip = ExampleTimeStats()
        write_access = ExampleTimeStats()
        read_access = ExampleTimeStats()
        global round_trip_overall
        global write_access_overall
        global read_access_overall
        
        print('# Parameters: payload size: {} | number of samples: {} | running time: {}'.format(payload_size, num_samples, running_time))
        
        # Wait for the Pong Thing
        self.wait_for_pong()
        
        # Init payload
        self.init_payload(payload_size)
        
        # Warm-up for 5s
        self.warm_up()
        
        print('# Round trip measurements (in us)')
        print(COLOR_LMAGENTA + '#             Round trip time [us]         Write-access time [us]       Read-access time [us]' + NO_COLOR)
        print(COLOR_LMAGENTA + '# Seconds     Count   median      min      Count   median      min      Count   median      min' + NO_COLOR)
        
        start_time = current_time_microseconds()
        elapsed_seconds = 0
        i = 0
        while num_samples == 0 or i < num_samples:
            # Write a sample that pong can send back
            pre_write_time = current_time_microseconds()
            self._thing.write('Ping', self._sample_data)
            post_write_time = current_time_microseconds()
            
            # Read sample
            pre_read_time = current_time_microseconds()
            samples = self._thing.read('Pong', wait_timeout)
            post_read_time = current_time_microseconds()
            
            # Validate sample count
            if samples.size() != 1:
                print('ERROR: Ping received {} samples but was expecting 1.'.format(samples.size()))
                return 1
            
            # Update stats
            write_access += (post_write_time - pre_write_time)
            read_access += (post_read_time - pre_read_time)
            round_trip += (post_read_time - pre_write_time)
            write_access_overall += (post_write_time - pre_write_time)
            read_access_overall += (post_read_time - pre_read_time)
            round_trip_overall += (post_read_time - pre_write_time)
            
            # Print stats each second
            if ((post_read_time - start_time) > US_IN_ONE_SEC) or (i == num_samples - 1):
                # Print stats
                elapsed_seconds += 1
                show_stats(False, elapsed_seconds, round_trip, write_access, read_access)
                
                # Reset stats for next run
                round_trip = round_trip.example_reset_time_stats()
                write_access = write_access.example_reset_time_stats()
                read_access = read_access.example_reset_time_stats()
                
                # Set values for next run
                start_time = current_time_microseconds()
                
                # Check for timeout
                if (running_time > 0 and elapsed_seconds >= running_time):
                    break
            
            i += 1
        
        # Print overall stats
        show_stats(True, 0, round_trip_overall, write_access_overall, read_access_overall)
        
        return 0


def str2bool(value):
    return value.lower() == 'true'


def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK ThingSDK ThroughputWriter')
    parser.add_argument('-p', '--payload-size', type=int,
                        help='Payload size', default=0)
    parser.add_argument('-n', '--num-samples', type=int,
                        help='Number of samples (0 is infinite)',
                        default=0)
    parser.add_argument('-r', '--running-time', type=int,
                        help='Running Time in seconds (0 is infinite)',
                        default=0)
    parser.add_argument('-q', '--quit', type=str2bool,
                        help='Send a quit signal to pong')
    
    args = parser.parse_args()
    
    return args.payload_size, args.num_samples, args.running_time, args.quit

def main():
    payload_size = 0
    num_samples = 0
    running_time = 0
    quit = False
    payload_size, num_samples, running_time, quit = get_command_line_parameters()
    
    result = 0
    try:
        ping = Ping('file://./config/PingProperties.json')
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with ping as p:
            if quit:
                result = p.send_terminate()
            else:
                result = p.run(payload_size, num_samples, running_time)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))
    except KeyboardInterrupt:
        show_stats(True, 0, round_trip_overall, write_access_overall, read_access_overall)
        sys.exit(1)
    
    

if __name__ == '__main__':
    main()

