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

#define LIGHT_SAMPLE_DELAY_MS 1000
#define ILLUMINANCE_THRESHOLD 400

class LightSensor {
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
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/IlluminanceTagGroup.json");
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/IlluminanceAlarmTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/LightSensorThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    void writeSample(unsigned int illuminance) {
        IOT_VALUE illuminance_v;
        illuminance_v.iotv_uint32(illuminance);
        IOT_NVP_SEQ sensorData = {
            IOT_NVP(string("illuminance"), illuminance_v)
        };

        m_thing.write("illuminance", sensorData);
    }

    void alarm(string message) {
        IOT_VALUE alarm_v;
        alarm_v.iotv_string(message);
        IOT_NVP_SEQ alarmData = {
            IOT_NVP(string("alarm"), alarm_v)
        };

        m_thing.write("alarm", alarmData);
    }

public:
    LightSensor(string thingPropertiesUri) :m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Light Sensor started" << endl;
    }

    ~LightSensor() {
        m_dataRiver.close();
        cout << "Light Sensor stopped" << endl;
    }

    int run(int runningTime) {
        int sampleCount = (runningTime * 1000) / LIGHT_SAMPLE_DELAY_MS;
        unsigned int actualIlluminance = 500;
        bool alarmState = false;

        while (sampleCount-- > 0) {
            // Simulate illuminance change
            int dir = (sampleCount % 20) > 10 ? -1 : 1;
            actualIlluminance += dir * 30;

            // Write sensor data to river
            writeSample(actualIlluminance);

            // Write alarm if value below threshold
            if (!alarmState && actualIlluminance < ILLUMINANCE_THRESHOLD) {
                alarm("Illuminance below threshold");
                alarmState = true;
            } else if (alarmState && actualIlluminance > ILLUMINANCE_THRESHOLD) {
                alarmState = false;
            }

            this_thread::sleep_for(chrono::milliseconds(LIGHT_SAMPLE_DELAY_MS));
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

    try {
        LightSensor(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}

