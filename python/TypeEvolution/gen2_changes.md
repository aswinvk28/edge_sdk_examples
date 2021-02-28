# About the Gen2 Application

The Gen2 application is a evolution of the Gen 1 application to use Google Protobuf tag groups. The motivation of changes were as follows:

* it was assumed that that it would be impractical for Gen1 applications to be completely removed from the
DataRiver prior to Gen2 deployment. The two applications generations must co-exist on a single DataRiver.
* as a result, the Gen2 dash board retains the ability to read Gen1 data, as well as Gen2 data.
* Because Google Protobuf is a new data format, the tag group representing the protobuf Temperature data has a different identifier from the original.
The identifier for the Gen1 tag group is "Temperature:com.adlinktech.example:v1.0", while the identifier for the new tag group is "Temperature:com.adlinktech.example:v1.0".
Tag groups with different identifiers are considered distinct by the DataRiver.

# Changes made to Gen1_Sensor_Dashboard

## File: config/TemperatureDashboardProperties.json

MOD1. Modify the .json file to have inlcude version v2.0 in classId:

		{
		  "id": "py-8838c6bf-43dd-4534-9be6-47f190537b23",
		  "classId": "TemperatureDashboard:com.adlinktech.example:v2.0",
		  "contextId": "floor1.dashboard1",
		  "description": "Edge SDK example dashboard Thing that shows temperature data from sensors on floor 1",
		  "inputSettings" : [
		     {
		        "name" : "temperature",
		        "filters" : {
		          "flowIdFilters": ["floor?.*"]
		        }
		     }
		  ]
		}

## File: config/TemperatureSensor1Properties.json

MOD2. Modify the .json file to have inlcude version v2.0 in classId:

		{
		    "id": "PY-E5784CB5302A",
		    "classId": "TemperatureSensor:com.adlinktech.example:v2.0",
		    "contextId": "floor1.room1",
		    "description": "Edge SDK example Floor 1 - room 1 temperature sensor"
		}


## File: config/TemperatureSensor2Properties.json

MOD3. Modify the .json file to have inlcude version v2.0 in classId:

		{
		    "id": "PY-C17F4CE2301B",
		    "classId": "TemperatureSensor:com.adlinktech.example:v2.0",
		    "contextId": "floor1.room2",
		    "description": "Edge SDK example Floor 1 - room 2 temperature sensor"
		}

## File: config/TemperatureSensor3Properties.json

MOD4. Modify the .json file to have inlcude version v2.0 in classId:

		{
		    "id": "PY-EEC22A12303C",
		    "classId": "TemperatureSensor:com.adlinktech.example:v2.0",
		    "contextId": "floor2.room1",
		    "description": "Edge SDK example Floor 2 - room 1 temperature sensor"
		}

## File: definitions/TemperatureTagGroup.proto


MOD5. Ran json2proto script on TemperatureTagGroup.json, with the following arguments:

        json2proto --suffix "" --outdir definitions definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json

MOD6. Make the protobuf tag group and the original JSON tag group have distinct identifiers by adding a 'version' attribute to the .proto fille:

       message Temperature {
        option(.adlinktech.datariver.tag_group) = {
          qosProfile: "telemetry"
          description: "ADLINK Edge SDK Example Temperature TagGroup"
          version: "v2.0"
        };
       ...

## File: CMakeLists.txt

MOD7. Add 'protobuf_generate()' commands the CMakeLists.txt file:

		set(PROTO_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/definitions)
		set(PROTOC_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR})
		
		protobuf_generate(LANGUAGE python
		    PROTOS ${PROTO_SOURCE_DIR}/TemperatureTagGroup.proto
		           ${THINGAPI_INCLUDE_DIR}/adlinktech/datariver/descriptor.proto
		    OUT_VAR PB_FILES
		    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
		)
		
		protobuf_generate(LANGUAGE pythondatariver
		    PROTOS ${PROTO_SOURCE_DIR}/TemperatureTagGroup.proto
		    OUT_VAR DR_FILES
		    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
		    GENERATE_EXTENSIONS "_dr.py"
		)
		
		add_custom_target(copy_python_te_protobuf ALL
		    COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR}
		    DEPENDS ${PB_FILES} ${DR_FILES}
		)

## File: TemperatureSensor.py

MOD8. In TemperatureSensor.py, modify the include statements to import ThingEx and add an import
of the generated tag group header:

		from __future__ import print_function
		import argparse
		import sys
		import os
		import time
		import random
		from adlinktech.datariver import DataRiver, JSonThingClassRegistry, JSonThingProperties, ThingEx
		from definitions.TemperatureTagGroup_pb2 import Temperature
		import definitions.TemperatureTagGroup_dr as tag_groups

MOD9. In TemperatureSensor.py, modify createThing() to register the Protobuf Temperature tag group:

    		def create_thing(self):
			# Create and Populate the TagGroup registry with JSON resource files.
		    tag_groups.Temperature_register_with_datariver(self._dr)
		 
		    # Create and Populate the ThingClass registry with JSON resource files.
		   	tcr = JSonThingClassRegistry()
		    tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json'))
		    self._dr.add_thing_class_registry(tcr)
		 
		    # Create a Thing based on properties specified in a JSON resource file.
		    tp = JSonThingProperties()
		    tp.read_properties_from_uri(self._thing_properties_uri)
		    return ThingEx(self._dr.create_thing(tp))

