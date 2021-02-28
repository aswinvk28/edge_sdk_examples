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

package dashboard;

public class TruckDataValue {
    private float lat = Float.MIN_VALUE;
    private float lng = Float.MIN_VALUE;
    private long locationUpdateTime = System.currentTimeMillis();

    private double distance = Float.MIN_VALUE;
    private float eta = Float.MIN_VALUE;
    private long positionUpdateTime = System.currentTimeMillis();
    
    public float getLat() {
        return lat;
    }
    
    public void setLat(float lat) {
        this.lat = lat;
    }
    
    public float getLng() {
        return lng;
    }
    
    public void setLng(float lng) {
        this.lng = lng;
    }
    
    public long getLocationUpdateTime() {
        return locationUpdateTime;
    }
    
    public void setLocationUpdateTime(long locationUpdateTime) {
        this.locationUpdateTime = locationUpdateTime;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public float getEta() {
        return eta;
    }
    
    public void setEta(float eta) {
        this.eta = eta;
    }
    
    public long getPositionUpdateTime() {
        return positionUpdateTime;
    }
    
    public void setPositionUpdateTime(long positionUpdateTime) {
        this.positionUpdateTime = positionUpdateTime;
    }

}
