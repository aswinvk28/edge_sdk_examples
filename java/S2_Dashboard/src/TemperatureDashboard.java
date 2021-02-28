/*
 *                         ADLINK Edge SDK
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
 * This code is part of example scenario 2 'Connect a Dashboard' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

import java.io.Closeable;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.FlowState;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpDataSample;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Selector;
import com.adlinktech.datariver.Thing;


public class TemperatureDashboard implements Closeable {
	private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    
    /**
     * Thread-safe println that guarantees println is not interleaved
     * with println output from another thread.
     */
    private void safePrintln(String msg) {
    	synchronized(System.out) {
    		System.out.println(msg);
    	}
    }
    public TemperatureDashboard(final String thingPropertiesUri) {
    	safePrintln("Temperature Dashboard started");
    	this.thingPropertiesUri = thingPropertiesUri;
    	this.dataRiver = DataRiver.getInstance();
    	this.thing = createThing();
	}
	
	private void cleanup() {
		if(this.dataRiver != null) {
			this.dataRiver.close();
		}
		safePrintln("Temperature Dashboard stopped");
	}
	
	private String getAbsFileUri(final String fileName) {
	    final String dir = System.getProperty("user.dir");
	    final String prefix = "file://";
		return prefix.concat(dir).concat("/").concat(fileName);
	}

    private Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/TemperatureDashboardThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }
    
    public void run(final String floor, final int runningTime) {
        final long start = System.currentTimeMillis();
        long elapsedSeconds = 0;

        // Add listener for new Things
        final TemperatureSensorDiscoveredListener tempSensorDiscoveredListener = new TemperatureSensorDiscoveredListener();
        this.dataRiver.addListener(tempSensorDiscoveredListener);

        // Add listener for lost Things
        final TemperatureSensorLostListener tempSensorLostListener = new TemperatureSensorLostListener();
        this.dataRiver.addListener(tempSensorLostListener);

        // Create selector for selecting current floor's temperature from input 'temperature'
        final String selection = floor + ".*";

        final Selector selector = this.thing.select("temperature").flow(selection);
        safePrintln("Selecting data using flowId selection: " + selection);

        do {
            // Read data using selector
            final IotNvpDataSampleSeq msgs = selector.readIotNvp((int)((runningTime - elapsedSeconds) * 1000));

            // Process samples
            for (final IotNvpDataSample msg : msgs) {
                final FlowState flowState = msg.getFlowState();
                if (flowState == FlowState.ALIVE) {
                    final IotNvpSeq dataSample = msg.getData();
                    float temperature = 0.0f;

                    try {
                        // Get temperature value from sample
                        for (final IotNvp nvp : dataSample) {
                            if (nvp.getName().equals("temperature")) {
                                temperature = nvp.getValue().getFloat32();
                            }
                        }
                    }
                    catch (Exception e) {
                        safePrintln("An unexpected error occured while processing data-sample: " + e.getMessage());
                        continue;
                    }

                    // Show output
                    safePrintln("Temperature data received for flow "
                        + msg.getFlowId() + ": "
                        + String.format("%3.1f", temperature));
                }
            }

            elapsedSeconds =  (System.currentTimeMillis() - start) / 1000;
        } while (elapsedSeconds < runningTime);

        // Remove listeners
        this.dataRiver.removeListener(tempSensorLostListener);
        this.dataRiver.removeListener(tempSensorDiscoveredListener);
    }

	@Override
	public void close() {
		cleanup();
	}
}