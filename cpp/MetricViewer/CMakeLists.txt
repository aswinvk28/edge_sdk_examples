project(Metrics)
cmake_minimum_required(VERSION 3.5)

find_package(ThingAPI REQUIRED)

add_executable(metricviewer
    src/metricviewer.cpp
)
target_link_libraries(metricviewer
    ThingAPI::ThingAPI
)
set_property(TARGET metricviewer PROPERTY CXX_STANDARD 11)

