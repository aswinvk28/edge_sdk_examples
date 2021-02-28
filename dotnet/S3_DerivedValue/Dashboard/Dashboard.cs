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
using System.Collections;
using System.Collections.Generic;
using Adlinktech.DataRiver;

namespace Dashboard
{

    public class TruckDataValue
    {
        public float Latitude { get; set; }
        public float Longitude { get; set; }
        public ulong LocationUpdateTime { get; set; }

        public double Distance { get; set; }
        public float Eta { get; set; }
        public ulong PositionUpdateTime { get; set; }

        public TruckDataValue()
        {
            Latitude = float.NaN;
            Longitude = float.NaN;
            LocationUpdateTime = 0;
            Distance = double.NaN;
            Eta = float.NaN;
            PositionUpdateTime = 0;
        }

    }

    public class Dashboard : IDisposable
    {
        static DateTime startTime;

        int ReadDelay = 500;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        private IDictionary<string, TruckDataValue> TruckData { get; set; }
        private string DistanceUnit = "";
        private string EtaUnit = "";

        public Dashboard(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            Stop = false;
            TruckData = new Dictionary<string, TruckDataValue>();
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Dashboard started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Dashboard stopped");
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
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/LocationDashboardThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(ulong runningTime)
        {
            startTime = DateTime.Now;

            // Clear console window before printing samples
            Console.Clear();

            do
            {
                // Retrieve and process location samples
                var locationSamples = Thing.ReadIotNvp("location", 0);
                foreach (var sample in locationSamples)
                {
                    ProcessLocationSample(sample);
                }

                // Retrieve and process distance samples
                var distanceSamples = Thing.ReadIotNvp("distance", 0);
                foreach (var sample in distanceSamples)
                {
                    ProcessDistanceSample(sample);
                }

                if (string.IsNullOrEmpty(DistanceUnit) || string.IsNullOrEmpty(EtaUnit))
                {
                    GetTagUnitDescriptions();
                }

                // Update console output
                DisplayStatus();

                // Sleep before next update
                System.Threading.Thread.Sleep(ReadDelay);

            } while (!Stop && (SecondsExecuting(startTime) < runningTime));

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }

        private void ProcessLocationSample(IotNvpDataSample dataSample)
        {
            if (dataSample.FlowState == FlowState.ALIVE)
            {
                GetLocationFromSample(dataSample);
            }
        }

        private void GetTagUnitDescriptions()
        {
            var tgr = DataRiver.DiscoveredTagGroupRegistry;
            var distanceTagGroup = tgr.FindTagGroup("Distance:com.adlinktech.example:v1.0");
            var typedefs = distanceTagGroup.TypeDefinitions;

            foreach (var typeD in typedefs)
            {
                foreach (var tag in typeD.Tags)
                {
                    if (tag.Name == "distance")
                    {
                        DistanceUnit = tag.Unit;
                    }
                    else if (tag.Name == "eta")
                    {
                        EtaUnit = tag.Unit;
                    }
                }
            }
        }


        private void ProcessDistanceSample(IotNvpDataSample dataSample)
        {
            if (dataSample.FlowState == FlowState.ALIVE)
            {
                GetDistanceFromSample(dataSample);
            }
        }


        private void GetDistanceFromSample(IotNvpDataSample dataSample)
        {
            double distance = 0.0;
            float eta = 0.0f;
            ulong timestamp = 0;

            var data = dataSample.Data;
            foreach (var nvp in data)
            {
                if (nvp.Name == "distance")
                {
                    distance = nvp.Value.Float64;
                }
                else if (nvp.Name == "eta")
                {
                    eta = nvp.Value.Float32;
                }
                else if (nvp.Name == "timestampUtc")
                {
                    timestamp = nvp.Value.UInt64;
                }
            }

            string key = dataSample.FlowId;
            if (!TruckData.ContainsKey(key))
            {
                TruckData.Add(key, new TruckDataValue());
            }
            var truck = TruckData[key];
            truck.Distance = distance;
            truck.Eta = eta;
            truck.PositionUpdateTime = timestamp;
        }

        private void GetLocationFromSample(IotNvpDataSample dataSample)
        {
            float lat = 0.0f;
            float lng = 0.0f;
            ulong timestamp = 0;

            var data = dataSample.Data;
            foreach (var nvp in data)
            {
                if (nvp.Name == "location")
                {
                    foreach (var locationNvp in nvp.Value.NvpSeq)
                    {
                        if (locationNvp.Name == "latitude")
                        {
                            lat = locationNvp.Value.Float32;
                        }
                        else if (locationNvp.Name == "longitude")
                        {
                            lng = locationNvp.Value.Float32;
                        }
                    }
                }
                else if (nvp.Name == "timestampUtc")
                {
                    timestamp = nvp.Value.UInt64;
                }
            }

            string key = dataSample.FlowId;
            if (!TruckData.ContainsKey(key))
            {
                TruckData.Add(key, new TruckDataValue());
            }
            var truck = TruckData[key];
            truck.Latitude = lat;
            truck.Longitude = lng;
            truck.LocationUpdateTime = timestamp;
        }

        private void DisplayStatus()
        {
            if( !Console.IsOutputRedirected )
            {
                // Move cursor position to the origin (0,0) of the console
                Console.SetCursorPosition(0, 0);
            }

            // Add header row for table
            DisplayHeader();

            // Write new data to console

            foreach (var item in TruckData)
            {
                string key = item.Key;
                TruckDataValue v = item.Value;

                Console.ForegroundColor = ConsoleColor.DarkGreen;
                Console.Write("{0,-20}", key);
                Console.ForegroundColor = ConsoleColor.Magenta;
                Console.Write("{0,-15}", FormatNumber(v.Latitude, 6));
                Console.Write("{0,-15}", FormatNumber(v.Longitude, 6));
                Console.ForegroundColor = ConsoleColor.DarkGreen;
                Console.Write("{0,-25}", FormatNumber(v.Distance, 3));
                Console.WriteLine("{0,-20}", FormatNumber(v.Eta, 1));
                Console.ResetColor();

                string locationTime = FormatTime(v.LocationUpdateTime);
                string positionTime = FormatTime(v.PositionUpdateTime);
                Console.ForegroundColor = ConsoleColor.DarkGray;
                Console.WriteLine("{0,-20}{1,-30}{2,-45}",
                    " ",
                    "  updated: " + locationTime,
                    "  updated: " + positionTime
                );
                Console.ResetColor();
            }

        }

        private void DisplayHeader()
        {
            Console.WriteLine("{0,-20}{1,-15}{2,-15}{3,-25}{4,-20}",
                "Truck Context",
                "Latitude",
                "Longitude",
                ("Distance (" + DistanceUnit + ")"),
                ("ETA (" + EtaUnit + ")")
            );

        }

        private string FormatTime(ulong time)
        {
            if (time == 0)
            {
                return "-";
            }
            DateTimeOffset offset = DateTimeOffset.FromUnixTimeSeconds((long)time);
            string localTime = offset.LocalDateTime.ToString("HH:mm:ss");
            return localTime;
        }

        private string FormatNumber(double num, int precision)
        {
            string result;

            if (float.NaN.Equals(num) || double.NaN.Equals(num))
            {
                result = "-";
            }
            else
            {
                result = num.ToString();
                result = num.ToString($"F{precision}");
            }
            return result;
        }

    }
}
