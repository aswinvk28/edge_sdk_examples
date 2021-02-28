project(python_TypeEvolution_Dashboard)
cmake_minimum_required(VERSION 3.5)

find_package(ThingAPI)
get_target_property(THINGAPI_INCLUDE_DIR ThingAPI::ThingAPI INTERFACE_INCLUDE_DIRECTORIES)

# Compile .proto files for Python
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

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/TypeEvolution DESTINATION examples/python
        PATTERN "*.sh"
        PERMISSIONS OWNER_EXECUTE OWNER_WRITE OWNER_READ
                GROUP_EXECUTE GROUP_READ
                WORLD_EXECUTE WORLD_READ)