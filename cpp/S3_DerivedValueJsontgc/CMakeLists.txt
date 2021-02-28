project(S3_DerivedValue)
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

add_executable(s3_gpssensor
    src/GpsSensor.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Location.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Coordinates.cpp
)

add_executable(s3_distanceservice
    src/DistanceService.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Distance.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Location.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Coordinates.cpp
)

add_executable(s3_dashboard
    src/Dashboard.cpp
    src/Utils.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Distance.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Location.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Coordinates.cpp
)

target_include_directories(s3_gpssensor
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/include
)

target_include_directories(s3_distanceservice
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/include
)

target_include_directories(s3_dashboard
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/include
)

target_link_libraries(s3_gpssensor
    ThingAPI::ThingAPI
)

target_link_libraries(s3_distanceservice
    ThingAPI::ThingAPI
)

target_link_libraries(s3_dashboard
    ThingAPI::ThingAPI
)

set_property(TARGET s3_gpssensor PROPERTY CXX_STANDARD 11)
set_property(TARGET s3_gpssensor PROPERTY OUTPUT_NAME "gpssensor")

set_property(TARGET s3_distanceservice PROPERTY CXX_STANDARD 11)
set_property(TARGET s3_distanceservice PROPERTY OUTPUT_NAME "distanceservice")

set_property(TARGET s3_dashboard PROPERTY CXX_STANDARD 11)
set_property(TARGET s3_dashboard PROPERTY OUTPUT_NAME "dashboard")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/DashboardProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/DistanceServiceProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GpsSensor1Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/GpsSensor2Properties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/DistanceTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/LocationTagGroup.json
        definitions
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/DistanceServiceThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/GpsSensorThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/LocationDashboardThingClass.json
        definitions/ThingClass/com.adlinktech.example
)

add_custom_command(
    OUTPUT
        ${CMAKE_CURRENT_BINARY_DIR}/include/com.adlinktech.example/Distance.h
        ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Distance.cpp
    COMMAND ${JSONTGC} -k ${CMAKE_CURRENT_SOURCE_DIR}/definitions/DistanceTagGroup.json -o ${CMAKE_CURRENT_BINARY_DIR}
    MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/definitions/DistanceTagGroup.json
)

add_custom_command(
    OUTPUT
        ${CMAKE_CURRENT_BINARY_DIR}/include/com.adlinktech.example/Location.h
        ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Location.cpp
        ${CMAKE_CURRENT_BINARY_DIR}/include/com.adlinktech.example/Coordinates.h
        ${CMAKE_CURRENT_BINARY_DIR}/src-gen/com.adlinktech.example/Coordinates.cpp
    COMMAND ${JSONTGC} -k ${CMAKE_CURRENT_SOURCE_DIR}/definitions/LocationTagGroup.json -o ${CMAKE_CURRENT_BINARY_DIR}
    MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/definitions/LocationTagGroup.json
)

configure_file(start_dashboard.sh start_dashboard.sh COPYONLY)
configure_file(start_distanceservice.sh start_distanceservice.sh COPYONLY)
configure_file(start_truck1.sh start_truck1.sh COPYONLY)
configure_file(start_truck2.sh start_truck2.sh COPYONLY)

configure_file(start_dashboard.bat start_dashboard.bat COPYONLY)
configure_file(start_distanceservice.bat start_distanceservice.bat COPYONLY)
configure_file(start_truck1.bat start_truck1.bat COPYONLY)
configure_file(start_truck2.bat start_truck2.bat COPYONLY)
