project(S1_ConnectSensor)
cmake_minimum_required(VERSION 3.5)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING
      "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)

# make sure we can find the ThingAPI package...
if(NOT TARGET ThingAPI::ThingAPI)
	if("$ENV{EDGE_SDK_HOME}" STREQUAL "")
	    message(FATAL_ERROR "Environment variable EDGE_SDK_HOME is not set. Please run config_env_variables.com/bat script to set the environment variables")
	endif()

    file(TO_CMAKE_PATH "$ENV{EDGE_SDK_HOME}" EDGE_SDK_HOME_PATH)
	list(APPEND CMAKE_MODULE_PATH "${EDGE_SDK_HOME_PATH}/cmake")

	find_package(ThingAPI REQUIRED)
endif()

find_program(JSONTGC jsontgc)

add_executable(s1_temperaturesensor
    src/TemperatureSensor.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Temperature.cpp
)

add_executable(s1_temperaturedisplay
    src/TemperatureDisplay.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Temperature.cpp
)

target_include_directories(s1_temperaturesensor
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/include
)

target_include_directories(s1_temperaturedisplay
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/include
)

target_link_libraries(s1_temperaturesensor
    ThingAPI::ThingAPI
)

target_link_libraries(s1_temperaturedisplay
    ThingAPI::ThingAPI
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
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureDisplayThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json
        definitions/ThingClass/com.adlinktech.example
)

add_custom_command(
    OUTPUT
        ${CMAKE_CURRENT_BINARY_DIR}/include/com.adlinktech.example/Temperature.h
        ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Temperature.cpp
    COMMAND ${JSONTGC} -k ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json -o ${CMAKE_CURRENT_BINARY_DIR}
    MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json
)

configure_file(start_display.sh start_display.sh COPYONLY)
configure_file(start_sensor.sh start_sensor.sh COPYONLY)

configure_file(start_display.bat start_display.bat COPYONLY)
configure_file(start_sensor.bat start_sensor.bat COPYONLY)
