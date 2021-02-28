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
import argparse
import sys
import os
import time
from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties, IotType
from adlinktech.datariver import ThingDiscoveredListener, Dispatcher, TimeoutError, ThingAPIRuntimeError, InvalidArgumentError

if sys.platform.lower() == 'win32':
    os.system('color')
    
NO_COLOR = '\33[0m'
COLOR_RED = '\33[0;31m'
COLOR_GREEN = '\33[0;32m'
COLOR_LGREEN = '\33[0;92m'
COLOR_LYELLOW = '\33[0;93m'
COLOR_YELLOW = '\33[0;33m'
COLOR_BLUE = '\33[0;34m'
COLOR_LBLUE = '\33[0;94m'
COLOR_MAGENTA = '\33[0;35m'
COLOR_LMAGENTA = '\33[0;95m'


class TagGroupNotFoundException(Exception):
    pass

class NewThingDiscoveredListener(ThingDiscoveredListener):
    
    def __init__(self, datariver):
        ThingDiscoveredListener.__init__(self)
        self._datariver = datariver
        
        self._discovered_tag_group_registry = self.create_discovered_tag_group_registry()
        self._discovered_thing_class_registry = self.create_discovered_thing_class_registry()
        self._discovered_thing_registry = self.create_discovered_thing_registry()
    
    def create_discovered_tag_group_registry(self):
        return self._datariver.discovered_tag_group_registry
    
    def create_discovered_thing_class_registry(self):
        return self._datariver.discovered_thing_class_registry
    
    def create_discovered_thing_registry(self):
        return self._datariver.discovered_thing_registry
    
    def display_type_definition(self, td, prefix = '', width = 0):
        print(prefix + COLOR_LBLUE + '{0:<{w}}'.format(td.name_of_type, w=width) + NO_COLOR + ': ')
        
        vtagd = td.tags
        
        for tagd in vtagd:
            self.display_tag(tagd, prefix, width + 3)
    
    def get_tag_type(self, tag):
        type = IotType(tag.kind).name
        prefix = 'TYPE_'
        if type.startswith(prefix):
            return type[len(prefix):]
        return type
    
    def display_tag(self, tag, prefix = '', width = 0):
        print(prefix + COLOR_YELLOW + '{0:<{w}}'.format(tag.name, w=width) + NO_COLOR + ': ' + tag.description +
              ' (kind: {}'.format(self.get_tag_type(tag)) +
              ' | unit: {})'.format(tag.unit)
              )
    
    def display_tag_group(self, tag_group, prefix = ''):
        print(COLOR_LBLUE + tag_group.name + ':' + tag_group.context + ':' + tag_group.version_tag + COLOR_BLUE + ' [TagGroup]' + NO_COLOR)
        print(prefix + 'Description: {}'.format(tag_group.description))
        print(prefix + 'QosProfile: {}'.format(tag_group.qos_profile))
        print(prefix + 'Tags: ')
        try:
            type = tag_group.top_level_type
            for tag in type.tags:
                self.display_tag(tag, prefix + '   ', 15)
        except ThingAPIRuntimeError as e:
            print(prefix + COLOR_RED + '   Error displaying TagGroup details: {}'.format(e) + NO_COLOR)
    
    
    def is_dynamic_tag_group(self, tag_group):
        return tag_group.find('*') != -1 or tag_group.find('?') != -1 or tag_group.find(',') != -1
    
    
    def find_tag_group(self, tag_group_name):
        retry_count = 50
        
        while retry_count > 0:
            retry_count -= 1
            
            try:
                return self._discovered_tag_group_registry.find_tag_group(tag_group_name)
            except InvalidArgumentError:
                # TagGroup not found
                pass
            
            # Sleep 100ms before retry
            time.sleep(100.0/1000.0)
        
        raise TagGroupNotFoundException()
    
    
    def display_inputs(self, inputs, prefix = ''):
        print(prefix + 'inputs:')
        if len(inputs) == 0:
            print(prefix + '   <none>')
        else:
            for input in inputs:
                input_tag_group = input.input_tag_group
                if self.is_dynamic_tag_group(input_tag_group):
                    print(prefix + '   ' + COLOR_GREEN + input.name + NO_COLOR + ': ' +
                          COLOR_MAGENTA + '[expression]' + NO_COLOR + ' ' + input_tag_group
                          )
                else:
                    try:
                        tag_group = self.find_tag_group(input_tag_group)
                        print(prefix + '   ' + COLOR_GREEN + input.name + NO_COLOR + ': ', end = '')
                        self.display_tag_group(tag_group, prefix + '      ')
                    except TagGroupNotFoundException:
                        print(prefix + COLOR_RED + '   TagGroup not found' + NO_COLOR)
    
    
    def display_outputs(self, outputs, prefix = ''):
        print(prefix + 'outputs:')
        if len(outputs) == 0:
            print(prefix + '   <none>')
        else:
            for output in outputs:
                try:
                    tag_group = self.find_tag_group(output.output_tag_group)
                    print(prefix + '   ' + COLOR_GREEN + output.name + NO_COLOR + ': ', end = '')
                    self.display_tag_group(tag_group, prefix + '      ')
                except TagGroupNotFoundException:
                    print(prefix + COLOR_RED + '   TagGroup not found' + NO_COLOR)
    
    
    def display_thing_class(self, thing_class, prefix = ''):
        inputs = thing_class.input_tag_groups
        outputs = thing_class.output_tag_groups
        
        print(prefix + COLOR_LMAGENTA + thing_class.id.name + ':' + thing_class.context + ':' + thing_class.version_tag + COLOR_MAGENTA + ' [ThingClass]'  + NO_COLOR)
        print(prefix + '   Description: ' + thing_class.description)
        
        self.display_inputs(inputs, prefix + '   ')
        self.display_outputs(outputs, prefix + '   ')
    
    
    def display_thing(self, thing, prefix = ''):
        thing_class_found = False
        retry_count = 30
        
        print(os.linesep + COLOR_LGREEN + thing.context_id + COLOR_GREEN + ' [Thing]' + NO_COLOR)
        print(prefix + '   Thing ID:    ' + thing.id)
        print(prefix + '   Context:     ' + thing.context_id)
        print(prefix + '   Description: ' + thing.description)
        
        while not thing_class_found and retry_count > 0:
            retry_count -= 1
            
            try:
                thing_class = self._discovered_thing_class_registry.find_thing_class(
                    thing.class_id.name + ':' + thing.class_id.context + ':' + thing.class_id.version_tag)
                
                self.display_thing_class(thing_class, prefix + '   ')
                thing_class_found = True
            except InvalidArgumentError:
                # ThingClass not found
                pass
            
            # Sleep 100ms before retry
            time.sleep(100.0/1000.0)
        
        if not thing_class_found:
            print(prefix + COLOR_RED + '   ThingClass not found' + NO_COLOR)
    
    
    
    def notify_thing_discovered(self, thing):
        self.display_thing(thing, '   ')




