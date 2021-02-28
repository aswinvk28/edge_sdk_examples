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

#include <atomic>
#include <chrono>
#include <signal.h>
#include <stdlib.h>
#include <string>
#include <time.h>
#include <vector>
#include <algorithm>
#ifdef _WIN32
#include <Windows.h>
#endif

using namespace std;

#define NO_COLOR "\x1b[0m"
#define COLOR_GREEN "\x1b[0;32m"
#define COLOR_LMAGENTA "\x1b[0;95m"

#ifdef _WIN32
#ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
#define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
#endif

#ifndef DISABLE_NEWLINE_AUTO_RETURN
#define DISABLE_NEWLINE_AUTO_RETURN 0x0008
#endif

#ifndef ENABLE_VIRTUAL_TERMINAL_INPUT
#define ENABLE_VIRTUAL_TERMINAL_INPUT 0x0200
#endif

#endif // _WIN32

using Clock = chrono::high_resolution_clock;
using Timepoint = chrono::time_point<Clock>;

#define US_IN_ONE_SEC 1000000LL
#define BYTES_PER_SEC_TO_MEGABITS_PER_SEC 125000

typedef std::chrono::seconds Seconds;
typedef std::chrono::milliseconds Milliseconds;
typedef std::chrono::microseconds Microseconds;
template<typename ToType, typename FromType>
unsigned long long Duration(const FromType& d)
{
    return chrono::duration_cast<ToType>(d).count();
}

static atomic<bool> stop(false);

#ifndef _WIN32
struct sigaction oldAction;
#endif

#ifdef _WIN32
static bool ctrlHandler(DWORD fdwCtrlType);
#else
static void ctrlHandler(int fdwCtrlType);
#endif

static void registerControlHandler() {
    /* Register handler for Ctrl-C */
#ifdef _WIN32
    SetConsoleCtrlHandler((PHANDLER_ROUTINE)ctrlHandler, true);
#else
    struct sigaction sat;
    sat.sa_handler = ctrlHandler;
    sigemptyset(&sat.sa_mask);
    sat.sa_flags = 0;
    sigaction(SIGINT,&sat,&oldAction);
#endif
}

static void unregisterControlHandler() {
#ifdef _WIN32
    SetConsoleCtrlHandler(0, false);
#else
    sigaction(SIGINT,&oldAction, 0);
#endif
}


typedef struct ExampleTimeStats {
    vector<unsigned long> values;
    double average;
    unsigned long min;
    unsigned long max;
} ExampleTimeStats;

ExampleTimeStats exampleInitTimeStats() {
    ExampleTimeStats stats;
    stats.average = 0;
    stats.min = 0;
    stats.max = 0;

    return stats;
}

ExampleTimeStats* exampleAddMicrosecondsToTimeStats(ExampleTimeStats* stats, unsigned long microseconds) {
    stats->average = (stats->values.size() * stats->average + microseconds) / (stats->values.size() + 1);
    stats->values.push_back(microseconds);
    stats->min = (stats->min == 0 || microseconds < stats->min) ? microseconds : stats->min;
    stats->max = (microseconds > stats->max) ? microseconds : stats->max;

    return stats;
}

double exampleGetMedianFromTimeStats(ExampleTimeStats* stats) {
    sort(stats->values.begin(), stats->values.end());

    if (stats->values.size() % 2 == 0) {
        return (double)(stats->values[stats->values.size() / 2 - 1] + stats->values[stats->values.size() / 2]) / 2;
    }

    return (double)stats->values[stats->values.size() / 2];
}

void exampleResetTimeStats(ExampleTimeStats& stats) {
    stats.values.clear();
    stats.average = 0;
    stats.min = 0;
    stats.max = 0;
}

ExampleTimeStats& operator+=(ExampleTimeStats& stats, unsigned long microseconds) {
    return *exampleAddMicrosecondsToTimeStats(&stats, microseconds);
}

double exampleGetMedianFromTimeStats(ExampleTimeStats& stats) {
    return exampleGetMedianFromTimeStats(&stats);
}

#ifdef _WIN32

static DWORD dwOriginalOutMode = 0;
static DWORD dwOriginalInMode = 0;
static DWORD dwOriginalCP;

bool setConsoleMode() {
    // Set output mode to handle virtual terminal sequences
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hOut == INVALID_HANDLE_VALUE)
    {
        return false;
    }
    HANDLE hIn = GetStdHandle(STD_INPUT_HANDLE);
    if (hIn == INVALID_HANDLE_VALUE)
    {
        return false;
    }

    if (!GetConsoleMode(hOut, &dwOriginalOutMode)) {
        return false;
    }
    if (!GetConsoleMode(hIn, &dwOriginalInMode)) {
        return false;
    }

    DWORD dwRequestedOutModes = ENABLE_VIRTUAL_TERMINAL_PROCESSING | DISABLE_NEWLINE_AUTO_RETURN;
    DWORD dwRequestedInModes = ENABLE_VIRTUAL_TERMINAL_INPUT;

    DWORD dwOutMode = dwOriginalOutMode | dwRequestedOutModes;
    if (!SetConsoleMode(hOut, dwOutMode))
    {
        // we failed to set both modes, try to step down mode gracefully.
        dwRequestedOutModes = ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        dwOutMode = dwOriginalOutMode | dwRequestedOutModes;
        if (!SetConsoleMode(hOut, dwOutMode))
        {
            // Failed to set any VT mode, can't do anything here.
            return false;
        }
    }

    DWORD dwInMode = dwOriginalInMode | ENABLE_VIRTUAL_TERMINAL_INPUT;
    if (!SetConsoleMode(hIn, dwInMode))
    {
        // Failed to set VT input mode, can't do anything here.
        return false;
    }

    // Store original and set console code page to UTF8 
    dwOriginalCP = GetConsoleOutputCP();
    SetConsoleOutputCP(CP_UTF8);

    // Enable buffering to prevent VS from chopping up UTF-8 byte sequences
    setvbuf(stdout, nullptr, _IOFBF, 1000);

    return true;
}

bool resetConsoleMode() {
    // Set output mode to handle virtual terminal sequences
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hOut == INVALID_HANDLE_VALUE)
    {
        return false;
    }
    HANDLE hIn = GetStdHandle(STD_INPUT_HANDLE);
    if (hIn == INVALID_HANDLE_VALUE)
    {
        return false;
    }

    SetConsoleMode(hOut, dwOriginalOutMode);
    SetConsoleMode(hIn, dwOriginalInMode);
    
    // Reset code page
    SetConsoleOutputCP(dwOriginalCP);

    // Disable buffering 
    setvbuf(stdout, nullptr, _IOFBF, 0);

    return true;
}

#endif
