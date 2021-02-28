This folder contains examples on how to use the Edge SDK .NET Core API.

-----------------------------------------------------------------------------------------------------------------------
Examples:

The Edge SDK .NET Core includes a number of examples that can be found 
in directory /examples/dotnet.

- Scenario 1: Connecting a sensor (S1_ConnectSensor)
- Scenario 2: Connect a dashboard (S2_Dashboard)
- Scenario 2A: Connect a dashboard: demonstrates usage of flowId filter (S2A_Dashboard)
- Scenario 3: A derived value processing service (S3_DerivedValue)
- Scenario 4: A gateway service (S4_GatewayService)
- Scenario 5: Dynamic Browsing (S5_DynamicBrowsing)
- ThingThroughput: a throughput-tester application (ThingThroughput)

The 'Edge SDK .NET Core User Guide' (in the /docs directory) gives 
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

1) Initialize the Edge SDK enviornment variables in your shell. Execute:
      . <Installation directory>/EdgeSDK/1.X/config_env_variables
2) Copy a sample to a personal directory, for example:
      cp -r $EDGE_SDK_HOME/examples/dotnet/S1_ConnectSensor ~
3) Change to the copied directory: 
      cd ~/S1_ConnectSensor
4) Publish the .Net solution to a folder, for example:
      dotnet publish -o pub
5) To run the applilcation, change the directory to which you published:
      cd pub/scripts
6) Add execute permission to the scripts. The publish action fails to maintain them:
      chmod +x *.sh
7) Start the Sensor and Display applications via these scripts:
      ./start_sensor.sh & ./start_display.sh

To run the other examples follow the same steps and start the shell 
scripts from a shell where config_env_variables.com has been run.

/**********************************************************************************************************************/
WINDOWS INSTRUCTION:

On Windows, Visual Studio needs to be installed. This needs to be the 
same version of visual studio used to build the Edge SDK, 
which can be determined from the name of the installer.

To run the scenario 1 Example:
0) Ensure the PLATFORM environment variable is the empty string.
      set PLATFORM=
1) Initialize the Edge SDK enviornment variables in your shell. Execute:
      CALL "<Installation directory>\EdgeSDK\1.X\config_env_variables.bat"
2) Copy a sample to a personal directory, for example:
      xcopy /s/e "%EDGE_SDK_HOME%\examples\dotnet\S1_ConnectSensor" "%USERPROFILE%\S1_ConnectoSensor\"
3) Change to the copied directory: 
      cd "%USERPROFILE%\S1_ConnectoSensor"
4) Publish the .Net solution to a folder, for example:
      dotnet publish -o pub
5) To run the applilcation, change the directory to which you published:
      cd pub\scripts
6) Start the Sensor and Display applications via these scripts:
      start_sensor
      start_display

To run the other examples follow the same steps and run the 
executables from a shell where config_env_variables.bat has been run. 
