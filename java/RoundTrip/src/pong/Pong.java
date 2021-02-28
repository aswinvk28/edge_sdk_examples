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

package pong;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.FlowState;
import com.adlinktech.datariver.IotNvpDataSample;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;

public class Pong implements Closeable {
    private AtomicBoolean stop = new AtomicBoolean(false);
    
    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    
    public Pong(final String thingPropertiesUri) {
        System.out.println("Pong started");
        this.thingPropertiesUri = thingPropertiesUri;
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
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Pong stopped");
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
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/PongThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    public void run() {
        boolean terminate = false;
        System.out.println("Waiting for samples from ping to send back...");

        while (!terminate && !stop.get()) {
            final IotNvpDataSampleSeq samples = this.thing.readIotNvp("Ping");

            for (final IotNvpDataSample sample : samples) {
                if (sample.getFlowState() == FlowState.PURGED) {
                    System.out.println("Received termination request. Terminating.");
                    terminate = true;
                    break;
                } else {
                    this.thing.write("Pong", sample.getData());
                }
            }
        }
    }

    @Override
    public void close() {
        cleanup();
    }

}

