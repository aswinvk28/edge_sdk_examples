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
 */﻿﻿﻿

﻿using System;
using System.Threading;
using System.Collections.Generic;
using CommandLine;
using Sensors;

namespace GeneratorB
{
    class Program
    {
        static void Main(string[] args)
        {
            Parser.Default.ParseArguments<Options>(args)
                .WithParsed<Options>(opts => Run(opts))
                .WithNotParsed<Options>(errs => HandleParseError(errs)); 
        }

        static int Run(Options options)
        {
            ParameterizedThreadStart pts2 = new ParameterizedThreadStart(TemperatureSensorTask);
            Thread t2 = new Thread(pts2);
            t2.Start(options); 

            ParameterizedThreadStart pts3 = new ParameterizedThreadStart(SpeedSensorTask);
            Thread t3 = new Thread(pts3);
            t3.Start(options);

            t2.Join();
            t3.Join();            

            return 0;
           
        }

        static void HandleParseError(IEnumerable<Error> errs)
        {
            Console.WriteLine("Failed to parse command line arguments");
        }

        static void TemperatureSensorTask(object o)
        {
            Options options = (Options) o;  
            using (TemperatureSensor fs = new TemperatureSensor(options.TempSensorURI))
            {
                fs.Run(options.RunningTime);
            } 
        }

        static void SpeedSensorTask(object o)
        {
            Options options = (Options) o;
            using (RotationalSpeedSensor fs = new RotationalSpeedSensor(options.SpeedSensorURI))
            {
                fs.Run(options.RunningTime);
            } 
        }

    }
}