MOD10. In TemperatureSensor.py, modify writeSample() to create and write a Protobuf Temperature object:

		def write_sample(self, temperature):
       		 sensor_data = Temperature()
       		 sensor_data.temperature = temperature
        		 self._thing.write('temperature', sensor_data)

## File: definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json

MOD11. In definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json, modify the referenced tag group to
the identifier of the protobuf tag group, ``Temperature:com.adlinktech.example:v2.0``. Update the ThingClass's version, too:

        {
          "name": "TemperatureSensor",
          "context": "com.adlinktech.example",
          "version": "v2.0",
          "description": "ADLINK Edge SDK Example Temperature sensor",
          "outputs": [{
            "name": "temperature",
            "tagGroupId": "Temperature:com.adlinktech.example:v2.0"
          }]
        }

## File: definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json

MOD12. In definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json, add the protobuf tag group identifier
to the ``temperature`` input. Update the ThingClass's version, too:

        {
          "name": "TemperatureDashboard",
          "context": "com.adlinktech.example",
          "version": "v2.0",
          "description": "ADLINK Edge SDK Example dashboard that shows temperature sensor data",
          "inputs": [{
            "name": "temperature",
            "tagGroupId": [
              "Temperature:com.adlinktech.example:v1.0",
              "Temperature:com.adlinktech.example:v2.0"
            ]
          }]
        }

## File:: TemperatureDashboard.py

MOD13. In TemperatureDashboard.py, modify the import statements to import ThingEx and add an import
of the generated tag group headers:

		from __future__ import print_function
		import argparse
		import os
		import time
		import sys
		from adlinktech.datariver import DataRiver, JSonTagGroupRegistry, JSonThingClassRegistry, JSonThingProperties, FlowState, ThingEx, IotNvpSeq
		from adlinktech.datariver import ThingDiscoveredListener, ThingLostListener
		from definitions.TemperatureTagGroup_pb2 import Temperature
		import definitions.TemperatureTagGroup_dr as tag_groups

MOD14. In TemperatureDashboard.py, modify createThing() to register the Protobuf Temperature tag group for both gen1 and 2:

	    def create_thing(self):
	        # Create and Populate the TagGroup registry with JSON resource files.
	        tgr = JSonTagGroupRegistry()
	        tgr.register_tag_groups_from_uri(get_abs_file_uri('definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json'))
	        self._dr.add_tag_group_registry(tgr) 
	        
	        # Register Gen2 sensor tag groups
	        tag_groups.Temperature_register_with_datariver(self._dr)
	  
	        # Create and Populate the ThingClass registry with JSON resource files.
	        tcr = JSonThingClassRegistry()
	        tcr.register_thing_classes_from_uri(get_abs_file_uri('definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json'))
	        self._dr.add_thing_class_registry(tcr)
	  
	        # Create a Thing based on properties specified in a JSON resource file.
	        tp = JSonThingProperties()
	        tp.read_properties_from_uri(self._thing_properties_uri)
	        return ThingEx(self._dr.create_thing(tp))

MOD15. In TemperatureDashboard.py, re-write the do/while loop to consider data from both a Gen1 sensor and a Gen2 sensor.
The modified code unpacks Gen2 sensor data directly into a Temperature object, while the Gen1 data is read as IOT_NVP_SEQ
data, and then transfered. The code notes and outputs the sensor generatation, too:

	    def run(self, running_time) :
	        start = time.time()
	        elapsed_seconds = 0
	         
	        # Add listener for new Things
	        temperature_sensor_discovered_listener = TemperatureSensorDiscoveredListener()
	        self._dr.add_listener(temperature_sensor_discovered_listener)
	 
	        # Add listener for lost Things
	        temperature_sensor_lost_listener = TemperatureSensorLostListener()
	        self._dr.add_listener(temperature_sensor_lost_listener)
	         
	        while True:
	            # Read data using selector
	            msgs = self._thing.read('temperature', int((running_time - elapsed_seconds) * 1000))
	            
	            # Process samples
	            for msg in msgs:
	                flow_state = msg.flow_state
	                if flow_state == FlowState.ALIVE:
	
	                    temperature = 0.0
	                    sensor_generation = ""
	                    
	                    if msg.is_compatible(Temperature):
	                        sensor_data = msg.get(Temperature)
	                        sensor_generation = "Gen2"
	                    else: 
	                        sensor_generation = "Gen1"
	                        data_sample = msg.get(IotNvpSeq)
	                        try:
	                            # Get temperature value from sample
	                            for nvp in data_sample:
	                                if nvp.name == 'temperature':
	                                    temperature = nvp.value.float32
	                        except Exception as e:
	                            print('An unexpected error occured while processing data-sample: ' + str(e))
	                            continue
	                     
	                    # Show output
	                    if(sensor_generation == "Gen1"):
	                    	print('Temperature data received for flow {} ({}): {:5.2f}'.format(str(msg.flow_id), sensor_generation, temperature))
	                    else:
	                    	print('Temperature data received for flow {} ({}): {:5.2f}'.format(str(msg.flow_id), sensor_generation, sensor_data.temperature))
	 
	            elapsed_seconds = time.time() - start
	             
	            if elapsed_seconds >= float(running_time):
	                break
	             
	        # Remove listeners
	        self._dr.remove_listener(temperature_sensor_lost_listener)
	        self._dr.remove_listener(temperature_sensor_discovered_listener)
