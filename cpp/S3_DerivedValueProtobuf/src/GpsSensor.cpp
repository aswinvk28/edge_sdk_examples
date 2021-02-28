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
 * This code is part of example scenario 3 'Derived Value Service' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */


#include <iostream>
#include <thread>
#include <chrono>

#include <JSonThingAPI.hpp>
#include <ThingAPIException.hpp>

#include "include/cxxopts.hpp"

#include "definitions/LocationTagGroup.dr.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::example::protobuf;

#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

#define MIN_SAMPLE_DELAY_MS 1500

class GpsSensor {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    ThingEx m_thing = createThing();
    float m_truckLat;
    float m_truckLng;

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Register TagGroup using .proto generated types.
        LocationHelper::registerWithDataRiver(m_dataRiver);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example.protobuf/GpsSensorThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    void writeSample(float locationLat, float locationLng, time_t timestamp) {
        // Create IoT data object
        Location sensorData;
        sensorData.mutable_location()->set_latitude(locationLat);
        sensorData.mutable_location()->set_longitude(locationLng);
        sensorData.set_timestamputc(timestamp);

        // Write data to DataRiver
        m_thing.write("location", sensorData);
    }

public:
    GpsSensor(string thingPropertiesUri, float truckLat, float truckLng) :
            m_thingPropertiesUri(thingPropertiesUri),
            m_truckLat(truckLat),
            m_truckLng(truckLng) {
        cout << "GPS Sensor started" << endl;
    }

    ~GpsSensor() {
        m_dataRiver.close();
        cout << "GPS Sensor stopped" << endl;
    }

    int run(int runningTime) {
        srand((unsigned int)time(NULL));
        auto startTimestamp = chrono::steady_clock::now();
        long long elapsedTime;

        do {
            // Simulate location change
            m_truckLat += (float)(rand() % 1000) / 100000.0f;
            m_truckLng += (float)(rand() % 1000) / 100000.0f;

            writeSample(m_truckLat, m_truckLng, time(nullptr));

            // Wait for random interval
            this_thread::sleep_for(chrono::milliseconds(MIN_SAMPLE_DELAY_MS + (rand() % 3000)));

            // Get elapsed time
            elapsedTime = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - startTimestamp).count();
        } while (elapsedTime < runningTime);

        return 0;
    }
};

static void getCommandLineParameters(int argc, char *argv[],
        string& thingPropertiesUri, float& lat, float& lng, int& runningTime) {
    try {
        cxxopts::Options options(argv[0], "ADLINK Edge SDK Example GPS Sensor");
        options.add_options()
            ("t,thing", "Thing properties URI", cxxopts::value<string>())
            ("lat", "Truck start location latitude", cxxopts::value<float>())
            ("lng", "Truck start location longitude", cxxopts::value<float>())
            ("r,running-time", "Running Time", cxxopts::value<int>())
            ("h,help", "Print help");

        auto cmdLineOptions = options.parse(argc, argv);

        if (cmdLineOptions.count("help")) {
            cout << options.help({""}) << endl;
            exit(0);
        }
        if (cmdLineOptions.count("thing") == 0 || cmdLineOptions.count("lat") == 0 || cmdLineOptions.count("lng") == 0) {
            cerr << "Please provide Thing Property URI and warehouse location" << endl;
            cerr << options.help({""}) << endl;
            exit(1);
        }

        thingPropertiesUri = cmdLineOptions["thing"].as<string>();
        lat = cmdLineOptions["lat"].as<float>();
        lng = cmdLineOptions["lng"].as<float>();
        runningTime = cmdLineOptions["r"].as<int>();
    }
    catch (exception& e) {
        cerr << "An error occurred while parsing command line parameters: " << e.what() << endl;
        exit(1);
    }
}

int main(int argc, char *argv[]) {
    // Get command line parameters
    string thingPropertiesUri;
    float truckLat;
    float truckLng;
    int runningTime;
    getCommandLineParameters(argc, argv, thingPropertiesUri, truckLat, truckLng, runningTime);

    try {
        GpsSensor(thingPropertiesUri, truckLat, truckLng).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}
