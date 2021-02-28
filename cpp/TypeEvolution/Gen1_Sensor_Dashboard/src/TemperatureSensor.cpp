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
 * This code is part of example scenario 2 'Connect a Dashboard' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

#include <iostream>
#include <thread>
#include <chrono>
#include <random>

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

#define SAMPLE_DELAY_MS 5000

class TemperatureSensor {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    Thing m_thing = createThing();

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        JSonTagGroupRegistry tgr;
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    void writeSample(const float temperature) {
        IOT_VALUE temperature_v;
        temperature_v.iotv_float32(temperature);
        IOT_NVP_SEQ sensorData = {
            IOT_NVP(string("temperature"), temperature_v)
        };

        try {
            m_thing.write("temperature", sensorData);
        }
        catch (InvalidArgumentError& e) {
            cerr << "Error writing data: " << e.what() << endl;
        }
    }

public:
    TemperatureSensor(string thingPropertiesUri) :m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Temperature Sensor started" << endl;
    }

    ~TemperatureSensor() {
        m_dataRiver.close();
        cout << "Temperature Sensor stopped" << endl;
    }

    int run(int runningTime) {
        // init random generator
        std::random_device seed;  //Will be used to obtain a seed for the random number engine
        std::mt19937 gen(seed()); //Standard mersenne_twister_engine seeded with humidity_seed()
        // generate temperature with mean of 20 and a standard deviation of 4.
        std::normal_distribution<> temperature_dist(20.0, 4.0);

        int sampleCount = (runningTime * 1000) / SAMPLE_DELAY_MS;
        float actualTemperature;

        while (sampleCount-- > 0) {
            // Simulate temperature change
            actualTemperature = static_cast<float>(temperature_dist(gen));

            writeSample(actualTemperature);

            this_thread::sleep_for(chrono::milliseconds(SAMPLE_DELAY_MS));
        }

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

    // Create Thing
    try {
        TemperatureSensor(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}
