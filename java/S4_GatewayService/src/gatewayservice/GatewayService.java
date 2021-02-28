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

package gatewayservice;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Use "AnsiConsole" from "jansi" library to print ANSI escape sequences both on Windows and Linux
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import com.adlinktech.datariver.*;

public class GatewayService implements Closeable {

    // This is a ConcurrentHashMap because it is used by a listener and listeners get triggered in different threads.
    public static Map<String, String> thingContext = new ConcurrentHashMap<String, String>();
    
    private static final int READ_DELAY = 10;
    private static final int TOTAL_HEADER_LINES = 2;
    private static final int TOTAL_FOOTER_MESSAGE_LINES = 1;
    private final Map<DataFlowKey, DataFlowValue> sampleCount;
    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Thing thing;
    private final int screenHeightInLines;
    
    public GatewayService(final String thingPropertiesUri, final int screenHeight) {
        System.out.println("Gateway Service started");
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.thing = createThing();
        this.sampleCount = new HashMap<DataFlowKey, DataFlowValue>();
        this.screenHeightInLines = screenHeight;
    }
    
    private void cleanup() {
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        System.out.println("Gateway Service stopped");
    }
    
    private String getAbsFileUri(final String fileName) {
        final String dir = System.getProperty("user.dir");
        final String prefix = "file://";
        return prefix.concat(dir).concat("/").concat(fileName);
    }

    private Thing createThing() {
        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/GatewayServiceThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }
    
    private String truncate(final String str, final int width) {
        if (str.length() > width - 3) {
            return str.substring(0, width - 3) + "...";
        }

        return str;
    }

    private void displayStatus() {
        // Return if there is nothing to display
        if (this.sampleCount.entrySet().isEmpty()) {
            return;
        }

        // Move cursor position to the origin (0,0) of the console
        AnsiConsole.out.print(Ansi.ansi().cursor(0, 0));

        // Add header row for table
        displayHeader();

        // Write new data to console
        int lineCount = 0;
        for(final HashMap.Entry<DataFlowKey, DataFlowValue> data : this.sampleCount.entrySet()) {
            final DataFlowKey key = data.getKey();
            final DataFlowValue value = data.getValue();

            // Set grey color for purged flows
            final boolean alive = value.getFlowState() == FlowState.ALIVE;
            final Ansi COLOR1 = alive ? Ansi.ansi().fgGreen() : Ansi.ansi().fgDefault();
            final Ansi COLOR2 = alive ? Ansi.ansi().fgMagenta() : Ansi.ansi().fgBrightBlack();
            final String flowState = alive ? "" : " <purged>";

            AnsiConsole.out.println(
                COLOR1 + String.format("%-32s", truncate(key.getSourceThingContext() + flowState, 32))
                + Ansi.ansi().fgDefault()
                + COLOR2 + String.format("%-30s", truncate(key.getFlowId(), 30))
                + COLOR2 + String.format("%-20s", truncate(key.getTagGroupName(), 20))
                + Ansi.ansi().fgDefault()
                + Ansi.ansi().fgBrightBlack() + String.format("%-12s", truncate(key.getTagGroupQos(), 12))
                + Ansi.ansi().fgDefault()
                + COLOR1 + String.format("%8s", value.getSampleCount())
                + Ansi.ansi().fgDefault());

            lineCount++;

            if(lineCount < this.sampleCount.size() &&
                    lineCount >= (this.screenHeightInLines - TOTAL_HEADER_LINES - TOTAL_FOOTER_MESSAGE_LINES - 1)) {

                AnsiConsole.out.println(
                        "... " + (this.sampleCount.size() - lineCount) + " more lines available. "
                        + "Set terminal height to " + (this.sampleCount.size() + TOTAL_HEADER_LINES + TOTAL_FOOTER_MESSAGE_LINES + 1) + ". "
                        + "See the README file for more instructions.");
                break;
            }
        }
    }

    private void displayHeader() {
        System.out.println(
                String.format("%-32s", "Thing's ContextId")
                + String.format("%-30s", "Flow Id")
                + String.format("%-20s", "TagGroup Name")
                + String.format("%-12s", "QoS")
                + String.format("%8s", "Samples")
                + "\n");
    }

    private void readThingsFromRegistry() {
        final DiscoveredThingRegistry discoveredThingsRegistry = this.dataRiver.getDiscoveredThingRegistry();
        final DiscoveredThingSeq things = discoveredThingsRegistry.getDiscoveredThings();
        for (final DiscoveredThing thing : things) {
            thingContext.put(thing.getId(), thing.getContextId());
        }
    }
    
    public void run(final int runningTime) {
        final long startTimestamp = System.currentTimeMillis();
        long elapsedTime = 0;

        // Add listener for discovering new Things
        final NewThingDiscoveredListener newThingDiscoveredListener = new NewThingDiscoveredListener();
        this.dataRiver.addListener(newThingDiscoveredListener);

        // Get meta-data (contextId) for Things in discovered things registry
        readThingsFromRegistry();

        // Clear console screen before printing samples
        AnsiConsole.out.print(Ansi.ansi().eraseScreen(Erase.ALL));

        do {
            // Read data
            final IotNvpDataSampleSeq msgs = this.thing.readNextIotNvp("dynamicInput", (int)((runningTime * 1000) - elapsedTime));

            // Loop received samples and update counters
            for (final IotNvpDataSample msg : msgs) {
                final FlowState flowState = msg.getFlowState();

                final DataFlowKey key = new DataFlowKey(msg);
                
                if(!this.sampleCount.containsKey(key)) {
                    final DataFlowValue dfVal = new DataFlowValue();
                    this.sampleCount.put(key, dfVal);
                }

                // Store state in value for this flow
                this.sampleCount.get(key).setFlowState(flowState);

                // In case flow is alive or if flow is purged but sample
                // contains data: increase sample count
                final boolean sampleContainsData = (flowState == FlowState.ALIVE) || (msg.getData().size() > 0);

                if (sampleContainsData) {
                    this.sampleCount.get(key).setSampleCount(this.sampleCount.get(key).getSampleCount() + 1);

                    // In a real-world use-case you would have additional processing
                    // of the data received by msg.getData()
                }
            }

            // Update console output
            displayStatus();

            // Sleep before reading next samples
            try {
                Thread.sleep(READ_DELAY);
            } catch (InterruptedException e) {
                System.out.println("GatewayService is interrupted. Exiting");
                break;
            }

            // Get elapsed time
            elapsedTime = System.currentTimeMillis() - startTimestamp;
        } while (elapsedTime / 1000 < runningTime);

        // Remove listener
        this.dataRiver.removeListener(newThingDiscoveredListener);
    }

    @Override
    public void close() {
        cleanup();
    }

}
