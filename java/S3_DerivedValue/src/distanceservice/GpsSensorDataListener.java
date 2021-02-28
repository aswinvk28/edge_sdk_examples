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

import com.adlinktech.datariver.FlowState;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpDataAvailableListener;
import com.adlinktech.datariver.IotNvpDataSample;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.IotNvpSeq;

public class GpsSensorDataListener extends IotNvpDataAvailableListener {
    final DistanceService distanceServiceThing;
    
    public GpsSensorDataListener(DistanceService distanceServiceThing) {
        this.distanceServiceThing = distanceServiceThing;
    }
    
    private double calculateDistance(final float truckLocationLat, final float truckLocationLng) {
        return Math.sqrt(Math.pow(truckLocationLat - this.distanceServiceThing.getWarehouseLat(), 2)
            + Math.pow(truckLocationLng - this.distanceServiceThing.getWarehouseLng(), 2));
    }
    
    public void notifyDataAvailable(final IotNvpDataSampleSeq data) {
        for (final IotNvpDataSample locationMessage : data) {
            final String myLocationFlowId = locationMessage.getFlowId();
            if (locationMessage.getFlowState() == FlowState.ALIVE) {
                // Get location data from sample
                final IotNvpSeq locationData = locationMessage.getData();
                float truckLocationLat = 0.0f;
                float truckLocationLng = 0.0f;
                long timestamp = 0;

                try {
                    for (final IotNvp nvp : locationData) {
                        if (nvp.getName().equals("location")) {
                            for (final IotNvp locationNvp : nvp.getValue().getNvpSeq()) {
                                if (locationNvp.getName().equals("latitude")) {
                                    truckLocationLat = locationNvp.getValue().getFloat32();
                                } else if (locationNvp.getName().equals("longitude")) {
                                    truckLocationLng = locationNvp.getValue().getFloat32();
                                }
                            }
                        } else if (nvp.getName().equals("timestampUtc")) {
                            timestamp = nvp.getValue().getUint64();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("An unexpected error occured while processing data-sample: " + e.getMessage());
                    continue;
                }

                // Calculate distance to the warehouse
                final double distance = calculateDistance(truckLocationLat, truckLocationLng);

                // This example uses a fixed multiplier for ETA. In a real-world
                // scenario this would be calculated based on e.g. real-time traffic information
                final float eta = (float)(distance * 5.12345);

                this.distanceServiceThing.writeDistance(myLocationFlowId, distance, eta, timestamp);
            }
        }
    }
}