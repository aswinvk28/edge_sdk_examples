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

package generators;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.adlinktech.datariver.ThingAPIException;

import sensors.FuelLevelSensor;
import sensors.RotationalSpeedSensor;
import sensors.TemperatureSensor;

public class GeneratorA {
    
    private static void fuelLevelSensorTask(final String thingPropertiesUri, final int runningTime) {
        try(final FuelLevelSensor flSensor = new FuelLevelSensor(thingPropertiesUri)) {
            flSensor.run(runningTime);
        } catch (ThingAPIException e) { 
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }
    
    private static void temperatureSensorTask(final String thingPropertiesUri, final int runningTime) {
        try(final TemperatureSensor tempSensor = new TemperatureSensor(thingPropertiesUri)) {
            tempSensor.run(runningTime);
        } catch (ThingAPIException e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }
    
    private static void speedSensorTask(final String thingPropertiesUri, final int runningTime) {
        try(final RotationalSpeedSensor rsSensor = new RotationalSpeedSensor(thingPropertiesUri)) {
            rsSensor.run(runningTime);
        } catch (ThingAPIException e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();
        
        try {
            // Get thing properties URI from command line parameter    
            final Option fuelSensorOption = new Option("f", "fuel-sensor", true, "Fuel Level Sensor Thing properties URI");
            fuelSensorOption.setRequired(true);
            final Option speedSensorOption = new Option("s", "speed-sensor", true, "Rotational Speed Sensor Thing properties URI");
            speedSensorOption.setRequired(true);
            final Option tempSensorOption = new Option("t", "temp-sensor", true, "Temperature Sensor Thing properties URI");
            tempSensorOption.setRequired(true);
            final Option runningTimeOption = new Option("r", "running-time", true, "Total running time of the program (in seconds)");
            runningTimeOption.setRequired(true);
    
            options.addOption(fuelSensorOption);
            options.addOption(speedSensorOption);
            options.addOption(tempSensorOption);
            options.addOption(runningTimeOption);
            
            final CommandLine cmd = parser.parse(options, args);
            
            final int runningTime = Integer.parseInt(cmd.getOptionValue("r"));
            final String fuelLevelSensorThingPropertiesUri = cmd.getOptionValue("f");
            final String speedSensorThingPropertiesUri = cmd.getOptionValue("s");
            final String temperatureSensorThingPropertiesUri = cmd.getOptionValue("t");
            
            // Create threads for Sensors
            Thread flSensorThread = new Thread(() -> fuelLevelSensorTask(fuelLevelSensorThingPropertiesUri, runningTime));
            flSensorThread.start();
            Thread tempSensorThread = new Thread(() -> temperatureSensorTask(temperatureSensorThingPropertiesUri, runningTime));
            tempSensorThread.start();
            Thread speedSensorThread = new Thread(() -> speedSensorTask(speedSensorThingPropertiesUri, runningTime));
            speedSensorThread.start();
            
            flSensorThread.join();
            tempSensorThread.join();
            speedSensorThread.join();
            
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        } catch(NumberFormatException ex) {
            System.out.println(ex.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        } catch (InterruptedException ex) {
            System.out.println("An unexpected error occurred: " + ex.getMessage());
            System.exit(1);
        }
    }
}
