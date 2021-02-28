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
#include <iomanip>
#include <thread>
#include <chrono>
#include <future>
#include <map>
#include <functional>
#include <sstream>

#include <Dispatcher.hpp>
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

#define READ_DELAY 500
#define CONSOLE_LINE_UP "\x1b[A"
#define COLOR_GREEN "\x1b[32m"
#define COLOR_MAGENTA "\x1b[35m"
#define COLOR_GREY "\x1b[90m"
#define NO_COLOR "\x1b[0m"

// Import util functions
extern string truncate(string str, size_t width);
#ifdef _WIN32
extern bool setConsoleMode();
#endif


using minutes = chrono::duration<float, ratio<60> >;
constexpr auto float_min = numeric_limits<float>::min();

struct TruckDataValue {
    float lat = float_min;
    float lng = float_min;
    time_t locationUpdateTime = 0;

    double distance = float_min;
    minutes eta = minutes(float_min);
    time_t positionUpdateTime = 0;
};
typedef struct TruckDataValue TruckDataValue;


class Dashboard {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    Thing m_thing = createThing();
    map<string, TruckDataValue> m_truckData;
    int m_lineCount = 0;
    string m_distanceUnit = "";
    string m_etaUnit = "";

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        JSonTagGroupRegistry tgr;
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/LocationTagGroup.json");
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/DistanceTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/LocationDashboardThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    void displayHeader() {
        cout
            << setw(20) << left << "Truck Context"
            << setw(15) << left << "Latitude"
            << setw(15) << left << "Longitude"
            << setw(25) << left << ("Distance (" + m_distanceUnit + ")")
            << setw(20) << left << ("ETA (" + m_etaUnit + ")")
            << endl;
    }

    string formatNumber(double value, int precision) {
        stringstream result;

        if (value != float_min)
            result << setprecision(precision) << fixed << value;
        else
            result << "-";

        return result.str();
    }

    string formatTime(time_t time) {
        if (time != 0) {
            tm * pLocalTime = localtime(&time);
            char buffer[32];
            strftime(buffer, 32, "%H:%M:%S", pLocalTime);
            return buffer;
        } else {
            return "-";
        }
    }

    void displayStatus() {
        // Reset cursor position for previous console update
        for (int i = 0; i < m_lineCount * 2 + 1; i++) {
            cout << CONSOLE_LINE_UP;
        }

        // Add header row for table
        displayHeader();

        // Write new data to console
        m_lineCount = 0;
        for (auto& data : m_truckData) {
            string key = data.first;
            TruckDataValue value = data.second;

            cout
                << COLOR_GREEN << setw(20) << left << truncate(key, 30) << NO_COLOR
                << COLOR_MAGENTA << setw(15) << left << formatNumber(value.lat, 6) << NO_COLOR
                << COLOR_MAGENTA << setw(15) << left << formatNumber(value.lng, 6) << NO_COLOR
                << COLOR_GREEN << setw(25) << left << formatNumber(value.distance, 3) << NO_COLOR
                << COLOR_GREEN << setw(20) << left << formatNumber(value.eta.count(), 1) << NO_COLOR
                << endl
                << setw(20) << " "
                << COLOR_GREY << setw(30) << left << ("  updated: " + formatTime(value.locationUpdateTime)) << NO_COLOR
                << COLOR_GREY << setw(45) << left << ("  updated: " + formatTime(value.positionUpdateTime)) << NO_COLOR
                << endl;

            m_lineCount++;
        }
    }

    void getLocationFromSample(const DataSample<IOT_NVP_SEQ>& sample, float& lat, float& lng, time_t& timestamp) {
        const IOT_NVP_SEQ& data = sample.getData();
        for (const IOT_NVP& nvp : data) {
            if (nvp.name() == "location") {
                for (const IOT_NVP& locationNvp : nvp.value().iotv_nvp_seq()) {
                    if (locationNvp.name() == "latitude") {
                        lat = locationNvp.value().iotv_float32();
                    } else if (locationNvp.name() == "longitude") {
                        lng = locationNvp.value().iotv_float32();
                    }
                }
            } else if (nvp.name() == "timestampUtc") {
                timestamp = nvp.value().iotv_uint64();
            }
        }
    }

