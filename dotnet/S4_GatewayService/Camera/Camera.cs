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
using System.IO;
using System.Threading;
using System.Collections;
using System.Collections.Generic;
using Adlinktech.DataRiver;

namespace Camera
{

    public interface ICamera
    {
        bool IsRelated(DiscoveredThing thing);
        void DiscoveredRelatedCamera(string thingId, string contextId);
        void LostRelatedCamera(string thingId);
    }

    // listeners 
    public class CameraThingDiscoveredListener : ThingDiscoveredListener
    {
        private ICamera CameraThingy { get; }

        public CameraThingDiscoveredListener(ICamera cameraThing)
        {
            CameraThingy = cameraThing;
        }

        public override void NotifyThingDiscovered(DiscoveredThing thing)
        {
            if (CameraThingy.IsRelated(thing))
            {
                CameraThingy.DiscoveredRelatedCamera(thing.Id, thing.ContextId);
            }
        }
    }

    public class CameraThingLostListener : ThingLostListener
    {
        private ICamera CameraThingy { get; }

        public CameraThingLostListener(ICamera cameraThing)
        {
            CameraThingy = cameraThing;
        }

        public override void NotifyThingLost(DiscoveredThing thing)
        {
            if (CameraThingy.IsRelated(thing))
            {
                CameraThingy.LostRelatedCamera(thing.Id);
            }
        }
    }

    public static class StaticRandom
    {
        static readonly ThreadLocal<Random> random =
            new ThreadLocal<Random>(() => new Random());

        public static int Rand(int minValue, int maxValue)
        {
            return random.Value.Next(minValue, maxValue);
        }
    }

    public class Camera : ICamera, IDisposable
    {

        static DateTime StartTimestamp;

        int CameraDelayMS = 100;
        int CameraInitialDelayMS = 2000;
        int BarcodeIntervalMS = 5000;
        int BarcodeLifespanMS = 15000;
        int CameraSampleDelayMS = 1000;
        int BarcodeSkipPercentage = 25;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        private IDictionary<string, string> RelatedCameras { get; set; }
        private List<Thread> Threads;
        private bool Closed = false;

        public Camera(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            RelatedCameras = new Dictionary<string, string>();
            Threads = new List<Thread>();

            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Camera started");

            SetState("on");
        }

        public void Dispose()
        {
            try
            {
                // Set camera state to 'off'
                SetState("off");
            }
            catch (ThingAPIException e)
            {
                Console.WriteLine($"Error setting camera state to off: {e.Message}");
            }

            // Stop and join threads
            Closed = true;
            foreach (Thread myThread in Threads)
            {
                myThread.Join();
            }

            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Camera stopped");
        }

        public bool IsRelated(DiscoveredThing compareThing)
        {
            string parentContext1 = GetParentContext(compareThing.ContextId);
            string parentContext2 = GetParentContext(Thing.ContextId);
            bool isRelated = (String.Equals(parentContext1, parentContext2))
                && (String.Equals(compareThing.ClassId.KeyString, Thing.ClassId.KeyString))
                && (!String.Equals(compareThing.Id, Thing.Id));
            return isRelated;
        }

        public void DiscoveredRelatedCamera(string thingId, string contextId)
        {
            if (!RelatedCameras.ContainsKey(thingId))
            {
                Console.WriteLine("Camera " + Thing.ContextId
                    + ": detected other camera with context "
                    + contextId + " (Thing Id " + thingId + ")");
            }
            RelatedCameras[thingId] = contextId;
        }

        public void LostRelatedCamera(string thingId)
        {
            RelatedCameras.Remove(thingId);
        }

        public int Run(ulong runningTime, string barcodeFilePath)
        {
            StartTimestamp = DateTime.Now;

            int barcodeIndex = 0;
            DateTime barcodeTimestamp = StartTimestamp.AddMilliseconds(-BarcodeIntervalMS);

            // Get barcodes
            List<string> barcodes = ReadBarCodes(barcodeFilePath);
            if (barcodes.Count == 0)
            {
                Console.WriteLine("Error: no barcodes found");
                return 1;
            }

            // Add listener for new Things
            var cameraDiscoveredListener = new CameraThingDiscoveredListener(this);
            DataRiver.AddListener(cameraDiscoveredListener);

            // Add listener for lost Things
            var cameraLostListener = new CameraThingLostListener(this);
            DataRiver.AddListener(cameraLostListener);

            // Check for related camera already in the discovered things registry
            System.Threading.Thread.Sleep(CameraInitialDelayMS);
            CheckRegistryForRelatedCameras();

            do
            {
                var now = DateTime.Now;

                // Check if next barcode should be read
                if (barcodeIndex < barcodes.Count
                    && ((now.Subtract(barcodeTimestamp).TotalSeconds * 1000) > BarcodeIntervalMS))
                {
                    string barcode = barcodes[barcodeIndex];
                    barcodeIndex++;

                    // Randomly skip some of the barcodes
                    if (StaticRandom.Rand(0, 100) > BarcodeSkipPercentage)
                    {
                        ParameterizedThreadStart pts = new ParameterizedThreadStart(BarcodeTask);
                        Thread t = new Thread(pts);
                        Threads.Add(t);
                        t.Start(barcode);
                    }

                    // Update timestamp
                    barcodeTimestamp = now;
                }

                // Sleep for some time
                System.Threading.Thread.Sleep(CameraDelayMS);

            } while (!Stop && SecondsExecuting(StartTimestamp) < runningTime);

            // Remove listeners
            DataRiver.RemoveListener(cameraLostListener);
            DataRiver.RemoveListener(cameraDiscoveredListener);

            return 0;
        }

