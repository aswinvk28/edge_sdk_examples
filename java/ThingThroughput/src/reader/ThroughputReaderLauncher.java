/*                         ADLINK Edge SDK
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
 * This is a simple throughput application measuring obtainable throughput using the thingapi
 *
 */

package reader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.adlinktech.datariver.ThingAPIException;

public class ThroughputReaderLauncher {

	public static void main(String[] args) {
        
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();
        
        try {
    		// Get thing properties URI from command line parameter
    		final Option pollingDelayOption = new Option("p", "polling-delay", true, "Polling delay (milliseconds)");
    		pollingDelayOption.setRequired(false);
    		final Option runningTimeOption = new Option("r", "running-time", true, "Running Time in seconds (0 is infinite)");
    		runningTimeOption.setRequired(false);
            final Option helpOption = new Option("h", "help", false, "Print help");
            helpOption.setRequired(false);
    		
    		options.addOption(pollingDelayOption);
    		options.addOption(runningTimeOption);
            options.addOption(helpOption);
    		
    		final CommandLine cmd = parser.parse(options, args);

            if(cmd.hasOption("help")) {
                // automatically generate the help statement
                formatter.printHelp( "java -jar throughput_reader.jar", options );
                System.exit(0);
            }
			
    		final long pollingDelay = Long.parseLong(cmd.getOptionValue("p", "0"));
    		final long runningTime = Long.parseLong(cmd.getOptionValue("r", "0"));
    	    
    	    try(final ThroughputReader tpReader = new ThroughputReader("file://./config/ThroughputReaderProperties.json")) {
    	        tpReader.registerCtrlHandler();
    	    	tpReader.run(pollingDelay, runningTime);
    	    }
    	    
        } catch (ParseException e1) {
            System.out.println(e1.getMessage());
            formatter.printHelp("java -jar throughput_reader.jar", options);
            System.exit(1);
        } catch(NumberFormatException ex) {
            System.out.println(ex.getMessage());
            formatter.printHelp("java -jar throughput_reader.jar", options);
            System.exit(1);
        } catch (ThingAPIException e) {
	    	System.out.println("An unexpected error occurred: " + e.getMessage());
	    }
	}

}
