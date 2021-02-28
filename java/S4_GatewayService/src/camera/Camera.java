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
 * This code is part of example scenario 4 'Gateway Service' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

package camera;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Vector;

import com.adlinktech.datariver.*;

public class Camera implements Closeable {
    private static final int CAMERA_SAMPLE_DELAY = 1000;
    private static final int CAMERA_INITIAL_DELAY = 2000;
    private static final int CAMERA_DELAY = 100;
    private static final int BARCODE_INTERVAL = 5000;
    private static final int BARCODE_LIFESPAN = 15000;
    private static final int BARCODE_SKIP_PERCENTAGE = 25;

    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    private final Map<String, String> relatedCameras;
    private final Vector<Thread> threads;
    
    private boolean closed = false;
    
    public Camera(final String thingPropertiesUri) {
        safePrintln("Camera started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
        this.relatedCameras = new HashMap<String, String>();
        this.threads = new Vector<Thread>();
        setState("on");
    }

    /**
     * Thread-safe println that guarantees println is not interleaved
     * with println output from another thread. Some of this class's
     * methods are called from listeners, which operate on separate threads.
     * For the integrity of output, all of this class's printing is done
     * via this method.
     */
    private void safePrintln(String msg) {
        synchronized(System.out) {
            System.out.println(msg);
        }
    }

    private void cleanup() {
        try {
            // Set camera state to 'off'
            setState("off");
        } catch (ThingAPIException e) {
            safePrintln("Error setting camera state to off: " + e.getMessage());
        }

        // Stop and join threads
        this.setClosed(true);
        for (int i = 0; i < this.threads.size(); ++i) {
            if (this.threads.get(i).isAlive()) {
                try {
                    this.threads.get(i).join();
                } catch (InterruptedException e) {
                    // someone tried to stop us, still wait for all threads to finish
                }
            }
        }
        
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        safePrintln("Camera stopped");
    }
    
    private String getAbsFileUri(final String fileName) {
        final String dir = System.getProperty("user.dir");
        final String prefix = "file://";
        return prefix.concat(dir).concat("/").concat(fileName);
    }

    private Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/CameraStateTagGroup.json"));
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/ObservationTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/CameraThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }
    
    private String getFlowId(final String barcode) {
        String flowId = "";
        if (hasRelatedCameras()) {
            flowId = getParentContext(this.thing.getContextId()) + ".cameras." + barcode;
        } else {
            flowId = this.thing.getContextId() + "." + barcode;
        }

        return flowId;
    }

    private void writeSample(final String barcode, final int x, final int y, final int z) {
        final IotValue barcodeValue = new IotValue();
        final IotValue positionXValue = new IotValue();
        final IotValue positionYValue = new IotValue();
        final IotValue positionZValue = new IotValue();;
        barcodeValue.setString(barcode);
        positionXValue.setInt32(x);
        positionYValue.setInt32(y);
        positionZValue.setInt32(z);
        final IotNvpSeq data = new IotNvpSeq();
        data.add(new IotNvp("barcode", barcodeValue));
        data.add(new IotNvp("position_x", positionXValue));
        data.add(new IotNvp("position_y", positionYValue));
        data.add(new IotNvp("position_z", positionZValue));

        this.thing.write("observation", getFlowId(barcode), data);
    }

    private void purgeFlow(final String barcode) {
        this.thing.purge("observation", getFlowId(barcode));
    }

    private void setState(final String state) {
        final IotValue stateValue = new IotValue();
        stateValue.setString(state);
        final IotNvpSeq data = new IotNvpSeq();
        data.add(new IotNvp("state", stateValue));

        this.thing.write("state", data);
    }

    private String getParentContext(final String contextId) {
        final int found = contextId.lastIndexOf(".");
        if (found != -1) {
            return contextId.substring(0, found);
        }

        return contextId;
    }

    private boolean hasRelatedCameras() {
        return this.relatedCameras.size() > 0;
    }

    private void checkRegistryForRelatedCameras() {
        final DiscoveredThingRegistry discoveredThingsRegistry = this.dataRiver.getDiscoveredThingRegistry();
        final DiscoveredThingSeq things = discoveredThingsRegistry.getDiscoveredThings();
        for (final DiscoveredThing thing : things) {
            if (isRelated(thing)) {
                discoveredRelatedCamera(thing.getId(), thing.getContextId());
            }
        }
    }

