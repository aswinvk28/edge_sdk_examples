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

using System;
using System.Collections;
using System.Collections.Generic;
using Adlinktech.DataRiver;

namespace ThingBrowser
{
    public class TagGroupNotFoundException : Exception
    {
        public TagGroupNotFoundException(){}

        public TagGroupNotFoundException(string message)
            : base(message){}

        public TagGroupNotFoundException(string message, Exception inner)
            : base(message, inner){}
    }

    public class NewThingDiscoveredListener : ThingDiscoveredListener
    {
        private DataRiver MyDataRiver { get; }

        public NewThingDiscoveredListener(DataRiver dr)
        {
            MyDataRiver = dr;
        }
        
        public override void NotifyThingDiscovered(DiscoveredThing thing)
        {
            DisplayThing(thing);            
        }

        private void DisplayThing(DiscoveredThing thing)
        {
            string prefix = "    ";
            bool thingClassFound = false;
            int retryCount = 30;

            Console.ForegroundColor = ConsoleColor.Green;
            Console.Write(thing.ContextId);
            Console.ForegroundColor = ConsoleColor.DarkGreen;
            Console.WriteLine(" [Thing]");
            Console.ResetColor();

            Console.WriteLine(prefix + "   Thing ID:    " + thing.Id);
            Console.WriteLine(prefix + "   Context:     " + thing.ContextId);
            Console.WriteLine(prefix + "   Description: " + thing.Description);

            while (!thingClassFound && retryCount-- > 0) 
            {
                try 
                {
                    var thingClassName = thing.ClassId.Name + ":" +
                        thing.ClassId.Context + ":" +
                        thing.ClassId.VersionTag; 
                    var registry = MyDataRiver.DiscoveredThingClassRegistry;
                    var thingClass = registry.FindThingClass(thingClassName); 

                    DisplayThingClass(thingClass, prefix + "   ");
                    thingClassFound = true;                    
                }
                catch (Exception e) 
                {
                    // ThingClass not found
                    Console.WriteLine("DisplayThing Exception caught: {0}", e);
                }

                // Sleep 100ms before retry
                System.Threading.Thread.Sleep(100);

            }  

            if (!thingClassFound) 
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine(prefix + "   ThingClass not found");
                Console.ResetColor();
            } 
        }

        private void DisplayThingClass(ThingClass thingClass, string prefix = "")
        { 
            var thingClassName = thingClass.Id.Name + ":" +
                        thingClass.Context + ":" +
                        thingClass.VersionTag;
            Console.ForegroundColor = ConsoleColor.Magenta;
            Console.Write(prefix + thingClassName);
            Console.ForegroundColor = ConsoleColor.DarkMagenta;
            Console.WriteLine(" [ThingClass]");
            Console.ResetColor();

            Console.WriteLine(prefix + "   Description: "+ thingClass.Description);
            
            DisplayInputs(thingClass, prefix + "   ");
            DisplayOutputs(thingClass, prefix + "   ");
        }       

        private void DisplayInputs(ThingClass thingClass, string prefix = "")
        {
            var inputs = thingClass.InputTagGroups;
            
            Console.WriteLine(prefix + "inputs:");
            if (inputs.Count == 0 )
            {
                Console.WriteLine(prefix + "   <none>");
            }
            else
            {
                foreach (var input in inputs)
                {
                    string inputTagGroup = input.InputTagGroup;
                    if (IsDynamicTagGroup(inputTagGroup))
                    {
                        Console.ForegroundColor = ConsoleColor.Green;
                        Console.Write(prefix + input.Name);
                        Console.ResetColor(); 
                        Console.Write(": ");
                        Console.ForegroundColor = ConsoleColor.Magenta;
                        Console.Write("[expression]");
                        Console.ResetColor();
                        Console.WriteLine(" " + inputTagGroup);                        
                    }
                    else 
                    {
                        try 
                        {
                            TagGroup tagGroup = FindTagGroup(inputTagGroup);
                            Console.ForegroundColor = ConsoleColor.Green;
                            Console.Write(prefix + "   " + input.Name);
                            Console.ResetColor(); 
                            Console.Write(": ");
                            DisplayTagGroup(tagGroup, prefix + "      ");
                        }
                        catch(TagGroupNotFoundException e) 
                        {
                            Console.ForegroundColor = ConsoleColor.Red;
                            Console.WriteLine(prefix + "   TagGroup not found");
                            Console.ResetColor();
                        }
                    } 
                }
            }           

        } 

        private TagGroup FindTagGroup(string tagGroupName) 
        {
            int retryCount = 50;
            var registry = MyDataRiver.DiscoveredTagGroupRegistry;
            
            while (retryCount-- > 0) 
            {
                try {
                    return registry.FindTagGroup(tagGroupName);
                }
                catch(InvalidArgumentError e) {
                    // TagGroup not found
                }

                // Sleep 100ms before retry
                System.Threading.Thread.Sleep(100);
            }

            throw new TagGroupNotFoundException(tagGroupName); 
        }

