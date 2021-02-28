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
 */
#include <atomic>
#include <chrono>
#include <cstring>
#include <iostream>
#include <signal.h>
#include <sstream>
#include <stdlib.h>
#include <thread>
#include <time.h>
#ifdef _WIN32
#include <Windows.h>
#endif

#include <IoTDataThing.hpp>
#include <JSonThingAPI.hpp>
#include <ThingAPIException.hpp>
#include "include/cxxopts.hpp"
#include "include/utilities.h"

#include "definitions/ThroughputTagGroup.dr.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;
using namespace com::adlinktech::example::protobuf;

#ifdef _WIN32
static bool ctrlHandler(DWORD fdwCtrlType)
{
    stop = true;
    return true;
}
#else
static void ctrlHandler(int fdwCtrlType)
{
    stop = true;
}
#endif


// Writer mode:
//    standard = use default write function
//    outputHandler = use output handler for writing
//    outputHandlerNotThreadSafe = use non-thread-safe write method for output handler
enum WriterMode {
    standard,
    outputHandler,
    outputHandlerNotThreadSafe
};

class ThroughputWriter {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    ThingEx m_thing = createThing();
    Throughput m_sample;

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    ThingEx createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        ThroughputHelper::registerWithDataRiver(m_dataRiver);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example.protobuf/ThroughputWriterThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);

        return m_dataRiver.createThing(tp);
    }

    void setupMessage(unsigned long payloadSize) {
        m_sample.set_sequencenumber(0);
        m_sample.set_sequencedata(string(payloadSize, 'a'));
    }

    void waitForReader() {
        // wait for throughputreader to appear by discovering its thingId and thingClass
        cout << "Waiting for Throughput reader.. " << endl;
        DiscoveredThingRegistry discoveredThingRegistry = m_dataRiver.getDiscoveredThingRegistry();
        bool readerFound = false;
        while (!readerFound && !stop)
        {
            try
            {
                auto thing = discoveredThingRegistry.findDiscoveredThing("*", "ThroughputReader:com.adlinktech.example.protobuf:v1.0");
                readerFound = true;
            }
            catch (...)
            {
                // Thing not available yet
            }

            this_thread::sleep_for(chrono::milliseconds(100));
        }

        if (!stop) {
            cout << "Throughput reader found" << endl;
        } else {
            cout << "Terminated" << endl;
            exit(1);
        }
    }

    void write(unsigned long burstInterval, unsigned long burstSize, unsigned long runningTime, WriterMode mode) {
        unsigned long burstCount = 0;
        unsigned long count = 0;
        bool timedOut = false;
        unsigned long long deltaTime;

        Timepoint pubStart = Clock::now();
        Timepoint burstStart = Clock::now();
        Timepoint currentTime = Clock::now();

        while (!stop && !timedOut)
        {
            // Write data until burst size has been reached
            if (burstCount++ < burstSize) {
                m_sample.set_sequencenumber(count++);
                m_thing.write("ThroughputOutput", m_sample);
            } else if (burstInterval != 0) {
                // Sleep until burst interval has passed
                currentTime = Clock::now();

                deltaTime = Duration<Milliseconds>(currentTime - burstStart);
                if (deltaTime < burstInterval)
                {
                    this_thread::sleep_for(chrono::milliseconds(burstInterval - deltaTime));
                }
                burstStart = Clock::now();
                burstCount = 0;
            } else {
                burstCount = 0;
            }

            // Check of timeout
            if (runningTime != 0) {
                currentTime = Clock::now();
                if (Duration<Seconds>(currentTime - pubStart) > runningTime) {
                    timedOut = true;
                }
            }
        }

        // Show stats
        if (stop) {
            std::cout << "Terminated: " << count << " samples written" << std::endl;
        } else {
            std::cout << "Timed out: " << count << " samples written" << std::endl;
        }
    }


