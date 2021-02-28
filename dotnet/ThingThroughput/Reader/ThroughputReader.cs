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
  * This is a simple throughput application measuring obtainable throughput using the thingSDK
  *
  */
using System;
using Adlinktech.DataRiver;

namespace Reader
{
    public class ThroughputReader : IDisposable
    {
        // constants
        int BytesPerMegaBit = 125000;

        // statistics
        private ulong sampleCount = 0;
        private DateTime currentTime;
        static DateTime startTime;
        private ulong bytesReceived = 0;
        private ulong outOfOrderCount = 0;
        private ulong batchCount = 0;
        private ulong batchMaxSize = 0;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }

        public ThroughputReader()
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver);
            Stop = false;
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Throughput reader started");
        }

        public void Dispose()
        {
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Throughput reader stopped");
        }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Console.WriteLine("Reader trapped Ctrl-C. Stopping.");
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
        }

        private static Thing CreateThing(in DataRiver dataRiver)
        {
            var tgr = new JSonTagGroupRegistry();
            tgr.RegisterTagGroupsFromString(Utilities.GetEmbeddedResourceContent("Reader.definitions.TagGroup.com.adlinktech.example.ThroughputTagGroup.json"));
            dataRiver.AddTagGroupRegistry(tgr);

            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromString(Utilities.GetEmbeddedResourceContent("Reader.definitions.ThingClass.com.adlinktech.example.ThroughputReaderThingClass.json"));
            dataRiver.AddThingClassRegistry(tcr);

            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromString(Utilities.GetEmbeddedResourceContent("Reader.config.ThroughputReaderProperties.json"));

            return dataRiver.CreateThing(tp);
        }

        public int Run(Options options)
        {
            Console.WriteLine("Waiting for samples...");
            ulong? lastReceivedSequenceNumber = null;

            startTime = DateTime.Now;

            // keep some details of the previous interation
            DateTime prevTime = startTime;
            ulong prevReceived = 0;
            ulong prevCount = 0;

            while(!Stop && (options.RunningTime == 0 || SecondsExecuting(startTime) < options.RunningTime))
            {
                if(options.PollingDelay > 0)
                {
                    System.Threading.Thread.Sleep((int)options.PollingDelay);
                }

                // New batch
                batchCount++;
                ulong sampleCountAtStartOfBatch = sampleCount;

                ulong payloadSize = 0;

                // Take samples and iterate through them
                var samples = Thing.ReadIotNvp("ThroughputInput", DataRiverConstants.BLOCKING_TIME_INFINITE);
                foreach (var sample in samples)
                {
                    if(sample.FlowState == FlowState.ALIVE)
                    {
                        var data = sample.Data;
                  
                        // find the message stored in the name-value-pair with name 'name'
                        ulong? receivedSequenceNumber = null;
                        foreach (var nvp in data)
                        {
                            if(nvp.Name == "sequencenumber")
                            {
                                receivedSequenceNumber = nvp.Value.UInt64;
                            }
                            else if(nvp.Name == "sequencedata")
                            {
                                var receivedData = nvp.Value.ByteSeq;
                                payloadSize = (ulong)receivedData.Count;
                                bytesReceived += payloadSize + 8; // add 8 bytes for sequence number field
                            }
                        }

                        if (receivedSequenceNumber.HasValue)
                        {
                            // Increase sample count
                            sampleCount++;

                            // Check that the sample is the next one expected
                            if (lastReceivedSequenceNumber.HasValue
                                && receivedSequenceNumber != lastReceivedSequenceNumber + 1)
                            {
                                outOfOrderCount += (ulong)receivedSequenceNumber - ((ulong)lastReceivedSequenceNumber + 1);
                            }

                            // Keep track of last received seq nr
                            lastReceivedSequenceNumber = receivedSequenceNumber;

                        }
                    }
                    else
                    {
                        Console.WriteLine("Writer flow purged, stop reader");
                        Stop = true;
                    }
                }

                if (!Stop)
                {
                    currentTime = DateTime.Now;

                    var deltaTimeInSec = (currentTime - (DateTime)prevTime).TotalSeconds;
                    if(deltaTimeInSec >= 1)
                    {
                        var deltaReceived = bytesReceived - prevReceived;
                        double sampleRate = (double)(sampleCount - prevCount) / deltaTimeInSec;
                        double mbitRate = ((double)deltaReceived / BytesPerMegaBit) / deltaTimeInSec;

                        Console.WriteLine($"Payload size: {payloadSize}" +
                            $" | Total: {sampleCount,9} samples, {bytesReceived,12} bytes" +
                            $" | Out of order: {outOfOrderCount,6} samples" +
                            $" | Transfer rate: {sampleRate,7:F0} samples/s, {mbitRate,9:F2} Mbit/s");

                        // Reset the previous values for next time we print
                        prevReceived = bytesReceived;
                        prevCount = sampleCount;
                        prevTime = currentTime;
                    }

                    var samplesInBatch = sampleCount - sampleCountAtStartOfBatch;
                    if (samplesInBatch > batchMaxSize)
                    {
                        batchMaxSize = samplesInBatch;
                    }
                }
            }
            showSummary();

            return 0;
        }

        private void showSummary()
        {
            // Output totals and averages
            if (batchCount > 0)
            {
                var deltaTime = (currentTime - startTime).TotalSeconds;
                var sampleRate = (double)sampleCount / deltaTime;
                var mbitRate = ((double)bytesReceived / BytesPerMegaBit) / deltaTime;
                var samplesPerBatch = sampleCount / batchCount;

                Console.WriteLine(
                    $"Total received: {sampleCount} samples, {bytesReceived} bytes");
                Console.WriteLine(
                    $"Out of order: {outOfOrderCount} samples");
                Console.WriteLine(
                    $"Average transfer rate: {sampleRate:F0} samples/s, {mbitRate:F2} Mbit/s");
                Console.WriteLine(
                    $"Average sample-count per batch: {samplesPerBatch}" +
                    $", maximum batch-size: {batchMaxSize}");
            }
        }

        private uint SecondsExecuting(DateTime originalTime)
        {
            return (uint)(DateTime.Now - originalTime).TotalSeconds;
        }

    }
}
