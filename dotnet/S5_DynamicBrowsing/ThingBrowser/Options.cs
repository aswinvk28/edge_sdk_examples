﻿/*                         ADLINK Edge SDK
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

namespace ThingBrowser
{
    public class Options
    {
        [Option('u', "THING_PROPERTIES_URI", Required = false, Default = "file://../config/ThingBrowserProperties.json", HelpText = "THING_PROPERTIES_URI")]
        public string ThingPropertiesURI { get; set; }
        [Option('r', "running-time", Required = false, Default = (ulong)60, HelpText = "Running time (seconds, 0 = infinite)")]
        public ulong RunningTime { get; set; }
    }
}