public:
    ThroughputWriter(string thingPropertiesUri) :
        m_thingPropertiesUri(thingPropertiesUri)
    {
        cout << "Throughput writer started" << endl;
    }

    ~ThroughputWriter() {
        m_dataRiver.close();
        cout << "Throughput writer stopped" << endl;
    }

    int run(unsigned long payloadSize, unsigned long burstInterval, unsigned long burstSize, unsigned long runningTime, WriterMode writerMode) {
        string writerModeStr = (writerMode == WriterMode::outputHandler) ?
            "outputHandler" : ((writerMode == WriterMode::outputHandlerNotThreadSafe) ?
                "outputHandlerNotThreadSafe" : "standard");

        cout << "payloadSize: " << payloadSize << " | burstInterval: " << burstInterval
            << " | burstSize: " << burstSize << " | runningTime: " << runningTime
            << " | writer-mode: " << writerModeStr << endl;
        // Wait for reader to be discovered
        waitForReader();

        // Create the message that is sent
        setupMessage(payloadSize);

        // Write data
        write(burstInterval, burstSize, runningTime, writerMode);

        // Give middleware some time to finish writing samples
        this_thread::sleep_for(chrono::seconds(2));

        return 0;
    }
};

static void GetCommandLineParameters(int argc, char *argv[],
        unsigned long& payloadSize,
        unsigned long& burstInterval,
        unsigned long& burstSize,
        unsigned long& runningTime,
        WriterMode& writerMode
) {
    cxxopts::Options options("ThroughputWriter", "ADLINK ThingSDK ThroughputWriter");
    try {
        options.add_options()
            ("p,payload-size", "Payload size", cxxopts::value<unsigned long>()->default_value("4096"))
            ("b,burst-interval", "Burst interval in milliseconds", cxxopts::value<unsigned long>()->default_value("0"))
            ("s,burst-size", "Burst size", cxxopts::value<unsigned long>()->default_value("1"))
            ("r,running-time", "Running Time in seconds (0 is infinite)", cxxopts::value<unsigned long>()->default_value("0"))
            ("w,writer-mode", "Writer mode (standard, outputHandler, outputHandlerNotThreadSafe)", cxxopts::value<string>()->default_value("standard"))
            ("h,help", "Print help")
            ;
        options.parse_positional({"payload-size", "burst-interval", "burst-size", "running-time", "writer-mode", "other"});
        options.positional_help("[payload-size] [burst-interval] [burst-size] [running-time]");

        auto cmdLineOptions = options.parse(argc, argv);

        if (cmdLineOptions.count("help")) {
            cout << options.help({""}) << endl;
            exit(0);
        }

        payloadSize = cmdLineOptions["p"].as<unsigned long>();
        burstInterval = cmdLineOptions["b"].as<unsigned long>();
        burstSize = cmdLineOptions["s"].as<unsigned long>();
        runningTime = cmdLineOptions["r"].as<unsigned long>();

        if (cmdLineOptions["w"].as<string>() == "outputHandler") {
            writerMode = WriterMode::outputHandler;
        } else if (cmdLineOptions["w"].as<string>() == "outputHandlerNotThreadSafe") {
            writerMode = WriterMode::outputHandlerNotThreadSafe;
        } else if (cmdLineOptions["w"].as<string>() == "standard") {
            writerMode = WriterMode::standard;
        } else {
            cerr << "Invalid writer-mode" << endl << endl;
            cout << options.help({""}) << endl;
            exit(1);
        }
    }
    catch (cxxopts::OptionException e) {
        cerr << e.what() << endl << endl;
        cout << options.help({""}) << endl;
        exit(1);
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }
}

int main(int argc, char *argv[]) {
    // Get command line parameters
    unsigned long payloadSize;
    unsigned long burstInterval;
    unsigned long burstSize;
    unsigned long runningTime;
    WriterMode writerMode = WriterMode::standard;
    GetCommandLineParameters(argc, argv, payloadSize, burstInterval, burstSize, runningTime, writerMode);

    // Register handler for Ctrl-C
    registerControlHandler();

    try
    {
        return ThroughputWriter("file://./config/ThroughputWriterProperties.json")
            .run(payloadSize, burstInterval, burstSize, runningTime, writerMode);
    }
    catch (ThingAPIException e)
    {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }

    unregisterControlHandler();
}
