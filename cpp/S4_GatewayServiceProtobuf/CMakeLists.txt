project(S4_GatewayService_Protobuf)
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
    PROTOS ${PROTO_SOURCE_DIR}/CameraStateTagGroup.proto ${PROTO_SOURCE_DIR}/IlluminanceAlarmTagGroup.proto ${PROTO_SOURCE_DIR}/IlluminanceTagGroup.proto ${PROTO_SOURCE_DIR}/ObservationTagGroup.proto 
    OUT_VAR PB_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

# Compile .proto files for DataRiver C++ extensions
protobuf_generate(LANGUAGE cppdatariver
    PROTOS ${PROTO_SOURCE_DIR}/CameraStateTagGroup.proto ${PROTO_SOURCE_DIR}/IlluminanceAlarmTagGroup.proto ${PROTO_SOURCE_DIR}/IlluminanceTagGroup.proto ${PROTO_SOURCE_DIR}/ObservationTagGroup.proto
    OUT_VAR DR_FILES
    ${CPPDATARIVER_EXTENSIONS} # let protoc know what extension are generated
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

add_executable(s4_camera
    src/Camera.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s4_lightsensor
    src/LightSensor.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s4_gatewayservice
    src/GatewayService.cpp
    src/Utils.cpp
    ${PB_FILES}
    ${DR_FILES}
)

target_link_libraries(s4_camera
    ThingAPI::ThingAPI
    ${CMAKE_THREAD_LIBS_INIT}
)

target_link_libraries(s4_lightsensor
    ThingAPI::ThingAPI
    ${CMAKE_THREAD_LIBS_INIT}
)

target_link_libraries(s4_gatewayservice
    ThingAPI::ThingAPI
)

target_include_directories(s4_camera
    PRIVATE ${PROTOC_OUTPUT_DIR}
)
target_include_directories(s4_lightsensor
    PRIVATE ${PROTOC_OUTPUT_DIR}
)
target_include_directories(s4_gatewayservice
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

set_property(TARGET s4_camera PROPERTY CXX_STANDARD 11)
set_property(TARGET s4_camera PROPERTY OUTPUT_NAME "camera")

set_property(TARGET s4_lightsensor PROPERTY CXX_STANDARD 11)
set_property(TARGET s4_lightsensor PROPERTY OUTPUT_NAME "lightsensor")

set_property(TARGET s4_gatewayservice PROPERTY CXX_STANDARD 11)
set_property(TARGET s4_gatewayservice PROPERTY OUTPUT_NAME "gatewayservice")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config/Station1
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/Station1/Camera1Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/Station1/LightSensorProperties.json
        config/Station1
    COMMAND ${CMAKE_COMMAND} -E make_directory config/Station2
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/Station2/Camera1Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/Station2/Camera2Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/Station2/LightSensorProperties.json
        config/Station2
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GatewayServiceProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example.protobuf
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/CameraThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/GatewayServiceThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/LightSensorThingClass.json
        definitions/ThingClass/com.adlinktech.example.protobuf
)

configure_file(barcodes1.txt barcodes1.txt COPYONLY)
configure_file(barcodes2.txt barcodes2.txt COPYONLY)

configure_file(start_gatewayservice.sh start_gatewayservice.sh COPYONLY)
configure_file(start_station1.sh start_station1.sh COPYONLY)
configure_file(start_station2.sh start_station2.sh COPYONLY)

configure_file(start_gatewayservice.bat start_gatewayservice.bat COPYONLY)
configure_file(start_station1.bat start_station1.bat COPYONLY)
configure_file(start_station2.bat start_station2.bat COPYONLY)
