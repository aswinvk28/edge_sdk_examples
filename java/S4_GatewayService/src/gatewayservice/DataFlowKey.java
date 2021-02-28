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

import com.adlinktech.datariver.*;

public class DataFlowKey {
    private final String tagGroupName;
    private final String tagGroupQos;
    private final String sourceThingClassId;
    private final String sourceThingId;
    private final String flowId;
    
    public DataFlowKey(final IotNvpDataSample dataSample) {
        this.tagGroupName = dataSample.getTagGroup().getName();
        this.tagGroupQos = dataSample.getTagGroup().getQosProfile();
        this.sourceThingClassId = dataSample.getSourceClass();
        this.sourceThingId = dataSample.getSourceId();
        this.flowId = dataSample.getFlowId();      
    }   

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((flowId == null) ? 0 : flowId.hashCode());
        result = prime * result + ((sourceThingClassId == null) ? 0 : sourceThingClassId.hashCode());
        result = prime * result + ((sourceThingId == null) ? 0 : sourceThingId.hashCode());
        result = prime * result + ((tagGroupName == null) ? 0 : tagGroupName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataFlowKey other = (DataFlowKey) obj;
        if (flowId == null) {
            if (other.flowId != null)
                return false;
        } else if (!flowId.equals(other.flowId))
            return false;
        if (sourceThingClassId == null) {
            if (other.sourceThingClassId != null)
                return false;
        } else if (!sourceThingClassId.equals(other.sourceThingClassId))
            return false;
        if (sourceThingId == null) {
            if (other.sourceThingId != null)
                return false;
        } else if (!sourceThingId.equals(other.sourceThingId))
            return false;
        if (tagGroupName == null) {
            if (other.tagGroupName != null)
                return false;
        } else if (!tagGroupName.equals(other.tagGroupName))
            return false;
        return true;
    }
    
    public String getSourceThingClassId() {
        return this.sourceThingClassId;
    }

    public String getSourceThingId() {
        return this.sourceThingId;
    }

    public String getSourceThingContext() {
        final String context = GatewayService.thingContext.get(sourceThingId);
        if (context.isEmpty()) {
            return "<unknown>";
        }

        return context;
    }

    public String getTagGroupName() {
        return this.tagGroupName;
    }

    public String getTagGroupQos() {
        return this.tagGroupQos;
    }

    public String getFlowId() {
        return this.flowId;
    }
}
