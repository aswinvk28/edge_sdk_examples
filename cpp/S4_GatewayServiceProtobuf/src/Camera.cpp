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
 * This code is part of example scenario 4 'Gateway Service' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

#include <iostream>
#include <random>
#include <exception>
#include <fstream>
#include <thread>
#include <chrono>

#include <JSonThingAPI.hpp>
#include <ThingAPIException.hpp>

#include "definitions/CameraStateTagGroup.dr.h"
#include "definitions/ObservationTagGroup.dr.h"

#include "../../S4_GatewayServiceProtobuf/src/include/cxxopts.hpp"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::example::protobuf;

#ifdef _MSC_VER
#pragma warning(disable:4996)
#endif

#define CAMERA_SAMPLE_DELAY 1000
#define CAMERA_INITIAL_DELAY 2000
#define CAMERA_DELAY 100
#define BARCODE_INTERVAL 5000
#define BARCODE_LIFESPAN 15000
#define BARCODE_SKIP_PERCENTAGE 25


class ICamera {
public:
    virtual bool isRelated(const DiscoveredThing& thing) = 0;
    virtual void discoveredRelatedCamera(string thingId, string contextId) = 0;
    virtual void lostRelatedCamera(string thingId) = 0;
};


class CameraThingDiscoveredListener : public ThingDiscoveredListener {
private:
    ICamera& m_camera;

    void notifyThingDiscovered(const DiscoveredThing& thing) {
        if (m_camera.isRelated(thing)) {
            m_camera.discoveredRelatedCamera(thing.getId(), thing.getContextId());
        }
    }

public:
    CameraThingDiscoveredListener(ICamera& camera) : m_camera(camera) { }
};


class CameraThingLostListener : public ThingLostListener {
private:
    ICamera& m_camera;

    void notifyThingLost(const DiscoveredThing& thing) {
        if (m_camera.isRelated(thing)) {
            m_camera.lostRelatedCamera(thing.getId());
        }
    }

public:
    CameraThingLostListener(ICamera& camera) : m_camera(camera) { }
};


class Camera : public ICamera {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    ThingEx m_thing = createThing();
    vector<string> m_barcodes;
    map<string, string> m_relatedCameras;
    vector<thread> m_threads;
    bool m_closed = false;

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Register TagGroups using .proto generated types.
    	CameraStateHelper::registerWithDataRiver(m_dataRiver);
    	ObservationHelper::registerWithDataRiver(m_dataRiver);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example.protobuf/CameraThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    string getFlowId(string barcode) {
        string flowId;
        if (hasRelatedCameras()) {
            flowId = getParentContext(m_thing.getContextId()) + ".cameras." + barcode;
        } else {
            flowId = m_thing.getContextId() + "." + barcode;
        }

        return flowId;
    }

    void writeSample(string barcode, int x, int y, int z) {
    	Observation data;
    	data.set_barcode(barcode);
    	data.set_position_x(x);
    	data.set_position_y(y);
    	data.set_position_z(z);

        m_thing.write("observation", getFlowId(barcode), data);
    }

    void purgeFlow(string barcode) {
        m_thing.purge("observation", getFlowId(barcode));
    }

    void setState(string state) {
    	CameraState data;
    	data.set_state(state);

        m_thing.write("state", data);
    }

    string getParentContext(string contextId) {
        size_t found = contextId.find_last_of(".");
        if (found != string::npos) {
            return contextId.substr(0, found);
        }

        return contextId;
    }

    bool hasRelatedCameras() {
        return m_relatedCameras.size() > 0;
    }

    void checkRegistryForRelatedCameras() {
        auto discoveredThingsRegistry = m_dataRiver.getDiscoveredThingRegistry();
        auto things = discoveredThingsRegistry.getDiscoveredThings();
        for (auto thing : things) {
            if (isRelated(thing)) {
                discoveredRelatedCamera(thing.getId(), thing.getContextId());
            }
        }
    }

    int intRand(const int & min, const int & max) {
        // Using thread_local generator to avoid use of a mutex to synchronize access across threads
        static thread_local std::mt19937 generator(std::random_device{}()); // first set constructs random_device, second set invokes operator()
        std::uniform_int_distribution<int> distribution(min,max); // inclusive lower and upper bound
        return distribution(generator);
    }

