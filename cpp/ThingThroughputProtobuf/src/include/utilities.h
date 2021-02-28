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
#include <time.h>
#ifdef _WIN32
#include <Windows.h>
#endif

using namespace std;

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