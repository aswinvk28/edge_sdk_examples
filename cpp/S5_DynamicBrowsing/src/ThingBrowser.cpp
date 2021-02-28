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

#define NO_COLOR "\x1b[0m"
#define COLOR_RED "\x1b[0;31m"
#define COLOR_GREEN "\x1b[0;32m"
#define COLOR_LGREEN "\x1b[0;92m"
#define COLOR_LYELLOW "\x1b[0;93m"
#define COLOR_YELLOW "\x1b[0;33m"
#define COLOR_BLUE "\x1b[0;34m"
#define COLOR_LBLUE "\x1b[0;94m"
#define COLOR_MAGENTA "\x1b[0;35m"
#define COLOR_LMAGENTA "\x1b[0;95m"

#ifdef _WIN32
extern bool setConsoleMode();
#endif


class NewThingDiscoveredListener : public ThingDiscoveredListener {
private:
    DataRiver &m_dataRiver;
    string m_thingPropertiesUri;
    DiscoveredTagGroupRegistry m_discoveredTagGroupRegistry = createDiscoveredTagGroupRegistry();
    DiscoveredThingClassRegistry m_discoveredThingClassRegistry = createDiscoveredThingClassRegistry();
    DiscoveredThingRegistry m_discoveredThingRegistry = createDiscoveredThingRegistry();

    const char * asString(const com::adlinktech::datariver::IOT_TYPE type) const noexcept
    {
        using com::adlinktech::datariver::IOT_TYPE;
        switch(type) {
        case IOT_TYPE::TYPE_NONE: return "None";
        case IOT_TYPE::TYPE_BYTE: return "BYTE";
        case IOT_TYPE::TYPE_UINT16: return "UINT16";
        case IOT_TYPE::TYPE_UINT32: return "UINT32";
        case IOT_TYPE::TYPE_UINT64: return "UINT64";
        case IOT_TYPE::TYPE_INT8: return "INT8";
        case IOT_TYPE::TYPE_INT16: return "INT16";
        case IOT_TYPE::TYPE_INT32: return "INT32";
        case IOT_TYPE::TYPE_INT64: return "INT64";
        case IOT_TYPE::TYPE_FLOAT32: return "FLOAT32";
        case IOT_TYPE::TYPE_FLOAT64: return "FLOAT64";
        case IOT_TYPE::TYPE_BOOLEAN: return "BOOLEAN";
        case IOT_TYPE::TYPE_STRING: return "STRING";
        case IOT_TYPE::TYPE_CHAR: return "CHAR";
        case IOT_TYPE::TYPE_UINT16_SEQ: return "UINT16_SEQ";
        case IOT_TYPE::TYPE_UINT32_SEQ: return "UINT32_SEQ";
        case IOT_TYPE::TYPE_UINT64_SEQ: return "UINT64_SEQ";
        case IOT_TYPE::TYPE_INT8_SEQ: return "INT8_SEQ";
        case IOT_TYPE::TYPE_INT16_SEQ: return "INT16_SEQ";
        case IOT_TYPE::TYPE_INT32_SEQ: return "INT32_SEQ";
        case IOT_TYPE::TYPE_INT64_SEQ: return "INT64_SEQ";
        case IOT_TYPE::TYPE_FLOAT32_SEQ: return "FLOAT32_SEQ";
        case IOT_TYPE::TYPE_FLOAT64_SEQ: return "FLOAT64_SEQ";
        case IOT_TYPE::TYPE_BOOLEAN_SEQ: return "BOOLEAN_SEQ";
        case IOT_TYPE::TYPE_STRING_SEQ: return "STRING_SEQ";
        case IOT_TYPE::TYPE_CHAR_SEQ: return "CHAR_SEQ";
        case IOT_TYPE::TYPE_BYTE_SEQ: return "BYTE_SEQ";
        case IOT_TYPE::TYPE_NVP_SEQ: return "NVP_SEQ";
        case IOT_TYPE::TYPE_MULTI_DIM_NVP: return "MULTI_DIM_NVP";
        case IOT_TYPE::TYPE_MULTI_DIM_NVP_SEQ: return "MULTI_DIM_NVP_SEQ";
        }
        return "None"; // Note: keeping C++ compilers happy, the above switch should cover all cases.
    }

    std::string unitAsString(const TagDefinition& tag)
    {
        using com::adlinktech::datariver::IOT_TYPE;
        if(tag.getKind() == IOT_TYPE::TYPE_MULTI_DIM_NVP) {
            return "n/a";
        } else {
            return tag.getUnit();
        }
    }

