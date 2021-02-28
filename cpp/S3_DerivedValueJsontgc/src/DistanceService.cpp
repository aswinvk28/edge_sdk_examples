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
#include <cmath>
#include <future>
#include <assert.h>
#include <exception>

#include <JSonThingAPI.hpp>
#include <ThingAPIException.hpp>

#include "include/cxxopts.hpp"


#include "com.adlinktech.example/Distance.h"
#include "com.adlinktech.example/Location.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;
using namespace com::adlinktech::example::v1_DOT_0;


#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

using minutes = chrono::duration<float, ratio<60> >;

class IDistanceServiceThing {
public:
    virtual float getWarehouseLat() = 0;
    virtual float getWarehouseLng() = 0;
    virtual void writeDistance(string myLocationFlowId, double distance, minutes eta, time_t timestamp) = 0;
};

class GpsSensorDataListener : public DataAvailableListener<Location> {
private:
    IDistanceServiceThing& m_distanceServiceThing;

    double calculateDistance(float truckLocationLat, float truckLocationLng) {
        return sqrt(pow(truckLocationLat - m_distanceServiceThing.getWarehouseLat(), 2)
            + pow(truckLocationLng - m_distanceServiceThing.getWarehouseLng(), 2));
    }

public:
    GpsSensorDataListener(IDistanceServiceThing& distanceServiceThing) : m_distanceServiceThing(distanceServiceThing) {
    }

    void notifyDataAvailable(const vector<DataSample<Location> >& data) {
        for (const DataSample<Location>& locationMessage : data) {
            string myLocationFlowId = locationMessage.getFlowId();
            if (locationMessage.getFlowState() == FlowState::ALIVE) {
                // Get location data from sample
                const Location & locationData = locationMessage.getData();
                float truckLocationLat = 0.0f;
                float truckLocationLng = 0.0f;
                time_t timestamp = 0;

                // Calculate distance to the warehouse
                double distance = calculateDistance(locationData.location().latitude(), locationData.location().longitude());

                // This example uses a fixed multiplier for ETA. In a real-world
                // scenario this would be calculated based on e.g. real-time traffic information
                minutes eta = minutes(distance * 5.12345f);

                m_distanceServiceThing.writeDistance(locationMessage.getFlowId(), distance, eta, locationData.timestampUtc());
            }
        }
    }
};

class DistanceServiceThing : public IDistanceServiceThing {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    Thing m_thing = createThing();
    float m_warehouseLat;
    float m_warehouseLng;

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        JSonTagGroupRegistry tgr;
        tgr.registerTagGroupsFromURI("file://definitions/LocationTagGroup.json");
        tgr.registerTagGroupsFromURI("file://definitions/DistanceTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/DistanceServiceThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

public:
    DistanceServiceThing(string thingPropertiesUri,float warehouseLat, float warehouseLng) :
            m_thingPropertiesUri(thingPropertiesUri),
            m_warehouseLat(warehouseLat),
            m_warehouseLng(warehouseLng) {
        cout << "Distance Service started" << endl;
    }

    ~DistanceServiceThing() {
        m_dataRiver.close();
        cout << "Distance Service stopped" << endl;
    }

    float getWarehouseLat() {
        return m_warehouseLat;
    }

    float getWarehouseLng() {
        return m_warehouseLng;
    }

    void writeDistance(string myLocationFlowId, double distance, minutes eta, time_t timestamp) {
        Distance distanceData;

        distanceData.set_distance(distance);
        distanceData.set_eta(eta.count());
        distanceData.set_timestampUtc(timestamp);

        // Write distance to DataRiver using flow ID from incoming location sample
        m_thing.write("distance", myLocationFlowId, distanceData);
    }

    int run(int runningTime) {
        // Use custom dispatcher for processing events
        Dispatcher dispatcher = Dispatcher();

        // Add listener for new GPS sensor Things using our custom dispatcher
        auto gpsDataReceivedListener = GpsSensorDataListener(*this);
        m_thing.addListener(gpsDataReceivedListener, dispatcher);

        // Process events with our dispatcher
        auto start = chrono::steady_clock::now();
        long long elapsedSeconds;
        do {
            try {
                // block the call for 1000ms
                dispatcher.processEvents(1000);
            } catch (TimeoutError e) {
                // Ignore.
            }
            elapsedSeconds = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - start).count();
        } while (elapsedSeconds < runningTime);

        // Remove listener
        m_thing.removeListener(gpsDataReceivedListener, dispatcher);

        return 0;
    }
};

static void getCommandLineParameters(int argc, char *argv[],
        string& thingPropertiesUri, float& lat, float& lng, int& runningTime) {
    try {
        cxxopts::Options options(argv[0], "ADLINK Edge SDK Example Derived value service");
        options.add_options()
            ("t,thing", "Thing properties URI", cxxopts::value<string>())
            ("lat", "Warehouse location latitude", cxxopts::value<float>())
            ("lng", "Warehouse location longitude", cxxopts::value<float>())
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
    float warehouseLat;
    float warehouseLng;
    int runningTime;
    getCommandLineParameters(argc, argv, thingPropertiesUri, warehouseLat, warehouseLng, runningTime);

    try {
        DistanceServiceThing(thingPropertiesUri, warehouseLat, warehouseLng).run(
                runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}