class ThingBrowser(object):
    
    # Initializing
    def __init__(self, thing_properties_uri):
        self._thing_properties_uri = thing_properties_uri
        self._datariver = None
        self._thing = None
        self._dispatcher = None
        self._new_thing_discovered_listener = None
    
    # Enter the runtime context related to the object
    def __enter__(self):
        self._datariver = DataRiver.get_instance()
        self._dispatcher = Dispatcher()
        self._new_thing_discovered_listener = NewThingDiscoveredListener(self._datariver)
        self._thing = self.create_thing()
        
        return self
    
    # Exit the runtime context related to the object
    def __exit__(self, exc_type, exc_value, exc_traceback):
        if self._datariver is not None:
            try:
                # Remove the discovered Thing listener that was added during class initialization
                self._datariver.remove_listener(self._new_thing_discovered_listener, self._dispatcher)
            except ThingAPIException as e:
                print('Unexpected error while removing discovered Thing listener: '.format(e))
            
            self._datariver.close()
        print(COLOR_GREEN + 'ThingBrowser stopped' + NO_COLOR)
    
    
    def create_thing(self):
        # Add listener for discovery of Things
        self._datariver.add_listener(self._new_thing_discovered_listener, self._dispatcher)
        
        # Create and Populate the ThingClass registry with JSON resource files.
        tcr = JSonThingClassRegistry()
        tcr.register_thing_classes_from_uri('file://definitions/ThingClass/com.adlinktech.example/ThingBrowserThingClass.json')
        self._datariver.add_thing_class_registry(tcr)
  
        # Create a Thing based on properties specified in a JSON resource file.
        tp = JSonThingProperties()
        tp.read_properties_from_uri(self._thing_properties_uri)
        
        return self._datariver.create_thing(tp)
    
    
    def run(self, running_time):
        # Process events with our dispatcher
        start = time.time()
        elapsed_seconds = 0
        
        while True:
            try:
                self._dispatcher.process_events(1000)
            except TimeoutError:
                # Ignore
                pass
            
            elapsed_seconds = time.time() - start
            
            if elapsed_seconds >= running_time:
                break
        
        return 0
    

def main():
    # Get thing properties URI from command line parameter
    parser = argparse.ArgumentParser()    
    parser.add_argument('thing_properties_uri', type=str, nargs='?',
                        help='URI of the thing properties file',
                        default='file://./config/ThingBrowserProperties.json')
    parser.add_argument('running_time', type=int, nargs='?',
                        help='Total running time of the program (in seconds)',
                        default=60)
    args = parser.parse_args()
    
    print(COLOR_GREEN + 'Starting ThingBrowser' + NO_COLOR)
    
    try:
        thing_browser = ThingBrowser(args.thing_properties_uri)
        # The 'with' statement supports a runtime context which is implemented
        # through a pair of methods executed (1) before the statement body is 
        # entered (__enter__()) and (2) after the statement body is exited (__exit__())
        with thing_browser as tb:
            tb.run(args.running_time)
    except Exception as e:
        print('An unexpected error occurred: {}'.format(e))
    

if __name__ == '__main__':
    main()