    void getDistanceFromSample(const DataSample<IOT_NVP_SEQ>& sample, double& distance, minutes& eta, time_t& timestamp) {
        const IOT_NVP_SEQ& data = sample.getData();
        for (const IOT_NVP& nvp : data) {
            if (nvp.name() == "distance") {
                distance = nvp.value().iotv_float64();
            }
            if (nvp.name() == "eta") {
                eta = minutes(nvp.value().iotv_float32());
            }
            if (nvp.name() == "timestampUtc") {
                timestamp = nvp.value().iotv_uint64();
            }
        }
    }

    void processLocationSample(const DataSample<IOT_NVP_SEQ>& dataSample) {
        float lat = 0.0f;
        float lng = 0.0f;
        time_t timestamp = time(nullptr);

        try {
            if (dataSample.getFlowState() == FlowState::ALIVE) {
                getLocationFromSample(dataSample, lat, lng, timestamp);

                string key = dataSample.getFlowId();
                m_truckData[key].lat = lat;
                m_truckData[key].lng = lng;
                m_truckData[key].locationUpdateTime = timestamp;
            }
        }
        catch (exception& e) {
            cerr << "An unexpected error occured while processing data-sample: " << e.what() << endl;
        }
    }

    void processDistanceSample(const DataSample<IOT_NVP_SEQ>& dataSample) {
        double distance = 0.0f;
        minutes eta = minutes(0);
        time_t timestamp = time(nullptr);

        try {
            if (dataSample.getFlowState() == FlowState::ALIVE) {
                getDistanceFromSample(dataSample, distance, eta, timestamp);

                string key = dataSample.getFlowId();
                m_truckData[key].distance = distance;
                m_truckData[key].eta = eta;
                m_truckData[key].positionUpdateTime = timestamp;
            }
        }
        catch (exception& e) {
            cerr << "An unexpected error occured while processing data-sample: " << e.what() << endl;
        }
    }

    void getTagUnitDescriptions() {
        auto tgr = m_dataRiver.getDiscoveredTagGroupRegistry();
        auto distanceTagGroup = tgr.findTagGroup("Distance:com.adlinktech.example:v1.0");
        auto typedefs = distanceTagGroup.getTypeDefinitions();
        for (auto typeD : typedefs) {
                for(auto& tag: typeD.getTags()){
                if (tag.getName() == "distance") {
                    m_distanceUnit = tag.getUnit();
                }
                if (tag.getName() == "eta") {
                    m_etaUnit = tag.getUnit();
                }
            }
        }
    }

public:
    Dashboard(string thingPropertiesUri) :m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Dashboard started" << endl;
    }

    ~Dashboard() {
        m_dataRiver.close();
        cout << "Dashboard stopped" << endl;
    }

    int run(int runningTime) {
        auto startTimestamp = chrono::steady_clock::now();
        long long elapsedTime;

        do {
            // Retrieve and process location samples
            vector<DataSample<IOT_NVP_SEQ> > locationSamples = m_thing.read<IOT_NVP_SEQ>("location", 0);
            for (const DataSample<IOT_NVP_SEQ>& sample : locationSamples) {
                processLocationSample(sample);
            }

            // Retrieve and process distance samples
            vector<DataSample<IOT_NVP_SEQ> > distanceSamples = m_thing.read<IOT_NVP_SEQ>("distance", 0);
            for (const DataSample<IOT_NVP_SEQ>& sample : distanceSamples) {
                processDistanceSample(sample);
            }

            if (m_distanceUnit.empty() || m_etaUnit.empty()) {
                getTagUnitDescriptions();
            }

            // Update console output
            displayStatus();

            // Sleep before next update
            this_thread::sleep_for(chrono::milliseconds(READ_DELAY));

            // Get elapsed time
            elapsedTime = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - startTimestamp).count();
        } while (elapsedTime < runningTime);

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

#ifdef _WIN32
    setConsoleMode();
#endif

    try {
        Dashboard(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }

}
