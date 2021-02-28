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

package thingbrowser;

import java.io.Closeable;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.Dispatcher;
import com.adlinktech.datariver.JSonThingClassRegistry;
import com.adlinktech.datariver.JSonThingProperties;
import com.adlinktech.datariver.Thing;
import com.adlinktech.datariver.ThingAPIException;
import com.adlinktech.datariver.TimeoutError;

//Use "AnsiConsole" from "jansi" library to print ANSI escape sequences both on Windows and Linux
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi;

public class ThingBrowser implements Closeable {

    /**
     * Thread-safe println that guarantees println is not interleaved
     * with println output from multiple threads. This program prints output
     * from listeners, which operate on separate threads, as well as from the
     * main thread.
     * For the integrity of output, all printing is done via this method.
     */
    static void safePrintln(String msg) {
        synchronized(System.out) {
            AnsiConsole.out.println(msg);
        }
    }
    /**
     * Thread-safe print that guarantees println is not interleaved
     * with println output from multiple threads. This program prints output
     * from listeners, which operate on separate threads, as well as from the
     * main thread.
     * For the integrity of output, all printing is done via this method.
     */
    static void safePrint(String msg) {
        synchronized(System.out) {
            AnsiConsole.out.print(msg);
        }
    }


    private final String thingPropertiesUri;
    private final DataRiver dataRiver;
    private final Dispatcher dispatcher;
    private final NewThingDiscoveredListener newThingDiscoveredListener;
    
    public ThingBrowser(final String thingPropertiesUri) {
        this.thingPropertiesUri = thingPropertiesUri;
        this.dataRiver = DataRiver.getInstance();
        this.dispatcher = new Dispatcher();
        this.newThingDiscoveredListener = new NewThingDiscoveredListener(dataRiver);
        createThing();
    }
    
    private void cleanup() {
        try {
            // Remove the discovered Thing listener that was added during class initialization
            this.dataRiver.removeListener(this.newThingDiscoveredListener, this.dispatcher);
        } catch(ThingAPIException e) {
            ThingBrowser.safePrintln("Unexpected error while removing discovered Thing listener: " + e.getMessage());
        }
        
        if(this.dataRiver != null) {
            this.dataRiver.close();
        }
        ThingBrowser.safePrintln(Ansi.ansi().fgGreen() + "ThingBrowser stopped"
                + Ansi.ansi().fgDefault());
    }
    
    private String getAbsFileUri(final String fileName) {
        final String dir = System.getProperty("user.dir");
        final String prefix = "file://";
        return prefix.concat(dir).concat("/").concat(fileName);
    }
    
    private Thing createThing() {
        // Add listener for discovery of Things
        this.dataRiver.addListener(this.newThingDiscoveredListener, this.dispatcher);

        // Create and Populate the ThingClass registry with JSON resource files.
        final JSonThingClassRegistry tcr = new JSonThingClassRegistry();
        tcr.registerThingClassesFromUri(getAbsFileUri("definitions/ThingClass/com.adlinktech.example/ThingBrowserThingClass.json"));
        this.dataRiver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        final JSonThingProperties tp = new JSonThingProperties();
        tp.readPropertiesFromUri(this.thingPropertiesUri);
        return this.dataRiver.createThing(tp);
    }

    public void run(final int runningTime) {
        // Process events with our dispatcher
        final long start = System.currentTimeMillis();
        long elapsedSeconds = 0;
        do {
            try {
                this.dispatcher.processEvents(1000);
            } catch (TimeoutError e) {
                // Ignore
            }
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000;
        } while (elapsedSeconds < runningTime);
    }

    @Override
    public void close() {
        cleanup();
    }
}
