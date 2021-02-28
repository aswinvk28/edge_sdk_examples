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
#include <atomic>
#include <chrono>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <map>
#include <signal.h>
#include <sstream>
#include <stdlib.h>
#include <thread>
#include <time.h>

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

unsigned long long sampleCount = 0;
unsigned long long startCount = 0;
static Timepoint currentTime = Timepoint();
static Timepoint startTime = Timepoint();
static unsigned long long bytesReceived = 0;
static unsigned long long outOfOrderCount = 0;
static unsigned long long batchCount = 0;
static unsigned long long batchMaxSize = 0;

static void showSummary() {
    // Output totals and averages
    if (batchCount > 0) {
        double deltaTime = (double)Duration<Microseconds>(currentTime - startTime) / US_IN_ONE_SEC;
        cout << endl << fixed
                    << "Total received: " << sampleCount << " samples, " << bytesReceived << " bytes" << endl
                    << "Out of order: " << outOfOrderCount << " samples" << endl
                    << "Average transfer rate: "
                            << setprecision(0) << (double)sampleCount / deltaTime << " samples/s, "
                            << setprecision(2) << ((double)bytesReceived / BYTES_PER_SEC_TO_MEGABITS_PER_SEC) / deltaTime << " Mbit/s"
                    << endl
                    << "Average sample-count per batch: " << sampleCount / batchCount << ", maximum batch-size: " << batchMaxSize << endl;
    }
}

#ifdef _WIN32
static bool ctrlHandler(DWORD fdwCtrlType)
{
    showSummary();
    ExitProcess(1);
}
#else
static void ctrlHandler(int fdwCtrlType)
{
    showSummary();
    exit(1);
}
#endif

class ThroughputReader {
private:

    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    ThingEx m_thing = createThing();

    DataRiver createDataRiver() {
        return DataRiver::getInstance();
    }

    ThingEx createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        ThroughputHelper::registerWithDataRiver(m_dataRiver);

        // Create and Populate the ThingClass registry with JSON resource files.
        JSonThingClassRegistry tcr;
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example.protobuf/ThroughputReaderThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);

        return m_dataRiver.createThing(tp);
    }

