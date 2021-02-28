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

import com.adlinktech.datariver.*;

public class TemperatureSensor implements Closeable {
    private static final int TEMP_SAMPLE_DELAY_MS = 1000;
    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    
    public TemperatureSensor(final String thingPropertiesUri) {
        System.out.println("Temperature Sensor started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
    }
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Temperature Sensor stopped");
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
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    private void writeSample(final float temperature) {
        final IotValue temperatureValue = new IotValue();
        temperatureValue.setFloat32(temperature);
        final IotNvpSeq sensorData = new IotNvpSeq();
        sensorData.add(new IotNvp("temperature", temperatureValue));

        this.thing.write("temperature", sensorData);
    }
    
    public void run(final int runningTime) {
        final Random rand = new Random();
        int sampleCount = (runningTime * 1000) / TEMP_SAMPLE_DELAY_MS;
        float actualTemperature = 21.5f;

        while (sampleCount-- > 0) {
            // Simulate temperature change
            actualTemperature += (float)(rand.nextInt(10) - 5) / 10.0f;

            writeSample(actualTemperature);
            
            try {
                Thread.sleep(TEMP_SAMPLE_DELAY_MS);
            } catch (InterruptedException e) {
                System.out.println("TemperatureSensor is interrupted. Exiting.");
                break;
            }
        }
    }

    @Override
    public void close() {
        cleanup();
    }

}
