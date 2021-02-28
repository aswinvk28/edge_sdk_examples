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

using System;
using Adlinktech.DataRiver;

namespace Sensors
{
    public class FuelLevelSensor : IDisposable
    {
        int SampleDelayMS = 3000;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }
        
        public FuelLevelSensor(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();            
            Thing = CreateThing(DataRiver, thingPropertiesURI);            

            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Fuel Level Sensor started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Fuel Level Sensor stopped");
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
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/SensorStateTagGroup.json");
            tgr.RegisterTagGroupsFromUri("file://./definitions/TagGroup/com.adlinktech.example/FuelLevelTagGroup.json");
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/FuelLevelSensorThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);            

            return dataRiver.CreateThing(tp);
        }

        public int Run(ulong runningTime)
        {
            var rand = new Random();
            ulong sampleCount = (runningTime * 1000) / (ulong) SampleDelayMS; 
            float fuelLevel = 1000.0f;

            while (!Stop && (sampleCount-- > 0))
            {
                // Simulate fuel level change
                fuelLevel -= (float)(rand.Next() % 100) / 10.0f;
                WriteSample(fuelLevel);

                System.Threading.Thread.Sleep(SampleDelayMS);
            }             

            return 0;
        }  

        private void WriteSample(float level)
        {
            IotNvpSeq data = new IotNvpSeq();
            var levelValue = new IotValue();
            levelValue.Float32 = level;
            data.Add(new IotNvp("level", levelValue));

            Thing.Write("level", data);
        } 

        private void SetState(string state)
        {
            IotNvpSeq data = new IotNvpSeq();
            var stateValue = new IotValue();
            stateValue.String = state;
            data.Add(new IotNvp("state", stateValue));

            Thing.Write("state", data);
        }    

    }
}
