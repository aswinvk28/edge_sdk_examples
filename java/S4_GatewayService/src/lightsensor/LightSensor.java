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

package lightsensor;

import java.io.Closeable;

import com.adlinktech.datariver.*;

public class LightSensor implements Closeable {
    private static final int LIGHT_SAMPLE_DELAY_MS = 1000;
    private static final int ILLUMINANCE_THRESHOLD = 400;
    
    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    
    public LightSensor(final String thingPropertiesUri) {
        System.out.println("Light Sensor started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
    }
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Light Sensor stopped");
    }
    
    private String getAbsFileUri(final String fileName) {
        final String dir = System.getProperty("user.dir");
        final String prefix = "file://";
        return prefix.concat(dir).concat("/").concat(fileName);
    }

    private Thing createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/IlluminanceTagGroup.json"));
        tgr.registerTagGroupsFromUri(getAbsFileUri("definitions/TagGroup/com.adlinktech.example/IlluminanceAlarmTagGroup.json"));
        this.dataRiver.addTagGroupRegistry(tgr);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/LightSensorThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    private void writeSample(final int illuminance) {
        final IotValue illuminanceValue = new IotValue();
        illuminanceValue.setUint32(illuminance);
        final IotNvpSeq sensorData = new IotNvpSeq();
        sensorData.add(new IotNvp("illuminance", illuminanceValue));

        this.thing.write("illuminance", sensorData);
    }

    private void alarm(final String message) {
        final IotValue alarmValue = new IotValue();
        alarmValue.setString(message);
        final IotNvpSeq alarmData = new IotNvpSeq();
        alarmData.add(new IotNvp("alarm", alarmValue));

        this.thing.write("alarm", alarmData);
    }

    public void run(final int runningTime) {
        int sampleCount = (runningTime * 1000) / LIGHT_SAMPLE_DELAY_MS;
        int actualIlluminance = 500;
        boolean alarmState = false;

        while (sampleCount-- > 0) {
            // Simulate illuminance change
            final int dir = (sampleCount % 20) > 10 ? -1 : 1;
            actualIlluminance += dir * 30;

            // Write sensor data to river
            writeSample(actualIlluminance);

            // Write alarm if value below threshold
            if (!alarmState && actualIlluminance < ILLUMINANCE_THRESHOLD) {
                alarm("Illuminance below threshold");
                alarmState = true;
            } else if (alarmState && actualIlluminance > ILLUMINANCE_THRESHOLD) {
                alarmState = false;
            }
            
            try {
                Thread.sleep(LIGHT_SAMPLE_DELAY_MS);
            } catch (InterruptedException e) {
                System.out.println("LightSensor is interrupted. Exiting");
                return;
            }
        }
    }

    @Override
    public void close() {
        cleanup();
    }

}