    class TagGroupNotFoundException : public exception { };

    DiscoveredTagGroupRegistry createDiscoveredTagGroupRegistry() {
        return m_dataRiver.getDiscoveredTagGroupRegistry();
    }

    DiscoveredThingClassRegistry createDiscoveredThingClassRegistry() {
        return m_dataRiver.getDiscoveredThingClassRegistry();
    }

    DiscoveredThingRegistry createDiscoveredThingRegistry() {
        return m_dataRiver.getDiscoveredThingRegistry();
    }

    Thing createThing() {
        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/ThingBrowserThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }

    void displayTypeDefinition(TypeDefinition td, string prefix = "", int width = 0)
    {
        cout << prefix << COLOR_LBLUE << setw(width) << left << td.getNameOfType() << NO_COLOR << ": " << endl;
        vector<TagDefinition> vtagd = td.getTags();
        for (auto& tagd : vtagd)
        {
            displayTag(tagd,prefix, width + 3);
        }
    }

    void displayTag(TagDefinition tag, string prefix = "", int width = 0) {
        cout << prefix << COLOR_YELLOW << setw(width) << left << tag.getName() << NO_COLOR << ": " << tag.getDescription()
            << " (kind: " << asString(tag.getKind())
            << " | unit: " << unitAsString(tag)
            << ")" << endl;
    }

    void displayTagGroup(TagGroup tagGroup, string prefix = "") {
        cout << COLOR_LBLUE << tagGroup.getName() << ":" << tagGroup.getContext() << ":" << tagGroup.getVersionTag() << COLOR_BLUE << " [TagGroup]" << NO_COLOR << endl;
        cout << prefix << "Description: " << tagGroup.getDescription() << endl;
        cout << prefix << "QosProfile: " << tagGroup.getQosProfile() << endl;
        cout << prefix << "Tags: " << endl;
        try {
            auto type = tagGroup.getToplevelType();
            for(auto& tag: type.getTags()){
                displayTag(tag, prefix + "   ", 15);
            }
        }
        catch (ThingAPIRuntimeError& e) {
            cerr << prefix << COLOR_RED "   Error displaying TagGroup details: " << e.what() << NO_COLOR << endl;
        }
    }

    bool isDynamicTagGroup(string tagGroup) {
        return tagGroup.find("*") != string::npos
            || tagGroup.find("?") != string::npos
            || tagGroup.find(",") != string::npos;
    }

    TagGroup findTagGroup(string tagGroupName) {
        int retryCount = 50;

        while (retryCount-- > 0) {
            try {
                return m_discoveredTagGroupRegistry.findTagGroup(tagGroupName);
            }
            catch(const InvalidArgumentError&) {
                // TagGroup not found
            }

            // Sleep 100ms before retry
            this_thread::sleep_for(chrono::milliseconds(100));
        }

        throw TagGroupNotFoundException();
    }

    void displayInputs(vector<InputTagGroup> inputs, string prefix = "") {
        cout << prefix << "inputs:" << endl;
        if (inputs.empty()) {
            cout << prefix << "   <none>" << endl;
        } else {
            for (auto input : inputs) {
                string inputTagGroup = input.getInputTagGroup();
                if (isDynamicTagGroup(inputTagGroup)) {
                    cout << prefix << "   " << COLOR_GREEN << input.getName() << NO_COLOR ": "
                        << COLOR_MAGENTA << "[expression]" << NO_COLOR << " " << inputTagGroup << endl;
                } else {
                    try {
                        TagGroup tagGroup = findTagGroup(inputTagGroup);
                        cout << prefix << "   " << COLOR_GREEN << input.getName() << NO_COLOR << ": ";
                        displayTagGroup(tagGroup, prefix + "      ");
                    }
                    catch(const TagGroupNotFoundException&) {
                        cout << prefix << COLOR_RED "   TagGroup not found" << NO_COLOR << endl;
                    }
                }
            }
        }
    }

    void displayOutputs(vector<OutputTagGroup> outputs, string prefix = "") {
        cout << prefix << "outputs:" << endl;
        if (outputs.empty()) {
            cout << prefix << "   <none>" << endl;
        } else {
            for (auto output : outputs) {
                try {
                    auto tagGroup = findTagGroup(output.getOutputTagGroup());
                    cout << prefix << "   " << COLOR_GREEN << output.getName() << NO_COLOR << ": ";
                    displayTagGroup(tagGroup, prefix + "      ");
                }
                catch(const TagGroupNotFoundException&) {
                    cout << prefix << COLOR_RED "   TagGroup not found" << NO_COLOR << endl;
                }
            }
        }
    }

