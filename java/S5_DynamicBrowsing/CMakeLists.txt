project(Java_S5_DynamicBrowsing LANGUAGES NONE)
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
# Copy ThingAPI libary to this project.
# Required because the .MF files specify Main-Class, meaning all
# class paths must be: a) declared in the .MF file; and,
# b) be relative to the built jar's directory

set(LOCAL_THINGAPI_JAR_NAME thingapi.jar)
# write the local file name into classpath.txt
configure_file(classpath.txt.in classpath.txt)
# create a target to copy from the Edge SDK install to our local location
set(LOCAL_THINGAPI_JAR ${CMAKE_CURRENT_BINARY_DIR}/${LOCAL_THINGAPI_JAR_NAME})
add_custom_target(S5_copy_deps
    ALL
    DEPENDS ${LOCAL_THINGAPI_JAR}
)

# This custom command does the actual copying of the ThingAPI Java jar.
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/thingapi.jar
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_PROPERTY:ThingAPIJar,JAR_FILE> ${LOCAL_THINGAPI_JAR}
)

add_jar(S5_GeneratorALauncher 
        src/generators/GeneratorA.java 
        src/sensors/FuelLevelSensor.java
        src/sensors/TemperatureSensor.java 
        src/sensors/RotationalSpeedSensor.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT generators.GeneratorA
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar
)

add_jar(S5_GeneratorBLauncher
        src/generators/GeneratorB.java
        src/sensors/TemperatureSensor.java
        src/sensors/RotationalSpeedSensor.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT generators.GeneratorB
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar
)

add_jar(S5_ThingBrowserLauncher
        src/thingbrowser/ThingBrowser.java
        src/thingbrowser/ThingBrowserLauncher.java
        src/thingbrowser/NewThingDiscoveredListener.java
        src/thingbrowser/TagGroupNotFoundException.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT thingbrowser.ThingBrowserLauncher
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar ${CMAKE_CURRENT_BINARY_DIR}/jansi-1.18.jar
)

