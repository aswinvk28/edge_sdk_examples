This folder contains examples on how to use the Edge SDK Java API.

-----------------------------------------------------------------------------------------------------------------------
Examples:

The Edge SDK Java includes a number of examples that can be found 
in directory /examples/java.

- Scenario 1: Connecting a sensor (S1_ConnectSensor)
- Scenario 2: Connect a dashboard (S2_Dashboard)
- Scenario 2A: Connect a dashboard: demonstrates usage of flowId filter (S2A_Dashboard)
- Scenario 3: A derived value processing service (S3_DerivedValue)
- Scenario 4: A gateway service (S4_GatewayService)
- Scenario 5: Dynamic Browsing (S5_DynamicBrowsing)
- ThingThroughput: a throughput-tester application

The 'Edge SDK Java User Guide' (in the /docs directory) gives 
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


/**********************************************************************************************************************/
LINUX INSTRUCTION:

To run the scenario 1 example:
1) cd <Installation directory>/EdgeSDK/1.X
2) . config_env_variables.com
3) cd to any directory, for example: cd /home/userid/EdgeSDK_Example_S1/
4) mkdir bld
5) cd bld
6) cmake $EDGE_SDK_HOME/examples/java/S1_ConnectSensor
7) make

Run the two jar files produced in shells where 
config_env_variables.com has been sourced, by using these shell scripts:

./start_sensor.sh    # in a shell where . config_env_variables.com has been sourced
./start_display.sh   # in another shell where . config_env_variables.com has been sourced

The -DCMAKE_BUILD_TYPE=Release flag is set by default. To change the flag to Debug run: 
cmake -DCMAKE_BUILD_TYPE=Debug $EDGE_SDK_HOME/examples/java/S1_ConnectSensor

By default these examples applications run for 60 seconds when started via the shell script. 
Using a command line parameter a different value can be used, e.g.: 

./start_sensor.sh 200    # sensor Thing runs for 200 seconds


To run the other examples follow the same steps and start the shell 
scripts from a shell where config_env_variables.com has been run.

/**********************************************************************************************************************/
WINDOWS INSTRUCTION:

On Windows, Visual Studio needs to be installed. This needs to be the 
same version of visual studio used to build the Edge SDK, 
which can be determined from the name of the installer.

To run the scenario 1 Example:
1) Open Visual Studio developer x64 Native Tools Command Prompt as Administrator, 
   or start a command prompt as Administrator 
   and run <Path to your VS installation>\vcvars64.bat
2) cd "<Installation directory>\EdgeSDK\1.X"
3) config_env_variables.bat
4) cd to any directory, for example, cd Desktop\EdgeSDK_Example_S1\
5) mkdir bld
6) cd bld
7) cmake -G"NMake Makefiles" -DCMAKE_BUILD_TYPE=Release "%EDGE_SDK_HOME%\examples\java\S1_ConnectSensor"
8) nmake


Run the two jar files produced in a command prompt where 
config_env_variables.com has been sourced, by using these batch files:

start_sensor.bat        # in a command prompt where config_env_variables.bat has been run
start_display.bat       # in a command prompt where config_env_variables.bat has been run

The -DCMAKE_BUILD_TYPE=Release flag is critical. If this is not 
included, the example will build, but will not run.

To run the other examples follow the same steps and run the 
jar files from a shell where config_env_variables.bat has been run. 
