project(S5_DynamicBrowsing)
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

add_executable(s5_generator_a
    src/GeneratorA.cpp
    src/Sensors/FuelLevelSensor.cpp
    src/Sensors/RotationalSpeedSensor.cpp
    src/Sensors/TemperatureSensor.cpp
)

add_executable(s5_generator_b
    src/GeneratorB.cpp
    src/Sensors/RotationalSpeedSensor.cpp
    src/Sensors/TemperatureSensor.cpp
)

add_executable(s5_thingbrowser
    src/ThingBrowser.cpp
    src/Utils.cpp
)

target_link_libraries(s5_generator_a
    ThingAPI::ThingAPI
    ${CMAKE_THREAD_LIBS_INIT}
)

target_link_libraries(s5_generator_b
    ThingAPI::ThingAPI
    ${CMAKE_THREAD_LIBS_INIT}
)

target_link_libraries(s5_thingbrowser
    ThingAPI::ThingAPI
)

set_property(TARGET s5_generator_a PROPERTY CXX_STANDARD 11)
set_property(TARGET s5_generator_a PROPERTY OUTPUT_NAME "generator_a")

set_property(TARGET s5_generator_b PROPERTY CXX_STANDARD 11)
set_property(TARGET s5_generator_b PROPERTY OUTPUT_NAME "generator_b")

set_property(TARGET s5_thingbrowser PROPERTY CXX_STANDARD 11)
set_property(TARGET s5_thingbrowser PROPERTY OUTPUT_NAME "thingbrowser")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config/GeneratorA
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorA/FuelLevelSensorProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorA/RotationalSpeedSensorProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorA/TemperatureSensorProperties.json
        config/GeneratorA
    COMMAND ${CMAKE_COMMAND} -E make_directory config/GeneratorB
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorB/RotationalSpeedSensorProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GeneratorB/TemperatureSensorProperties.json
        config/GeneratorB
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/ThingBrowserProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/FuelLevelTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/RotationalSpeedTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/SensorStateTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/FuelLevelSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/RotationalSpeedSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/ThingBrowserThingClass.json
        definitions/ThingClass/com.adlinktech.example
)

configure_file(start_thingbrowser.sh start_thingbrowser.sh COPYONLY)
configure_file(start_generator_a.sh start_generator_a.sh COPYONLY)
configure_file(start_generator_b.sh start_generator_b.sh COPYONLY)

configure_file(start_thingbrowser.bat start_thingbrowser.bat COPYONLY)
configure_file(start_generator_a.bat start_generator_a.bat COPYONLY)
configure_file(start_generator_b.bat start_generator_b.bat COPYONLY)
