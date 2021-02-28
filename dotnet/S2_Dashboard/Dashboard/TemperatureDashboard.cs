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
 * This code is part of example scenario 1 'Connect a Sensor' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.  
 */

using System;
using Adlinktech.DataRiver;

namespace Dashboard
{

    // listeners 
    public class TemperatureSensorDiscoveredListener : ThingDiscoveredListener
    {
        public override void NotifyThingDiscovered(DiscoveredThing thing)
        {
            if (thing.ClassId.Name == "TemperatureSensor")
            {
                Console.ForegroundColor = ConsoleColor.DarkGreen;
                Console.WriteLine("New temperature sensor discovered: "
                    + thing.Description
                    + " ("
                    + thing.Id
                    + ")"
                );
            }
            else
            {
                Console.ForegroundColor = ConsoleColor.DarkGray;
                Console.WriteLine("New incompatible sensor type: "
                    + thing.ClassId.Name
                    + " ("
                    + thing.Id
                    + ")"
                );
            }
            Console.ResetColor();
        }
    }

    public class TemperatureSensorLostListener : ThingLostListener
    {
        public override void NotifyThingLost(DiscoveredThing thing)
        {
            if (thing.ClassId.Name == "TemperatureSensor")
            {
                Console.ForegroundColor = ConsoleColor.Magenta;
                Console.WriteLine("Temperature sensor stopped: "
                    + thing.Description
                    + " ("
                    + thing.Id
                    + ")"
                );
            }
            else
            {
                Console.ForegroundColor = ConsoleColor.DarkGray;
                Console.WriteLine("Other sensor stopped: "
                    + thing.ClassId.Name
                    + " ("
                    + thing.Id
                    + ")"
                );
            }
            Console.ResetColor();
        }
    }

    public class TemperatureDashboard : IDisposable
    {
        static DateTime startTime;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        public TemperatureDashboard(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Temperature Dashboard started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Temperature Dashboard stopped");
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
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file. 
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(string floor, ulong runningTime)
        {
            startTime = DateTime.Now;

            // Add listener for new Things
            var temperatureSensorDiscoveredListener = new TemperatureSensorDiscoveredListener();
            DataRiver.AddListener(temperatureSensorDiscoveredListener);

            // Add listener for lost Things
            var temperatureSensorLostListener = new TemperatureSensorLostListener();
            DataRiver.AddListener(temperatureSensorLostListener);

            // Create selector for selecting current floor's temperature from input 'temperature'
            string selection = floor + ".*";

            var selector = Thing.Select("temperature").Flow(selection);
            Console.WriteLine("Selecting data using flowId selection: " + selection);

            do
            {
                // Read data using selector
                var msgs = selector.Read((int)(runningTime - SecondsExecuting(startTime)) * 1000);

                // Process samples
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

                        // Show output
                        Console.WriteLine("Temperature data received for flow "
                             + msg.FlowId + ": "
                             + $"{temperature,3:F1}");
                    }
                }
                
            } while (!Stop && SecondsExecuting(startTime) < runningTime);

            // Remove listeners
            DataRiver.RemoveListener(temperatureSensorLostListener);
            DataRiver.RemoveListener(temperatureSensorDiscoveredListener);

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (uint)(DateTime.Now - originalTime).TotalSeconds;
        }

    }
}
