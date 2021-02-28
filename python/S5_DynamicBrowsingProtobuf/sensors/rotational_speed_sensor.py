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
This code is part of example scenario 5 'Dynamic Browsing' of the
ADLINK Edge SDK. For a description of this scenario see the
'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.

For instructions on running the example see the README
file in the Edge SDK installation directory.
'''

from __future__ import print_function
import time
import random
from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, ThingEx
from definitions.RotationalSpeedTagGroup_pb2 import RotationalSpeed
from definitions.RotationalSpeedTagGroup_dr import RotationalSpeed_register_with_datariver

SPEED_SAMPLE_DELAY_MS = 3000

class RotationalSpeedSensor(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        self._datariver = None
        self._thing = None
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Rotational Speed Sensor started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Rotational Speed Sensor stopped')
         
    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        RotationalSpeed_register_with_datariver(self._datariver)
  
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri('file://definitions/ThingClass/com.adlinktech.example.protobuf/RotationalSpeedSensorThingClass.json')
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        
        return ThingEx(self._datariver.create_thing(tp))
    
    def write_sample(self, speed, last_hour_min, last_hour_max, last_hour_average):
        data = RotationalSpeed()
        
        data.speed = speed
        data.lastHourMin = last_hour_min
        data.lastHourMax = last_hour_max
        data.lastHourAverage = last_hour_average
        
        self._thing.write('rotationalSpeed', data)
    
    def run(self, running_time):
        random.seed()
        sample_count = (running_time * 1000) / SPEED_SAMPLE_DELAY_MS
        speed = 1000
        
        while sample_count > 0:
            sample_count -= 1
            
            # Simulate speed change
            speed += (random.randrange(10) - 4)
            self.write_sample(speed, 0, 0, 0.0)
            
            time.sleep(float(SPEED_SAMPLE_DELAY_MS) / 1000.0)
        
        return 0



