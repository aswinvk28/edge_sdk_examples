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
using CommandLine;

namespace Writer
{
    public class Options
    {

        [Option('p', "payload-size", Required = false, Default = (ulong)4096, HelpText = "Payload size")]
        public ulong PayloadSize { get; set; }
        [Option('b', "burst-interval", Required = false, Default = (ulong)0, HelpText = "Burst interval in milliseconds")]
        public ulong BurstInterval { get; set; }
        [Option('s', "burst-size", Required = false, Default = (ulong)1, HelpText = "Burst size")]
        public ulong BurstSize { get; set; }
        [Option('r', "running-time", Required = false, Default = (ulong)0, HelpText = "Running Time in seconds (0 is infinite, default)")]
        public ulong RunningTime { get; set; }
        [Option('w', "writer-mode", Required = false, Default = WriterMode.standard, HelpText = "Writer mode (standard, outputHandler, outputHandlerNotThreadSafe)")]
        public WriterMode WriterMode { get; set; }

    }
}
