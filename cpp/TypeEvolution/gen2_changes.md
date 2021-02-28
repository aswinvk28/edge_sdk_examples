# About the Gen2 Application

The Gen2 application is a evolution of the Gen 1 application to use Google Protobuf tag groups. THe motivation of changes were as follows:

* it was assumed that that it would be impractical for Gen1 applications to be completely removed from the
DataRiver prior to Gen2 deployment. The two applications generations must co-exist on a single DataRiver.
* as a result, the Gen2 dash board retains the ability to read Gen1 data, as well as Gen2 data.
* Because Google Protobuf is a new data format, the tag group representing the protobuf Temperature data has a different identifier from the original.
The identifier for the Gen1 tag group is "Temperature:com.adlinktech.example:V1.0", while the identifier for the new tag group is "Temperature:com.adlinktech.example:V1.0".
Tag groups with different identifiers are considered distinct by the DataRiver.

# Changes made to Gen1_Sensor_Dashboard

## File: definitions/TemperatureTagGroup.proto


MOD1. Ran json2proto script on TemperatureTagGroup.json, with the following arguments:

        json2proto --suffix "" --outdir definitions definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json

MOD2. Make the protobuf tag group and the original JSON tag group have distinct identifiers by adding a 'version' attribute to the .proto fille:

       message Temperature {
        option(.adlinktech.datariver.tag_group) = {
          qosProfile: "telemetry"
          description: "ADLINK Edge SDK Example Temperature TagGroup"
          version: "v2.0"
        };
       ...

## File: CMakeLists.txt

MOD3. Add 'protobuf_generate()' commands the CMakeLists.txt file:

        set(PROTO_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/definitions)
        set(PROTOC_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR})

        # Compile .proto files for C++
        protobuf_generate(LANGUAGE cpp
            PROTOS ${PROTO_SOURCE_DIR}/TemperatureTagGroup.proto
            OUT_VAR PB_FILES
            PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
        )

        # Compile .proto files for DataRiver C++ extensions
        protobuf_generate(LANGUAGE cppdatariver
            PROTOS ${PROTO_SOURCE_DIR}/TemperatureTagGroup.proto
            OUT_VAR DR_FILES
            ${CPPDATARIVER_EXTENSIONS} # let protoc know what extension are generated
            PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
        )

MOD4. Add the generated files to the executable targets in CMakeLists.txt:

        add_executable(temperaturesensor
            src/TemperatureSensor.cpp
            ${PB_FILES} ${DR_FILES}
        )

        add_executable(temperaturedashboard
            src/TemperatureDashboard.cpp
            ${PB_FILES} ${DR_FILES}
        )

MOD5. Add 'target_include_directories()' commands to CMakeLists.txt to let the compiler find the generated header files:

        target_include_directories(temperaturesensor
            PRIVATE ${PROTOC_OUTPUT_DIR}
        )

        target_include_directories(temperaturedashboard
            PRIVATE ${PROTOC_OUTPUT_DIR}
        )

## File: src/TemperatureSensor.cpp

MOD6. In TemperatureSensor.cpp, modify the include statements to remove IOT_NVP_SEQ-related includes, and add the an include
of the generated tag group header:

        #include <iostream>
        #include <thread>
        #include <chrono>
        
        #include <JSonThingAPI.hpp>
        #include <ThingAPIException.hpp>
        
        #include "definitions/TemperatureTagGroup.dr.h"

MOD7. In TemperatureSensor.cpp, replace the using namespace of 'com::adlinktech::iot' with 'com::adlinktech::example', the namespace of the
generated protobuf Temperature class:

        using namespace com::adlinktech::example;

MOD8. In TemperatureSensor.cpp, change the declaration of m_thing from Thing to ThingEx so that it can access Protobuf content:

        ThingEx m_thing = createThing();

