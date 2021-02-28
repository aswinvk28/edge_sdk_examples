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
 * This is a simple roundtrip application
 */

package ping;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.DiscoveredThingRegistry;
import com.adlinktech.datariver.IotByteSeq;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.IotValue;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;
import com.adlinktech.datariver.ThingAPIRuntimeError;

//Use "AnsiConsole" from "jansi" library to print ANSI escape sequences both on Windows and Linux
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi;

public class Ping implements Closeable {
    
    private static final long US_IN_ONE_SEC = 1000000;
    
    private AtomicBoolean stop = new AtomicBoolean(false);
    
	private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
	private final IotNvpSeq sampleData;
    private final ExampleTimeStats roundTripStatsOverall;
    private final ExampleTimeStats writeAccessStatsOverall;
    private final ExampleTimeStats readAccessStatsOverall;
	
	public Ping(final String thingPropertiesUri) {
        // Enable ANSI escape sequences
        AnsiConsole.systemInstall();
		System.out.println("# Ping started");
		this.thingPropertiesUri = thingPropertiesUri;
		this.dataRiver = DataRiver.getInstance();
		this.thing = createThing();
        this.roundTripStatsOverall = new ExampleTimeStats();
        this.writeAccessStatsOverall = new ExampleTimeStats();
        this.readAccessStatsOverall = new ExampleTimeStats();
        this.sampleData = new IotNvpSeq();
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

    private void showStats(
            final boolean overall,
            final long elapsedSeconds,
            final ExampleTimeStats roundTripStats,
            final ExampleTimeStats writeAccessStats,
            final ExampleTimeStats readAccessStats) {
        
        if (overall) {
            System.out.print("\n" + Ansi.ansi().fgGreen() + "# Overall");
        } else {
            System.out.print(String.format("%9s", elapsedSeconds));
        }

        System.out.println(
                String.format("%10d", roundTripStats.getCount()) +
                String.format("%9.0f", roundTripStats.exampleGetMedianFromTimeStats()) +
                String.format("%9d", roundTripStats.getMin()) +
                String.format("%11d", writeAccessStats.getCount()) +
                String.format("%9.0f", writeAccessStats.exampleGetMedianFromTimeStats()) +
                String.format("%9d", writeAccessStats.getMin()) +
                String.format("%11d", readAccessStats.getCount()) +
                String.format("%9.0f", readAccessStats.exampleGetMedianFromTimeStats()) +
                String.format("%9d", readAccessStats.getMin()) +
                Ansi.ansi().fgDefault()
            );
    }
	
	public void cleanup() {
		if(this.dataRiver != null) {
			this.dataRiver.close();
		}
		System.out.println("# Ping stopped");
		// Disable Jansi ANSI support
		AnsiConsole.systemUninstall();
	}
	
	private String getAbsFileUri(final String fileName) {
		final String dir = System.getProperty("user.dir");
		final String prefix = "file://";
		return prefix.concat(dir).concat("/").concat(fileName);
	}
    
    private Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/PingTagGroup.json"));
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/PongTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/PingThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);

