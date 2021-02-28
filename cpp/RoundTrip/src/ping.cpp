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
#include <chrono>
#include <cstring>
#include <iomanip>
#include <iostream>
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

#include "include/cxxopts.h"
#include "include/utilities.h"

using namespace std;
using namespace com::adlinktech::datariver;
using namespace com::adlinktech::iot;

static ExampleTimeStats roundTripOverall = exampleInitTimeStats();
static ExampleTimeStats writeAccessOverall = exampleInitTimeStats();
static ExampleTimeStats readAccessOverall = exampleInitTimeStats();

static void showStats(bool overall, unsigned long elapsedSeconds, ExampleTimeStats roundTrip, ExampleTimeStats writeAccess, ExampleTimeStats readAccess) {
	if (overall) {
		cout << endl << COLOR_GREEN << "# Overall";
	} else {
		cout << setw(9) << right << elapsedSeconds;
	}

	cout
		<< setw(10) << right << roundTrip.values.size()
		<< setw(9) << right << fixed << setprecision(0) << exampleGetMedianFromTimeStats(roundTrip)
		<< setw(9) << right << roundTrip.min
		<< setw(11) << right << writeAccess.values.size()
		<< setw(9) << right << fixed << setprecision(0) << exampleGetMedianFromTimeStats(writeAccess)
		<< setw(9) << right << writeAccess.min
		<< setw(11) << right << readAccess.values.size()
		<< setw(9) << right << fixed << setprecision(0) << exampleGetMedianFromTimeStats(readAccess)
		<< setw(9) << right << readAccess.min << NO_COLOR << endl;
}

#ifdef _WIN32
static bool ctrlHandler(DWORD fdwCtrlType)
{
    showStats(true, 0, roundTripOverall, writeAccessOverall, readAccessOverall);
    resetConsoleMode();
	ExitProcess(1); // perform a brute-force exit for now
    return true;
}
#else
static void ctrlHandler(int fdwCtrlType)
{
    showStats(true, 0, roundTripOverall, writeAccessOverall, readAccessOverall);
    exit(1);
}
#endif

class Ping {
private:
    string m_thingPropertiesUri;
    DataRiver m_dataRiver = createDataRiver();
    Thing m_thing = createThing();
	IOT_NVP_SEQ m_sampleData;

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
        tcr.registerThingClassesFromURI("file://definitions/ThingClass/com.adlinktech.example/PingThingClass.json");
        m_dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        JSonThingProperties tp;
        tp.readPropertiesFromURI(m_thingPropertiesUri);

        return m_dataRiver.createThing(tp);
    }

	void waitForPong() {
		// wait for pong to appear by discovering its thingId and thingClass
		cout << "# Waiting for pong to run..." << endl;
		auto discoveredThingRegistry = m_dataRiver.getDiscoveredThingRegistry();
		bool readerFound = false;

		while (!readerFound) {
			try {
				// see if we already know pongs's thing class
				auto thing = discoveredThingRegistry.findDiscoveredThing("pongThing1", "Pong:com.adlinktech.example:v1.0");
				readerFound = true;
			} catch (...) {}
			this_thread::sleep_for(chrono::seconds(1));
		}
	}

	void initPayload(unsigned long payloadSize) {
	    IOT_VALUE payload;
		payload.iotv_byte_seq(IOT_BYTE_SEQ());
		for (unsigned long i = 0; i < payloadSize; i++) {
			payload.iotv_byte_seq().push_back('a');
		}
		m_sampleData = { IOT_NVP(string("payload"), payload) };
	}

	void warmUp() {
		Timepoint startTime = Clock::now();
		int64_t waitTimeout = 10000;

		cout << "# Warming up 5s to stabilise performance..." << endl;
		while (Duration<Microseconds>(Clock::now() - startTime) / US_IN_ONE_SEC < 5) {
			m_thing.write("Ping", m_sampleData);
			m_thing.read<IOT_NVP_SEQ>("Pong", waitTimeout);
		}
		cout << "# Warm up complete" << endl;
	}