MOD9. In TemperatureSensor.cpp, modify createThing() to register the Protobuf Temperature tag group:

        Thing createThing() {
            // Create and Populate the TagGroup registry with JSON resource files.
            TemperatureHelper::registerWithDataRiver(m_dataRiver);
    
            // Create and Populate the ThingClass registry with JSON resource files.
            JSonThingClassRegistry tcr;
            tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json");
            m_dataRiver.addThingClassRegistry(tcr);
    
            // Create a Thing based on properties specified in a JSON resource file.
            JSonThingProperties tp;
            tp.readPropertiesFromURI(m_thingPropertiesUri);
            return m_dataRiver.createThing(tp);
        }

MOD10. In TemperatureSensor.cpp, modify writeSample() to create and write a Protobuf Temperature object:

        void writeSample(const float temperature) {
            Temperature sensorData;
    
            sensorData.set_temperature(temperature);
    
            m_thing.write("temperature", sensorData);
        }

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

## File:: src/TemperatureDashboard.cpp

MOD13. In TemperatureDashboard.cpp, add an include of "TemperatureTagGroup.dr.h":

        #include <iostream>
        #include <iomanip>
        #include <thread>
        #include <chrono>
        #include <future>
        #include <exception>
        
        #include <Dispatcher.hpp>
        #include <IoTDataThing.hpp>
        #include <JSonThingAPI.hpp>
        #include <thing_IoTData.h>
        #include <ThingAPIException.hpp>
        #include "definitions/TemperatureTagGroup.dr.h"

MOD14. In TemperatureDashboard.cpp, add a 'using namespace' statement to import the generated protobuf classes:

        using namespace std;
        using namespace com::adlinktech::datariver;
        using namespace com::adlinktech::iot;
        using namespace com::adlinktech::example; // generated protobuf classes

MOD15. In TemperatureDashboard.cpp, change the type of m_thing to ThingEx:

        ThingEx m_thing = createThing();

MOD16. In TemperatureDashboard.cpp, modify createThing() to register both the Gen1 and Gen2 sensor tag groups:

        Thing createThing() {
            // Register the Gen1 sensor tag group.
            JSonTagGroupRegistry tgr;
            tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json");
            m_dataRiver.addTagGroupRegistry(tgr);
            
            // Register the Gen2 sensor tag group.
            TemperatureHelper::registerWithDataRiver(m_dataRiver);
            ...
        }

MOD17. In TemperatureDashboard.cpp, re-write the do/while loop to consider data from both a Gen1 sensor and a Gen2 sensor.
The modified code unpacks Gen2 sensor data directly into a Temperature object, while the Gen1 data is read as IOT_NVP_SEQ
data, and then transfered. The code notes and outputs the sensor generatation, too:

        do {
            VLoanedDataSamples msgs = m_thing.read("temperature",(runningTime - elapsedSeconds) * 1000);

            // Process samples
            for (const VDataSample& msg : msgs) {
                auto flowState = msg.getFlowState();
                if (flowState == FlowState::ALIVE) {
                    // translate everything into a Temperature object.
                    Temperature tempData;
                    std::string sensorGeneration;
                    if(msg.isCompatible(tempData)) {
                        sensorGeneration = "Gen2";
                        msg.get(tempData);
                    } else {
                        sensorGeneration = "Gen1";
                        // unpack the temperature from an IOT_NVP_SEQ
                        const IOT_NVP_SEQ& dataSample = msg.get<IOT_NVP_SEQ>();

                        try {
                            // Get temperature value from sample
                            for (const IOT_NVP& nvp : dataSample) {
                                if (nvp.name() == "temperature") {
                                    tempData.set_temperature(nvp.value().iotv_float32());
                                }
                            }
                        }
                        catch (exception& e) {
                            cerr << "An unexpected error occured while processing data-sample: " << e.what() << endl;
                            continue;
                        }
                    }

                    // Show output
                    cout << "Temperature data received for flow "
                        << msg.getFlowId() << "(" << sensorGeneration << "): "
                        << fixed << setw(3) << setprecision(1)
                        << tempData.temperature() << endl;
                }
            }

            elapsedSeconds = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - start).count();
        } while (elapsedSeconds < runningTime);
