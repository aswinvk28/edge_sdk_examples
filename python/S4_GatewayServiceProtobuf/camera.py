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
import sys
import os
import time
import random
from threading import Thread
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, ThingEx
from adlinktech.datariver import ThingDiscoveredListener, ThingLostListener
from definitions.CameraStateTagGroup_pb2 import CameraState
from definitions.IlluminanceAlarmTagGroup_pb2 import IlluminanceAlarm
from definitions.IlluminanceTagGroup_pb2 import Illuminance
from definitions.ObservationTagGroup_pb2 import Observation

from definitions.CameraStateTagGroup_dr import CameraState_register_with_datariver
from definitions.IlluminanceAlarmTagGroup_dr import IlluminanceAlarm_register_with_datariver
from definitions.IlluminanceTagGroup_dr import Illuminance_register_with_datariver
from definitions.ObservationTagGroup_dr import Observation_register_with_datariver

CAMERA_SAMPLE_DELAY = 1000
CAMERA_INITIAL_DELAY = 2000
CAMERA_DELAY = 100
BARCODE_INTERVAL = 5000
BARCODE_LIFESPAN = 15000
BARCODE_SKIP_PERCENTAGE = 25

# Convert python returned current time expressed in seconds(as floating point number) to milliseconds
current_milli_time = lambda: int(round(time.time() * 1000))

'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))


class CameraThingDiscoveredListener(ThingDiscoveredListener):
    def __init__(self, camera):
        ThingDiscoveredListener.__init__(self)
        self._camera = camera
    
    def notify_thing_discovered(self, thing):
        if self._camera.is_related(thing):
            self._camera.discovered_related_camera(thing.id, thing.context_id)


class CameraThingLostListener(ThingLostListener):
    def __init__(self, camera):
        ThingLostListener.__init__(self)
        self._camera = camera
    
    def notify_thing_lost(self, thing):
        if self._camera.is_related(thing):
            self._camera.lost_related_camera(thing.id)