public:
    Ping(string thingPropertiesUri) :
        m_thingPropertiesUri(thingPropertiesUri)
    {
        cout << "# Ping started" << endl;
    }

    ~Ping() {
        m_dataRiver.close();
        cout << "# Ping stopped" << endl;
    }

	int sendTerminate() {
        cout << "# Sending termination request." << endl;
        m_thing.purge("Ping", "ping");
        this_thread::sleep_for(chrono::seconds(1));
		return 0;
	}

    int run(unsigned long payloadSize, unsigned long numSamples, unsigned long runningTime) {
		Timepoint startTime;
		Timepoint preWriteTime;
		Timepoint postWriteTime;
		Timepoint preReadTime;
		Timepoint postReadTime;
		int64_t waitTimeout = 10000;
		ExampleTimeStats roundTrip = exampleInitTimeStats();
		ExampleTimeStats writeAccess = exampleInitTimeStats();
		ExampleTimeStats readAccess = exampleInitTimeStats();

		cout << "# Parameters: payload size: " << payloadSize << " | number of samples: " << numSamples << " | running time: " << runningTime << endl;

		// Wait for the Pong Thing
		waitForPong();

		// Init payload
		initPayload(payloadSize);

		// Warm-up for 5s
		warmUp();

		cout << "# Round trip measurements (in us)" << endl;
		cout << COLOR_LMAGENTA << "#             Round trip time [us]         Write-access time [us]       Read-access time [us]" << NO_COLOR << endl;
		cout << COLOR_LMAGENTA << "# Seconds     Count   median      min      Count   median      min      Count   median      min" << NO_COLOR << endl;

		startTime = Clock::now();
		unsigned long elapsedSeconds = 0;
		for (unsigned long i = 0; !numSamples || i < numSamples; i++) {
			// Write a sample that pong can send back
			preWriteTime = Clock::now();
			m_thing.write("Ping", m_sampleData);
			postWriteTime = Clock::now();

			// Read sample
			preReadTime = Clock::now();
			vector<DataSample<IOT_NVP_SEQ>> samples = m_thing.read<IOT_NVP_SEQ>("Pong", waitTimeout);
			postReadTime = Clock::now();

			// Validate sample count
			if (samples.size() != 1) {
				cerr << "ERROR: Ping received " << samples.size() << " samples but was expecting 1." << endl;
				return 1;
			}

			// Update stats
			writeAccess += (unsigned long)Duration<Microseconds>(postWriteTime - preWriteTime);
			readAccess += (unsigned long)Duration<Microseconds>(postReadTime - preReadTime);
			roundTrip += (unsigned long)Duration<Microseconds>(postReadTime - preWriteTime);
			writeAccessOverall += (unsigned long)Duration<Microseconds>(postWriteTime - preWriteTime);
			readAccessOverall += (unsigned long)Duration<Microseconds>(postReadTime - preReadTime);
			roundTripOverall += (unsigned long)Duration<Microseconds>(postReadTime - preWriteTime);

			// Print stats each second
			if ((Duration<Microseconds>(postReadTime - startTime) > US_IN_ONE_SEC) || (i && i == numSamples)) {
				// Print stats
				showStats(false, ++elapsedSeconds, roundTrip, writeAccess, readAccess);

				// Reset stats for next run
				exampleResetTimeStats(roundTrip);
				exampleResetTimeStats(writeAccess);
				exampleResetTimeStats(readAccess);

				// Set values for next run
				startTime = Clock::now();

				// Check for timeout
				if (runningTime > 0 && elapsedSeconds >= runningTime) {
					break;
				}
			}
		}

		// Print overall stats
		showStats(true, 0, roundTripOverall, writeAccessOverall, readAccessOverall);

		return 0;
	}
};


static void GetCommandLineParameters(int argc, char *argv[],
        unsigned long& payloadSize,
        unsigned long& numSamples,
        unsigned long& runningTime,
		bool& quit
) {
    cxxopts::Options options("ThroughputWriter", "ADLINK ThingSDK ThroughputWriter");
    try {
        options.add_options()
            ("p,payload-size", "Payload size", cxxopts::value<unsigned long>()->default_value("0"))
            ("n,num-samples", "Number of samples (0 is infinite)", cxxopts::value<unsigned long>()->default_value("0"))
            ("r,running-time", "Running Time in seconds (0 is infinite)", cxxopts::value<unsigned long>()->default_value("0"))
			("q,quit", "Send a quit signal to pong", cxxopts::value<bool>())
            ("h,help", "Print help")
            ;
        options.parse_positional({"payload-size", "num-samples", "running-time", "other"});
        options.positional_help("[payload-size] [num-samples] [running-time]");

        auto cmdLineOptions = options.parse(argc, argv);

        if (cmdLineOptions.count("help")) {
            cout << options.help({""}) << endl;
            exit(0);
        }

		quit = cmdLineOptions["q"].as<bool>();
        payloadSize = cmdLineOptions["p"].as<unsigned long>();
        numSamples = cmdLineOptions["n"].as<unsigned long>();
        runningTime = cmdLineOptions["r"].as<unsigned long>();
    }
    catch (cxxopts::OptionException e) {
        cerr << e.what() << endl << endl;
        cout << options.help({""}) << endl;
        exit(1);
    }
}

int main(int argc, char *argv[]) {
	unsigned long payloadSize = 0;
    unsigned long numSamples = 0;
    unsigned long runningTime = 0;
	bool quit = false;
   	GetCommandLineParameters(argc, argv, payloadSize, numSamples, runningTime, quit);

    // Register handler for Ctrl-C
    registerControlHandler();

#ifdef _WIN32
    setConsoleMode();
#endif

	int result = 0;
    try
    {
        Ping ping("file://./config/PingProperties.json");
		if (quit) {
			result = ping.sendTerminate();
		} else {
	        result = ping.run(payloadSize, numSamples, runningTime);
		}
    }
    catch (ThingAPIException e)
    {
        cerr << "An unexpected error occurred: " << e.what() << endl;
    }catch(std::exception& e1){
        cerr << "An unexpected error occurred: " << e1.what() << endl;
    }

#ifdef _WIN32
    resetConsoleMode();
#endif

    unregisterControlHandler();

	return result;
}


