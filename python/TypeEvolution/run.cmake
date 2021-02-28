include(CMakePrintHelpers)

# loop through CMAKE_ARGn, looking for -P <script-name>.
# Build an argument list from the remaining values
set(_ARGC -1)
set(_ARGV)
math(EXPR _stop "${CMAKE_ARGC} - 1")
foreach(_index RANGE ${_stop})
  if(_ARGC EQUAL -1 AND "${CMAKE_ARGV${_index}}" STREQUAL "-P")
    # -P start counting argments
    set(_ARGC 0)
  elseif(_ARGC EQUAL 0)
    # script name. Ignore
    set(_ARGC 1)
  elseif(_ARGC GREATER 0)
    # record a parameter
    list(APPEND _ARGV "${CMAKE_ARGV${_index}}")
    math(EXPR _ARGC "${_ARGC} + 1")
  endif()
endforeach()

# configure command-line parsing
set(_options)
set(_oneValueArgs DASH)
set(_multiValueArgs SENSORS)
cmake_parse_arguments(RUN "${_options}" "${_oneValueArgs}"
                          "${_multiValueArgs}" ${_ARGV} )

# validate input parameters
if(NOT DEFINED RUN_DASH OR NOT DEFINED RUN_SENSORS)
  message(FATAL_ERROR "Usage: cmake -P run.cmake DASH (gen1|gen2|gen3) SENSORS (gen1|gen2|gen3) (gen1|gen2|gen3) (gen1|gen2|gen3)")
endif()
if(NOT RUN_DASH MATCHES "^gen[123]$")
  message(FATAL_ERROR "DASH parameter must be one of: gen1, gen2 or gen3. Found: ${RUN_DASH}")
endif()
list(LENGTH RUN_SENSORS _length)
if(NOT _length EQUAL 3)
  message(FATAL_ERROR "SENSORS parameter must have three values, each from one of: gen1, gen2 or gen3. Found: ${_length}")
endif()
foreach(_gen ${RUN_SENSORS})
  if(NOT _gen MATCHES "^gen[123]$")
    cmake_print_variables(_gen)
    message(FATAL_ERROR "SENSORS parameter values must be one of: gen1, gen2 or gen3. Found: ${_gen}")
  endif()
endforeach()

set(_base_dir "${CMAKE_CURRENT_LIST_DIR}")

if(CMAKE_HOST_WIN32)
  set(SHELL_EXT ".bat")
else()
  set(SHELL_EXT ".sh")
endif()

set(DASH_COMMAND "${_base_dir}/python_${RUN_DASH}-prefix/src/python_${RUN_DASH}-build/start_dashboard${SHELL_EXT}")

foreach(_index RANGE 0 2)
  list(GET RUN_SENSORS ${_index} _gen)
  math(EXPR _sensor_no "${_index} + 1")
  set(SENSOR${_sensor_no}_COMMAND "${_base_dir}/python_${_gen}-prefix/src/python_${_gen}-build/start_sensor${SHELL_EXT}")
endforeach()

if(CMAKE_HOST_WIN32)
  execute_process(
    COMMAND cmd /c "${DASH_COMMAND}"
    COMMAND cmd /c "${SENSOR1_COMMAND} 1"
    COMMAND cmd /c "${SENSOR2_COMMAND} 2"
    COMMAND cmd /c "${SENSOR3_COMMAND} 3"
  )
else()
  execute_process(
    COMMAND bash -c "${SENSOR1_COMMAND} 1 & ${SENSOR2_COMMAND} 2 & ${SENSOR3_COMMAND} 3 & ${DASH_COMMAND}"
  )
endif()

