project(ThingThroughputProtobuf CXX)
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
    PROTOS ${PROTO_SOURCE_DIR}/ThroughputTagGroup.proto
    OUT_VAR PB_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

# Compile .proto files for DataRiver C++ extensions
protobuf_generate(LANGUAGE cppdatariver
    PROTOS ${PROTO_SOURCE_DIR}/ThroughputTagGroup.proto
    OUT_VAR DR_FILES
    ${CPPDATARIVER_EXTENSIONS} # let protoc know what extension are generated
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

add_executable(throughputgpbwriter
    src/ThroughputWriter.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(throughputgpbreader
    src/ThroughputReader.cpp
    ${PB_FILES}
    ${DR_FILES}
)

target_link_libraries(throughputgpbwriter
    ThingAPI::ThingAPI
)

target_include_directories(throughputgpbwriter
    PRIVATE ${PROTOC_OUTPUT_DIR}
)
target_link_libraries(throughputgpbreader
    ThingAPI::ThingAPI
)

target_include_directories(throughputgpbreader
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

set_property(TARGET throughputgpbwriter PROPERTY CXX_STANDARD 11)
set_property(TARGET throughputgpbreader PROPERTY CXX_STANDARD 11)

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/ThroughputReaderProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/ThroughputWriterProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example.protobuf
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/ThroughputReaderThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/ThroughputWriterThingClass.json
        definitions/ThingClass/com.adlinktech.example.protobuf
)

