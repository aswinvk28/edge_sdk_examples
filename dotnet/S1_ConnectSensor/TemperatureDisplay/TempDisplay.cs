/*                         ADLINK Edge SDK
 *
 *   This software and documentation are Copyright 2018 to 2020 ADLINK
 *   Technology Limited, its affiliated companies and licensors. All rights
 *   reserved.
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
 * This code is part of example scenario 1 'Connect a Sensor' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.  
 */

using System;
using Adlinktech.DataRiver;

namespace TemperatureDisplay
{
    public class TempDisplay : IDisposable
    {
        static DateTime startTime;

        int ReadSampleDelay = 100;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        public TempDisplay(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Temperature Display started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Temperature Display stopped");
        }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Console.WriteLine("Trapped Ctrl-C. Stopping.");
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
        }

        private static Thing CreateThing(in DataRiver dataRiver, string thingPropertiesURI)
        {
            // Create and Populate the TagGroup registry with JSON resource files.
            var tgr = new JSonTagGroupRegistry();
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json");
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/TemperatureDisplayThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(Options options)
        {
            startTime = DateTime.Now;

            do
            {
                // Read all data for input 'temperature', timeout 1000 MS
                var msgs = Thing.ReadIotNvp("temperature", 1000);
               
                foreach (var msg in msgs)
                {
                    if (msg.FlowState == FlowState.ALIVE)
                    {
                        var data = msg.Data;
                        float temperature = 0.0f;

                        foreach (var nvp in data)
                        {
                            if (nvp.Name == "temperature")
                            {
                                temperature = nvp.Value.Float32;
                            }
                        }
                        Console.WriteLine($"Sensor data received: {temperature,5:F1}");
                    }
                }

                // Wait for some time (in milliseconds) before reading next samples
                System.Threading.Thread.Sleep(ReadSampleDelay);

            } while (!Stop && (SecondsExecuting(startTime) < options.RunningTime));

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }

    }
}
