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
#include <iomanip>
#include <thread>
#include <chrono>
#include <future>
#include <algorithm>
#include <map>

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

#define NO_COLOR "\x1b[m"
#define COLOR_RED "\x1b[31m"
#define COLOR_GREEN "\x1b[32m"
#define COLOR_MAGENTA "\x1b[35m"
#define COLOR_GREY "\x1b[90m"
#define CLEAR_SCREEN "\x1b[2J"
#define MOVE_CURSOR_TO_ORIGIN "\x1b[0;0H"

#define GATEWAY_INITIAL_DELAY 2000
#define READ_DELAY 10
#define DISPLAY_REFRESH_RATE 10.0f
#define SCREEN_HEIGHT_IN_LINES 45
#define TOTAL_HEADER_LINES 2
#define TOTAL_FOOTER_MESSAGE_LINES 1


extern string truncate(string str, size_t width);
#ifdef _WIN32
extern bool setConsoleMode();
#endif

class DataFlowKey;

struct DataFlowValue {
    unsigned int sampleCount;
    FlowState flowState;
};

typedef struct DataFlowValue DataFlowValue;

map<string, string> g_thingContext;

class DataFlowKey {
private:
    TagGroup m_tagGroup;
    string m_sourceThingClassId;
    string m_sourceThingId;
    string m_flowId;

public:
    DataFlowKey(const DataSample<IOT_NVP_SEQ>& dataSample) :
        m_tagGroup(dataSample.getTagGroup()),
        m_sourceThingClassId(dataSample.getSourceClass()),
        m_sourceThingId(dataSample.getSourceId()),
        m_flowId(dataSample.getFlowId()) {
    }

    bool operator< (const DataFlowKey& other) const {
        if (getTagGroupName() < other.getTagGroupName()) return true;
        if (getTagGroupName() > other.getTagGroupName()) return false;

        if (m_sourceThingClassId < other.getSourceThingClassId()) return true;
        if (m_sourceThingClassId > other.getSourceThingClassId()) return false;

        if (m_sourceThingId < other.getSourceThingId()) return true;
        if (m_sourceThingId > other.getSourceThingId()) return false;

        if (m_flowId < other.getFlowId()) return true;
        if (m_flowId > other.getFlowId()) return false;

        return false;
    }

    const string getSourceThingClassId() const {
        return m_sourceThingClassId;
    }

    const string getSourceThingId() const {
        return m_sourceThingId;
    }

    const string getSourceThingContext() const {
        string context = g_thingContext[m_sourceThingId];
        if (context.empty()) {
            context = "<unknown>";
        }

        return context;
    }

    const string getTagGroupName() const {
        return m_tagGroup.getName();
    }

    const string getTagGroupQos() const {
        return m_tagGroup.getQosProfile();
    }

    const string getFlowId() const {
        return m_flowId;
    }

};

class NewThingDiscoveredListener : public ThingDiscoveredListener {
    void notifyThingDiscovered(const DiscoveredThing& thing) {
        g_thingContext[thing.getId()] = thing.getContextId();
    }
};