        private void DisplayTagGroup(TagGroup tagGroup, string prefix = "") 
        {
            Console.ForegroundColor = ConsoleColor.Blue;
            Console.Write(tagGroup.Name + ":" + tagGroup.Context + ":" + tagGroup.VersionTag);
            Console.ForegroundColor = ConsoleColor.DarkBlue;             
            Console.WriteLine(" [TagGroup]");
            Console.ResetColor();           

            Console.WriteLine(prefix + "Description: " + tagGroup.Description);
            Console.WriteLine(prefix + "QosProfile: " + tagGroup.QosProfile);
            Console.WriteLine(prefix + "Tags: ");

            try 
            {
                 var type = tagGroup.TopLevelType;
            
                 foreach (var tag in type.Tags) 
                 {
                     DisplayTag(tag, prefix + "   ", 15);
                 }
            }
            catch (ThingAPIRuntimeError e) 
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine(prefix + $"   Error displaying TagGroup details: {e.Message}");  
                Console.ResetColor();
            }
        }

        private void DisplayTag(TagDefinition tag, string prefix = "", int width = 0) 
        {
            string strippedTagKind = tag.Kind.ToString().Replace("TYPE_","");

            Console.ForegroundColor = ConsoleColor.Yellow;
            Console.Write(prefix);
            Console.Write("{0,1}", tag.Name, -width);  
            Console.ResetColor(); 
            Console.Write(": " + tag.Description + " (kind: " + strippedTagKind);  
            Console.WriteLine(" | unit: " + tag.Unit + ")");           

        }

        private void DisplayOutputs(ThingClass thingClass, string prefix = "")
        {
            var outputs = thingClass.OutputTagGroups;

            Console.WriteLine(prefix + "outputs:");
            if (outputs.Count == 0 )
            {
                Console.WriteLine(prefix + "   <none>");
            }
            else
            {
                foreach (var output in outputs)
                {
                    try
                    {
                        TagGroup tagGroup = FindTagGroup(output.OutputTagGroup);
                        Console.ForegroundColor = ConsoleColor.Green;
                        Console.Write(prefix + "   " + output.Name);
                        Console.ResetColor(); 
                        Console.Write(": ");
                        DisplayTagGroup(tagGroup, prefix + "      ");
                    }
                    catch(TagGroupNotFoundException e)
                    {
                        Console.ForegroundColor = ConsoleColor.Red;
                        Console.WriteLine(prefix + "   TagGroup not found");
                        Console.ResetColor(); 
                    }
                } 
            }           
        }

        private bool IsDynamicTagGroup(string tagGroup) 
        {
            return tagGroup.Contains("*")
                || tagGroup.Contains("?") 
                || tagGroup.Contains(",");
        }


    }  

    public class ThingBrowser : IDisposable
    {
        static DateTime StartTime;

        public bool Stop { get; set; }
        private DataRiver DataRiver { get; }
        private Thing Thing { get; }
        private Dispatcher CustomDispatcher { get; set; }
        private NewThingDiscoveredListener NewThingListener { get; set; }      

        public ThingBrowser(string thingPropertiesURI)
        {
            DataRiver = DataRiver.GetInstance();
             
            // Add listener for discovery of Things
            CustomDispatcher = new Dispatcher();
            NewThingListener = new NewThingDiscoveredListener(DataRiver);
            DataRiver.AddListener(NewThingListener, CustomDispatcher);
            
            Thing = CreateThing(DataRiver, thingPropertiesURI);
            Stop = false;           
            
            Console.CancelKeyPress += this.CtrlCHandler;
            Console.WriteLine("ThingBrowser started");
        }

        public void Dispose()
        {
            // Remove the discovered Thing listener that was added during class initialization
            try
            {
                DataRiver.RemoveListener(NewThingListener, CustomDispatcher);  
            }
            catch(ThingAPIException e)
            {
                Console.WriteLine($"Unexpected error while removing discovered Thing listener: {e.Message}");  
            }           

            DataRiver.Close();
            DataRiver.Dispose();
            Console.WriteLine("ThingBrowser stopped");
        }

        private void CtrlCHandler(object sender, ConsoleCancelEventArgs args)
        {
            Console.WriteLine("Trapped Ctrl-C. Stopping.");
            Stop = true;
            args.Cancel = true; // don't terminate the process, we'll do that.
        }

        private static Thing CreateThing(in DataRiver dataRiver, string thingPropertiesURI)
        {   
            // Create and Populate the ThingClass registry with JSON resource files.
            var tcr = new JSonThingClassRegistry();
            tcr.RegisterThingClassesFromUri("file://./definitions/ThingClass/com.adlinktech.example/ThingBrowserThingClass.json");
            dataRiver.AddThingClassRegistry(tcr);

            // Create a Thing based on properties specified in a JSON resource file.
            var tp = new JSonThingProperties();
            tp.ReadPropertiesFromUri(thingPropertiesURI);             

            return dataRiver.CreateThing(tp);
        }

        public int Run(ulong runningTime)
        {
            StartTime = DateTime.Now;

            do
            {
                try
                {
                    // block the call for 1000ms
                    CustomDispatcher.ProcessEvents(1000);                    
                }
                catch //(TimeoutError e)
                {
                    // Ignore.
                }

            } while (!Stop && (SecondsExecuting(StartTime) < runningTime));

            return 0;
        }

        private double SecondsExecuting(DateTime originalTime)
        {
            return (DateTime.Now - originalTime).TotalSeconds;
        }                    

    }
}
