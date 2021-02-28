project(ExampleSecurity)
cmake_minimum_required(VERSION 3.5)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING
      "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)

# make sure we can find the ThingAPI package...
find_package(ThingAPI REQUIRED)

add_executable(security_temperaturesensor
    src/TemperatureSensor.cpp
)

add_executable(security_temperaturedisplay
    src/TemperatureDisplay.cpp
)

target_link_libraries(security_temperaturesensor
    ThingAPI::ThingAPI
)

target_link_libraries(security_temperaturedisplay
    ThingAPI::ThingAPI
)

set_property(TARGET security_temperaturesensor PROPERTY CXX_STANDARD 11)
set_property(TARGET security_temperaturesensor PROPERTY OUTPUT_NAME "temperaturesensor")

set_property(TARGET security_temperaturedisplay PROPERTY CXX_STANDARD 11)
set_property(TARGET security_temperaturedisplay PROPERTY OUTPUT_NAME "temperaturedisplay")

find_program(SECURITY_COMPOSER_TOOL securitycomposer REQUIRED
    PATHS
        $ENV{EDGE_SDK_HOME}/tools
    NO_DEFAULT_PATH
)

file(TO_NATIVE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/config/input/data_river_config.json" DATA_RIVER_CONFIG_PATH)
file(TO_NATIVE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/config/input/sensor_config.json" SENSOR_APP_CONFIG_PATH)
file(TO_NATIVE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/config/input/display_config.json" DISPLAY_APP_CONFIG_PATH)
file(TO_NATIVE_PATH "./config/generated/sensor/" SENSOR_OUTPUT_PATH)
file(TO_NATIVE_PATH "./config/generated/display/" DISPLAY_OUTPUT_PATH)

add_custom_command(
    OUTPUT
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_datariver_config.xml
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_identity_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_identity_priv_key.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_permissions.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/governance.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/identity_ca_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/permissions_ca_cert.pem
    COMMAND ${SECURITY_COMPOSER_TOOL}
        --datariver-config=${DATA_RIVER_CONFIG_PATH}
        --app-config=${SENSOR_APP_CONFIG_PATH}
        --app-name=sensor
        --output-path=${SENSOR_OUTPUT_PATH}
    DEPENDS ${DATA_RIVER_CONFIG_PATH} ${SENSOR_APP_CONFIG_PATH}
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
    MAIN_DEPENDENCY ${DATA_RIVER_CONFIG_PATH}
)

add_custom_target(security_composer_sensor_files ALL
    DEPENDS
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_datariver_config.xml
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_identity_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_identity_priv_key.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/sensor_permissions.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/governance.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/identity_ca_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/sensor/permissions_ca_cert.pem
)

add_custom_command(
    OUTPUT
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_datariver_config.xml
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_identity_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_identity_priv_key.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_permissions.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/governance.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/identity_ca_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/permissions_ca_cert.pem
    COMMAND ${SECURITY_COMPOSER_TOOL}
        --datariver-config=${DATA_RIVER_CONFIG_PATH}
        --app-config=${DISPLAY_APP_CONFIG_PATH}
        --app-name=display
        --output-path=${DISPLAY_OUTPUT_PATH}
    DEPENDS ${DATA_RIVER_CONFIG_PATH} ${DISPLAY_APP_CONFIG_PATH}
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
    MAIN_DEPENDENCY ${DATA_RIVER_CONFIG_PATH}
)

add_custom_target(security_composer_display_files ALL
    DEPENDS
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_datariver_config.xml
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_identity_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_identity_priv_key.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/display_permissions.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/governance.p7s
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/identity_ca_cert.pem
        ${CMAKE_CURRENT_BINARY_DIR}/config/generated/display/permissions_ca_cert.pem
)

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

configure_file(start_display.sh start_display.sh COPYONLY)
configure_file(start_sensor.sh start_sensor.sh COPYONLY)

configure_file(start_display.bat start_display.bat COPYONLY)
configure_file(start_sensor.bat start_sensor.bat COPYONLY)
