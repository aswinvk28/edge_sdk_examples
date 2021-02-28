project(S5_DynamicBrowsing_Protobuf)
cmake_minimum_required(VERSION 3.5)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING
      "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING
      "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)

find_package(ThingAPI REQUIRED)

set(PROTO_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/definitions)
set(PROTOC_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR})

# Compile .proto files for C++
protobuf_generate(LANGUAGE cpp
    PROTOS ${PROTO_SOURCE_DIR}/FuelLevelTagGroup.proto ${PROTO_SOURCE_DIR}/RotationalSpeedTagGroup.proto ${PROTO_SOURCE_DIR}/SensorStateTagGroup.proto ${PROTO_SOURCE_DIR}/TemperatureTagGroup.proto
    OUT_VAR PB_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

# Compile .proto files for DataRiver C++ extensions
protobuf_generate(LANGUAGE cppdatariver
    PROTOS ${PROTO_SOURCE_DIR}/FuelLevelTagGroup.proto ${PROTO_SOURCE_DIR}/RotationalSpeedTagGroup.proto ${PROTO_SOURCE_DIR}/SensorStateTagGroup.proto ${PROTO_SOURCE_DIR}/TemperatureTagGroup.proto
    OUT_VAR DR_FILES
    ${CPPDATARIVER_EXTENSIONS} # let protoc know what extension are generated
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

find_package (Threads)

add_executable(s5_generator_a
    src/GeneratorA.cpp
    src/Sensors/FuelLevelSensor.cpp
    src/Sensors/RotationalSpeedSensor.cpp
    src/Sensors/TemperatureSensor.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s5_generator_b
    src/GeneratorB.cpp
    src/Sensors/RotationalSpeedSensor.cpp
    src/Sensors/TemperatureSensor.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s5_thingbrowser
    src/ThingBrowser.cpp
    src/Utils.cpp
)

target_link_libraries(s5_generator_a
    ThingAPI::ThingAPI
    ${CMAKE_THREAD_LIBS_INIT}
)

target_link_libraries(s5_generator_b
    ThingAPI::ThingAPI
    ${CMAKE_THREAD_LIBS_INIT}
)

target_link_libraries(s5_thingbrowser
    ThingAPI::ThingAPI
)

target_include_directories(s5_generator_a
    PRIVATE ${PROTOC_OUTPUT_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/src/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src/Sensors
)
target_include_directories(s5_generator_b
    PRIVATE ${PROTOC_OUTPUT_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/src/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src/Sensors
)
target_include_directories(s5_thingbrowser
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

set_property(TARGET s5_generator_a PROPERTY CXX_STANDARD 11)
set_property(TARGET s5_generator_a PROPERTY OUTPUT_NAME "generator_a")

set_property(TARGET s5_generator_b PROPERTY CXX_STANDARD 11)
set_property(TARGET s5_generator_b PROPERTY OUTPUT_NAME "generator_b")

set_property(TARGET s5_thingbrowser PROPERTY CXX_STANDARD 11)
set_property(TARGET s5_thingbrowser PROPERTY OUTPUT_NAME "thingbrowser")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config/GeneratorA
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorA/FuelLevelSensorProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorA/RotationalSpeedSensorProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorA/TemperatureSensorProperties.json
        config/GeneratorA
    COMMAND ${CMAKE_COMMAND} -E make_directory config/GeneratorB
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorB/RotationalSpeedSensorProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorB/TemperatureSensorProperties.json
        config/GeneratorB
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/ThingBrowserProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example.protobuf
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/FuelLevelSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/RotationalSpeedSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/TemperatureSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/ThingBrowserThingClass.json
        definitions/ThingClass/com.adlinktech.example.protobuf
)

configure_file(start_thingbrowser.sh start_thingbrowser.sh COPYONLY)
configure_file(start_generator_a.sh start_generator_a.sh COPYONLY)
configure_file(start_generator_b.sh start_generator_b.sh COPYONLY)

configure_file(start_thingbrowser.bat start_thingbrowser.bat COPYONLY)
configure_file(start_generator_a.bat start_generator_a.bat COPYONLY)
configure_file(start_generator_b.bat start_generator_b.bat COPYONLY)