        private List<string> ReadBarCodes(string barcodeFilePath)
        {
            List<string> barcodes = new List<string>();

            if (!File.Exists(barcodeFilePath))
            {
                Console.WriteLine("Cannot open barcode file: " + barcodeFilePath);
                return barcodes;
            }

            using (StreamReader sr = new StreamReader(barcodeFilePath))
            {
                while (sr.Peek() >= 0)
                {
                    string line = sr.ReadLine();
                    if (line.Length > 0)
                    {
                        barcodes.Add(line);
                    }
                }
            }
            return barcodes;
        }


        private static Thing CreateThing(in DataRiver dataRiver, string thingPropertiesURI)
        {
            // Create and Populate the TagGroup registry with JSON resource files.
            var tgr = new JSonTagGroupRegistry();
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/CameraStateTagGroup.json");
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/ObservationTagGroup.json");
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/CameraThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Console.WriteLine("Trapped Ctrl-C. Stopping.");
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }

        private bool HasRelatedCameras()
        {
            return RelatedCameras.Count > 0;
        }

        private void BarcodeTask(object barcode)
        {
            var start = DateTime.Now;

            int x = StaticRandom.Rand(0, 100);
            int y = StaticRandom.Rand(0, 100);
            int z = StaticRandom.Rand(0, 100);

            do
            {
                // Simulate position change
                x += StaticRandom.Rand(-5, 5);
                y += StaticRandom.Rand(-5, 5);
                z += StaticRandom.Rand(-1, 1);

                // Sleep before sending next update
                System.Threading.Thread.Sleep(CameraSampleDelayMS);

                // Send location update for this barcode
                WriteSample((string)barcode, x, y, z);

            } while (!Closed && MillisecondsExecuting(start) < BarcodeLifespanMS);

            PurgeFlow((string)barcode);
        }

        private uint MillisecondsExecuting(DateTime originalTime)
        {
            return (uint)(DateTime.Now.Subtract(originalTime).TotalSeconds) * 1000;
        }

        private void CheckRegistryForRelatedCameras()
        {
            var dtr = DataRiver.DiscoveredThingRegistry;
            var things = dtr.DiscoveredThings;
            foreach (var t in things)
            {
                if (IsRelated(t))
                {
                    DiscoveredRelatedCamera(t.Id, t.ContextId);
                }
            }
        }

        private string GetParentContext(string contextId)
        {
            int index = contextId.LastIndexOf('.');
            if (index != -1)
            {
                return contextId.Substring(0, index);
            }
            return contextId;
        }

        private string GetFlowId(string barcode)
        {
            string flowId;
            if (HasRelatedCameras())
            {
                flowId = GetParentContext(Thing.ContextId) + ".cameras." + barcode;
            }
            else
            {
                flowId = Thing.ContextId + "." + barcode;
            }
            return flowId;
        }

        private void PurgeFlow(string barcode)
        {
            Thing.Purge("observation", GetFlowId(barcode));
        }

        private void SetState(string state)
        {
            IotNvpSeq data = new IotNvpSeq();
            var stateValue = new IotValue();
            stateValue.String = state;
            data.Add(new IotNvp("state", stateValue));

            Thing.Write("state", data);
        }

        private void WriteSample(string barcode, int x, int y, int z)
        {
            // Create IoT data object
            IotNvpSeq cameraData = new IotNvpSeq();

            var barcodeValue = new IotValue();
            barcodeValue.String = barcode;

            var xValue = new IotValue();
            xValue.Int32 = x;

            var yValue = new IotValue();
            yValue.Int32 = y;

            var zValue = new IotValue();
            zValue.Int32 = z;

            cameraData.Add(new IotNvp("barcode", barcodeValue));
            cameraData.Add(new IotNvp("position_x", xValue));
            cameraData.Add(new IotNvp("position_y", yValue));
            cameraData.Add(new IotNvp("position_z", zValue));

            // Write data to DataRiver
            Thing.Write("observation", GetFlowId(barcode), cameraData);
        }

    }
}
