Project(python_TypeEvolution)
cmake_minimum_required(VERSION 3.5)

##########################################################
# See the README.md file for a description of this project
##########################################################

if(NOT CMAKE_BUILD_TYPE)
  # No CMAKE_BUILD_TYPE specified, force to Release
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING
      "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)


# Check for cross-compiling, and pass on tool chain information if doing so
if (NOT CMAKE_CROSSCOMPILING)
    set(CROSS_BUILD_TOOLCHAIN_ARG "")
else()
    get_filename_component(CMAKE_TOOLCHAIN_FILE ${CMAKE_TOOLCHAIN_FILE} ABSOLUTE)
    set(CROSS_BUILD_TOOLCHAIN_ARG "-DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}")
endif()


# Using ExternalProject to allow each generation build to be completely independent
# of the other. They can repeat target names, something that would be impossible if
# we used add_subdirectory().
# This keeps the differences between the generators to the actual code changes necessary
# to evolve the application.

include(ExternalProject)

# Configure a child CMake build of the Gen1 Senor_Dashboard
ExternalProject_Add(python_gen1
  SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/Gen1_Sensor_Dashboard
  CMAKE_ARGS
    -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
    -DCMAKE_GENERATOR=${CMAKE_GENERATOR}
    ${CROSS_BUILD_TOOLCHAIN_ARG}
  # don't use timestamps; always invoke the child build
  BUILD_ALWAYS YES
  INSTALL_COMMAND ""
)

# Configure a child CMake build of the Gen1 Senor_Dashboard
ExternalProject_Add(python_gen2
  SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/Gen2_Sensor_Dashboard
  CMAKE_ARGS
    -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
    -DCMAKE_GENERATOR=${CMAKE_GENERATOR}
    ${CROSS_BUILD_TOOLCHAIN_ARG}
  # don't use timestamps; always invoke the child build
  BUILD_ALWAYS YES
  INSTALL_COMMAND ""
)

# Configure a child CMake build of the Gen1 Senor_Dashboard
ExternalProject_Add(python_gen3
  SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/Gen3_Sensor_Dashboard
  CMAKE_ARGS
    -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
    -DCMAKE_GENERATOR=${CMAKE_GENERATOR}
    ${CROSS_BUILD_TOOLCHAIN_ARG}
  # don't use timestamps; always invoke the child build
  BUILD_ALWAYS YES
  INSTALL_COMMAND ""
)

configure_file(run.cmake run.cmake COPYONLY)
