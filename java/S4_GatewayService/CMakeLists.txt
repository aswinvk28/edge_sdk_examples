project(Java_S4_GatewayService LANGUAGES NONE)
cmake_minimum_required(VERSION 3.5)

# work around old CMake version that do not know that javah disappeared in Java 10
if(CMAKE_VERSION VERSION_LESS 3.11.2)
   # This program does not need javah. However, older versions of CMAKE will fail on find_package(Java) if
   # the Java version found is Java 10 or later.
   # Work around this by setting the following variable:
   set(Java_JAVAH_EXECUTABLE "javah" CACHE FILEPATH "")
endif()

find_package(Java REQUIRED)
include(UseJava)

# Ensure the ThingAPIJar target is available 
# If not, use find_package to find ThingAPIJava, which will define it.
if(NOT TARGET ThingAPIJar)
    # Not found, ensure EDGE_SDK_HOME is set...
    if("$ENV{EDGE_SDK_HOME}" STREQUAL "")
        message(FATAL_ERROR "Environment variable EDGE_SDK_HOME is not set. Please run config_env_variables.com/bat script to set the environment variables")
    endif()
    # Use find_package to find and define the target.
    find_package(ThingAPIJar REQUIRED PATHS "$ENV{EDGE_SDK_HOME}" NO_DEFAULT_PATH)
endif()

# Download our external dependencies at configuration time
file(DOWNLOAD "https://repo1.maven.org/maven2/commons-cli/commons-cli/1.4/commons-cli-1.4.jar" "${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar" STATUS AC_PACKAGE_STATUS)
file(DOWNLOAD "https://repo1.maven.org/maven2/org/fusesource/jansi/jansi/1.18/jansi-1.18.jar" "${CMAKE_CURRENT_BINARY_DIR}/jansi-1.18.jar" STATUS AC_PACKAGE_STATUS)

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

# Copy ThingAPI libary to this project.
# Required because the .MF files specify Main-Class, meaning all
# class paths must be: a) declared in the .MF file; and,
# b) be relative to the built jar's directory

set(LOCAL_THINGAPI_JAR_NAME thingapi.jar)
# write the local file name into classpath.txt
configure_file(classpath.txt.in classpath.txt)
# create a target to copy from the Edge SDK install to our local location
set(LOCAL_THINGAPI_JAR ${CMAKE_CURRENT_BINARY_DIR}/${LOCAL_THINGAPI_JAR_NAME})
add_custom_target(S4_copy_deps
    ALL
    DEPENDS ${LOCAL_THINGAPI_JAR}
)

# This custom command does the actual copying of the ThingAPI Java jar.
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/thingapi.jar
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_PROPERTY:ThingAPIJar,JAR_FILE> ${LOCAL_THINGAPI_JAR}
)

# Command to create target "S4_CameraLauncher.jar"
add_jar(S4_CameraLauncher 
        src/camera/Camera.java
        src/camera/CameraLauncher.java
        src/camera/CameraThingDiscoveredListener.java
        src/camera/CameraThingLostListener.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT camera.CameraLauncher
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar
)

# Command to create target "S4_LightSensorLauncher.jar"
add_jar(S4_LightSensorLauncher
        src/lightsensor/LightSensor.java
        src/lightsensor/LightSensorLauncher.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT lightsensor.LightSensorLauncher
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar
)

# Command to create target "S4_GatewayServiceLauncher.jar"
add_jar(S4_GatewayServiceLauncher
        src/gatewayservice/GatewayService.java
        src/gatewayservice/GatewayServiceLauncher.java
        src/gatewayservice/NewThingDiscoveredListener.java
        src/gatewayservice/DataFlowKey.java
        src/gatewayservice/DataFlowValue.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT gatewayservice.GatewayServiceLauncher
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar ${CMAKE_CURRENT_BINARY_DIR}/jansi-1.18.jar
)
