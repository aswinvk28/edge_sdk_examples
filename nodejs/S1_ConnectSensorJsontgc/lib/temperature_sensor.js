/*
 *                         ADLINK Edge SDK
 *
 *   This software and documentation are Copyright 2020 to 2020 ADLINK
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
 * For instructions on running the example see the README
 * file in the Edge SDK installation directory.
 */

'use strict';

const datariver = require('@adlinktech/datariver');
const tg = require('./TemperatureTagGroup');
  
const util = require('util');
const sleep = util.promisify(setTimeout);

const SAMPLE_DELAY_MS = 100;

function getRandomInt(max) {
    return Math.floor(Math.random() * Math.floor(max));
}

class TemperatureSensor {
    
    // Initializing
    constructor(thingPropertiesUri) {
        this._thingPropertiesUri = thingPropertiesUri;
        
        this._datariver = null;
        this._thing = null;
    }

    // Enter the runtime context related to the object
    attachToDataRiver() {
        this._datariver = datariver.DataRiver.getInstance();
        this._thing = this.createThing();
        
        console.log('Temperature Sensor started');
    }

    // Exit the runtime context related to the object
    detachFromDataRiver() {
        if (this._datariver !== null) {
            this._datariver.close();
        }
        console.log('Temperature Sensor stopped');
    }

    createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        const tgr = new datariver.JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri('file://definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json');
        this._datariver.addTagGroupRegistry(tgr) ;

        // Create and Populate the ThingClass registry with JSON resource files.
        const tcr = new datariver.JSonThingClassRegistry();
        tcr.registerThingClassesFromUri('file://definitions/ThingClass/com.adlinktech.example/TemperatureSensorThingClass.json');
        this._datariver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        const tp = new datariver.JSonThingProperties();
        tp.readPropertiesFromUri(this._thingPropertiesUri);
        
        const thing = this._datariver.createThing(tp);
        thing.getOutputHandler('temperature').validateSamples = true;
        return thing;
    }

    async writeSample(temperature) {
        try {
            const sensorData = new tg.Temperature();

            sensorData.temperature = temperature;

            // Write the data
            this._thing.write('temperature', tg.Temperature.to_nvp_seq(sensorData));

        } catch(error) {
            console.log('Error writing data: %O', error);
        }
    }
    
    async run(runningTime) {
        try {
            let sampleCount = (runningTime * 1000.0) / SAMPLE_DELAY_MS;
            let actualTemp = 21.5;

            while(sampleCount-- > 0) {
                actualTemp += (getRandomInt(10) - 5)/ 5.0;
                await this.writeSample(actualTemp);

                // Give middleware some time to finish writing samples
                await sleep(SAMPLE_DELAY_MS);
            }

        } catch(error) {
            console.log('TemperatureSensor run error: %O', error);
        }
    }
}
    
function getCommandLineParameters() {
    const argv = require('yargs')
        .usage('Usage: $0 [options]')
        .alias('t','thing-properties-uri')
        .nargs('t',1)
        .default('t', 'file://./config/TemperatureSensorProperties.json')
        .describe('t', 'Thing properties URI')

        .alias('r', 'running-time')
        .nargs('r', 1)
        .default('r', 60)
        .describe('r', 'Running time (seconds, 0 = infinite)')

        .help('h')
        .alias('h', 'help')
        .argv;

    return {
        thingPropertiesUri: argv['thing-properties-uri'],
        runningTime: argv['running-time']
    };
}

async function main() {
    // Get command line parameters
    const { thingPropertiesUri, runningTime } = getCommandLineParameters();

    const tempSensor = new TemperatureSensor(thingPropertiesUri);
    try {
        tempSensor.attachToDataRiver();
        await tempSensor.run(runningTime);
    } catch(error) {
        console.log('TemperatureSensor error: %O', error);
    } finally {
        tempSensor.detachFromDataRiver();
    }
}

main().then(() => console.log('TemperatureSensor done.')).catch(error => console.log('TemperatureSensor error: %O', error));