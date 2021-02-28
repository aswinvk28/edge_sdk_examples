project(python_rt_protobuf)
cmake_minimum_required(VERSION 3.5)

find_package(ThingAPI)
get_target_property(THINGAPI_INCLUDE_DIR ThingAPI::ThingAPI INTERFACE_INCLUDE_DIRECTORIES)

# Compile .proto files for Python
set(PROTO_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/definitions)
set(PROTOC_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR})

protobuf_generate(LANGUAGE python
    PROTOS ${PROTO_SOURCE_DIR}/PingTagGroup.proto
    	   ${PROTO_SOURCE_DIR}/PongTagGroup.proto
           ${THINGAPI_INCLUDE_DIR}/adlinktech/datariver/descriptor.proto
    OUT_VAR PB_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
)

protobuf_generate(LANGUAGE pythondatariver
    PROTOS ${PROTO_SOURCE_DIR}/PingTagGroup.proto
    	   ${PROTO_SOURCE_DIR}/PongTagGroup.proto
    OUT_VAR DR_FILES
    PROTOC_OUT_DIR ${PROTOC_OUTPUT_DIR}
    GENERATE_EXTENSIONS "_dr.py"
)

add_custom_target(copy_python_rt_protobuf ALL
    COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR}
    DEPENDS ${PB_FILES} ${DR_FILES}
)
