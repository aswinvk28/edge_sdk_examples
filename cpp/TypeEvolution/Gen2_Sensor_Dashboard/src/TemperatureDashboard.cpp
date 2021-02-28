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
#include <iomanip>
#include <thread>
#include <chrono>
#include <future>
#include <exception>

#include <Dispatcher.hpp>
#include <IoTDataThing.hpp>
#include <JSonThingAPI.hpp>
#include <thing_IoTData.h>
#include <ThingAPIException.hpp>
#include "definitions/TemperatureTagGroup.dr.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;
using namespace com::adlinktech::example; // generated protobuf classes

#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

#ifdef _WIN32
#define NO_COLOR ""
#define COLOR_GREEN ""
#define COLOR_MAGENTA ""
#define COLOR_GREY ""
#else
#define NO_COLOR "\e[0m"
#define COLOR_GREEN "\e[0;32m"
#define COLOR_MAGENTA "\e[0;35m"
#define COLOR_GREY "\e[0;90m"
#endif

class TemperatureSensorDiscoveredListener : public ThingDiscoveredListener {
    void notifyThingDiscovered(const DiscoveredThing& thing) {
        if (thing.getClassId().getName() == "TemperatureSensor") {
            cout << COLOR_GREEN << "New temperature sensor discovered: " << thing.getDescription()
                << " (" << thing.getId() << ")" << NO_COLOR << endl;
        } else {
            cout << COLOR_GREY << "New incompatible sensor type '"
                << thing.getClassId().getName() << "' discovered (" << thing.getId() << ")" << NO_COLOR << endl;
        }
    }
};

class TemperatureSensorLostListener : public ThingLostListener {
    void notifyThingLost(const DiscoveredThing& thing) {
        if (thing.getClassId().getName() == "TemperatureSensor") {
            cout << COLOR_MAGENTA << "Temperature sensor stopped: " << thing.getDescription()
                << " (" << thing.getId() << ")" << NO_COLOR << endl;
        } else {
            cout << COLOR_GREY << "Other sensor stopped: '"
                << thing.getClassId().getName() << "' (" << thing.getId() << ")" << NO_COLOR << endl;
        }
    }
};

class TemperatureDashboard {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    ThingEx m_thing = createThing();

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Register the Gen1 sensor tag group.
        JSonTagGroupRegistry tgr;
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Register the Gen2 sensor tag group.
        TemperatureHelper::registerWithDataRiver(m_dataRiver);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

public:
    TemperatureDashboard(string thingPropertiesUri) :m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Temperature Dashboard started" << endl;
    }

    ~TemperatureDashboard() {
        m_dataRiver.close();
        cout << "Temperature Dashboard stopped" << endl;
    }

    int run(string floor, int runningTime) {
        auto start = chrono::steady_clock::now();
        long long elapsedSeconds = 0;

        // Add listener for new Things
        auto temperatureSensorDiscoveredListener = TemperatureSensorDiscoveredListener();
        m_dataRiver.addListener(temperatureSensorDiscoveredListener);

        // Add listener for lost Things
        auto temperatureSensorLostListener = TemperatureSensorLostListener();
        m_dataRiver.addListener(temperatureSensorLostListener);

        do {
            VLoanedDataSamples msgs = m_thing.read("temperature",(runningTime - elapsedSeconds) * 1000);

            // Process samples
            for (const VDataSample& msg : msgs) {
                auto flowState = msg.getFlowState();
                if (flowState == FlowState::ALIVE) {
                    // translate everything into a Temperature object.
                    Temperature tempData;
                    std::string sensorGeneration;
                    if(msg.isCompatible(tempData)) {
                        sensorGeneration = "Gen2";
                        msg.get(tempData);
                    } else {
                        sensorGeneration = "Gen1";
                        // unpack the temperature from an IOT_NVP_SEQ
                        const IOT_NVP_SEQ& dataSample = msg.get<IOT_NVP_SEQ>();

                        try {
                            // Get temperature value from sample
                            for (const IOT_NVP& nvp : dataSample) {
                                if (nvp.name() == "temperature") {
                                    tempData.set_temperature(nvp.value().iotv_float32());
                                }
                            }
                        }
                        catch (exception& e) {
                            cerr << "An unexpected error occured while processing data-sample: " << e.what() << endl;
                            continue;
                        }
                    }

                    // Show output
                    cout << "Temperature data received for flow "
                        << msg.getFlowId() << "(" << sensorGeneration << "): "
                        << fixed << setw(3) << setprecision(1)
                        << tempData.temperature() << endl;
                }
            }

            elapsedSeconds = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - start).count();
        } while (elapsedSeconds < runningTime);

        // Remove listeners
        m_dataRiver.removeListener(temperatureSensorLostListener);
        m_dataRiver.removeListener(temperatureSensorDiscoveredListener);

        return 0;
    }
};

int main(int argc, char *argv[]) {
    // Get thing properties URI from command line parameter
    if (argc < 4) {
        cerr << "Usage: " << argv[0] << " THING_PROPERTIES_URI FILTER RUNNING_TIME" << endl;
        exit(1);
    }
    string thingPropertiesUri = string(argv[1]);
    string filter = string(argv[2]);
    int runningTime = atoi(argv[3]);

    try {
        TemperatureDashboard(thingPropertiesUri).run(filter, runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}

