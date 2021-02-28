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

namespace Writer
{
    public class ThroughputWriter : IDisposable
    {
        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }
        private IotNvpSeq Sample { get; }
        public WriterMode OutputHandlerNotThreadSafe { get; private set; }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Console.WriteLine("Writer trapped Ctrl-C. Stopping.");
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
        }

        private static Thing CreateThing(in DataRiver dataRiver)
        {
            var tgr = new JSonTagGroupRegistry();
            tgr.RegisterTagGroupsFromString(Utilities.GetEmbeddedResourceContent("Writer.definitions.TagGroup.com.adlinktech.example.ThroughputTagGroup.json"));
            dataRiver.AddTagGroupRegistry(tgr);

            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromString(Utilities.GetEmbeddedResourceContent("Writer.definitions.ThingClass.com.adlinktech.example.ThroughputWriterThingClass.json"));
            dataRiver.AddThingClassRegistry(tcr);

            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromString(Utilities.GetEmbeddedResourceContent("Writer.config.ThroughputWriterProperties.json"));

            return dataRiver.CreateThing(tp);
        }

        public ThroughputWriter()
        {
            DataRiver = DataRiver.GetInstance();
            Thing = CreateThing(DataRiver);
            Stop = false;
            Sample = new IotNvpSeq(2);

            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("Throughput writer started");
        }

        public int Run(in Options options)
        {
            Console.WriteLine($"payloadSize: {options.PayloadSize} " +
                $"| burstInterval: {options.BurstInterval} " +
                $"| burstSize: {options.BurstSize} " +
                $"| runningTime: {options.RunningTime} " +
                $"| writer-mode: {options.WriterMode}");

            // wait for a reader to be discovered
            WaitForReader();

            // create the message that is to be sent
            SetupMessage(options.PayloadSize);

            // write the data
            Write(options.BurstInterval, options.BurstSize, options.RunningTime, options.WriterMode);

            // sleep for two seconds to allow middleware to finish writing samples
            System.Threading.Thread.Sleep(2000);

            return 0;
        }

        private void WaitForReader()
        {
            Console.WriteLine("Waiting for Throughput reader.. ");
            var discoveredThingRegistry = DataRiver.DiscoveredThingRegistry;
            bool readerFound = false;
            while (!readerFound && !Stop)
            {
                try
                {
                    var thing = discoveredThingRegistry.FindDiscoveredThing("*", "ThroughputReader:com.adlinktech.example:v1.0");
                    readerFound = true;
                }
                catch(ThingAPIRuntimeError)
                {
                    // No result from FindDiscoveredThing
                    // Sleep 100ms and try again
                    System.Threading.Thread.Sleep(100);
                }
            }
            Console.WriteLine(Stop ? "Terminated" : "Throughput reader found");
        }

        private void SetupMessage(ulong payloadSize)
        {
            var sequenceData = new IotByteSeq((int)payloadSize);
            for (ulong i = 0; i < payloadSize; i++)
            {
                sequenceData.Add((byte)'a');
            }

            var snValue = new IotValue();
            snValue.UInt64 = 0;
            Sample.Add(new IotNvp("sequencenumber", snValue));
            var sdValue = new IotValue();
            sdValue.ByteSeq = sequenceData;
            Sample.Add(new IotNvp("sequencedata", sdValue));
        }

        private void Write(ulong burstInterval, ulong burstSize, ulong runningTime, WriterMode mode)
        {
            ulong burstCount = 0;
            uint count = 0;
            bool timedOut = false;

            var pubStart = DateTime.Now;
            var burstStart = DateTime.Now;

            OutputHandler outputHandler = Thing.GetOutputHandler("ThroughputOutput");
            IotValue internalSequencenumberV = null;

            if (mode == WriterMode.outputHandlerNotThreadSafe)
            {
                outputHandler.NonReentrantFlowId = Thing.ContextId;
                var internalNvpSeq = outputHandler.SetupNonReentrantNvpSeq(Sample);
                internalSequencenumberV = internalNvpSeq[0].Value;
            }

            while (!Stop && !timedOut)
            {
                // Write data until burst size has been reached
                if (burstCount++ < burstSize)
                {
                    if (mode == WriterMode.outputHandler)
                    {
                        // Fill the nvp_seq with updated sequencenr
                        Sample[0].Value.UInt64 = count++;

                        // Write the data using output handler
                        outputHandler.Write(Sample);
                    }
                    else if (mode == WriterMode.outputHandlerNotThreadSafe)
                    {
                        // Fill the nvp_seq with updated sequencenr
                        internalSequencenumberV.UInt64 = count++;

                        // Write the data using non-reentrant write on output handler
                        outputHandler.WriteNonReentrant();
                    }
                    else
                    {
                        // Fill the nvp_seq with updated sequencenr
                        Sample[0].Value.UInt64 = count++;

                        // Write the data
                        Thing.Write("ThroughputOutput", Sample);
                    }
                }
                else if (burstInterval != 0)
                {
                    // Sleep until burst interval has passed
                    TimeSpan deltaTime = DateTime.Now - burstStart;
                    var remainingBurstIntervalMillis = (int)burstInterval
                        - (int)deltaTime.TotalMilliseconds;
                    if (remainingBurstIntervalMillis > 0)
                    {
                        System.Threading.Thread.Sleep(remainingBurstIntervalMillis);
                    }
                    burstStart = DateTime.Now;
                    burstCount = 0;
                }
                else
                {
                    burstCount = 0;
                }

                // Check of timeout
                if (runningTime != 0)
                {
                    TimeSpan deltaTime = DateTime.Now - pubStart;
                    var remainingSeconds = (int)runningTime - deltaTime.TotalSeconds;
                    if (remainingSeconds <= 0)
                    {
                        timedOut = true;
                    }
                }
            }


            // Show stats
            if (Stop)
            {
                Console.WriteLine($"Terminated: {count} samples written");
            }
            else
            {
                Console.WriteLine($"Timed out: {count} samples written");
            }
        }

        public void Dispose()
        {
            Thing.Purge("ThroughputOutput");
            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("Throughput writer stopped");
        }
    }
}
