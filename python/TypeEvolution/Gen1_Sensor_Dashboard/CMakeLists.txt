project(python_TypeEvolution_Dashboard)
cmake_minimum_required(VERSION 3.5)

find_package(ThingAPI)

add_custom_target(copy_python_te ALL
    COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR}
    DEPENDS ${PB_FILES}
)

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} DESTINATION examples/python/TypeEvolution
        PATTERN "*.sh"
        PERMISSIONS OWNER_EXECUTE OWNER_WRITE OWNER_READ
                GROUP_EXECUTE GROUP_READ
                WORLD_EXECUTE WORLD_READ)