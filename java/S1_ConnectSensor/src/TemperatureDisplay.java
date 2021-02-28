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

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.FlowState;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpDataSample;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;

public class TemperatureDisplay implements Closeable {
    private final long READ_SAMPLE_DELAY = 100; // 100 miliseconds

    private final String thingPropertiesURI;
    private final DataRiver dr;
    private final Thing thing;

    public TemperatureDisplay(final String thingPropertiesURI) {
        System.out.println("Temperature Display started");
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
		System.out.println("Temperature Display stopped");
	}

	private Thing createThing() {
	    final JSonTagGroupRegistry tgr = new JSonTagGroupRegistry();
		tgr.registerTagGroupsFromUri(getAbsFileURI("definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json"));
		this.dr.addTagGroupRegistry(tgr);

		final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
		tcr.registerThingClassesFromUri(getAbsFileURI("definitions/ThingClass/com.adlinktech.example/TemperatureDisplayThingClass.json"));
		this.dr.addThingClassRegistry(tcr);

		final JSonThingProperties tp = new JSonThingProperties();
		tp.readPropertiesFromUri(thingPropertiesURI);

		return this.dr.createThing(tp);
	}

	public void run(final long runningTime) {
		final long start = System.currentTimeMillis();
		long elapsed = 0;

		do {
		    final IotNvpDataSampleSeq msgs = this.thing.readIotNvp("temperature", (int)((runningTime * 1000) - elapsed));
		    for (final IotNvpDataSample msg : msgs) {
		        final FlowState flowState = msg.getFlowState();
				if (flowState == FlowState.ALIVE) {
				    final IotNvpSeq dataSample = msg.getData();
					float temp = 0.0f;
					for (final IotNvp nvp: dataSample) {
						if (nvp.getName().equals("temperature")) {
							temp = nvp.getValue().getFloat32();
						}
					}
					System.out.println("Sensor data received: " + String.format("%5.1f", temp));
				}
			}
            try {
                Thread.sleep(READ_SAMPLE_DELAY);
            } catch (InterruptedException e) {
                System.out.println("TemperatureDisplay is interrupted. Exiting.");
                return;
            }
			elapsed = System.currentTimeMillis() - start;
		} while (elapsed < (runningTime * 1000));
	}

	@Override
	public void close() {
		cleanup();
	}
}