class Camera(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        self._datariver = None
        self._thing = None
        
        self._barcodes = []
        self._related_cameras = dict()
        self._threads = []
        self._closed = False
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Camera started')
        self.set_state('on')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):        
        try:
            # Set camera state to 'off'
            self.set_state('off')
        except ThingAPIException as e:
            print('Error setting camera state to off: {}'.format(e))

        # Stop and join threads
        self._closed = True
        for thread in self._threads:
            thread.join()
        
        if self._datariver is not None:
            self._datariver.close()
        print('Camera stopped')
        
        
    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        CameraState_register_with_datariver(self._datariver)
        IlluminanceAlarm_register_with_datariver(self._datariver)
        Illuminance_register_with_datariver(self._datariver)
        Observation_register_with_datariver(self._datariver)
        
 
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example.protobuf/CameraThingClass.json'))
        self._datariver.add_thing_class_registry(tcr)
 
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return ThingEx(self._datariver.create_thing(tp))
    
    def get_flow_id(self, barcode):
        flow_id = ''
        if self.has_related_cameras():
            flow_id = self.get_parent_context(self._thing.context_id) + '.cameras.' + barcode
        else:
            flow_id = self._thing.context_id + '.' + barcode

        return flow_id

    def write_sample(self, barcode, x, y, z):      
        data = Observation()
        data.barcode = barcode
        data.position_x = x
        data.position_y = y
        data.position_z = z
        
        self._thing.write('observation', self.get_flow_id(barcode), data)

    def purge_flow(self, barcode):
        self._thing.purge('observation', self.get_flow_id(barcode))

    def set_state(self, state):
        data = CameraState()
        data.state = state
        
        self._thing.write('state', data)

    def get_parent_context(self, context_id):
        found = context_id.rfind('.')
        if found != -1:
            return context_id[0:found]
        return context_id

    def has_related_cameras(self):
        return len(self._related_cameras) > 0

    def check_registry_for_related_cameras(self):
        discovered_things_registry = self._datariver.discovered_thing_registry
        things = discovered_things_registry.discovered_things
        for thing in things:
            if self.is_related(thing):
                self.discovered_related_camera(thing.id, thing.context_id)

    def barcode_task(self, barcode):
        start = current_milli_time()
        elapsed_milliseconds = 0
        x = random.randrange(0,100) # randrange returns a random integer N such that a <= N < b
        y = random.randrange(0,100)
        z = random.randrange(0,100)
        
        while True:
            # Simulate position change
            x += random.randrange(-5,5)
            y += random.randrange(-5,5)
            z += random.randrange(-1,1)

            # Sleep before sending next update
            time.sleep(float(CAMERA_SAMPLE_DELAY) / 1000.0)

            # Send location update for this barcode
            self.write_sample(str(barcode), int(x), int(y), int(z))

            elapsed_milliseconds = current_milli_time() - start
            
            if self._closed or (elapsed_milliseconds >= BARCODE_LIFESPAN):
                break

        self.purge_flow(barcode)
    
    def is_related(self, thing):
        return ((self.get_parent_context(thing.context_id) == self.get_parent_context(self._thing.context_id))
            and (thing.class_id == self._thing.class_id) and (thing.id != self._thing.id))

    def discovered_related_camera(self, thing_id, context_id):
        if thing_id not in self._related_cameras:
            print('Camera ' + self._thing.context_id + ': detected other camera with context ' + context_id + ' (Thing Id ' + thing_id + ')')
        
        self._related_cameras[thing_id] = context_id

    def lost_related_camera(self, thing_id):
        self._related_cameras.pop(thing_id)

    def run(self, running_time, barcodes):
        random.seed()
        start = current_milli_time()
        barcode_seqnr = 0
        barcode_timestamp = start - BARCODE_INTERVAL
        elapsed_seconds = 0

        # Add listeners for Thing discovered and Thing lost
        new_thing_discovered_listener = CameraThingDiscoveredListener(self)
        self._datariver.add_listener(new_thing_discovered_listener)

        thing_lost_listener = CameraThingLostListener(self)
        self._datariver.add_listener(thing_lost_listener)

        # Check for related camera already in the discovered things registry
        time.sleep(float(CAMERA_INITIAL_DELAY) / 1000.0)
        self.check_registry_for_related_cameras()

        # Start processing
        while True:
            now = current_milli_time()

            # Check if next barcode should be read
            if (barcode_seqnr < len(barcodes)) and ((now - barcode_timestamp) > BARCODE_INTERVAL):
                barcode = barcodes[barcode_seqnr]
                barcode_seqnr += 1

                # Randomly skip some of the barcodes
                if random.randrange(0,100) > BARCODE_SKIP_PERCENTAGE :
                    t1 = Thread(target=self.barcode_task, args=(barcode,))
                    self._threads.append(t1)
                    t1.start()

                # Update timestamp and seqnr
                barcode_timestamp = now

            # Sleep for some time
            time.sleep(float(CAMERA_DELAY) / 1000.0)

            # Check if camera should keep running
            elapsed_seconds = (current_milli_time() - start) / 1000
            
            if elapsed_seconds >= running_time:
                break

        # Remove listeners
        self._datariver.remove_listener(new_thing_discovered_listener)
        self._datariver.remove_listener(thing_lost_listener)

def read_bar_codes(barcode_file_path):
    barcodes = []
    
    try:
        barcode_file = open(barcode_file_path, 'r')
        
        for barcode in barcode_file.readlines():
            barcode = barcode.rstrip('\n')
            if len(barcode) > 0:
                barcodes.append(barcode)
        
        barcode_file.close()
        
    except:
        print('Cannot open barcode file: {}'.format(barcode_file_path))
    
    return barcodes

def get_command_line_parameters():
    parser = argparse.ArgumentParser(description='ADLINK Edge SDK Example Camera')
    requiredNamed = parser.add_argument_group('required named arguments')
    requiredNamed.add_argument('--thing', type=str, nargs='?',
                               help='Thing properties URI',
                               default='file://./config/Station1/Camera1Properties.json')
    requiredNamed.add_argument('--barcodes', type=str, nargs='?',
                               help='Barcode file path',
                               default='./barcodes1.txt')
    requiredNamed.add_argument('--running-time', type=int, nargs='?',
                               help='Running Time (in seconds)',
                               default=60)
    args = parser.parse_args()
    
    return args.thing, args.barcodes, args.running_time

def main():
    # Get command line parameters
    thing_properties_uri, barcode_file_path, running_time = get_command_line_parameters()
    
    # Get barcodes
    barcodes = read_bar_codes(barcode_file_path)
    if len(barcodes) == 0:
        print('Error: no barcodes found')
        sys.exit(1)
    
    try:
        camera = Camera(thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with camera as cam:
            cam.run(running_time, barcodes)
    except Exception as e:
        print('Camera: An unexpected error occurred: {}'.format(e))
    

if __name__ == '__main__':
    main()
