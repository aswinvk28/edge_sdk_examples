project(Java_ThingThroughput LANGUAGES NONE)
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

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/ThroughputReaderProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/ThroughputWriterProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/ThroughputTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/ThroughputReaderThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/ThroughputWriterThingClass.json
        definitions/ThingClass/com.adlinktech.example
)

# Copy ThingAPI libary to this project.
# Required because the .MF files specify Main-Class, meaning all
# class paths must be: a) declared in the .MF file; and,
# b) be relative to the built jar's directory

set(LOCAL_THINGAPI_JAR_NAME thingapi.jar)
# write the local file name into classpath.txt
configure_file(classpath.txt.in classpath.txt)
# create a target to copy from the Edge SDK install to our local location
set(LOCAL_THINGAPI_JAR ${CMAKE_CURRENT_BINARY_DIR}/${LOCAL_THINGAPI_JAR_NAME})
add_custom_target(Throughput_copy_deps
    ALL
    DEPENDS ${LOCAL_THINGAPI_JAR}
)

# This custom command does the actual copying of the ThingAPI Java jar.
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/thingapi.jar
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_PROPERTY:ThingAPIJar,JAR_FILE> ${LOCAL_THINGAPI_JAR}
)

# Command to create target "throughput_reader.jar"
add_jar(throughput_reader 
        src/reader/ThroughputReader.java
        src/reader/ThroughputReaderLauncher.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT reader.ThroughputReaderLauncher
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar
)

# Command to create target "throughput_writer.jar"
add_jar(throughput_writer
        src/writer/ThroughputWriter.java
        src/writer/ThroughputWriterLauncher.java
        src/writer/WriterMode.java
    MANIFEST ${CMAKE_CURRENT_BINARY_DIR}/classpath.txt
    ENTRY_POINT writer.ThroughputWriterLauncher
    INCLUDE_JARS ThingAPIJar ${CMAKE_CURRENT_BINARY_DIR}/commons-cli-1.4.jar
)