    void displayThingClass(ThingClass thingClass, string prefix = "") {
        vector<InputTagGroup> inputs = thingClass.getInputTagGroups();
        vector<OutputTagGroup> outputs = thingClass.getOutputTagGroups();

        cout << prefix << COLOR_LMAGENTA << thingClass.getId().getName() << ":" << thingClass.getContext() << ":" << thingClass.getVersionTag() << COLOR_MAGENTA << " [ThingClass]" << NO_COLOR << endl;
        cout << prefix << "   Description: " << thingClass.getDescription() << endl;
        displayInputs(inputs, prefix + "   ");
        displayOutputs(outputs, prefix + "   ");
    }

    void displayThing(DiscoveredThing thing, string prefix = "") {
        bool thingClassFound = false;
        int retryCount = 30;

        cout << endl << COLOR_LGREEN << thing.getContextId() << COLOR_GREEN << " [Thing]" << NO_COLOR << endl;
        cout << prefix << "   Thing ID:    " << thing.getId() << endl;
        cout << prefix << "   Context:     " << thing.getContextId() << endl;
        cout << prefix << "   Description: " << thing.getDescription() << endl;

        while (!thingClassFound && retryCount-- > 0) {
            try {
                auto thingClass = m_discoveredThingClassRegistry.findThingClass(
                    thing.getClassId().getName() +
                    ":" + thing.getClassId().getContext() +
                    ":" + thing.getClassId().getVersionTag());

                displayThingClass(thingClass, prefix + "   ");
                thingClassFound = true;
            }
            catch(const InvalidArgumentError&) {
                // ThingClass not found
            }

            // Sleep 100ms before retry
            this_thread::sleep_for(chrono::milliseconds(100));
        }

        if (!thingClassFound) {
            cout << prefix << COLOR_RED "   ThingClass not found" << NO_COLOR << endl;
        }
    }

    void notifyThingDiscovered(const DiscoveredThing& thing) {
        displayThing(thing, "   ");
    }

public:
    NewThingDiscoveredListener(DataRiver &dataRiver): m_dataRiver(dataRiver) { }
};

class ThingBrowser
{
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    Dispatcher m_dispatcher = Dispatcher();
    NewThingDiscoveredListener m_newThingDiscoveredListener = NewThingDiscoveredListener(m_dataRiver);
    Thing m_thing = createThing();

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    Thing createThing() {
        // Add listener for discovery of Things
        m_dataRiver.addListener(m_newThingDiscoveredListener, m_dispatcher);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/ThingBrowserThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);
        return m_dataRiver.createThing(tp);
    }


public:
    ThingBrowser(string thingPropertiesUri) :m_thingPropertiesUri(thingPropertiesUri) {
    }

    ~ThingBrowser() {
        try {
            // Remove the discovered Thing listener that was added during class initialization
            m_dataRiver.removeListener(m_newThingDiscoveredListener, m_dispatcher);
        }
        catch(ThingAPIException& e) {
            cerr << "Unexpected error while removing discovered Thing listener: " << e.what() << endl;
        }

        m_dataRiver.close();
        cout << COLOR_GREEN << "ThingBrowser stopped" << NO_COLOR << endl;
    }

    int run(int runningTime) {
        // Process events with our dispatcher
        auto start = chrono::steady_clock::now();
        long long elapsedSeconds;
        do {
            try {
                m_dispatcher.processEvents(1000);
            }
            catch (TimeoutError&) {
                // Ignore
            }
            elapsedSeconds = chrono::duration_cast<chrono::seconds>(chrono::steady_clock::now() - start).count();
        } while (elapsedSeconds < runningTime);

        return 0;
    }
};

int main(int argc, char *argv[]) {
    // Get thing properties URI from command line parameter
    if (argc < 3) {
        cerr << "Usage: " << argv[0] << " THING_PROPERTIES_URI RUNNING_TIME" << endl;
        return 1;
    }
    string thingPropertiesUri = string(argv[1]);
    int runningTime = atoi(argv[2]);

#ifdef _WIN32
    setConsoleMode();
#endif

    cout << COLOR_GREEN << "Starting ThingBrowser" << NO_COLOR << endl;

    try {
        ThingBrowser(thingPropertiesUri).run(runningTime);
    }
    catch (ThingAPIException& e) {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}

