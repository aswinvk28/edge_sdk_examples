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
 * This is a simple throughput application measuring obtainable throughput using the thingapi
 *
 */

package reader;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.DataRiverAPIConstants;
import com.adlinktech.datariver.FlowState;
import com.adlinktech.datariver.IotByteSeq;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpDataSample;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;

public class ThroughputReader implements Closeable {
    private static final long MS_IN_ONE_SEC = 1000;
    private static final long BYTES_PER_SEC_TO_MEGABITS_PER_SEC = 125000;
    
    private AtomicBoolean stop = new AtomicBoolean(false);
    
	private final String thingPropertiesURI;
	private final DataRiver dataRiver;
	private final Thing thing;

    public ThroughputReader(final String thingPropertiesURI) {
        System.out.println("Throughput reader started");
        this.thingPropertiesURI = thingPropertiesURI;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
    }
    
    public synchronized void registerCtrlHandler() {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                stop.set(true);
                while (true) {
                    try {
                        mainThread.join();
                        return;
                    } catch (InterruptedException e) {
                        // someone tried to stop us, still wait for mainThread
                    }
                }
            }
        });
    }
	
	private String getAbsFileURI(final String fileName) {
	    final String dir = System.getProperty("user.dir");
	    final String prefix = "file://";
		return prefix.concat(dir).concat("/").concat(fileName);
	}
	
	private void cleanup() {
		if (this.dataRiver != null) {
			this.dataRiver.close();
		}
		System.out.println("Throughput reader stopped");
	}
	
	private Thing createThing() {
		// Create and Populate the TagGroup registry with JSON resource files.
	    final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
		tgr.registerTagGroupsFromUri(getAbsFileURI("definitions/TagGroup/com.adlinktech.example/ThroughputTagGroup.json"));
		this.dataRiver.addTagGroupRegistry(tgr);
		
		// Create and Populate the ThingClass registry with JSON resource files.
		final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
		tcr.registerThingClassesFromUri(getAbsFileURI("definitions/ThingClass/com.adlinktech.example/ThroughputReaderThingClass.json"));
		this.dataRiver.addThingClassRegistry(tcr);
		
		// Create a Thing based on properties specified in a JSON resource file.
		final JSonThingProperties tp = new JSonThingProperties();
		tp.readPropertiesFromUri(this.thingPropertiesURI);
		
		return this.dataRiver.createThing(tp);
	}
	
	public void run(final long pollingDelay, final long runningTime) {
	    long sampleCount = 0;
	    long currentTime = System.currentTimeMillis();
	    long startTime = System.currentTimeMillis();
	    long bytesReceived = 0;
	    long outOfOrderCount = 0;
	    long batchCount = 0;
	    long batchMaxSize = 0;
	    
        long prevCount = 0;
        long prevReceived = 0;
        long samplesInBatch = 0;
        long payloadSize = 0;
        boolean firstIteration = true;
        long prevTime = System.currentTimeMillis();
        long receivedSequenceNumber = 0;
        long lastReceivedSequenceNumber = 0;
        boolean firstSample = true;

        System.out.println("Waiting for samples...");

        // Loop through until the runningTime has been reached (0 = infinite)
        // each cycle is 1 second
        long cycles = 0;

        while (!stop.get() && (runningTime == 0 || cycles < runningTime)) {
            if (pollingDelay > 0) {
            	try {
					Thread.sleep(pollingDelay);
				} catch (InterruptedException e) {
				    System.out.println("ThroughputReader intertrupted");
	                break;
				}
            }

            // New batch
            batchCount++;
            samplesInBatch = sampleCount;

            // Take samples and iterate through them
            final IotNvpDataSampleSeq samples = this.thing.readIotNvp("ThroughputInput", DataRiverAPIConstants.BLOCKING_TIME_INFINITE);
            for (final IotNvpDataSample sample : samples) {
                if (sample.getFlowState() == FlowState.ALIVE) {
                    final IotNvpSeq data = sample.getData();

                    // find the message, stored in the name-value-pair with name 'name':
                    boolean seqNrFound = false;
                    for (final IotNvp nvp : data) {
                        if (nvp.getName().equals("sequencenumber")) {
                            receivedSequenceNumber = nvp.getValue().getUint64();
                            seqNrFound = true;
                        } else if (nvp.getName().equals("sequencedata")) {
                            // Add the sample payload size to the total received
                        	final IotByteSeq receivedData = nvp.getValue().getByteSeq();
                            payloadSize = receivedData.size();
                            bytesReceived += payloadSize + 8; // add 8 bytes for sequence number field
                        }
                    }
                    if (seqNrFound) {
                        // Increase sample count
                        sampleCount++;

                        if (firstSample) {
                            lastReceivedSequenceNumber = receivedSequenceNumber - 1;
                            firstSample = false;
                        }

                        // Check that the sample is the next one expected
                        if (receivedSequenceNumber != lastReceivedSequenceNumber + 1) {
                            outOfOrderCount += (receivedSequenceNumber - (lastReceivedSequenceNumber + 1 ));
                        }

                        // Keep track of last received seq nr
                        lastReceivedSequenceNumber = receivedSequenceNumber;
                    }
                } else {
                	System.out.println("Writer flow purged, stop reader");
                    stop.set(true);
                }
            }

            if (!stop.get()) {
                currentTime = System.currentTimeMillis();
                if ((currentTime - prevTime) > MS_IN_ONE_SEC) {
                	// If not the first iteration
                    if(!firstIteration) {
                        // Calculate the samples and bytes received and the time passed since the  last iteration and output
                        final long deltaReceived = bytesReceived - prevReceived;
                        final long deltaTime = (currentTime - prevTime) / MS_IN_ONE_SEC;

                        System.out.println(
                            "Payload size: " + payloadSize + " | "
                            + "Total: " + String.format("%9s", sampleCount) + " samples, "
                            + String.format("%12s", bytesReceived) + " bytes | "
                            + "Out of order: " + String.format("%6s", outOfOrderCount) + " samples | "
                            + "Transfer rate: " + String.format("%7.0f", (double)(sampleCount - prevCount) / deltaTime) + " samples/s, "
                            + String.format("%9.2f", ((double)deltaReceived / BYTES_PER_SEC_TO_MEGABITS_PER_SEC) / deltaTime) + " Mbit/s");

                        cycles++;
                    } else {
                        // Set the start time if it is the first iteration
                    	firstIteration = false;
                        startTime = currentTime;
                    }

                    // Update the previous values for next iteration
                    prevReceived = bytesReceived;
                    prevCount = sampleCount;
                    prevTime = currentTime;
                }

                // Update max samples per batch
                samplesInBatch = sampleCount - samplesInBatch;
                if (samplesInBatch > batchMaxSize) {
                    batchMaxSize = samplesInBatch;
                }
            }
        }

        showSummary(sampleCount, currentTime, startTime, bytesReceived, outOfOrderCount, batchCount, batchMaxSize);
    }
    
    private void showSummary(
            final long sampleCount,
            final long currentTime,
            final long startTime,
            final long bytesReceived,
            final long outOfOrderCount,
            final long batchCount,
            final long batchMaxSize) {
        
        // Output totals and averages
        if (batchCount > 0) {
            final double deltaTime = (currentTime - startTime) / MS_IN_ONE_SEC;
            final double sampleRate = (double)sampleCount / deltaTime;
            final double mbitRate = ((double)bytesReceived / BYTES_PER_SEC_TO_MEGABITS_PER_SEC) / deltaTime;
            final long samplesPerBatch = sampleCount / batchCount;
            
            System.out.println("Total received: " + sampleCount + " samples, " + bytesReceived + " bytes");
            System.out.println("Out of order: " + outOfOrderCount + " samples");
            System.out.println("Average transfer rate: "
                                + String.format("%.0f", sampleRate) + " samples/s, "
                                + String.format("%.2f", mbitRate) + " Mbit/s");
            System.out.println("Average sample-count per batch: " + samplesPerBatch + ", maximum batch-size: " + batchMaxSize);
        }
    }

	@Override
	public void close() {
		cleanup();
	}
 }