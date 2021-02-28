project(S3_DerivedValue)
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
    PROTOS ${PROTO_SOURCE_DIR}/LocationTagGroup.proto ${PROTO_SOURCE_DIR}/DistanceTagGroup.proto
    OUT_VAR PB_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

# Compile .proto files for DataRiver C++ extensions
protobuf_generate(LANGUAGE cppdatariver
    PROTOS ${PROTO_SOURCE_DIR}/LocationTagGroup.proto ${PROTO_SOURCE_DIR}/DistanceTagGroup.proto
    OUT_VAR DR_FILES
    ${CPPDATARIVER_EXTENSIONS} # let protoc know what extension are generated
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

add_executable(s3_gpssensor
    src/GpsSensor.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s3_distanceservice
    src/DistanceService.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(s3_dashboard
    src/Dashboard.cpp
    src/Utils.cpp
    ${PB_FILES}
    ${DR_FILES}
)

target_link_libraries(s3_gpssensor
    ThingAPI::ThingAPI
)

target_link_libraries(s3_distanceservice
    ThingAPI::ThingAPI
)

target_link_libraries(s3_dashboard
    ThingAPI::ThingAPI
)

target_include_directories(s3_gpssensor
    PRIVATE ${PROTOC_OUTPUT_DIR}
)
target_include_directories(s3_distanceservice
    PRIVATE ${PROTOC_OUTPUT_DIR}
)
target_include_directories(s3_dashboard
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

set_property(TARGET s3_gpssensor PROPERTY CXX_STANDARD 11)
set_property(TARGET s3_gpssensor PROPERTY OUTPUT_NAME "gpssensor")

set_property(TARGET s3_distanceservice PROPERTY CXX_STANDARD 11)
set_property(TARGET s3_distanceservice PROPERTY OUTPUT_NAME "distanceservice")

set_property(TARGET s3_dashboard PROPERTY CXX_STANDARD 11)
set_property(TARGET s3_dashboard PROPERTY OUTPUT_NAME "dashboard")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/DashboardProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/DistanceServiceProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GpsSensor1Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GpsSensor2Properties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example.protobuf
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/DistanceServiceThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/GpsSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/LocationDashboardThingClass.json
        definitions/ThingClass/com.adlinktech.example.protobuf
)

configure_file(start_dashboard.sh start_dashboard.sh COPYONLY)
configure_file(start_distanceservice.sh start_distanceservice.sh COPYONLY)
configure_file(start_truck1.sh start_truck1.sh COPYONLY)
configure_file(start_truck2.sh start_truck2.sh COPYONLY)

configure_file(start_dashboard.bat start_dashboard.bat COPYONLY)
configure_file(start_distanceservice.bat start_distanceservice.bat COPYONLY)
configure_file(start_truck1.bat start_truck1.bat COPYONLY)
configure_file(start_truck2.bat start_truck2.bat COPYONLY)
