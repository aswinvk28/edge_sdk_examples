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

const {
    performance,
    PerformanceObserver
} = require('perf_hooks');
  
const util = require('util');
const sleep = util.promisify(setTimeout);

const READ_SAMPLE_DELAY = 100;

function currentTimeMilliseconds() {
    // performance.now() is in milliseconds.
    return performance.now();
}

function getRandomInt(max) {
    return Math.floor(Math.random() * Math.floor(max));
}
  
class TemperatureDisplay {
    
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
        
        console.log('Temperature Display started');
    }

    // Exit the runtime context related to the object
    detachFromDataRiver() {
        if (this._datariver !== null) {
            this._datariver.close();
        }
        console.log('Temperature Display stopped');
    }

    createThing() {
        // Create and Populate the TagGroup registry with JSON resource files.
        const tgr = new datariver.JSonTagGroupRegistry();
        tgr.registerTagGroupsFromUri('file://definitions/TagGroup/com.adlinktech.example/TemperatureTagGroup.json');
        this._datariver.addTagGroupRegistry(tgr) ;

        // Create and Populate the ThingClass registry with JSON resource files.
        const tcr = new datariver.JSonThingClassRegistry();
        tcr.registerThingClassesFromUri('file://definitions/ThingClass/com.adlinktech.example/TemperatureDisplayThingClass.json');
        this._datariver.addThingClassRegistry(tcr);

        // Create a Thing based on properties specified in a JSON resource file.
        const tp = new datariver.JSonThingProperties();
        tp.readPropertiesFromUri(this._thingPropertiesUri);
        
        const thing = this._datariver.createThing(tp);
        return thing;
    }
    
    async run(runningTime) {
        try {
            const start = currentTimeMilliseconds();
            let elapsed = 0;

            do {
                const msgs = await datariver.asyncThingReadIotNvp(this._thing, 'temperature', (runningTime * 1000) - elapsed);
                for (let i = 0; i < msgs.size(); i++) {
                    const msg = msgs.get(i);
                    if(msg.flowState === datariver.FLOW_STATE_ALIVE) {
                        const tempdata = tg.Temperature.from_nvp_seq(msg.data);
                        console.log('Sensor data received: %s', tempdata.temperature.toFixed(1).padStart(5, ' '));
                    }
                }

                await sleep(READ_SAMPLE_DELAY);

                elapsed = currentTimeMilliseconds() - start;
            } while (elapsed < (runningTime * 1000));
        } catch(error) {
            console.log('TemperatureDisplay run error: %O', error);
        }
    }
}
    
function getCommandLineParameters() {
    const argv = require('yargs')
        .usage('Usage: $0 [options]')
        .alias('t','thing-properties-uri')
        .nargs('t',1)
        .default('t', 'file://./config/TemperatureDisplayProperties.json')
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

    const tempDisplay = new TemperatureDisplay(thingPropertiesUri);
    try {
        tempDisplay.attachToDataRiver();
        await tempDisplay.run(runningTime);
    } catch(error) {
        console.log('TemperatureDisplay error: %O', error);
    } finally {
        tempDisplay.detachFromDataRiver();
    }
}

main().then(() => console.log('TemperatureDisplay done.')).catch(error => console.log('TemperatureDisplay error: %O', error));