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

package camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.adlinktech.datariver.ThingAPIException;

public class CameraLauncher {
    
    private static Vector<String> readBarCodes(final String barcodeFilePath) {
        final Vector<String> barcodes = new Vector<String>();
        
        try {
            final File file = new File(barcodeFilePath);
            final BufferedReader br = new BufferedReader(new FileReader(file));
            String barcode = "";
    
            // Read barcodes from file
            while ((barcode = br.readLine()) != null)
            {
                barcode = barcode.replaceAll("^\n+", "");
                if (barcode.length() > 0)
                    barcodes.add(barcode);
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("Cannot open barcode file: " + barcodeFilePath);
        } catch (IOException e) {
            System.out.println("Cannot open barcode file: " + barcodeFilePath);
        }

        return barcodes;
    }


    public static void main(String[] args) {
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();
        
        try {
            // Get thing properties URI from command line parameter
            final Option propertiesUriOption = new Option("t", "thing", true, "Thing properties URI");
            propertiesUriOption.setRequired(true);
            final Option barcodesOption = new Option("b", "barcodes", true, "Barcode file path");
            barcodesOption.setRequired(true);
            final Option runningTimeOption = new Option("r", "running-time", true, "Total running time of the program (in seconds)");
            runningTimeOption.setRequired(true);
    
            options.addOption(propertiesUriOption);
            options.addOption(barcodesOption);
            options.addOption(runningTimeOption);
            
            final CommandLine cmd = parser.parse(options, args);
            
            final int runningTime = Integer.parseInt(cmd.getOptionValue("r"));
            final String thingPropertiesUri = cmd.getOptionValue("t");
            final String barcodeFilePath = cmd.getOptionValue("b");
            
            // Get barcodes
            final Vector<String> barcodes = readBarCodes(barcodeFilePath);
            if (barcodes.isEmpty()) {
                System.out.println("Error: no barcodes found");
                System.exit(1);
            }
            
            try(final Camera cam = new Camera(thingPropertiesUri)) {
                cam.run(runningTime, barcodes);
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
