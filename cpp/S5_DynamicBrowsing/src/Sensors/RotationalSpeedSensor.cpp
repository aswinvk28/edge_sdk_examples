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

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;

#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

#define SPEED_SAMPLE_DELAY_MS 3000

class RotationalSpeedSensor {
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
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/RotationalSpeedTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/RotationalSpeedSensorThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    void writeSample(int speed, int lastHourMin, int lastHourMax, float lastHourAverage) {
        IOT_VALUE speed_v;
        speed_v.iotv_uint32(speed);

        IOT_VALUE lastHourMin_v;
        lastHourMin_v.iotv_uint32(lastHourMin);

        IOT_VALUE lastHourMax_v;
        lastHourMax_v.iotv_uint32(lastHourMax);

        IOT_VALUE lastHourAverage_v;
        lastHourAverage_v.iotv_float32(lastHourAverage);

        IOT_NVP_SEQ data = {
            IOT_NVP(string("speed"), speed_v),
            IOT_NVP(string("lastHourMin"), lastHourMin_v),
            IOT_NVP(string("lastHourMax"), lastHourMax_v),
            IOT_NVP(string("lastHourAverage"), lastHourAverage_v)
        };

        m_thing.write("rotationalSpeed", data);
    }

public:
    RotationalSpeedSensor(string thingPropertiesUri) :m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Rotational Speed Sensor started" << endl;
    }

    ~RotationalSpeedSensor() {
        m_dataRiver.close();
        cout << "Rotational Speed Sensor stopped" << endl;
    }

    int run(int runningTime) {
        srand((unsigned int)time(NULL));
        int sampleCount = (runningTime * 1000) / SPEED_SAMPLE_DELAY_MS;
        int speed = 1000;

        while (sampleCount-- > 0) {
            // Simulate speed change
            speed += (rand() % 10) - 4;

            writeSample(speed, 0, 0, 0.0f);

            this_thread::sleep_for(chrono::milliseconds(SPEED_SAMPLE_DELAY_MS));
        }

        return 0;
    }
};