        return this.dataRiver.createThing(tp);
    }
    
    private long currentTimeMicroseconds() {
        return System.nanoTime() / 1000;
    }
    
    private void waitForPong() throws InterruptedException {
        // wait for pong to appear by discovering its thingId and thingClass
        System.out.println("# Waiting for pong to run...");
        final DiscoveredThingRegistry discoveredThingRegistry = this.dataRiver.getDiscoveredThingRegistry();

        while (!stop.get()) {
            try {
                // see if we already know pongs's thing class
                discoveredThingRegistry.findDiscoveredThing("java-pongThing1", "Pong:com.adlinktech.example:v1.0");
                break;
            } catch (ThingAPIRuntimeError e) {
                // Have not found the discovered thing yet. Sleep for 1 sec and try again.
            }

            Thread.sleep(1000);
        }
    }

	private void initPayload(final long payloadSize) {
		final IotValue payload = new IotValue();
		final IotByteSeq byteSeq = new IotByteSeq();
		for (long i = 0; i < payloadSize; i++) {
		    byteSeq.add((byte)Character.getNumericValue('a')); 
		}
		payload.setByteSeq(byteSeq);
		this.sampleData.add(new IotNvp("payload", payload));
	}

	private void warmUp() {
		final long startTime = currentTimeMicroseconds();
		final int waitTimeout = 10000;
		
		System.out.println("# Warming up 5s to stabilise performance...");
		while (((float)(currentTimeMicroseconds() - startTime) / (float)US_IN_ONE_SEC) < 5) {
			this.thing.write("Ping", this.sampleData);
			this.thing.readIotNvp("Pong", waitTimeout);
		}
		System.out.println("# Warm up complete");
	}
    

	public void sendTerminate() {
		System.out.println("# Sending termination request.");
		this.thing.purge("Ping", "ping");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		    System.out.println("Ping is interrupted");
		}
	}

    public void run(final long payloadSize, final long numSamples, final long runningTime) {
    	final int waitTimeout = 10000;
		final ExampleTimeStats roundTripStats = new ExampleTimeStats();
		final ExampleTimeStats writeAccessStats = new ExampleTimeStats();
		final ExampleTimeStats readAccessStats = new ExampleTimeStats();

		System.out.println("# Parameters: payload size: " + payloadSize + " | number of samples: " + numSamples + " | running time: " + runningTime);
		
		try {
    		// Wait for the Pong Thing
    		waitForPong();
    
    		// Init payload
    		initPayload(payloadSize);
    
    		// Warm-up for 5s
    		warmUp();
    		
    		System.out.println("# Round trip measurements (in us)");
            System.out.println(Ansi.ansi().fgBrightMagenta()
    		        + "#             Round trip time [us]         Write-access time [us]       Read-access time [us]"
                    + Ansi.ansi().fgDefault());
            System.out.println(Ansi.ansi().fgBrightMagenta()
    		        + "# Seconds     Count   median      min      Count   median      min      Count   median      min"
                    + Ansi.ansi().fgDefault());
    
    		long startTime = currentTimeMicroseconds();
    		long elapsedSeconds = 0;
    		long i = 0;
    		
    		while ((numSamples == 0 || i < numSamples) && !stop.get()) {
    			// Write a sample that pong can send back
    		    final long preWriteTime = currentTimeMicroseconds();
    		    this.thing.write("Ping", this.sampleData);
    		    final long postWriteTime = currentTimeMicroseconds();
    		    
    		    // Read sample
    		    final long preReadTime = currentTimeMicroseconds();
    		    final IotNvpDataSampleSeq samples = this.thing.readIotNvp("Pong", waitTimeout);
    		    final long postReadTime = currentTimeMicroseconds();
    
    			// Validate sample count
    			if (samples.size() != 1) {
    			    System.out.println("ERROR: Ping received " + samples.size() + " samples but was expecting 1.");
    				return;
    			}
    
    			// Update stats
    			writeAccessStats.add(postWriteTime - preWriteTime);
                readAccessStats.add(postReadTime - preReadTime);
                roundTripStats.add(postReadTime - preWriteTime);
                this.writeAccessStatsOverall.add(postWriteTime - preWriteTime);
                this.readAccessStatsOverall.add(postReadTime - preReadTime);
                this.roundTripStatsOverall.add(postReadTime - preWriteTime);
    
    			// Print stats each second
    		    if (((postReadTime - startTime) > US_IN_ONE_SEC) || (i == numSamples - 1)) {
    				// Print stats
    				showStats(false, ++elapsedSeconds, roundTripStats, writeAccessStats, readAccessStats);
    
    				// Reset stats for next run
    				roundTripStats.exampleResetTimeStats();
    				writeAccessStats.exampleResetTimeStats();
    				readAccessStats.exampleResetTimeStats();
    
    				// Set values for next run
    				startTime = currentTimeMicroseconds();
    
    				// Check for timeout
    				if (runningTime > 0 && elapsedSeconds >= runningTime) {
    					break;
    				}
    			}
    			
    			i++;
    		}
		} catch(InterruptedException e) {
		    System.out.println("Ping is interrupted");
		}

		// Print overall stats
		showStats(true, 0, roundTripStatsOverall, writeAccessStatsOverall, readAccessStatsOverall);
	}

    @Override
    public void close() {
        cleanup();
    }
}