class GatewayService {
private:
    string m_thingPropertiesUri;
    int m_screenHeightInLines;
    DataRiver m_dataRiver = createDataRiver();
    Thing m_thing = createThing();
    map<DataFlowKey, DataFlowValue> m_sampleCount;
    int m_lineCount = 0;

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/GatewayServiceThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);

        return m_dataRiver.createThing(tp);
    }

    void displayStatus() {
        // Move cursor position to the origin (0,0) of the console
        cout << MOVE_CURSOR_TO_ORIGIN;

        // Add header row for table
        displayHeader();

        // Write new data to console
        m_lineCount = 0;
        for (auto& data : m_sampleCount) {
            DataFlowKey key = data.first;
            DataFlowValue value = data.second;

            // Set grey color for purged flows
            bool alive = value.flowState == FlowState::ALIVE;
            string COLOR1 = alive ? COLOR_GREEN : NO_COLOR;
            string COLOR2 = alive ? COLOR_MAGENTA : COLOR_GREY;
            string flowState = alive ? "" : " <purged>";

            cout
                << COLOR1 << setw(32) << left << truncate(key.getSourceThingContext() + flowState, 32) << NO_COLOR
                << COLOR2 << setw(30) << left << truncate(key.getFlowId(), 30) << NO_COLOR
                << COLOR2 << setw(20) << left << truncate(key.getTagGroupName(), 20) << NO_COLOR
                << COLOR_GREY << setw(12) << left << truncate(key.getTagGroupQos(), 12) << NO_COLOR
                << COLOR1 << setw(8) << right << to_string(value.sampleCount) << NO_COLOR
                << endl;

            m_lineCount++;

            if(m_lineCount < m_sampleCount.size() &&
                    m_lineCount >= (m_screenHeightInLines - TOTAL_HEADER_LINES - TOTAL_FOOTER_MESSAGE_LINES - 1)) {

                cout
                    << "... " << m_sampleCount.size() - m_lineCount << " more lines available. "
                    << "Set terminal height to " << m_sampleCount.size() + TOTAL_HEADER_LINES + TOTAL_FOOTER_MESSAGE_LINES + 1 << ". "
                    << "See the README file for more instructions." << endl;
                break;
            }
        }
    }

    void displayHeader() {
        cout
            << setw(32) << left << "Thing's ContextId"
            << setw(30) << left << "Flow Id"
            << setw(20) << left << "TagGroup Name"
            << setw(12) << left << "QoS"
            << setw(8) << right << "Samples"
            << endl << endl;
    }

    void readThingsFromRegistry() {
        auto discoveredThingsRegistry = m_dataRiver.getDiscoveredThingRegistry();
        auto things = discoveredThingsRegistry.getDiscoveredThings();
        for (auto thing : things) {
            g_thingContext[thing.getId()] = thing.getContextId();
        }
    }

public:
    GatewayService(string thingPropertiesUri, int screenHeightInLines) :
        m_thingPropertiesUri(thingPropertiesUri), m_screenHeightInLines(screenHeightInLines) {
        cout << "Gateway Service started" << endl;
    }

    ~GatewayService() {
        m_dataRiver.close();
        cout << "Gateway Service stopped" << endl;
    }

    int run(int runningTime) {
        auto startTimestamp = chrono::steady_clock::now();
        auto displayUpdatedTimestamp = startTimestamp;
        long long elapsedTime = 0;

        // Add listener for discovering new Things
        auto newThingDiscoveredListener = NewThingDiscoveredListener();
        m_dataRiver.addListener(newThingDiscoveredListener);

        // Get meta-data (contextId) for Things in discovered things registry
        readThingsFromRegistry();

        // Clear console screen before printing samples
        cout << CLEAR_SCREEN;

        do {
            // Read data
            const vector<DataSample<IOT_NVP_SEQ> >& msgs =
                m_thing.read_next<IOT_NVP_SEQ>("dynamicInput", (runningTime * 1000) - elapsedTime);

            // Loop received samples and update counters
            for (const DataSample<IOT_NVP_SEQ>& msg : msgs) {
                auto flowState = msg.getFlowState();

                DataFlowKey key = DataFlowKey(msg);

                // Store state in value for this flow
                m_sampleCount[key].flowState = flowState;

                // In case flow is alive or if flow is purged but sample
                // contains data: increase sample count
                bool sampleContainsData = (flowState == FlowState::ALIVE) || msg.getData().size();

                if (sampleContainsData) {
                    m_sampleCount[key].sampleCount++;

                    // In a real-world use-case you would have additional processing
                    // of the data received by msg.getData()
                }
            }

            // Update console output
            displayStatus();

            // Sleep before reading next samples
            this_thread::sleep_for(chrono::milliseconds(READ_DELAY));

            // Get elapsed time
            auto now = chrono::steady_clock::now();
            elapsedTime = chrono::duration_cast<chrono::milliseconds>(now - startTimestamp).count();
        } while (elapsedTime / 1000 < runningTime);

        // Remove listener
        m_dataRiver.removeListener(newThingDiscoveredListener);

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

    // Get LINES (terminal's height) from environment variable
    const char * linesKey = "LINES";
    const char * linesEnv = getenv(linesKey);
    int screenHeightInLines = SCREEN_HEIGHT_IN_LINES;
    if (linesEnv == nullptr) {
        cout << "Environment variable " << linesKey << " not set" << endl;
        cout << "Assuming "<< linesKey << "(terminal height) = " << screenHeightInLines << endl;
    } else {
        screenHeightInLines = atoi(linesEnv);
    }

#ifdef _WIN32
    setConsoleMode();
#endif

    try {
        GatewayService(thingPropertiesUri, screenHeightInLines).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }

}