public:
    ThroughputReader(string thingPropertiesUri) :
            m_thingPropertiesUri(thingPropertiesUri) {
        cout << "Throughput reader started" << endl;
    }

    ~ThroughputReader() {
        m_dataRiver.close();
        cout << "Throughput reader stopped" << endl;
    }

    int run(unsigned long pollingDelay, unsigned long runningTime) {
        unsigned long long prevCount = 0;
        unsigned long long prevReceived = 0;
        unsigned long long samplesInBatch = 0;
//        unsigned long long payloadSize = 0;
        unsigned long long deltaReceived;
        unsigned long long deltaTime;
        Timepoint prevTime = Timepoint();
        IOT_UINT64 receivedSequenceNumber;
        IOT_UINT64 lastReceivedSequenceNumber;
        unsigned long long payloadSize = 0;
        bool firstSample = true;

        cout << "Waiting for samples..." << endl;

        // Loop through until the runningTime has been reached (0 = infinite)
        // each cycle is 1 second
        unsigned long long cycles = 0;

        while (!stop && (runningTime == 0 || cycles < runningTime))
        {
            if (pollingDelay > 0) {
                this_thread::sleep_for(chrono::milliseconds(pollingDelay));
            }

            // New batch
            batchCount++;
            samplesInBatch = sampleCount;

            // Take samples and iterate through them
            VLoanedDataSamples samples = m_thing.read("ThroughputInput", BLOCKING_TIME_INFINITE);
            // in loops, take care to declare auto variables 'auto&', otherwise, you will
            // get a copy of the data
            for(auto& sample : samples) {
                if (sample.getFlowState() == FlowState::ALIVE) {
                    Throughput data;
                    sample.get(data);
                    receivedSequenceNumber = data.sequencenumber();
                    payloadSize = data.sequencedata().size();
                    bytesReceived += payloadSize;
                    sampleCount++;

                    if (firstSample) {
                        lastReceivedSequenceNumber = receivedSequenceNumber - 1;
                        firstSample = false;
                    }

                    // Check that the sample is the next one expected
                    if (receivedSequenceNumber != lastReceivedSequenceNumber + 1) {
                        outOfOrderCount += (receivedSequenceNumber - (lastReceivedSequenceNumber + 1 ));
                    }

                    // Keep track of last received seq nr
                    lastReceivedSequenceNumber = receivedSequenceNumber;
                } else {
                    cout << "Writer flow purged, stop reader" << endl;
                    stop = true;
                }
            }

            if (!stop) {
                currentTime = Clock::now();
                if (Duration<Microseconds>(currentTime - prevTime) > US_IN_ONE_SEC) {
                    // If not the first iteration
                    if (prevTime.time_since_epoch() != Timepoint().time_since_epoch()) {
                        // Calculate the samples and bytes received and the time passed since the  last iteration and output
                        deltaReceived = bytesReceived - prevReceived;
                        deltaTime = Duration<Microseconds>(currentTime - prevTime) / US_IN_ONE_SEC;

                        cout << fixed
                                    << "Payload size: " << payloadSize << " | "
                                    << "Total: " << setw(9) << right << sampleCount << " samples, "
                                    << setw(12) << right << bytesReceived << " bytes | "
                                    << setw(6) << right << "Out of order: " << outOfOrderCount << " samples | "
                                    << "Transfer rate: " << setw(7) << right << setprecision(0) << (double)(sampleCount - prevCount) / deltaTime << " samples/s, "
                                        << setw(9) << right << setprecision(2) << ((double)deltaReceived / BYTES_PER_SEC_TO_MEGABITS_PER_SEC) / deltaTime << " Mbit/s"
                                    << endl;

                        cycles++;
                    }
                    else
                    {
                        // Set the start time if it is the first iteration
                        startTime = currentTime;
                    }

                    // Update the previous values for next iteration
                    prevReceived = bytesReceived;
                    prevCount = sampleCount;
                    prevTime = currentTime;
                }

                // Update max samples per batch
                samplesInBatch = sampleCount - samplesInBatch;
                if (samplesInBatch > batchMaxSize) {
                    batchMaxSize = samplesInBatch;
                }
            }
        }

        showSummary();

        return 0;
    }
};


static void GetCommandLineParameters(int argc, char *argv[],
        unsigned long& pollingDelay,
        unsigned long& runningTime
) {
    cxxopts::Options options("ThroughputReader", "ADLINK ThingSDK ThroughputReader");
    try {
        options.add_options()
            ("p,polling-delay", "Polling delay (milliseconds)", cxxopts::value<unsigned long>()->default_value("0"))
            ("r,running-time", "Running time (seconds, 0 = infinite)", cxxopts::value<unsigned long>()->default_value("0"))
            ("h,help", "Print help")
            ;
        options.parse_positional({"polling-delay", "running-time", "other"});
        options.positional_help("[polling-delay] [running-time]");

        auto cmdLineOptions = options.parse(argc, argv);

        if (cmdLineOptions.count("help")) {
            cout << options.help({""}) << endl;
            exit(0);
        }

        pollingDelay = cmdLineOptions["p"].as<unsigned long>();
        runningTime = cmdLineOptions["r"].as<unsigned long>();
    }
    catch (cxxopts::OptionException e) {
        cerr << e.what() << endl << endl;
        cout << options.help({""}) << endl;
        exit(1);
    }catch (const std::domain_error e1) {
        cerr << e1.what() << endl << endl;
        cout << options.help({""}) << endl;
        exit(1);
    }
}


int main(int argc, char *argv[]) {
    // Get command line parameters
    unsigned long pollingDelay;
    unsigned long runningTime;
    GetCommandLineParameters(argc, argv, pollingDelay, runningTime);

    // Register handler for Ctrl-C
    registerControlHandler();

    try
    {
        return ThroughputReader("file://./config/ThroughputReaderProperties.json")
            .run(pollingDelay, runningTime);
    }
    catch (ThingAPIException e)
    {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }

    unregisterControlHandler();
}
