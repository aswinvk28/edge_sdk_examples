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
 * This is a simple throughput application measuring obtainable 
 * throughput using the Edge SDK
 */

using System;
using System.Collections.Generic;
using Adlinktech.DataRiver;

namespace Pinger
{
    public class Ping : IDisposable
    {

        public static ExampleTimeStats RoundTripOverall = new ExampleTimeStats();
        public static ExampleTimeStats WriteAccessOverall = new ExampleTimeStats();
        public static ExampleTimeStats ReadAccessOverall = new ExampleTimeStats();    

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }
        private IotNvpSeq SampleData { get; }          

        public Ping(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            SampleData = new IotNvpSeq();
            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("# Ping started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("# Ping stopped");
        }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
            SendTerminate();

            ShowStats(true, 0, RoundTripOverall, WriteAccessOverall, ReadAccessOverall);

        }

        private static Thing CreateThing(in DataRiver dataRiver, string thingPropertiesURI)
        {
            // Create and Populate the TagGroup registry with JSON resource files.
            var tgr = new JSonTagGroupRegistry();
            tgr.RegisterTagGroupsFromUri("file://definitions/TagGroup/com.adlinktech.example/PingTagGroup.json");
            tgr.RegisterTagGroupsFromUri("file://definitions/TagGroup/com.adlinktech.example/PongTagGroup.json");
            dataRiver.AddTagGroupRegistry(tgr);

            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://definitions/ThingClass/com.adlinktech.example/PingThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);

            return dataRiver.CreateThing(tp);
        }

        public int Run(Options options)
        {
            var payloadSize = options.PayloadSize;
            var numSamples = options.NumSamples;
            var runningTime = options.RunningTime;

            DateTime startTime;
            DateTime preWriteTime;
            DateTime postWriteTime;
            DateTime preReadTime;
            DateTime postReadTime;

	    int waitTimeout = 10000;

	    ExampleTimeStats roundTrip = new ExampleTimeStats();
	    ExampleTimeStats writeAccess = new ExampleTimeStats();
	    ExampleTimeStats readAccess = new ExampleTimeStats();

	    Console.WriteLine("# Parameters: payload size: " + payloadSize 
                + " | number of samples: "  + numSamples
                + " | running time: " + runningTime);

	    // Wait for the Pong Thing
	    WaitForPong();

	    // Init payload
	    InitPayload(payloadSize);

	    // Warm-up for 5s
	    WarmUp();

	    Console.WriteLine("# Round trip measurements (in us)");

	    Console.ForegroundColor = ConsoleColor.Magenta;
            Console.WriteLine("#             Round trip time [us]         Write-access time [us]       Read-access time [us]");
            Console.WriteLine("# Seconds     Count   median      min      Count   median      min      Count   median      min");
            Console.ResetColor();

	    startTime = DateTime.Now;	    
            ulong elapsedSeconds = 0;

	    for (ulong i = 0; numSamples == 0 || i < numSamples; i++)
            {
	        preWriteTime = DateTime.Now;		
                Thing.Write("Ping", SampleData);		
                postWriteTime = DateTime.Now;

		// Read sample
		preReadTime = DateTime.Now;
                var samples = Thing.ReadIotNvp("Pong", waitTimeout);
                postReadTime = DateTime.Now;

                if (Stop)
                {
		    return 0;
                }

		// Validate sample count
		if (samples.Count != 1)
                {
                    Console.WriteLine("ERROR: Ping received " + samples.Count + " samples but was expecting 1."); 
                    return 1;
                }

		// Update stats
		var writeTimeMicroSecs = Convert.ToUInt64((postWriteTime.Subtract(preWriteTime).TotalSeconds) * 1000000);
                var readTimeMicroSecs = Convert.ToUInt64((postReadTime.Subtract(preReadTime).TotalSeconds) * 1000000);
                var roundTripMicroSecs = Convert.ToUInt64((postReadTime.Subtract(preWriteTime).TotalSeconds) * 1000000);

                writeAccess.AddStats(writeTimeMicroSecs);
                readAccess.AddStats(readTimeMicroSecs);
                roundTrip.AddStats(roundTripMicroSecs);
                WriteAccessOverall.AddStats(writeTimeMicroSecs);
                ReadAccessOverall.AddStats(readTimeMicroSecs);
                RoundTripOverall.AddStats(roundTripMicroSecs);

		// Print stats each second
		if ( (postReadTime.Subtract(startTime).TotalSeconds) > 1 ||  (i == numSamples - 1) )  
                {
		    // Print stats
		    ShowStats(false, ++elapsedSeconds, roundTrip, writeAccess, readAccess);

		    // Reset stats for next run
		    roundTrip.ResetStats();
                    writeAccess.ResetStats();
                    readAccess.ResetStats();

		    // Set values for next run
		    startTime = DateTime.Now;

		    // Check for timeout
		    if (runningTime > 0 && elapsedSeconds >= runningTime) 
                    {
		        break;
		    }
		}

	    }  

	    // Print overall stats
	    ShowStats(true, 0, RoundTripOverall, WriteAccessOverall, ReadAccessOverall);

	    return 0;
        }