    public void barcodeTask(final String barcode) {
        final long start = System.currentTimeMillis();
        long elapsedMilliseconds = 0;
        int x = ThreadLocalRandom.current().nextInt(100);
        int y = ThreadLocalRandom.current().nextInt(100);
        int z = ThreadLocalRandom.current().nextInt(100);

        do {
            // Simulate position change
            x += ThreadLocalRandom.current().nextInt(-5, 5);
            y += ThreadLocalRandom.current().nextInt(-5, 5);
            z += ThreadLocalRandom.current().nextInt(-1, 1);

            // Sleep before sending next update
            try {
                Thread.sleep(CAMERA_SAMPLE_DELAY);
            } catch (InterruptedException e) {
                safePrintln("Camera is interrupted");
                return;
            }

            // Send location update for this barcode
            writeSample(barcode, x, y, z);

            elapsedMilliseconds = System.currentTimeMillis() - start;
        } while (!this.isClosed() && elapsedMilliseconds < BARCODE_LIFESPAN);

        purgeFlow(barcode);
    }
    
    public boolean isRelated(final DiscoveredThing thing) {
        return
            getParentContext(thing.getContextId()).equals(getParentContext(this.thing.getContextId()))
                && thing.getClassId().getKeyString().equals(this.thing.getClassId().getKeyString())
                && !thing.getId().equals(this.thing.getId());
    }

    public void discoveredRelatedCamera(final String thingId, final String contextId) {
        if (!this.relatedCameras.containsKey(thingId)) {
            safePrintln("Camera " + this.thing.getContextId() + ": detected other camera with context " + contextId + " (Thing Id " + thingId + ")");
        }
        this.relatedCameras.put(thingId, contextId);
    }

    public void lostRelatedCamera(final String thingId) {
        this.relatedCameras.remove(thingId);
    }
    
    public void run(final int runningTime, final Vector<String> barcodes) {
        final long start = System.currentTimeMillis();
        int barcodeSeqnr = 0;
        long barcodeTimestamp = start - BARCODE_INTERVAL;
        long elapsedSeconds = 0;

        // Add listeners for Thing discovered and Thing lost
        final CameraThingDiscoveredListener newThingDiscoveredListener = new CameraThingDiscoveredListener(this);
        this.dataRiver.addListener(newThingDiscoveredListener);

        final CameraThingLostListener thingLostListener = new CameraThingLostListener(this);
        this.dataRiver.addListener(thingLostListener);

        // Check for related camera already in the discovered things registry
        try {
            Thread.sleep(CAMERA_INITIAL_DELAY);
        } catch (InterruptedException e) {
            safePrintln("Camera is interrupted. Exiting.");
            return;
        }
        checkRegistryForRelatedCameras();

        // Start processing
        do {
            final long now = System.currentTimeMillis();;

            // Check if next barcode should be read
            if (barcodeSeqnr < barcodes.size()
                    && ((System.currentTimeMillis() - barcodeTimestamp) > BARCODE_INTERVAL)) {
                final String barcode = barcodes.get(barcodeSeqnr++);

                // Randomly skip some of the barcodes
                if (ThreadLocalRandom.current().nextInt(100) > BARCODE_SKIP_PERCENTAGE) {
                    final Thread t1 = new Thread(() -> barcodeTask(barcode));
                    this.threads.add(t1);
                    t1.start();
                }

                // Update timestamp and seqnr
                barcodeTimestamp = now;
            }

            // Sleep for some time
            try {
                Thread.sleep(CAMERA_DELAY);
            } catch (InterruptedException e) {
                safePrintln("Camera is interrupted. Exiting.");
                break;
            }

            // Check if camera should keep running
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000;
        } while (elapsedSeconds < runningTime);

        // Remove listeners
        this.dataRiver.removeListener(newThingDiscoveredListener);
        this.dataRiver.removeListener(thingLostListener);
    }

    private boolean isClosed() {
        return closed;
    }

    private void setClosed(final boolean closed) {
        this.closed = closed;
    }

    @Override
    public void close() {
        cleanup();
    }

}
