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
 * This code is part of example scenario 1 'Connect a Sensor' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

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

public class TemperatureSensor implements Closeable {
	private final DataRiver dr;
	private final Thing thing;
	private final String thingPropertiesURI;
	static final int SAMPLE_DELAY_MS = 100;

	public TemperatureSensor(final String thingPropertiesURI) {
        System.out.println("Temperature Sensor started");
		this.thingPropertiesURI = thingPropertiesURI;
		this.dr = DataRiver.getInstance();
		this.thing = createThing();
	}

	private String getAbsFileURI(final String fileName) {
	    final String dir = System.getProperty("user.dir");
	    final String prefix = "file://";
		return prefix.concat(dir).concat("/").concat(fileName);
	}

	private void cleanup() {
		if (dr != null) {
			dr.close();
		}
		System.out.println("Temperature Sensor stopped");
	}

	private Thing createThing() {
	    final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
		tgr.registerTagGroupsFromUri(getAbsFileURI("definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json"));
		this.dr.addTagGroupRegistry(tgr);

		final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
		tcr.registerThingClassesFromUri(getAbsFileURI("definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json"));
		dr.addThingClassRegistry(tcr);

		final JSonThingProperties tp = new JSonThingProperties();
		tp.readPropertiesFromUri(thingPropertiesURI);
		
		return dr.createThing(tp);
	}

	private void writeSample(final float temperature) {
	    final IotValue tempV = new IotValue();
		tempV.setFloat32(temperature);

		final IotNvpSeq sensorData = new IotNvpSeq();
		sensorData.add(new IotNvp("temperature", tempV));

		thing.write("temperature", sensorData);
	}

	public void run(final long runningTime) {
	    final Random random = new Random();
		double sampleCount = (runningTime * 1000.0) / SAMPLE_DELAY_MS;
		float actualTemp = 21.5f;

		while (sampleCount-- > 0) {
			actualTemp += (random.nextInt(10) - 5)/ 5.0;
			writeSample(actualTemp);
			try {
                Thread.sleep(SAMPLE_DELAY_MS);
            } catch (InterruptedException e) {
                System.out.println("TemperatureSensor is interrupted. Exiting");
                return;
            }
		}
	}

	@Override
	public void close() {
		cleanup();
	}

}

