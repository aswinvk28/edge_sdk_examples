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

package thingbrowser;

import com.adlinktech.datariver.DataRiver;
import com.adlinktech.datariver.DiscoveredTagGroupRegistry;
import com.adlinktech.datariver.DiscoveredThing;
import com.adlinktech.datariver.DiscoveredThingClassRegistry;
import com.adlinktech.datariver.InputDefinition;
import com.adlinktech.datariver.InputDefinitionSeq;
import com.adlinktech.datariver.InvalidArgumentError;
import com.adlinktech.datariver.OutputDefinition;
import com.adlinktech.datariver.OutputDefinitionSeq;
import com.adlinktech.datariver.TagDefinition;
import com.adlinktech.datariver.TagGroup;
import com.adlinktech.datariver.ThingAPIRuntimeError;
import com.adlinktech.datariver.ThingClass;
import com.adlinktech.datariver.ThingDiscoveredListener;
import com.adlinktech.datariver.TypeDefinition;

//Use "Ansi" from "jansi" library to print ANSI escape sequences both on Windows and Linux
import org.fusesource.jansi.Ansi;

public class NewThingDiscoveredListener extends ThingDiscoveredListener {
    private final DataRiver dataRiver;
    private final DiscoveredTagGroupRegistry discoveredTagGroupRegistry;
    private final DiscoveredThingClassRegistry discoveredThingClassRegistry;
    
    public NewThingDiscoveredListener(final DataRiver dr) {
        this.dataRiver = dr;
        discoveredTagGroupRegistry = createDiscoveredTagGroupRegistry();
        discoveredThingClassRegistry = createDiscoveredThingClassRegistry();
    }
    
    private DiscoveredTagGroupRegistry createDiscoveredTagGroupRegistry() {
        return this.dataRiver.getDiscoveredTagGroupRegistry();
    }

    private DiscoveredThingClassRegistry createDiscoveredThingClassRegistry() {
        return this.dataRiver.getDiscoveredThingClassRegistry();
    }
    
    private String getTagType(final TagDefinition tag) {
        final String type = tag.getKind().name();
        final String prefix = "TYPE_";
        if(type.startsWith(prefix)) {
            return type.substring(prefix.length());
        }
        return type;
    }

    private void displayTag(final TagDefinition tag, final String prefix, final int width) {
    	ThingBrowser.safePrintln(prefix + Ansi.ansi().fgYellow()
                + String.format("%-" + width + "s", tag.getName()) + Ansi.ansi().fgDefault() + ": "
                + tag.getDescription()
                + " (kind: " + getTagType(tag)
                + " | unit: " + tag.getUnit() + ")");
    }

    private void displayTagGroup(final TagGroup tagGroup, final String prefix) {
    	ThingBrowser.safePrintln(
                Ansi.ansi().fgBrightBlue() + tagGroup.getName() + ":"
                + tagGroup.getContext() + ":" + tagGroup.getVersionTag() + Ansi.ansi().fgDefault()
                + Ansi.ansi().fgBlue() + " [TagGroup]" + Ansi.ansi().fgDefault());
        ThingBrowser.safePrintln(prefix + "Description: " + tagGroup.getDescription());
        ThingBrowser.safePrintln(prefix + "QosProfile: " + tagGroup.getQosProfile());
        ThingBrowser.safePrintln(prefix + "Tags: ");
        try {
            final TypeDefinition type = tagGroup.getToplevelType();
            for(final TagDefinition tag: type.getTags()){
                displayTag(tag, prefix + "   ", 15);
            }
        } catch (ThingAPIRuntimeError e) {
        	ThingBrowser.safePrintln(prefix + Ansi.ansi().fgRed()
                    + "   Error displaying TagGroup details: " + e.getMessage()
                    + Ansi.ansi().fgDefault());
        }
    }

    private boolean isDynamicTagGroup(final String tagGroup) {
        return tagGroup.contains("*") || tagGroup.contains("?") || tagGroup.contains(",");
    }

    private TagGroup findTagGroup(final String tagGroupName) throws TagGroupNotFoundException {
        int retryCount = 50;

        while (retryCount-- > 0) {
            try {
                return this.discoveredTagGroupRegistry.findTagGroup(tagGroupName);
            }
            catch(InvalidArgumentError e) {
                // TagGroup not found
            }

            // Sleep 100ms before retry
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                ThingBrowser.safePrintln("NewThingDiscoveredListener.findTagGroup is interrupted");
                break;
            }
        }

