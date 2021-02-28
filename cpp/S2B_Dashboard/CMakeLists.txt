project(S2B_Dashboard)
cmake_minimum_required(VERSION 3.5)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING
      "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)

find_package(ThingAPI REQUIRED)

add_executable(s2b_temperaturesensor
    src/TemperatureSensor.cpp
)

add_executable(s2b_temperaturedashboard
    src/TemperatureDashboard.cpp
)

target_link_libraries(s2b_temperaturesensor
    ThingAPI::ThingAPI
)

target_link_libraries(s2b_temperaturedashboard
    ThingAPI::ThingAPI
)

set_property(TARGET s2b_temperaturesensor PROPERTY CXX_STANDARD 11)
set_property(TARGET s2b_temperaturesensor PROPERTY OUTPUT_NAME "temperaturesensor")

set_property(TARGET s2b_temperaturedashboard PROPERTY CXX_STANDARD 11)
set_property(TARGET s2b_temperaturedashboard PROPERTY OUTPUT_NAME "temperaturedashboard")

add_custom_target(${PROJECT_NAME}_copy_config_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory config
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureDashboardProperties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensor1Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensor2Properties.json
        ${CMAKE_CURRENT_SOURCE_DIR}/config/TemperatureSensor3Properties.json
        config
)

add_custom_target(${PROJECT_NAME}_copy_definition_files ALL
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json
        definitions/TagGroup/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E make_directory definitions/ThingClass/com.adlinktech.example
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json
        ${CMAKE_CURRENT_SOURCE_DIR}/definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json
        definitions/ThingClass/com.adlinktech.example
)


configure_file(start_dashboard.sh start_dashboard.sh COPYONLY)
configure_file(start_sensor1.sh start_sensor1.sh COPYONLY)
configure_file(start_sensor2.sh start_sensor2.sh COPYONLY)
configure_file(start_sensor3.sh start_sensor3.sh COPYONLY)

configure_file(start_dashboard.bat start_dashboard.bat COPYONLY)
configure_file(start_sensor1.bat start_sensor1.bat COPYONLY)
configure_file(start_sensor2.bat start_sensor2.bat COPYONLY)
configure_file(start_sensor3.bat start_sensor3.bat COPYONLY)

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} DESTINATION examples/cpp
        PATTERN "*.sh"
        PERMISSIONS OWNER_EXECUTE OWNER_WRITE OWNER_READ
                GROUP_EXECUTE GROUP_READ
                WORLD_EXECUTE WORLD_READ)

