project(RoundTrip)
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

add_executable(ping
    src/ping.cpp
)

add_executable(pong
    src/pong.cpp
)

target_link_libraries(ping
    ThingAPI::ThingAPI
)

target_link_libraries(pong
    ThingAPI::ThingAPI
)

set_property(TARGET ping PROPERTY CXX_STANDARD 11)
set_property(TARGET pong PROPERTY CXX_STANDARD 11)

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/PingProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/PongProperties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/PingTagGroup.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/PongTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/PingThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/PongThingClass.json
        definitions/ThingClass/com.adlinktech.example
)

