/*
 *                         ADLINK Edge SDK
 *
 *   This software and documentation are Copyright 2018 to 2020 ADLINK
 *   Technology Limited, its affiliated companies and licensors. All rights
 *   reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

/**
 * This code is part of example scenario 5 'Dynamic Browsing' of the 
 * ADLINK Edge SDK. For a description of this scenario see the 
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation. 
 * 
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory. 
 */

#include <iostream>
#include <thread>
#include <chrono>

#include <IoTDataThing.hpp>
#include <JSonThingAPI.hpp>
#include <thing_IoTData.h>
#include <ThingAPIException.hpp>

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;

#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

#include "include/cxxopts.hpp"
#include "Sensors/TemperatureSensor.cpp"
#include "Sensors/RotationalSpeedSensor.cpp"

static void TemperatureSensorTask(string thingPropertiesUri, int runningTime) {
    try {
        TemperatureSensor(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }
}

static void SpeedSensorTask(string thingPropertiesUri, int runningTime) {
    try {
        RotationalSpeedSensor(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }
}

static void GetCommandLineParameters(int argc, char *argv[],
        string& temperatureSensorThingPropertiesUri, 
        string& speedSensorThingPropertiesUri,
        int& runningTime) {
    try {
        cxxopts::Options options("GeneratorA", "ADLINK ThingSDK Example Generator B");
        options.add_options()
            ("s,speed-sensor", "Rotational Speed Sensor Thing properties URI", cxxopts::value<string>())
            ("t,temp-sensor", "Temperature Sensor Thing properties URI", cxxopts::value<string>())
            ("r,running-time", "Running Time", cxxopts::value<int>())
            ("help", "Print help")
            ;

        auto cmdLineOptions = options.parse(argc, argv);

        if (cmdLineOptions.count("help")) {
            cout << options.help({""}) << endl;
            exit(0);
        }
        if (cmdLineOptions.count("s") == 0 || cmdLineOptions.count("t") == 0) {
            cerr << "Please provide Thing Property URIs for all sensors" << endl;
            cerr << options.help({""}) << endl;
            exit(1);
        }
        
        temperatureSensorThingPropertiesUri = cmdLineOptions["t"].as<string>();
        speedSensorThingPropertiesUri = cmdLineOptions["s"].as<string>();
        runningTime = cmdLineOptions["r"].as<int>();
    }
    catch (exception& e) {
        cerr << "An error occurred while parsing command line parameters: " << e.what() << endl;
        exit(1);
    }
}

int main(int argc, char *argv[]) {
    // Get command line parameters
    string temperatureSensorThingPropertiesUri;
    string speedSensorThingPropertiesUri;
    int runningTime;
    GetCommandLineParameters(argc, argv, temperatureSensorThingPropertiesUri, 
            speedSensorThingPropertiesUri, runningTime);

    // Create threads for Sensors
    thread tTemperatureSensor(TemperatureSensorTask, temperatureSensorThingPropertiesUri, runningTime);
    thread tSpeedSensor(SpeedSensorTask, speedSensorThingPropertiesUri, runningTime);

    tTemperatureSensor.join();
    tSpeedSensor.join();
}
