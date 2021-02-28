# MetricsViewer application

This application demonstrates how to build an application that
retrieves metrics from EdgeSDK-based applications, as they run.

## Introduction to EdgeSDK Metrics

Obtaining metrics is a provisional feature of EdgeSDK 1.7. 

**Note**: All the metric-related APIs used by this application are subject to change or removal.

To enable metric collection on an application, it is sufficient to set the ADLINK_DATARIVER_METRICS environment variable to 'true' prior to starting the application. Enabling metrics in an application SHOULD NOT cause any additional network traffic, unless an application like Metric Viewer is running.

To set the variable on Windows systems:

    set ADLINK_DATARIVER_METRICS=true

To set the variable on Linux/MacOS systems:

    export ADLINK_DATARIVER_METRICS=true

## Using Metric Viewer

The Metric Viewer application has three commands: 'agents', 'metrics' and 'display'. Each is described below.

### Viewing Metric Agents

The 'agents' command allows you to see Metric Agents running on the DataRiver. If an application is started with metrics enabled (by setting ADLINK_DATARIVER_METRICS to 'true'), then one metric agent will be created for each Thing created by the application. Each agent will be identified by the 'Contenxt ID' of a Thing.

To see metric agents on your DataRiver, run:

    metricviewer agents

Will list all agents, one per line.

### Viewing Available Metric information

The 'metrics' command allows you to see which metrics an Agent is capable of publishing.

To see available metrics, run:

    metricviewer metrics

The command expects to receive one or more agent names on its standard input, and will list all the availabe metrics on its
standard output.

You can see all available metrics in the system by using a simple pipe:

    metricviewer agents | metricviewer metrics

You could also capture the agents of interest into a file (say agents.txt), and view their available metrics as follows:

    metricviewer metrics <agents.txt

### Display Metrics

The 'display' command allows you to see values for metrics from
one or more agents.

To display the metric values for all agents in a agents.txt file, you would issue the command:

    metricviewer display < agents.txt

If you are interested in only subset of metrics, you could use a tool such as grep to filter the output:

    metricviewer display < agents.txt | grep num_

This will display only lines containing the text "num_".

Note that the 'display' command will run until the monitored agents terminate. While running, it will print refreshed metric values approximately every second.

## Tutorial: Monitoring the S1_ConnectSensor example

This brief tutorial leads you through the steps of monitoring the S1_ConnectSensor application included with the EdgeSDK.
This tutorial assumes you are using Linux or MacOS.

1. Start a shell to run the example.
2. For the shell, run:

    \# Configure the shell for EdgeSDK use\
    . <EdgeSDK-install-dir>/config_env_variables.com\
    \# build the S1_ConnectSensor application in C++\
    mkdir ~/s1\
    cd ~/s1\
    cmake $EDGE_SDK_HOME/examples/cpp/S1ConnectSensor\
    make

3. Enable metric collection for applications started in the shell

    export ADLINK_DATARIVER_METRICS=true

4. Start a second shell for metric viewing. Configure it and build the metric viewer application:

    \# Configure the shell for EdgeSDK use\
    . <EdgeSDK-install-dir>/config_env_variables.com\
    \# build the MetricViewer application in C++\
    mkdir ~/viewer\
    cd ~/viewer\
    cmake $EDGE_SDK_HOME/examples/cpp/MetricViewer\
    make

5. In the first shell, start the S1_ConnectSensor applications:

    ./start_sensor.sh & ./start_display

6. In the second shell, use metric viewer to agents:

    ./metricviewer agents

7. In the second shell, save the agents to a text file:

    ./metricviewer agents | grep "example1" > S1agents.txt

8. In the second shell, display metrics for the S1 application:

    ./metricviewer display < S1agents.txt

9. Press Ctrl-C if you get bored, otherwise, output will stop once the S1 applications stop.

10. Whenever necessary, in the first shell, restart the applications:

    ./start_sensor.sh & ./start_display

11. In the second shell, experiment with filter metric results. This will show the number of reads, writes and samples read by the applications:

    ./metricviewer < S1agents.txt | grep "num_"