        private void ShowStats(bool overall, 
            ulong elapsedSeconds, 
            ExampleTimeStats roundTrip, 
            ExampleTimeStats writeAccess, 
            ExampleTimeStats readAccess)
        {
            if (overall)
            {
                Console.ForegroundColor = ConsoleColor.DarkGreen;
                Console.Write("# Overall");
            }
            else 
            {
                Console.Write("{0, 9}", elapsedSeconds);  
            }

	     Console.Write("{0,10}", roundTrip.SortedValues.Count);
             Console.Write("{0,9}", roundTrip.CalculateMedian());
             Console.Write("{0,9}", roundTrip.Min);
             Console.Write("{0,11}", writeAccess.SortedValues.Count);
             Console.Write("{0,9}", writeAccess.CalculateMedian());
             Console.Write("{0,9}", writeAccess.Min);
             Console.Write("{0,11}", readAccess.SortedValues.Count);
             Console.Write("{0,9}", readAccess.CalculateMedian());             
             Console.WriteLine("{0,9}", readAccess.Min);
             Console.ResetColor();

        }

        private void WaitForPong() 
        {
            // wait for pong to appear by discovering its thingId and thingClass
            Console.WriteLine("# Waiting for pong to run...");
            var discoveredThingRegistry = DataRiver.DiscoveredThingRegistry;
            bool readerFound = false;

            while (!Stop && !readerFound) 
            {
                try 
                {
                    var thing = discoveredThingRegistry.FindDiscoveredThing("pongThing1", "Pong:com.adlinktech.example:v1.0");
                    readerFound = true;
                }
                catch (Exception e) 
                {
                    //Sleep for 1 second
                    System.Threading.Thread.Sleep(1000);
                }
            }
        }

        private void InitPayload(ulong payloadSize)
        {
            var payload = new IotValue();
            var byteSequence = new IotByteSeq();
            for (ulong i = 0; i < payloadSize; i++)
            {
                byte a = (byte)'a';
                byteSequence.Add(a);
            }
            payload.ByteSeq = byteSequence;
            SampleData.Add(new IotNvp("payload", payload));

        }

        private void WarmUp()
        {
            var startTime = DateTime.Now;
            var waitTimeout = 10000;

            Console.WriteLine("# Warming up 5s to stabilise performance...");
            while ( ((DateTime.Now - startTime).TotalSeconds ) < 5)
            {
                Thing.Write("Ping", SampleData);
                Thing.ReadIotNvp("Pong", waitTimeout);
            }
            Console.WriteLine("# Warm up complete");

        }

        private int SendTerminate() 
        {
            Console.WriteLine("# Sending termination request.");
            Thing.Purge("Ping", "ping");
            System.Threading.Thread.Sleep(1000);
            return 0;
        }       

    }


    public class ExampleTimeStats 
    {
        public SortedList<int, ulong> SortedValues { get; set; }
        public double Average { get; set; }   
        public ulong Min { get; set; }
        public ulong Max { get; set; }
       
        public ExampleTimeStats()
        {
            SortedValues = new SortedList<int, ulong>();
            Average = 0;
            Min = 0;
            Max = 0;            
        }

        public void AddStats(ulong microseconds)
        {                        
	    Average = ((SortedValues.Count * Average) + microseconds) / (SortedValues.Count + 1);             
            SortedValues.Add(SortedValues.Count, microseconds);

            // check min
            if (Min == 0 || microseconds < Min)
            {
                Min = microseconds;
            }
            
            // check max
            if (microseconds > Max)
            {
                Max = microseconds;
            }           
        }

        public void ResetStats()
        {
            SortedValues = new SortedList<int, ulong>();
            Average = 0;
            Min = 0;
            Max = 0; 
        }

        public ulong CalculateMedian()
        {
             var stats = SortedValues.Values;

             if (SortedValues.Count % 2 == 0) 
             {
                 ulong value1 = stats[SortedValues.Count / 2];
                 ulong value2 = stats[(SortedValues.Count / 2) + 1];   
                 return  (value1 + value2) / 2;
             }
             else 
             {
                 int medianIndex = (SortedValues.Count + 1)/ 2;
                 return stats[medianIndex];
             }
        }

    }

}
