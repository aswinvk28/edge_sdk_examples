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
from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties
from adlinktech.datariver import IotValue, IotNvp, IotNvpSeq

LIGHT_SAMPLE_DELAY_MS = 1000
ILLUMINANCE_THRESHOLD = 400

'''
Returns an absolute file uri of a given relative file path.
Allows to run this example from any location
'''
def get_abs_file_uri(filepath):
    dirpath = os.path.dirname(os.path.abspath(__file__))
    return 'file://' + str(os.path.join(dirpath, filepath))

class LightSensor(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        self._datariver = None
        self._thing = None
        
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._thing = self.create_thing()
        
        print('Light Sensor started')
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            self._datariver.close()
        print('Light Sensor stopped')
    
    def create_thing(self):
        # Create and Populate the TagGroup registry with JSON resource files.
        tgr = JSonTagGroupRegistry()
        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/IlluminanceTagGroup.json'))
        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/IlluminanceAlarmTagGroup.json'))
        self._datariver.add_tag_group_registry(tgr) 
 
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example/LightSensorThingClass.json'))
        self._datariver.add_thing_class_registry(tcr)
 
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        return self._datariver.create_thing(tp)
    
    def write_sample(self, illuminance):
        illuminance_v = IotValue()
        illuminance_v.uint32 = illuminance
        sensor_data = IotNvpSeq()
        sensor_data.append(IotNvp('illuminance', illuminance_v))
        
        self._thing.write('illuminance', sensor_data)

    def alarm(self, message):
        alarm_v = IotValue()
        alarm_v.string = message
        alarm_data = IotNvpSeq()
        alarm_data.append(IotNvp('alarm', alarm_v))

        self._thing.write('alarm', alarm_data)

    def run(self, running_time):
        sample_count = (running_time * 1000) / LIGHT_SAMPLE_DELAY_MS;
        actual_illuminance = 500
        alarm_state = False

        while sample_count > 0:
            sample_count -= 1
            # Simulate illuminance change
            dir = 1
            if (sample_count % 20) > 10:
                dir = -1
            actual_illuminance += dir * 30

            # Write sensor data to river
            self.write_sample(actual_illuminance)

            # Write alarm if value below threshold
            if (not alarm_state) and (actual_illuminance < ILLUMINANCE_THRESHOLD):
                self.alarm('Illuminance below threshold')
                alarm_state = True
            elif alarm_state and (actual_illuminance > ILLUMINANCE_THRESHOLD):
                alarm_state = False
            
            time.sleep(float(LIGHT_SAMPLE_DELAY_MS) / 1000.0)


def main():
    # Get thing properties URI from command line parameter
    parser = argparse.ArgumentParser()
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/Station1/LightSensorProperties.json')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
    args = parser.parse_args()
    
    try:
        light_sensor = LightSensor(args.thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with light_sensor as ls:
            ls.run(args.running_time)
    except Exception as e:
        print('LightSensor: An unexpected error occurred: {}'.format(e))
    

if __name__ == '__main__':
    main()

