
# Instructions for building and running this example


## 1.0 About this example

This example allows exploring data throughput of the ThingAPI under different packet sizes
and other parameters.

## 2.0 Building the example

Follow the following steps to build and execute the example.

### 2.1 Start a command shell

The examples are built and executed from command line shells. Start one before proceeding to the next step.

### 2.2 Initialize the EdgeSDK

From the command shell, execute the `config_env_variables` script.

On Linux, use:

	source <path-to-EdgeSDK-install>/config_env_variables.com

On Windows, use:


	call <path-to-EdgeSDK-install>\config_env_variables.bat


### 2.3 Copy the example to a new directory

**It is strongly recommened that you copy this example to a directory outside of EdgeSDK installation.**

On Linux, use the following command:

	cp -r $EDGE_SDK_HOME/examples/dotnet/ThingThroughput <path-to-build-directory>

On Windows, use the following command:

	robocopy /e %EDGE_SDK_HOME%\examples\dotnet\ThingThroughput <path-to-build-directory>

### 2.4 Build the project

The EdgeSDK includes a NuGet package for accessing the ThingAPI. However, this package is
not published on nuget.org. Instead, it ships as part of the EdgeSDK. The examples project
is configured to access package, but the first time the package is used on a machine, it
must be copied to the NuGet global package cache.

The following commands will to build the project, and ensure the the ThingAPI is inthe NuGet global package cache.

On Linux, execute

	dotnet build --source $EDGE_SDK_HOME/dotnet <path-to-build-directory>

On Windows, execute:

	dotnet build --source "%EDGE_SDK_HOME%\dotnet <path-to-build-directory>

### 2.5 Publish the project to a folder

To execute the project, it must be published to a folder. This creates executables that you can run,
as well as works around a problem in the .Net Core 3.0 that prevents console programs started with
the `dotnet run` command from running in the background.

On Linux and Windows, use this command:

	dotnet publish --output <path-to-publish-folder> <path-to-build-directory>

If you are interested in running the example between multiple machines, you can copy the publish folder to
other machines. In order to run the examples, the EdgeSDK must be installed on each machine that will run the example.

### 2.6 Running the example

Typically, you will need to command shells in order run the example: one shell to run the `Reader` program and the other to run the `Writer` program.

As you start each shell, ensure the `config_env_variables` is run prior to starting the example programs.

#### 2.6.1 Starting the Writer application

In a command shell, change to the published folder. 

On Linux, type

	./Writer

On Windows, type:

	Writer

Without parameters, the command will run indefinitely. Ctrl-C can be used to terminate it. Otherwise the `--running-time` parameter along number
of seconds will limit it's execution time.

The `Writer` program will echo its command line parameters, and then start waiting for a `Reader` program to start.

To see other `Writer` parameters, add the `--help` parameter to the command line.

#### 2.6.2 Starting the Reader application


In a command shell, change to the published folder.

On Linux, type

    ./Reader

On Windows, type:

    Reader

The `Reader` application will connect to the Writer, and, once-per-second, output statistics on data received from the `Writer`.
The `Reader` will terminate automatically, once the `Writer` terminates.

To see other `Reader` parameters, add the `--help` parameter to the command line.

