This folder contains examples on how to use the Edge SDK Python API.

-----------------------------------------------------------------------------------------------------------------------
Examples:

The Edge SDK Python API includes a number of examples that can be found 
in directory /examples/python.

- Scenario 1: Connecting a sensor (S1_ConnectSensor)
- Scenario 2: Connect a dashboard (S2_Dashboard)
- Scenario 2A: Connect a dashboard: demonstrates usage of flowId filter (S2A_Dashboard)
- Scenario 3: A derived value processing service (S3_DerivedValue)
- Scenario 4: A gateway service (S4_GatewayService)
- Scenario 5: Dynamic Browsing (S5_DynamicBrowsing)
- ThingThroughput: a throughput-tester application

There are also versions of the examples making use of Google Protocol Buffers.

The 'Edge SDK Python User Guide' (in the /docs directory) gives 
a more detailed description of these example scenarios.  

-----------------------------------------------------------------------------------------------------------------------
Setup:

1.) Install ADLINK Edge SDK - the default location for the installation is:
  Windows:  C:\Program Files\ADLINK\EdgeSDK\[version]\
  Linux:    /home/[userid]/ADLINK/EdgeSDK/[version]/

2.) Configure environment variables by running config_env_variables.com 
or config_env_variables.bat. This will set OSPL_HOME and EDGE_SDK_HOME.

When running the setup config_env_variables script, a message will 
appear indicating that config_env_variables file must be updated IF a 
variable named release_file is invalid.   

This message appears when :  On EdgeSDK installation, 1 or more 
existing valid OSPL installations(s) are found, AND the OSPL_HOME 
environment variable is not set.

Please see the installation guide for more information on setting the 
release_file variable in config_env_variables file.

3.) For those using the Edge SDK Python API, an additional step
is required.  Install the EdgeSDK Python API wheel file using pip.

The Python api is bundled as Python wheel files 
in the $EDGE_SDK_HOME/python directory.

Multiple wheels are provided, to accomodate different python versions.   
A user can install a wheel that matches their chosen python version.

3.1)  Navigate to the location of Python API wheel files: $EDGE_SDK_HOME/python

3.2)  Select wheel file for desired Python version and install it using pip

     pip install adlinktech_datariver-1.X.X-cp35-cp35m-linux_x86_64.whl


/**********************************************************************************************************************/
LINUX INSTRUCTIONS:

To run the scenario 1 example:
1) Ensure the EgdeSDK environment variables are configured:
    . <Installation directory>/EdgeSDK/1.X/config_env_variables
2) Copy the python example to location of your choice (named directory does not yet exist)
    cp -r $EDGE_SDK_HOME/examples/dotnet/S1_ConnectSensor ~/s1p
3) Change to that directory:
    cd ~/s1p
4) Run the two .sh scripts in command shells where config_env_variables.com has been sourced:
    ./start_sensor.sh & ./start_display.sh

By default these examples applications run for 60 seconds. 

It is also possible to run these example using other Python interpreters where EdgeSDK Python API wheel is installed:
- modify scripts to use the correct python interpreter : <python-interpreter> xxx.py  

To run the other examples follow the same steps and start the .sh 
scripts from a shell where config_env_variables.com has been run.

/**********************************************************************************************************************/
WINDOWS INSTRUCTIONS:

To run the scenario 1 Example:
1) Start a command prompt
2) Run
    "<Installation directory>\EdgeSDK\1.x\config_env_variables.bat"
3) Copy the example to new directory:
   xcopy /s/e "%EDGE_SDK_HOME%\examples\dotnet\S1_ConnectSensor" "%USERPROFILE%\S1_ConnectoSensor\"
4) change to the directory:
   cd "%USERPROFILE%\S1_ConnectoSensor\"

Run the two .bat scripts in command prompts where 
config_env_variables.bat has been run:

start_sensor.bat        # in a command prompt where config_env_variables.bat has been run
start_display.bat       # in a command prompt where config_env_variables.bat has been run

By default these examples applications run for 60 seconds. 

It is also possible to run these example using other Python interpreters where EdgeSDK Python API wheel is installed:
- modify scripts to use the correct python interpreter : <python-interpreter> xxx.py 

To run the other examples follow the same steps and run the 
.bat scripts from a shell where config_env_variables.bat has been run. 

/**********************************************************************************************************************/
ADDITIONAL INSTRUCTIONS, Google Protocol Buffers:

These examples require 'compilation' of .proto files using the Google Protobuf compiler protoc.
You can either do this manually (described in the Python User's Guide), or you can make use
of CMake.

To build use of CMake, follow these steps:

1) Create a directory anywhere on your system, and change to it.
2) Invoke cmake to create make files, then invoke make
   On Linux:
     cmake ${EDGE_SDK_HOME}/examples/python/S1_ConnectSensorProtobuf
     make
   On Windows:
     cmake -G "NMake Makefiles" "%EDGE_SDK_HOME%/examples/python/S1_ConnectSensorProtobuf"
     nmake
3) Run the examples using the provided start_ scripts.

To compile .proto files manually:

1) Copy the example files to a directory of your choice. The directory should not exist
   On Linux:
     cp -r ${EDGE_SDK_HOME}/examples/python/S1_ConnectSensorProtobuf ~/s1p
   On Windows:
     xcopy /s/e "%EDGE_SDK_HOME%/examples/python/S1_ConnectSensorProtobuf" "%USERPROFILE%/s1p"
2) Change to the newly created directory
3) Manually invoke protoc:
   On Linux:
     protoc --python_out=. $EDGE_SDK_HOME/include/adlinktech/datariver/descriptor.proto
     protoc --python_out=. definitions/*.proto
     protoc --pythondatariver_out definitions/*.proto
   On Windows:
     protoc --python_out=. "%EDGE_SDK_HOME%\include\adlinktech\datariver\descriptor.proto"
     protoc --python_out=. "definitions\*.proto"
     protoc --pythondatariver_out "definitions\*.proto"
4) Run the examples using the provided start_ scripts.
