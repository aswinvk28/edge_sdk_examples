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

import java.io.Closeable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.DiscoveredTagGroupRegistry;
import com.adlinktech.datariver.FlowState;
import com.adlinktech.datariver.IotNvp;
import com.adlinktech.datariver.IotNvpDataSample;
import com.adlinktech.datariver.IotNvpDataSampleSeq;
import com.adlinktech.datariver.IotNvpSeq;
import com.adlinktech.datariver.JSonTagGroupRegistry;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.TagDefinition;
import com.adlinktech.datariver.TagGroup;
import com.adlinktech.datariver.Thing;
import com.adlinktech.datariver.TypeDefinition;
import com.adlinktech.datariver.TypeDefinitionSeq;

// Use "AnsiConsole" from "jansi" library to print ANSI escape sequences both on Windows and Linux
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi;

public class Dashboard implements Closeable {
    
    private static final long READ_DELAY = 500;
    
    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    private final Map<String, TruckDataValue> truckData;
    private int lineCount = 0;
    private String distanceUnit = "";
    private String etaUnit = "";
    
    public Dashboard(final String thingPropertiesUri) {
        System.out.println("Dashboard started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
        this.truckData = new HashMap<String, TruckDataValue>();
    }
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Dashboard stopped");
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
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/LocationDashboardThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    private void displayHeader() {
        System.out.println(
                String.format("%-20s", "Truck Context") +
                String.format("%-15s", "Latitude") +
                String.format("%-15s", "Longitude") +
                String.format("%-25s", ("Distance (" + this.getDistanceUnit() + ")")) +
                String.format("%-25s", ("ETA (" + this.getEtaUnit() + ")")));
    }

    private String formatNumber(final double value, final int precision) {
        String result = "";

        if (value != Float.MIN_VALUE)
            result = String.format("%."+precision+"f", value);
        else
            result = "-";

        return result;
    }

    private String formatTime(final long time) {
        if (time != 0) {
            // "time" is in seconds. Multiplying it with 1000 to
            // pass milliseconds in new Date method.
            final Date date = new Date(time * 1000);
            final DateFormat df = new SimpleDateFormat("HH:mm:ss");
            
            return df.format(date);
        } else {
            return "-";
        }
    }

    private void displayStatus() {
        // Reset cursor position for previous console update
    	AnsiConsole.out.print(Ansi.ansi().cursorUp(this.getLineCount() * 2 + 1));

        // Add header row for table
        displayHeader();

        // Write new data to console
        this.setLineCount(0);
        for (final HashMap.Entry<String, TruckDataValue> data : this.truckData.entrySet()) {
            final String key = data.getKey();
            final TruckDataValue value = data.getValue();

            AnsiConsole.out.println(
                Ansi.ansi().fgGreen() + String.format("%-20s", key) +
                Ansi.ansi().fgDefault() +
                Ansi.ansi().fgMagenta() +
                String.format("%-15s", formatNumber(value.getLat(), 6)) +
                String.format("%-15s", formatNumber(value.getLng(), 6)) +
                Ansi.ansi().fgDefault() +
                Ansi.ansi().fgGreen() +
                String.format("%-25s", formatNumber(value.getDistance(), 3)) +
                String.format("%-20s\n", formatNumber(value.getEta(), 1)) +
                String.format("%-20s", " ") +
                Ansi.ansi().fgDefault() +
                Ansi.ansi().fgBrightBlack() +
                String.format("%-30s", ("  updated: " + formatTime(value.getLocationUpdateTime()))) +
                String.format("%-45s", ("  updated: " + formatTime(value.getPositionUpdateTime()))) +
                Ansi.ansi().fgDefault());
            
            this.setLineCount(this.getLineCount() + 1);
        }
    }

    private TruckDataValue getLocationFromSample(final IotNvpDataSample sample) {
        final TruckDataValue tdvalue = new TruckDataValue();
        final IotNvpSeq data = sample.getData();
        for (final IotNvp nvp : data) {
            if (nvp.getName().equals("location")) {
                for (final IotNvp locationNvp : nvp.getValue().getNvpSeq()) {
                    if (locationNvp.getName().equals("latitude")) {
                        tdvalue.setLat(locationNvp.getValue().getFloat32());
                    } else if (locationNvp.getName().equals("longitude")) {
                        tdvalue.setLng(locationNvp.getValue().getFloat32());
                    }
                }
            } else if (nvp.getName().equals("timestampUtc")) {
                tdvalue.setLocationUpdateTime(nvp.getValue().getUint64());
            }
        }
        
        return tdvalue;
    }

    private TruckDataValue getDistanceFromSample(final IotNvpDataSample sample) {
        final TruckDataValue tdvalue = new TruckDataValue();
        final IotNvpSeq data = sample.getData();
        for (final IotNvp nvp : data) {
            if (nvp.getName().equals("distance")) {
                tdvalue.setDistance(nvp.getValue().getFloat64());
            }
            if (nvp.getName().equals("eta")) {
                tdvalue.setEta(nvp.getValue().getFloat32());
            }
            if (nvp.getName().equals("timestampUtc")) {
                tdvalue.setPositionUpdateTime(nvp.getValue().getUint64());
            }
        }
        
        return tdvalue;
    }

    private void processLocationSample(final IotNvpDataSample dataSample) {
        try {
            if (dataSample.getFlowState() == FlowState.ALIVE) {
                final TruckDataValue tdval = getLocationFromSample(dataSample);
                final String key = dataSample.getFlowId();
                if (this.truckData.containsKey(key)) {
                    this.truckData.get(key).setLat(tdval.getLat());
                    this.truckData.get(key).setLng(tdval.getLng());
                    this.truckData.get(key).setLocationUpdateTime(tdval.getLocationUpdateTime());
                } else {
                    this.truckData.put(key, tdval);
                }
            }
        } catch (Exception e) {
            System.out.println("An unexpected error occured while processing data-sample: " + e.getMessage());
        }
    }

    private void processDistanceSample(final IotNvpDataSample dataSample) {
        try {
            if (dataSample.getFlowState() == FlowState.ALIVE) {
                final TruckDataValue tdval = getDistanceFromSample(dataSample);
                final String key = dataSample.getFlowId();
                if (this.truckData.containsKey(key)) {
                    this.truckData.get(key).setDistance(tdval.getDistance());
                    this.truckData.get(key).setEta(tdval.getEta());
                    this.truckData.get(key).setPositionUpdateTime(tdval.getPositionUpdateTime());
                } else {
                    this.truckData.put(key, tdval);
                }
            }
        }
        catch (Exception e) {
            System.out.println("An unexpected error occured while processing data-sample: " + e.getMessage());
        }
    }

    private void getTagUnitDescriptions() {
        final DiscoveredTagGroupRegistry tgr = this.dataRiver.getDiscoveredTagGroupRegistry();
        final TagGroup distanceTagGroup = tgr.findTagGroup("Distance:com.adlinktech.example:v1.0");
        final TypeDefinitionSeq typedefs = distanceTagGroup.getTypeDefinitions();
        for (final TypeDefinition typeD : typedefs) {
            for(final TagDefinition tag: typeD.getTags()){
                if (tag.getName().equals("distance")) {
                    this.setDistanceUnit(tag.getUnit());
                }
                if (tag.getName().equals("eta")) {
                    this.setEtaUnit(tag.getUnit());
                }
            }
        }
    }

    public void run(final int runningTime) {
        final long startTimestamp = System.currentTimeMillis();
        long elapsedTime = 0;

        do {
            // Retrieve and process location samples
            final IotNvpDataSampleSeq locationSamples = this.thing.readIotNvp("location", 0);
            for (final IotNvpDataSample sample : locationSamples) {
                processLocationSample(sample);
            }

            // Retrieve and process distance samples
            final IotNvpDataSampleSeq distanceSamples = this.thing.readIotNvp("distance", 0);
            for (final IotNvpDataSample sample : distanceSamples) {
                processDistanceSample(sample);
            }

            if (this.getDistanceUnit().isEmpty() || this.getEtaUnit().isEmpty()) {
                getTagUnitDescriptions();
            }

            // Update console output
            displayStatus();

            // Sleep before next update
            try {
                Thread.sleep(READ_DELAY);
            } catch (InterruptedException e) {
                System.out.println("Dashboard interrupted. Exiting.");
                break;
            }

            // Get elapsed time
            elapsedTime = (System.currentTimeMillis() - startTimestamp) / 1000;
        } while (elapsedTime < runningTime);
    }

    private int getLineCount() {
        return lineCount;
    }

    private void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    private String getDistanceUnit() {
        return distanceUnit;
    }

    private void setDistanceUnit(String distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    private String getEtaUnit() {
        return etaUnit;
    }

    private void setEtaUnit(String etaUnit) {
        this.etaUnit = etaUnit;
    }

    @Override
    public void close() {
        cleanup();
    }
}