    void barcodeTask(string barcode) {
        auto start = chrono::steady_clock::now();
        long long elapsedMilliseconds;
        int x = intRand(0, 99);
        int y = intRand(0, 99);
        int z = intRand(0, 99);

        do {
            // Simulate position change
            x += intRand(-5, 4);
            y += intRand(-5, 4);
            z += intRand(-1, 0);

            // Sleep before sending next update
            this_thread::sleep_for(chrono::milliseconds(CAMERA_SAMPLE_DELAY));

            // Send location update for this barcode
            writeSample(barcode, x, y, z);

            elapsedMilliseconds = chrono::duration_cast<chrono::milliseconds>(chrono::steady_clock::now() - start).count();
        } while (!m_closed && elapsedMilliseconds < BARCODE_LIFESPAN);

        purgeFlow(barcode);
    }

public:
    Camera(string thingPropertiesUri) :
            m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Camera started" << endl;
        setState("on");
    }

    ~Camera() {
        try {
            // Set camera state to 'off'
            setState("off");
        }
        catch (ThingAPIException e) {
            cerr << "Error setting camera state to off: " << e.what() << endl;
        }

        // Stop and join threads
        m_closed = true;
        for (int i = 0; i < m_threads.size(); ++i) {
            if (m_threads.at(i).joinable()) {
                m_threads.at(i).join();
            }
        }

        m_dataRiver.close();
        cout << "Camera stopped" << endl;
    }

    bool isRelated(const DiscoveredThing& thing) {
        return
            getParentContext(thing.getContextId()) == getParentContext(m_thing.getContextId())
                && thing.getClassId() == m_thing.getClassId()
                && thing.getId() != m_thing.getId();
    }

    void discoveredRelatedCamera(string thingId, string contextId) {
        if (m_relatedCameras.count(thingId) == 0) {
            cout << "Camera " << m_thing.getContextId() << ": detected other camera with context " << contextId << " (Thing Id " << thingId << ")" << endl;
        }
        m_relatedCameras[thingId] = contextId;
    }

    void lostRelatedCamera(string thingId) {
        m_relatedCameras.erase(thingId);
    }

    int run(int runningTime, vector<string> barcodes) {
        auto start = chrono::steady_clock::now();
        auto barcodeSeqnr = 0;
        auto barcodeTimestamp = start - chrono::milliseconds(BARCODE_INTERVAL);
        long long elapsedSeconds;

        // Add listeners for Thing discovered and Thing lost
        auto newThingDiscoveredListener = CameraThingDiscoveredListener(*this);
        m_dataRiver.addListener(newThingDiscoveredListener);

        auto thingLostListener = CameraThingLostListener(*this);
        m_dataRiver.addListener(thingLostListener);

        // Check for related camera already in the discovered things registry
        this_thread::sleep_for(chrono::milliseconds(CAMERA_INITIAL_DELAY));
        checkRegistryForRelatedCameras();

        // Start processing
        do {
            auto now = chrono::steady_clock::now();

            // Check if next barcode should be read
            if (barcodeSeqnr < barcodes.size()
                    && chrono::duration_cast<chrono::milliseconds>(now - barcodeTimestamp).count() > BARCODE_INTERVAL) {
                string barcode = barcodes[barcodeSeqnr++];

                // Randomly skip some of the barcodes
                if (intRand(0,99) > BARCODE_SKIP_PERCENTAGE) {
                    m_threads.push_back(thread(&Camera::barcodeTask, this, barcode));
                }

                // Update timestamp and seqnr
                barcodeTimestamp = now;
            }

            // Sleep for some time
            this_thread::sleep_for(chrono::milliseconds(CAMERA_DELAY));

            // Check if camera should keep running
            elapsedSeconds = chrono::duration_cast<chrono::seconds>(now - start).count();
        } while (elapsedSeconds < runningTime);

        // Remove listeners
        m_dataRiver.removeListener(newThingDiscoveredListener);
        m_dataRiver.removeListener(thingLostListener);

        return 0;
    }
};


static void getCommandLineParameters(int argc, char *argv[],
        string& thingPropertiesUri, string& barcodeFilePath, int& runningTime) {
    try {
        cxxopts::Options options(argv[0], "ADLINK Edge SDK Example Camera");
        options.add_options()
            ("t,thing", "Thing properties URI", cxxopts::value<string>())
            ("b,barcodes", "Barcode file path ", cxxopts::value<string>())
            ("r,running-time", "Running Time", cxxopts::value<int>())
            ("h,help", "Print help");

        auto cmdLineOptions = options.parse(argc, argv);

        if (cmdLineOptions.count("help")) {
            cout << options.help({""}) << endl;
            exit(0);
        }
        if (cmdLineOptions.count("thing") == 0 || cmdLineOptions.count("barcodes") == 0) {
            cerr << "Please provide Thing Property URI and barcode file path" << endl;
            cerr << options.help({""}) << endl;
            exit(1);
        }

        thingPropertiesUri = cmdLineOptions["thing"].as<string>();
        barcodeFilePath = cmdLineOptions["barcodes"].as<string>();
        runningTime = cmdLineOptions["r"].as<int>();
    }
    catch (exception& e) {
        cerr << "An error occurred while parsing command line parameters: " << e.what() << endl;
        exit(1);
    }
}

static vector<string> readBarCodes(string barcodeFilePath) {
    ifstream barcodeFile(barcodeFilePath);
    vector<string> barcodes;
    string barcode;

    if (!barcodeFile)    {
        cerr << "Cannot open barcode file: " << barcodeFilePath << endl;
        return vector<string>();
    }

    // Read barcodes from file
    while (getline(barcodeFile, barcode))
    {
        barcode.erase(remove(barcode.begin(), barcode.end(), '\r'), barcode.end());
        if (barcode.size() > 0)
            barcodes.push_back(barcode);
    }

    return barcodes;
}

int main(int argc, char *argv[]) {
    // Get command line parameters
    string thingPropertiesUri;
    string barcodeFilePath;
    int runningTime;

    getCommandLineParameters(argc, argv, thingPropertiesUri, barcodeFilePath, runningTime);

    // Get barcodes
    vector<string> barcodes = readBarCodes(barcodeFilePath);
    if (barcodes.size() == 0) {
        cerr << "Error: no barcodes found" << endl;
        return 1;
    }

    try {
        Camera(thingPropertiesUri).run(runningTime, barcodes);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}
