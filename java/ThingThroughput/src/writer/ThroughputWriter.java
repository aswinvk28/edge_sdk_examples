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

package writer;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.DiscoveredThingRegistry;
import com.adlinktech.datariver.IotByteSeq;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.IotValue;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.OutputHandler;
import com.adlinktech.datariver.Thing;
import com.adlinktech.datariver.ThingAPIRuntimeError;


public class ThroughputWriter implements Closeable {
	
	private AtomicBoolean stop = new AtomicBoolean(false);
	
	private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    private final IotNvpSeq sample;
	
	public ThroughputWriter(final String thingPropertiesURI) {
		System.out.println("Throughput writer started");
		this.thingPropertiesUri = thingPropertiesURI;
		this.dataRiver = DataRiver.getInstance();
		this.thing = createThing();
		this.sample = new IotNvpSeq();
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
	
	private void cleanup() {
		if(this.dataRiver != null) {
			this.dataRiver.close();
		}
		System.out.println("Throughput writer stopped");
	}
	
	private String getAbsFileURI(final String fileName) {
	    final String dir = System.getProperty("user.dir");
	    final String prefix = "file://";
		return prefix.concat(dir).concat("/").concat(fileName);
	}
	
	private Thing createThing() {
	    final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
		tgr.registerTagGroupsFromUri(getAbsFileURI("definitions/TagGroup/com.adlinktech.example/ThroughputTagGroup.json"));
		this.dataRiver.addTagGroupRegistry(tgr);
		
		final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
		tcr.registerThingClassesFromUri(getAbsFileURI("definitions/ThingClass/com.adlinktech.example/ThroughputWriterThingClass.json"));
		this.dataRiver.addThingClassRegistry(tcr);
		
		final JSonThingProperties tp = new JSonThingProperties();
		tp.readPropertiesFromUri(this.thingPropertiesUri);
		
		return this.dataRiver.createThing(tp);
	}

    private void setupMessage(final long payloadSize) {
        final IotValue sequenceDataValue = new IotValue();
        final IotValue sequenceNumberValue = new IotValue();
    	
    	// Init data Tag
        final IotByteSeq sequenceData = new IotByteSeq();
    	for(long i = 0; i < payloadSize; i++) {
    		sequenceData.add((byte) Character.getNumericValue('a'));
    	}
    	sequenceDataValue.setByteSeq(sequenceData);
    	
    	
    	// Init sequence number and size Tags
    	sequenceNumberValue.setUint64((long) 0);

        // Create sample
    	this.sample.add(new IotNvp("sequencenumber", sequenceNumberValue));
    	this.sample.add(new IotNvp("sequencedata", sequenceDataValue));
    }

    private void waitForReader() throws InterruptedException {
        // wait for throughputreader to appear by discovering its thingId and thingClass
    	System.out.println("Waiting for Throughput reader.. ");
    	final DiscoveredThingRegistry discoveredThingRegistry = this.dataRiver.getDiscoveredThingRegistry();
    	
        while (!stop.get()) {
            try {
                discoveredThingRegistry.findDiscoveredThing("*", "ThroughputReader:com.adlinktech.example:v1.0");
                break;
            } catch (ThingAPIRuntimeError e) {
                // Thing not available yet. Sleep for 100 milliseconds and try again.
            }
            
            Thread.sleep(100);
        }

        if (!stop.get()) {
        	System.out.println("Throughput reader found");
        } else {
        	System.out.println("Terminated");
        	System.exit(1);
        }
    }


    private void write(
            final long burstInterval,
            final long burstSize,
            final long runningTime,
            final WriterMode mode) throws InterruptedException {
        
        long burstCount = 0;
        long count = 0;
        boolean timedOut = false;

        final long pubStart = System.currentTimeMillis();
        long burstStart = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();

        final OutputHandler outputHandler = this.thing.getOutputHandler("ThroughputOutput");
        IotValue internalSequenceNumberValue = new IotValue();

        if (mode == WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE) {
            outputHandler.setNonReentrantFlowId(this.thing.getContextId());
            final IotNvpSeq internalNvpSeq = outputHandler.setupNonReentrantNvpSeq(this.sample);

            final IotNvp internalSequenceNumberNvp = internalNvpSeq.get(0);
            internalSequenceNumberValue = internalSequenceNumberNvp.getValue();
        }

        while (!stop.get() && !timedOut)
        {
            // Write data until burst size has been reached
            if (burstCount++ < burstSize) {
                if (mode == WriterMode.OUTPUT_HANDLER) {
                    // Fill the nvp_seq with updated sequencenr
                    this.sample.get(0).getValue().setUint64(count++);

                    // Write the data using output handler
                    outputHandler.write(this.sample);
                } else if (mode == WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE) {
                    // Fill the nvp_seq with updated sequencenr
                	internalSequenceNumberValue.setUint64(count++);

                    // Write the data using non-reentrant write on output handler
                    outputHandler.writeNonReentrant();
                } else {
                    // Fill the nvp_seq with updated sequencenr
                	this.sample.get(0).getValue().setUint64(count++);

                    // Write the data
                    this.thing.write("ThroughputOutput", this.sample);
                }
            } else if (burstInterval != 0) {
                // Sleep until burst interval has passed
                currentTime = System.currentTimeMillis();

                final long deltaTime = currentTime - burstStart;
                if (deltaTime < burstInterval) {
                	Thread.sleep(burstInterval - deltaTime);
                }
                burstStart = System.currentTimeMillis();
                burstCount = 0;
            } else {
                burstCount = 0;
            }

            // Check of timeout
            if (runningTime != 0) {
                currentTime = System.currentTimeMillis();
                if (((float)(currentTime - pubStart) / 1000.0) > runningTime) {
                    timedOut = true;
                }
            }
        }

        // Show stats
        if (stop.get()) {
        	System.out.println("Terminated: " + count + " samples written");
        } else {
        	System.out.println("Timed out: " + count + " samples written");
        }
    }
    
    public void run(final long payloadSize, final long burstInterval, final long burstSize, final long runningTime, final WriterMode writerMode) {
        final String writerModeStr = (writerMode == WriterMode.OUTPUT_HANDLER) ?
            "outputHandler" : ((writerMode == WriterMode.OUTPUT_HANDLER_NOT_THREAD_SAFE) ?
                "outputHandlerNotThreadSafe" : "standard");

        System.out.println("payloadSize: " + payloadSize + " | burstInterval: " + burstInterval
            + " | burstSize: " + burstSize + " | runningTime: " + runningTime
            + " | writer-mode: " + writerModeStr);
        
        try {
            // Wait for reader to be discovered
            waitForReader();
    
            // Create the message that is sent
            setupMessage(payloadSize);
    
            // Write data
            write(burstInterval, burstSize, runningTime, writerMode);
    
            // Give middleware some time to finish writing samples
            Thread.sleep(2000);
            
		} catch (InterruptedException e) {
		    System.out.println("ThroughputWriter intertrupted. Exiting");
		}
    }

    @Override
    public void close() {
        cleanup();
    }
    
}