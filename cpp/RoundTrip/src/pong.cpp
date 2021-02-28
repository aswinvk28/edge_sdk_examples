/*                         ADLINK Edge SDK
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
 * This is a simple throughput application measuring obtainable throughput using the thingSDK
 *
 */
#include <iostream>
#include <thread>
#include <chrono>

#include <IoTDataThing.hpp>
#include <JSonThingAPI.hpp>
#include <ThingAPIException.hpp>
#include "include/cxxopts.h"
#include "include/utilities.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;

#ifdef _WIN32
static bool ctrlHandler(DWORD fdwCtrlType)
{
    cout << "Ctrl-c detected .. exiting" << endl;
	ExitProcess(1);
    return true;
}
#else
static void ctrlHandler(int fdwCtrlType)
{
    cout << "Ctrl-c detected .. exiting" << endl;
    exit(1);
}
#endif

class Pong {
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
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/PingTagGroup.json");
        tgr.registerTagGroupsFromURI("file://definitions/TagGroup/com.adlinktech.example/PongTagGroup.json");
        m_dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/PongThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);

        return m_dataRiver.createThing(tp);
    }

public:
    Pong(string thingPropertiesUri) :
        m_thingPropertiesUri(thingPropertiesUri)
    {
        cout << "Pong started" << endl;
    }

    ~Pong() {
        m_dataRiver.close();
        cout << "Pong stopped" << endl;
    }

    int run() {
        bool terminate = false;
        cout << "Waiting for samples from ping to send back..." << endl;

        while (!terminate) {
        	vector<DataSample<IOT_NVP_SEQ> > samples = m_thing.read<IOT_NVP_SEQ>("Ping");

            for (const DataSample<IOT_NVP_SEQ>& sample : samples) {
            	if (sample.getFlowState() == FlowState::PURGED) {
                    cout << "Received termination request. Terminating." << endl;
                    terminate = true;
                    break;
                } else {
                	m_thing.write("Pong", sample.getData());
                }
            }
        }

        return 0;
    }
};


int main(int argc, char *argv[]) {
    // Register handler for Ctrl-C
    registerControlHandler();

    try
    {
        return Pong("file://./config/PongProperties.json").run();
    }
    catch (ThingAPIException e)
    {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }

    unregisterControlHandler();
}
