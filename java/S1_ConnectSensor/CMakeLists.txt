project(Java_S1_ConnectSensor LANGUAGES NONE)
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

# Makes the ThingAPI Java interface JAR available to the build 
# Check that the 'ThingAPIJar' target is available.
# If not, use find_package to find ThingAPIJava, which will define it.
if(NOT TARGET ThingAPIJar)
    if("$ENV{EDGE_SDK_HOME}" STREQUAL "")
        message(FATAL_ERROR "Environment variable EDGE_SDK_HOME is not set. Please run config_env_variables.com/bat script to set the environment variables")
    endif()

    find_package(ThingAPIJar REQUIRED PATHS "$ENV{EDGE_SDK_HOME}" NO_DEFAULT_PATH)
endif()

# Download our external dependencies at configuration time
file(DOWNLOAD "https://repo1.maven.org/maven2/commons-cli/commons-cli/1.4/commons-cli-1.4.jar" "${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar" STATUS AC_PACKAGE_STATUS)

# Copy configuration and start scripts to the build directory...
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

# Copy ThingAPI libary to this project.
# Required because the .MF files specify Main-Class, meaning all
# class paths must be: a) declared in the .MF file; and,
# b) be relative to the built jar's directory

set(LOCAL_THINGAPI_JAR_NAME thingapi.jar)
# write the local file name into classpath.txt
configure_file(classpath.txt.in classpath.txt)
# create a target to copy from the Edge SDK install to our local location
set(LOCAL_THINGAPI_JAR ${CMAKE_CURRENT_BINARY_DIR}/${LOCAL_THINGAPI_JAR_NAME})
add_custom_target(S1_copy_deps
    ALL
    DEPENDS ${LOCAL_THINGAPI_JAR}
)

# This custom command does the actual copying of the ThingAPI Java jar.
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/thingapi.jar
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_PROPERTY:ThingAPIJar,JAR_FILE> ${LOCAL_THINGAPI_JAR}
)

# Build a JAR for the Temperature Display application
add_jar(S1_TemperatureDisplayLauncher src/TemperatureDisplay.java src/TemperatureDisplayLauncher.java
	MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT TemperatureDisplayLauncher
	INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar)

# Build a JAR for the Temperature Sensor application
add_jar(S1_TemperatureSensorLauncher src/TemperatureSensor.java src/TemperatureSensorLauncher.java
	MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT TemperatureSensorLauncher
	INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar)

