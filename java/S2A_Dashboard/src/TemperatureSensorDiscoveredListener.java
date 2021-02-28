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
 * This code is part of example scenario 2 'Connect a Dashboard' of the
 * ADLINK Edge SDK. For a description of this scenario see the
 * 'Edge SDK User Guide' in the /doc directory of the Edge SDK instalation.
 *
 * For instructions on building and running the example see the README
 * file in the Edge SDK installation directory.
 */

import com.adlinktech.datariver.DiscoveredThing;
import com.adlinktech.datariver.ThingDiscoveredListener;

//Use "AnsiConsole" from "jansi" library to print ANSI escape sequences both on Windows and Linux
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi;

public class TemperatureSensorDiscoveredListener extends ThingDiscoveredListener {
	
    public void notifyThingDiscovered(final DiscoveredThing thing) {
    	if (thing.getClassId().getName().equals("TemperatureSensor")) {
    		AnsiConsole.out.println(Ansi.ansi().fgGreen()
    		        + "New temperature sensor discovered: " + thing.getDescription()
                    + " (" + thing.getId() + ")"
                    + Ansi.ansi().fgDefault());
        } else {
        	AnsiConsole.out.println(Ansi.ansi().fgBrightBlack()
        	        + "New incompatible sensor type '"
        	        + thing.getClassId().getName() + "' discovered (" + thing.getId() + ")"
        	        + Ansi.ansi().fgDefault());
        }
    }
}