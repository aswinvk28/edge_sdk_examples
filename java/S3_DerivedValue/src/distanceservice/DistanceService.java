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

package distanceservice;

import java.io.Closeable;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.Dispatcher;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.IotValue;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;
import com.adlinktech.datariver.TimeoutError;

public class DistanceService implements Closeable {
    
    private final String thingPropertiesUri;
    private final float warehouseLat;
    private final float warehouseLng;
    private final DataRiver dataRiver;
    private final Thing thing;
    
    public DistanceService(final String thingPropertiesUri, final float warehouseLat, final float warehouseLng) {
        System.out.println("Distance Service started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.warehouseLat = warehouseLat;
        this.warehouseLng = warehouseLng;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
    }
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Distance Service stopped");
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
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/DistanceTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/DistanceServiceThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    public float getWarehouseLat() {
        return this.warehouseLat;
    }

    public float getWarehouseLng() {
        return this.warehouseLng;
    }

    public void writeDistance(final String myLocationFlowId, final double distance, final float eta, final long timestamp) {
        final IotValue distValue = new IotValue();
        distValue.setFloat64(distance);
        final IotValue etaValue = new IotValue();
        etaValue.setFloat32(eta);
        final IotValue timestampValue = new IotValue();
        timestampValue.setUint64(timestamp);
        
        final IotNvpSeq distanceData = new IotNvpSeq();
        distanceData.add(new IotNvp("distance", distValue));
        distanceData.add(new IotNvp("eta", etaValue));
        distanceData.add(new IotNvp("timestampUtc", timestampValue));

        // Write distance to DataRiver using flow ID from incoming location sample
        this.thing.write("distance", myLocationFlowId, distanceData);
    }
    
    public void run(final int runningTime) {
        // Use custom dispatcher for processing events
        final Dispatcher dispatcher = new Dispatcher();

        // Add listener for new GPS sensor Things using our custom dispatcher
        final GpsSensorDataListener gpsDataReceivedListener = new GpsSensorDataListener(this);
        this.thing.addListener(gpsDataReceivedListener, dispatcher);

        // Process events with our dispatcher
        final long start = System.currentTimeMillis();
        long elapsedSeconds = 0;
        do {
            try {
                // block the call for 1000ms
                dispatcher.processEvents(1000);
            } catch (TimeoutError e) {
                // Ignore.
            }
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000;
        } while (elapsedSeconds < runningTime);

        // Remove listener
        this.thing.removeListener(gpsDataReceivedListener, dispatcher);
    }

    @Override
    public void close() {
        cleanup();
    }

}
