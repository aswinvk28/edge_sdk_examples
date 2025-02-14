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
 * This code is part of example scenario 1 'Connect a Sensor' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

#include <iostream>
#include <iomanip>
#include <thread>
#include <chrono>
#include <future>
#include <assert.h>
#include <exception>

#include <JSonThingAPI.hpp>
#include <ThingAPIException.hpp>

#include "definitions/TemperatureTagGroup.dr.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;

#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

#define READ_SAMPLE_DELAY 100

class TemperatureDisplay {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    ThingEx m_thing = createThing();

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Register TagGroup using .proto generated types.
        ::com::adlinktech::example::protobuf::TemperatureHelper::registerWithDataRiver(m_dataRiver);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example.protobuf/TemperatureDisplayThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);

        return m_dataRiver.createThing(tp);
    }

public:
    TemperatureDisplay(string thingPropertiesUri) : m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Temperature Display started" << endl;
    }

    ~TemperatureDisplay() {
        m_dataRiver.close();
        cout << "Temperature Display stopped" << endl;
    }

    int run(int runningTime) {
        using ::com::adlinktech::example::protobuf::Temperature;
        auto start = chrono::steady_clock::now();
        long long elapsedSeconds;
        do {
            // Read all data for input 'temperature'
            VLoanedDataSamples msgs = m_thing.read("temperature");

            for (const VDataSample& msg : msgs) {
                auto flowState = msg.getFlowState();
                if (flowState == FlowState::ALIVE) {
                    Temperature dataSample;
                    msg.get(dataSample);
                    float temperature = dataSample.temperature();

                    cout << "Sensor data received: "
                        << fixed << setw(5) << setprecision(1) << temperature << endl;
                }
            }

            // Wait for some time before reading next samples
            this_thread::sleep_for(chrono::milliseconds(READ_SAMPLE_DELAY));

            elapsedSeconds = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - start).count();
        } while (elapsedSeconds < runningTime);

        return 0;
    }
};

int main(int argc, char *argv[]) {
    // Get thing properties URI from command line parameter
    if (argc < 3) {
        cerr << "Usage: " << argv[0] << " THING_PROPERTIES_URI RUNNING_TIME" << endl;
        exit(1);
    }
    string thingPropertiesUri = string(argv[1]);
    int runningTime = atoi(argv[2]);

    try {
        TemperatureDisplay(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}
