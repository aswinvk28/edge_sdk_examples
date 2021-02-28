ABOUT THIS PROJECT
==================

This project attempts to show the evolution of an application over time, and how
it is modified to take advantage of evolving EdgeSDK capabilities, in particular, 
the addition of Google Protobuf support, and to show how Google Protobuf type
evolution can be integrated into an application.

There are currently 3 generations of application built here, each representing
a snapshot of an applicaiton in time:

Gen1 - is the original S2A_Dashboard, very lightly modified. It uses only EdgeSDK 1.3
features. In particular, it describes Temperature using TemperatureTagGroup.json, and
manipulates that data using the com::adlinktech::iot::IOT_NVP_SEQ data type.

A detailed description of changes is found in [gen1_changes.md](gen1_changes.md).

Gen2 - is an upgrade application that takes advantage of Google Protobuf support. A new
temperature sensor is developed that describes the Temperature data using Google Protobuf
.proto file (TemperatureTagGroup.proto), and uses the generated Temperature class to
manipulate data. The Dashboard application is aware of the new sensor, but also aware of
the Gen1 sensor, which may still be present on the datariver. It uses both IOT_NVP_SEQ to
handle Gen1 sensor data, and the new Temperature type to handled Gen2 sensor data.


A detailed description of changes is found in [gen2_changes.md](gen2_changes.md).

Gen3 - represents an upgraded temperature sensor that provides relative humidity as well
as temperature. The Sensor uses a revised TemperatureTagGroup.proto, that has been evolved
according to the Google Protobuf type evolution rules. The Dashboard is evolved to handle
Gen1, Gen2 and Gen3 temperature sensors.

A detailed description of changes is found in [gen3_changes.md](gen3_changes.md).

## Running examples

Once the examples are build, a cmake script `run.cmake` is included to allow you to
experiment with different configurations of dashboard and sensor. The command format is:

    cmake -P run.cmake DASH (gen1|gen2|gen3) SENSORS (gen1|gen2|gen3) (gen1|gen2|gen3) (gen1|gen2|gen3)
    
For example, you can run a Gen3 dashboard with two Gen3 sensors and a Gen1 sensor as follows:

    cmake -P run.cmake DASH gen3 SENSORS gen3 gen3 gen1
