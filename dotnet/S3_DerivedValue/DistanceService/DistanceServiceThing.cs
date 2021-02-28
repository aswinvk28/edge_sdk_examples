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
 * This code is part of example scenario 3 'Derived Value Service' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

using System;
using Adlinktech.DataRiver;

namespace DistanceService
{

    public interface IDistanceServiceThing
    {
        float GetWarehouseLat();
        float GetWarehouseLng();
        void WriteDistance(string myLocationFlowId, double distance, float eta, ulong timestamp);
    }

    //listener
    public class GpsSensorDataListener : IotNvpDataAvailableListener
    {
        private IDistanceServiceThing DistanceServiceThingy { get; }

        private double CalculateDistance(float truckLocationLat, float truckLocationLng)
        {
            var x = Math.Pow(truckLocationLat - DistanceServiceThingy.GetWarehouseLat(), 2);
            var y = Math.Pow(truckLocationLng - DistanceServiceThingy.GetWarehouseLng(), 2);
            return Math.Sqrt(x + y);
        }

        public GpsSensorDataListener(IDistanceServiceThing distanceServiceThing)
        {
            DistanceServiceThingy = distanceServiceThing;
        }

        public override void NotifyDataAvailable(IotNvpDataSampleSeq data)
        {
            foreach (var locationMessage in data)
            {
                string myLocationFlowId = locationMessage.FlowId;

                if (locationMessage.FlowState == FlowState.ALIVE)
                {
                    // Get location data from sample
                    var locationData = locationMessage.Data;
                    float truckLocationLat = 0.0f;
                    float truckLocationLng = 0.0f;
                    ulong timestamp = 0;

                    try
                    {
                        foreach (var nvp in locationData)
                        {
                            if (nvp.Name == "location")
                            {
                                foreach (var locationNvp in nvp.Value.NvpSeq)
                                {
                                    if (locationNvp.Name == "latitude")
                                    {
                                        truckLocationLat = locationNvp.Value.Float32;
                                    }
                                    else if (locationNvp.Name == "longitude")
                                    {
                                        truckLocationLng = locationNvp.Value.Float32;
                                    }
                                }
                            }
                            else if (nvp.Name == "timestampUtc")
                            {
                                timestamp = nvp.Value.UInt64;
                            }
                        }
                    }
                    catch (ApplicationException e)
                    {
                        Console.WriteLine($"An unexpected error occured while processing data-sample: {e}");
                        continue;
                    }

                    // Calculate distance to the warehouse
                    double distance = CalculateDistance(truckLocationLat, truckLocationLng);

                    // This example uses a fixed multiplier for ETA. In a real-world
                    // scenario this would be calculated based on e.g. real-time traffic information
                    float eta = (float)distance * 5.12345f;

                    DistanceServiceThingy.WriteDistance(myLocationFlowId, distance, eta, timestamp);
                }
            }
        }
    }

    public class DistanceServiceThing : IDisposable, IDistanceServiceThing
    {
        static DateTime StartTimestamp;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        private float WarehouseLat { get; set; }
        private float WarehouseLng { get; set; }

        public DistanceServiceThing(string thingPropertiesURI, float latitude, float longitude)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);

            WarehouseLat = latitude;
            WarehouseLng = longitude;
            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Distance Service started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Distance Service stopped");
        }

        public float GetWarehouseLat()
        {
            return WarehouseLat;
        }

        public float GetWarehouseLng()
        {
            return WarehouseLng;
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
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/LocationTagGroup.json");
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/DistanceTagGroup.json");
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/DistanceServiceThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(ulong runningTime)
        {
            // Use custom dispatcher for processing events
            var dispatcher = new Dispatcher();

            // Add listener for new GPS sensor Things using our custom dispatcher
            var gpsDataReceivedListener = new GpsSensorDataListener(this);
            Thing.AddListener(gpsDataReceivedListener, dispatcher);

            StartTimestamp = DateTime.Now;
            do
            {
                try
                {
                    // block the call for 1000ms
                    dispatcher.ProcessEvents(1000);
                }
                catch (TimeoutError)
                {
                    // Ignore.
                }

            } while (!Stop && (SecondsExecuting(StartTimestamp) < runningTime));

            // Remove listener
            Thing.RemoveListener(gpsDataReceivedListener, dispatcher);

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }

        public void WriteDistance(string myLocationFlowId, double distance, float eta, ulong timestamp)
        {
            // Create IoT data object            
            IotNvpSeq distanceData = new IotNvpSeq();

            //distance
            var distanceValue = new IotValue();
            distanceValue.Float64 = distance;
            distanceData.Add(new IotNvp("distance", distanceValue));

            //eta
            var etaValue = new IotValue();
            etaValue.Float32 = eta;
            distanceData.Add(new IotNvp("eta", etaValue));

            //timestamp
            var timestampValue = new IotValue();
            timestampValue.UInt64 = timestamp;   //TODO check this
            distanceData.Add(new IotNvp("timestampUtc", timestampValue));

            // Write distance to DataRiver using flow ID from incoming location sample
            Thing.Write("distance", myLocationFlowId, distanceData);
        }

    }
}
