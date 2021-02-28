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

import com.adlinktech.datariver.DiscoveredThing;
import com.adlinktech.datariver.ThingLostListener;

public class CameraThingLostListener extends ThingLostListener {
    private final Camera camera;
    
    public CameraThingLostListener(final Camera cam) {
        this.camera = cam;
    }
    
    public void notifyThingLost(final DiscoveredThing thing) {
        if (this.camera.isRelated(thing)) {
            this.camera.lostRelatedCamera(thing.getId());
        }
    }
}
