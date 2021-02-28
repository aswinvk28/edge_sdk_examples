project(Sensor_Dashboard)
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


add_executable(temperaturesensor
    src/TemperatureSensor.cpp
    ${PB_FILES} ${DR_FILES}
)

add_executable(temperaturedashboard
    src/TemperatureDashboard.cpp
    ${PB_FILES} ${DR_FILES}
)

target_link_libraries(temperaturesensor
    ThingAPI::ThingAPI
)

target_link_libraries(temperaturedashboard
    ThingAPI::ThingAPI
)

target_include_directories(temperaturesensor
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

target_include_directories(temperaturedashboard
    PRIVATE ${PROTOC_OUTPUT_DIR}
)


set_property(TARGET temperaturesensor PROPERTY CXX_STANDARD 11)
set_property(TARGET temperaturesensor PROPERTY OUTPUT_NAME "temperaturesensor")

set_property(TARGET temperaturedashboard PROPERTY CXX_STANDARD 11)
set_property(TARGET temperaturedashboard PROPERTY OUTPUT_NAME "temperaturedashboard")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureDashboardProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensor1Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensor2Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensor3Properties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json
        definitions/ThingClass/com.adlinktech.example
)


configure_file(start_dashboard.sh start_dashboard.sh COPYONLY)
configure_file(start_sensor.sh start_sensor.sh COPYONLY)

configure_file(start_dashboard.bat start_dashboard.bat COPYONLY)
configure_file(start_sensor.bat start_sensor.bat COPYONLY)
