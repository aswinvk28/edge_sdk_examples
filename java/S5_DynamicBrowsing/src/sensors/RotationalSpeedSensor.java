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
 * This code is part of example scenario 5 'Dynamic Browsing' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

package sensors;

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

public class RotationalSpeedSensor implements Closeable {
    private static final int SPEED_SAMPLE_DELAY_MS = 3000;
    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    
    public RotationalSpeedSensor(final String thingPropertiesUri) {
        System.out.println("Rotational Speed Sensor started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
    }
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Rotational Speed Sensor stopped");
    }
    
    private String getAbsFileUri(final String fileName) {
        final String dir = System.getProperty("user.dir");
        final String prefix = "file://";
        return prefix.concat(dir).concat("/").concat(fileName);
    }

    private Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/RotationalSpeedTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/RotationalSpeedSensorThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    private void writeSample(final int speed, final int lastHourMin, final int lastHourMax, final float lastHourAverage) {
        final IotValue speedValue = new IotValue();
        speedValue.setUint32(speed);

        final IotValue lastHourMinValue = new IotValue();
        lastHourMinValue.setUint32(lastHourMin);

        final IotValue lastHourMaxValue = new IotValue();
        lastHourMaxValue.setUint32(lastHourMax);

        final IotValue lastHourAverageValue = new IotValue();
        lastHourAverageValue.setFloat32(lastHourAverage);
        
        final IotNvpSeq data = new IotNvpSeq();
        data.add(new IotNvp("speed", speedValue));
        data.add(new IotNvp("lastHourMin", lastHourMinValue));
        data.add(new IotNvp("lastHourMax", lastHourMaxValue));
        data.add(new IotNvp("lastHourAverage", lastHourAverageValue));

        this.thing.write("rotationalSpeed", data);
    }

    public void run(final int runningTime) {
        final Random rand = new Random();
        int sampleCount = (runningTime * 1000) / SPEED_SAMPLE_DELAY_MS;
        int speed = 1000;

        while (sampleCount-- > 0) {
            // Simulate speed change
            speed += (rand.nextInt(10)) - 4;

            writeSample(speed, 0, 0, 0.0f);
            
            try {
                Thread.sleep(SPEED_SAMPLE_DELAY_MS);
            } catch (InterruptedException e) {
                System.out.println("RotationalSpeedSensor is interrupted. Exiting.");
                break;
            }
        }
    }

    @Override
    public void close() {
        cleanup();
    }

}
