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
 * This code is part of example scenario 3 'Derived Value Service' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

package gpssensor;

import java.io.Closeable;
import java.util.Random;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.IotValue;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;

public class GpsSensor implements Closeable {
	private static final long MIN_SAMPLE_DELAY_MS = 1500;
	
	private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    private final float truckLat;
    private final float truckLng;
    
    public GpsSensor(final String thingPropertiesUri, final float truckLat, final float truckLng) {
    	System.out.println("GPS Sensor started");
    	this.thingPropertiesUri = thingPropertiesUri;
    	this.truckLat = truckLat;
    	this.truckLng = truckLng;
    	this.dataRiver = DataRiver.getInstance();
    	this.thing = createThing();
	}
	
	private void cleanup() {
		if(this.dataRiver != null) {
			this.dataRiver.close();
		}
		System.out.println("GPS Sensor stopped");
	}
	
	private String getAbsFileUri(final String fileName) {
	    final String dir = System.getProperty("user.dir");
	    final String prefix = "file://";
		return prefix.concat(dir).concat("/").concat(fileName);
	}

    private Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/LocationTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/GpsSensorThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    private void writeSample(final float locationLat, final float locationLng, final long timestamp) {
        // Create IoT data object
        final IotValue latValue = new IotValue();
    	latValue.setFloat32(locationLat);
    	final IotValue lngValue = new IotValue();
    	lngValue.setFloat32(locationLng);
    	
    	final IotNvpSeq locationData = new IotNvpSeq();
    	locationData.add(new IotNvp("latitude", latValue));
    	locationData.add(new IotNvp("longitude", lngValue));
    	
    	final IotValue locationValue = new IotValue();
    	locationValue.setNvpSeq(locationData);
    	
    	final IotValue timestampValue = new IotValue();
    	timestampValue.setUint64(timestamp);
    	
    	final IotNvpSeq sensorData = new IotNvpSeq();
    	sensorData.add(new IotNvp("location", locationValue));
    	sensorData.add(new IotNvp("timestampUtc", timestampValue));

        // Write data to DataRiver
        this.thing.write("location", sensorData);
    }
    
    public void run(final int runningTime) {
        final Random rand = new Random();
        final long startTimestamp = System.currentTimeMillis();
        long elapsedTime = 0;
        
        float tructLocationLat = this.truckLat;
        float truckLocationLng = this.truckLng;

        do {
            // Simulate location change
            tructLocationLat += (float)(rand.nextInt(1000)) / 100000.0;
            truckLocationLng += (float)(rand.nextInt(1000)) / 100000.0;

            writeSample(tructLocationLat, truckLocationLng, System.currentTimeMillis() / 1000);

            // Wait for random interval
            try {
				Thread.sleep(MIN_SAMPLE_DELAY_MS + rand.nextInt(3000));
			} catch (InterruptedException e) {
			    System.out.println("GpsSensor interrupted. Exiting.");
			    break;
			}

            // Get elapsed time
            elapsedTime = System.currentTimeMillis() - startTimestamp;
        } while (elapsedTime < runningTime * 1000);
    }

	@Override
	public void close() {
		cleanup();
	}
}
