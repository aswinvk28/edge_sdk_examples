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
 * This code is part of example scenario 4 'Gateway Service' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

using System;
using System.Collections;
using System.Collections.Generic;
using Adlinktech.DataRiver;

namespace GatewayService
{
    public static class Globals
    {
        public static IDictionary<string, string> ThingContexts = new Dictionary<string, string>();
    }

    public class DataFlowValue
    {
        public uint SampleCount { get; set; }
        public FlowState MyFlowState { get; set; }

        public DataFlowValue()
        {
            SampleCount = 0;
        }
    }

    public class DataFlowKey
    {
        public TagGroup MyTagGroup { get; set; }
        public string MySourceThingClassId { get; set; }
        public string MySourceThingId { get; set; }
        public string MyFlowId { get; set; }

        public DataFlowKey(IotNvpDataSample dataSample)
        {
            MyTagGroup = dataSample.TagGroup;
            MySourceThingClassId = dataSample.SourceClass;
            MySourceThingId = dataSample.SourceId;
            MyFlowId = dataSample.FlowId;
        }

        public string GetSourceThingContext()
        {
            string context = Globals.ThingContexts[MySourceThingId];
            if (String.IsNullOrEmpty(context))
            {
                context = "<unknown>";
            }
            return context;
        }
    }

    public class DataFlowKeyComparer : IEqualityComparer<DataFlowKey>
    {
        public bool Equals(DataFlowKey dataFlowKey1, DataFlowKey dataFlowKey2)
        {
            if (dataFlowKey1 == null && dataFlowKey2 == null)
                return true;
            else if (dataFlowKey1 == null || dataFlowKey2 == null)
                return false;
            else if (string.Equals(dataFlowKey1.MyTagGroup.Name, dataFlowKey2.MyTagGroup.Name)
                && string.Equals(dataFlowKey1.MySourceThingClassId, dataFlowKey2.MySourceThingClassId)
                && string.Equals(dataFlowKey1.MySourceThingId, dataFlowKey2.MySourceThingId)
                && string.Equals(dataFlowKey1.MyFlowId, dataFlowKey2.MyFlowId))
                return true;
            else
                return false;
        }

        public int GetHashCode(DataFlowKey dataFlowKey)
        {
            string fullId = dataFlowKey.MyTagGroup.Name
                + dataFlowKey.MySourceThingClassId
                + dataFlowKey.MySourceThingId
                + dataFlowKey.MyFlowId;
            return fullId.GetHashCode();
        }

    }

    //Listeners
    public class NewThingDiscoveredListener : ThingDiscoveredListener
    {
        public override void NotifyThingDiscovered(DiscoveredThing thing)
        {
            Globals.ThingContexts[thing.Id] = thing.ContextId;
        }
    }

    public class GatewayService : IDisposable
    {
        static DateTime StartTime;

        int ReadDelayMS = 10;
        int TotalHeaderLines = 2;
        int TotalFooterMessageLines = 1;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }
        private int WindowHeightInLines { get; }

        private IDictionary<DataFlowKey, DataFlowValue> SampleCount { get; set; }

        public GatewayService(string thingPropertiesURI, int windowHeight)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            Stop = false;
            WindowHeightInLines = windowHeight;