        throw new TagGroupNotFoundException();
    }

    private void displayInputs(final InputDefinitionSeq inputs, final String prefix) {
        ThingBrowser.safePrintln(prefix + "inputs:");
        if (inputs.isEmpty()) {
            ThingBrowser.safePrintln(prefix + "   <none>");
        } else {
            for (final InputDefinition input : inputs) {
                final String inputTagGroup = input.getInputTagGroup();
                if (isDynamicTagGroup(inputTagGroup)) {
                	ThingBrowser.safePrintln(prefix + "   " + Ansi.ansi().fgGreen() + input.getName()
                            + Ansi.ansi().fgDefault() + ": "
                            + Ansi.ansi().fgMagenta() + "[expression]"
                            + Ansi.ansi().fgDefault() + " " + inputTagGroup);
                } else {
                    try {
                        final TagGroup tagGroup = findTagGroup(inputTagGroup);
                        ThingBrowser.safePrint(prefix + "   " + Ansi.ansi().fgGreen() + input.getName()
                                + Ansi.ansi().fgDefault() + ": ");
                        displayTagGroup(tagGroup, prefix + "      ");
                    } catch(TagGroupNotFoundException e) {
                    	ThingBrowser.safePrintln(prefix + Ansi.ansi().fgRed() + "   TagGroup not found"
                                + Ansi.ansi().fgDefault());
                    }
                }
            }
        }
    }

    private void displayOutputs(final OutputDefinitionSeq outputs, final String prefix) {
        ThingBrowser.safePrintln(prefix + "outputs:");
        if (outputs.isEmpty()) {
            ThingBrowser.safePrintln(prefix + "   <none>");
        } else {
            for (final OutputDefinition output : outputs) {
                try {
                    final TagGroup tagGroup = findTagGroup(output.getOutputTagGroup());
                    ThingBrowser.safePrint(prefix + "   " + Ansi.ansi().fgGreen()
                            + output.getName() + Ansi.ansi().fgDefault() + ": ");
                    displayTagGroup(tagGroup, prefix + "      ");
                } catch(TagGroupNotFoundException e) {
                	ThingBrowser.safePrintln(prefix + Ansi.ansi().fgRed()
                            + "   TagGroup not found" + Ansi.ansi().fgDefault());
                }
            }
        }
    }

    private void displayThingClass(final ThingClass thingClass, final String prefix) {
        final InputDefinitionSeq inputs = thingClass.getInputTagGroups();
        final OutputDefinitionSeq outputs = thingClass.getOutputTagGroups();

        ThingBrowser.safePrintln(prefix + Ansi.ansi().fgBrightMagenta()
                + thingClass.getId().getName() + ":" + thingClass.getContext() + ":"
                + thingClass.getVersionTag() + Ansi.ansi().fgDefault()
                + Ansi.ansi().fgMagenta() + " [ThingClass]" + Ansi.ansi().fgDefault());
        ThingBrowser.safePrintln(prefix + "   Description: " + thingClass.getDescription());
        displayInputs(inputs, prefix + "   ");
        displayOutputs(outputs, prefix + "   ");
    }

    private void displayThing(final DiscoveredThing thing, final String prefix) {
        boolean thingClassFound = false;
        int retryCount = 30;

        ThingBrowser.safePrintln("\n" + Ansi.ansi().fgBrightGreen()
                + thing.getContextId() + Ansi.ansi().fgDefault()
                + Ansi.ansi().fgGreen() + " [Thing]" + Ansi.ansi().fgDefault());
        ThingBrowser.safePrintln(prefix + "   Thing ID:    " + thing.getId());
        ThingBrowser.safePrintln(prefix + "   Context:     " + thing.getContextId());
        ThingBrowser.safePrintln(prefix + "   Description: " + thing.getDescription());

        while (!thingClassFound && retryCount-- > 0) {
            try {
                final ThingClass thingClass = this.discoveredThingClassRegistry.findThingClass(
                    thing.getClassId().getName() +
                    ":" + thing.getClassId().getContext() +
                    ":" + thing.getClassId().getVersionTag());

                displayThingClass(thingClass, prefix + "   ");
                thingClassFound = true;
            } catch(InvalidArgumentError e) {
                // ThingClass not found
            }

            // Sleep 100ms before retry
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                ThingBrowser.safePrintln("NewThingDiscoveredListener.displayThing is interrupted");
                break;
            }
        }

        if (!thingClassFound) {
        	ThingBrowser.safePrintln(prefix + Ansi.ansi().fgRed()
                + "   ThingClass not found" + Ansi.ansi().fgDefault());
        }
    }

    public void notifyThingDiscovered(final DiscoveredThing thing) {
        displayThing(thing, "   ");
    }
}
