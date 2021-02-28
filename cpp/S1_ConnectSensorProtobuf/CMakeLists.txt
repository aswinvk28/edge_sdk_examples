project(S1_ConnectSensor_Protobuf)
cmake_minimum_required(VERSION 3.5)

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

add_executable(s1_temperaturesensor
    src/TemperatureSensor.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s1_temperaturedisplay
    src/TemperatureDisplay.cpp
    ${PB_FILES}
    ${DR_FILES}
)

target_link_libraries(s1_temperaturesensor
    ThingAPI::ThingAPI
)

target_link_libraries(s1_temperaturedisplay
    ThingAPI::ThingAPI
)

target_include_directories(s1_temperaturesensor
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

target_include_directories(s1_temperaturedisplay
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

set_property(TARGET s1_temperaturesensor PROPERTY CXX_STANDARD 11)
set_property(TARGET s1_temperaturesensor PROPERTY OUTPUT_NAME "temperaturesensor")

set_property(TARGET s1_temperaturedisplay PROPERTY CXX_STANDARD 11)
set_property(TARGET s1_temperaturedisplay PROPERTY OUTPUT_NAME "temperaturedisplay")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureDisplayProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensorProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example.protobuf
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/TemperatureDisplayThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/TemperatureSensorThingClass.json
        definitions/ThingClass/com.adlinktech.example.protobuf
)

configure_file(start_display.sh start_display.sh COPYONLY)
configure_file(start_sensor.sh start_sensor.sh COPYONLY)

configure_file(start_display.bat start_display.bat COPYONLY)
configure_file(start_sensor.bat start_sensor.bat COPYONLY)