            var comparer = new DataFlowKeyComparer();
            SampleCount = new Dictionary<DataFlowKey, DataFlowValue>(comparer);
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Gateway Service started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Gateway Service stopped");
        }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Console.WriteLine("Trapped Ctrl-C. Stopping.");
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
        }

        private static Thing CreateThing(in DataRiver dataRiver, string thingPropertiesURI)
        {
            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/GatewayServiceThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(int runningTime)
        {
            StartTime = DateTime.Now;

            // Add listener for discovering new Things
            var newThingDiscoveredListener = new NewThingDiscoveredListener();
            DataRiver.AddListener(newThingDiscoveredListener);

            // Get meta-data (contextId) for Things in discovered things registry
            ReadThingsFromRegistry();

            // Clear console window before printing samples
            Console.Clear();

            do
            {
                // Read data
                int timeout = (int)(runningTime - SecondsExecuting(StartTime)) * 1000;
                var msgs = Thing.ReadNextIotNvp("dynamicInput", timeout);

                // Loop received samples and update counters
                foreach (var msg in msgs)
                {
                    var flowState = msg.FlowState;

                    // Store state in value for this flow
                    var key = new DataFlowKey(msg);
                    if (!SampleCount.ContainsKey(key))
                    {
                        SampleCount.Add(key, new DataFlowValue());
                    }
                    var v = SampleCount[key];
                    v.MyFlowState = flowState;

                    // In case flow is alive or if flow is purged but sample
                    // contains data: increase sample count
                    bool sampleContainsData = (flowState == FlowState.ALIVE) || (msg.Data.Count > 0);

                    if (sampleContainsData)
                    {
                        v.SampleCount++;

                        // In a real-world use-case you would have additional processing
                        // of the data received by msg.getData()
                    }
                }

                // Update console output
                DisplayStatus();

                // Sleep before reading next samples
                System.Threading.Thread.Sleep(ReadDelayMS);

            } while (!Stop && SecondsExecuting(StartTime) < runningTime);

            // Remove listener
            DataRiver.RemoveListener(newThingDiscoveredListener);

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }

        private void ReadThingsFromRegistry()
        {
            var discoveredThingsRegistry = DataRiver.DiscoveredThingRegistry;
            var things = discoveredThingsRegistry.DiscoveredThings;
            foreach (var t in things)
            {
                Globals.ThingContexts[t.Id] = t.ContextId;
            }
        }

        private void DisplayStatus()
        {
            // Move cursor position to the origin (0,0) of the console
            Console.SetCursorPosition(0, 0);

            // Add header row for table
            DisplayHeader();

            // Write new data to console
            int lineCount = 0;

            foreach (var item in SampleCount)
            {
                DataFlowKey key = item.Key;
                DataFlowValue v = item.Value;

                // Set grey color for purged flows
                bool alive = v.MyFlowState == FlowState.ALIVE;
                var color1 = alive ? ConsoleColor.DarkGreen : Console.ForegroundColor;
                var color2 = alive ? ConsoleColor.Magenta : ConsoleColor.DarkGray;
                string flowStateStatus = alive ? "" : " <purged>";

                Console.ForegroundColor = color1;
                Console.Write("{0,-32}", Truncate(key.GetSourceThingContext() + flowStateStatus, 32));
                Console.ForegroundColor = color2;
                Console.Write("{0,-30}", Truncate(key.MyFlowId, 30));
                Console.ForegroundColor = color2;
                Console.Write("{0,-20}", Truncate(key.MyTagGroup.Name, 20));
                Console.ForegroundColor = ConsoleColor.DarkGray;
                Console.Write("{0,-12}", Truncate(key.MyTagGroup.QosProfile, 12));
                Console.ForegroundColor = color1;
                Console.WriteLine("{0,8}", v.SampleCount);
                Console.ResetColor();

                lineCount++;

                if (lineCount < SampleCount.Count &&
                    lineCount >= (WindowHeightInLines - TotalHeaderLines - TotalFooterMessageLines - 1))
                {
                    Console.Write($"... {SampleCount.Count - lineCount} more lines available. ");
                    Console.Write($"Set terminal height to {SampleCount.Count + TotalHeaderLines + TotalFooterMessageLines + 1}. ");
                    Console.WriteLine("See the README file for more instructions.");
                    break;
                }
            }

        }

        private void DisplayHeader()
        {
            Console.WriteLine("{0,-32}{1,-30}{2,-20}{3,-12}{4,8}\n",
              "Thing's ContextId",
              "Flow Id",
              "TagGroup Name",
              "QoS",
              "Samples"
          );

        }

        private string Truncate(string source, int length)
        {
            if (source.Length > length)
            {
                source = source.Substring(0, length);
            }
            return source;
        }
    }
}
