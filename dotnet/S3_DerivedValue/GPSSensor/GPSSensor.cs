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

namespace GPSSensor
{
    public class GPSSensor : IDisposable
    {

        static DateTime StartTimestamp;

        int MinSampleDelayMS = 1500;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }
        private float TruckLat { get; set; }
        private float TruckLng { get; set; }

        public GPSSensor(string thingPropertiesURI, float latitude, float longitude)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);

            TruckLat = latitude;
            TruckLng = longitude;
            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("GPS Sensor started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("GPS Sensor stopped");
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
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/GpsSensorThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(ulong runningTime)
        {
            var rand = new Random();

            StartTimestamp = DateTime.Now;
            do
            {
                // Simulate location change
                TruckLat += (float)(rand.Next() % 1000) / 100000.0f;
                TruckLng += (float)(rand.Next() % 1000) / 100000.0f;

                WriteSample(TruckLat, TruckLng, DateTimeOffset.Now.ToUnixTimeSeconds());

                // Wait for random interval
                System.Threading.Thread.Sleep(MinSampleDelayMS + (rand.Next() % 3000));

            } while (!Stop && (SecondsExecuting(StartTimestamp) < runningTime));

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }

        private void WriteSample(float latitude, float longitude, long timestamp)
        {
            // Create IoT data object
            IotNvpSeq sensorData = new IotNvpSeq();

            //location 
            var locationIoTNvpSeq = new IotNvpSeq();

            var latValue = new IotValue();
            latValue.Float32 = latitude;
            locationIoTNvpSeq.Add(new IotNvp("latitude", latValue));

            var lngValue = new IotValue();
            lngValue.Float32 = longitude;
            locationIoTNvpSeq.Add(new IotNvp("longitude", lngValue));

            var locationValue = new IotValue();
            locationValue.NvpSeq = locationIoTNvpSeq;

            sensorData.Add(new IotNvp("location", locationValue));

            //timestamp
            var timestampValue = new IotValue();
            timestampValue.UInt64 = (uint)timestamp;

            sensorData.Add(new IotNvp("timestampUtc", timestampValue));

            // Write data to DataRiver
            Thing.Write("location", sensorData);
        }

    }
}
