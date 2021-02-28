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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.adlinktech.datariver.ThingAPIException;

public class GatewayServiceLauncher {

    public static void main(String[] args) {

        final int SCREEN_HEIGHT_IN_LINES = 45;
    	
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();
        
        try {
            // Get thing properties URI from command line parameter
            final Option propertiesUriOption = new Option("t", "thing", true, "Thing properties URI");
            propertiesUriOption.setRequired(true);
            final Option runningTimeOption = new Option("r", "running-time", true, "Total running time of the program (in seconds)");
            runningTimeOption.setRequired(true);

            options.addOption(propertiesUriOption);
            options.addOption(runningTimeOption);
            
            final CommandLine cmd = parser.parse(options, args);
            final int runningTime = Integer.parseInt(cmd.getOptionValue("r"));
            final String thingPropertiesUri = cmd.getOptionValue("t");
            
            // Get LINES (terminal's height) from environment variable
            final String linesKey = "LINES";
            final String linesEnv = System.getenv(linesKey);
            int screenHeightInLines = SCREEN_HEIGHT_IN_LINES;
            if (linesEnv == null) {
            	System.out.println("Environment variable " + linesKey + " not set");
            	System.out.println("Assuming " + linesKey + "(terminal height) = " + screenHeightInLines);
            } else {
            	screenHeightInLines = Integer.parseInt(linesEnv);
            }
            
            try(final GatewayService gateway = new GatewayService(thingPropertiesUri, screenHeightInLines)) {
                gateway.run(runningTime);
            } 
            
        } catch (ParseException e1) {
            System.out.println(e1.getMessage());
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
