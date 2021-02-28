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
using Adlinktech.DataRiver;

namespace LightSensor
{
    public class LightSensor : IDisposable
    {
        int LightSampleDelayMS = 1000;
        int IlluminanceThreshold = 400;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        public LightSensor(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);

            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Light Sensor started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Light Sensor stopped");
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
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/IlluminanceTagGroup.json");
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/IlluminanceAlarmTagGroup.json");
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/LightSensorThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(int runningTime)
        {
            int sampleCount = (runningTime * 1000) / LightSampleDelayMS;
            uint actualIlluminance = 500;
            bool alarmState = false;

            while (!Stop && (sampleCount-- > 0))
            {
                // Simulate illuminance change
                int dir = (sampleCount % 20) > 10 ? -1 : 1;
                actualIlluminance += (uint)dir * 30;

                // Write sensor data to river
                WriteSample(actualIlluminance);

                // Write alarm if value below threshold
                if (!alarmState && (actualIlluminance < IlluminanceThreshold))
                {
                    Alarm("Illuminance below threshold");
                    alarmState = true;
                }
                else if (alarmState && (actualIlluminance > IlluminanceThreshold))
                {
                    alarmState = false;
                }

                System.Threading.Thread.Sleep(LightSampleDelayMS);
            }
            return 0;

        }

        private void WriteSample(uint illuminance)
        {
            IotNvpSeq sensorData = new IotNvpSeq();
            var illuminanceValue = new IotValue();
            illuminanceValue.UInt32 = illuminance;
            sensorData.Add(new IotNvp("illuminance", illuminanceValue));

            Thing.Write("illuminance", sensorData);
        }

        private void Alarm(string message)
        {
            IotNvpSeq alarmData = new IotNvpSeq();
            var alarmValue = new IotValue();
            alarmValue.String = message;
            alarmData.Add(new IotNvp("alarm", alarmValue));

            Thing.Write("alarm", alarmData);
        }

    }
}
