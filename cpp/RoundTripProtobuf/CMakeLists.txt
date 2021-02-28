project(RoundTrip_Protobuf)
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
    PROTOS ${PROTO_SOURCE_DIR}/PingTagGroup.proto ${PROTO_SOURCE_DIR}/PongTagGroup.proto
    OUT_VAR PB_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

# Compile .proto files for DataRiver C++ extensions
protobuf_generate(LANGUAGE cppdatariver
    PROTOS ${PROTO_SOURCE_DIR}/PingTagGroup.proto ${PROTO_SOURCE_DIR}/PongTagGroup.proto
    OUT_VAR DR_FILES
    ${CPPDATARIVER_EXTENSIONS} # let protoc know what extension are generated
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

add_executable(ping
    src/ping.cpp
    ${PB_FILES}
    ${DR_FILES}
)

add_executable(pong
    src/pong.cpp
    ${PB_FILES}
    ${DR_FILES}
)

target_link_libraries(ping
    ThingAPI::ThingAPI
)

target_link_libraries(pong
    ThingAPI::ThingAPI
)

target_include_directories(ping
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

target_include_directories(pong
    PRIVATE ${PROTOC_OUTPUT_DIR}
)

set_property(TARGET ping PROPERTY CXX_STANDARD 11)
set_property(TARGET ping PROPERTY OUTPUT_NAME "ping")

set_property(TARGET pong PROPERTY CXX_STANDARD 11)
set_property(TARGET pong PROPERTY OUTPUT_NAME "pong")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/PingProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/PongProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example.protobuf
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/PingThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example.protobuf/PongThingClass.json
        definitions/ThingClass/com.adlinktech.example.protobuf
)


