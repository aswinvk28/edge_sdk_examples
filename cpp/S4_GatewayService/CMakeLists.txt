project(S4_GatewayService)
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

find_package (Threads)

add_executable(s4_camera
    src/Camera.cpp
)

add_executable(s4_lightsensor
    src/LightSensor.cpp
)

add_executable(s4_gatewayservice
    src/GatewayService.cpp
    src/Utils.cpp
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
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/CameraStateTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/IlluminanceAlarmTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/IlluminanceTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/ObservationTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/CameraThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/GatewayServiceThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/LightSensorThingClass.json
        definitions/ThingClass/com.adlinktech.example
)

configure_file(barcodes1.txt barcodes1.txt COPYONLY)
configure_file(barcodes2.txt barcodes2.txt COPYONLY)

configure_file(start_gatewayservice.sh start_gatewayservice.sh COPYONLY)
configure_file(start_station1.sh start_station1.sh COPYONLY)
configure_file(start_station2.sh start_station2.sh COPYONLY)

configure_file(start_gatewayservice.bat start_gatewayservice.bat COPYONLY)
configure_file(start_station1.bat start_station1.bat COPYONLY)
configure_file(start_station2.bat start_station2.bat COPYONLY)
