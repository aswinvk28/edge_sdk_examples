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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.adlinktech.datariver.ThingAPIException;

public class TemperatureDisplayLauncher {
	public static void main(String[] args) {
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();
        
        try {
            // Get thing properties URI from command line parameter
            final Option thingPropertiesUriOption = new Option("t", "thing", true, "Thing properties URI");
            thingPropertiesUriOption.setRequired(true);
            final Option runningTimeOption = new Option("r", "running-time", true, "Total running time of the program (in seconds)");
            runningTimeOption.setRequired(true);
    
            options.addOption(thingPropertiesUriOption);
            options.addOption(runningTimeOption);
            
            final CommandLine cmd = parser.parse(options, args);
            final long runningTime = Long.parseLong(cmd.getOptionValue("r"));
            final String thingPropertiesUri = cmd.getOptionValue("t");
            
            try(final TemperatureDisplay temperatureDisplay = new TemperatureDisplay(thingPropertiesUri)) {
                temperatureDisplay.run(runningTime);
            }
            
        } catch(ParseException ex) {
            System.out.println(ex.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        } catch(NumberFormatException ex) {
            System.out.println(ex.getMessage());
            formatter.printHelp("utility-name", options);
             System.exit(1);
        } catch (ThingAPIException e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            System.exit(1);
        }
	}
}
