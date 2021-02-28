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
 * This code is part of simple roundtrip application
 */

package ping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExampleTimeStats {

    private List<Long> values;
    private double average;
    private long min;
    private long max;
    
    public ExampleTimeStats() {
        this.setValues(Collections.synchronizedList(new ArrayList<Long>()));
        this.setAverage(0);
        this.setMin(0);
        this.setMax(0);
    }

    public void add(final long microseconds) {
        this.setAverage(((this.getValues().size() * this.getAverage()) + microseconds) / (this.getValues().size() + 1));
        this.getValues().add(microseconds);
        if (this.getMin() == 0 || microseconds < this.getMin()) {
            this.setMin(microseconds);
        }
        if (microseconds > this.getMax()) {
            this.setMax(microseconds);
        }
    }
    
    public void exampleResetTimeStats() {
        this.setValues(Collections.synchronizedList(new ArrayList<Long>()));
        this.setAverage(0);
        this.setMin(0);
        this.setMax(0);
    }
    
    public double exampleGetMedianFromTimeStats() {
        if(this.getValues().size() == 0) {
            return 0;
        }
        
        Collections.sort(this.getValues());

        if (this.getValues().size() % 2 == 0) {
            return (double)(this.getValues().get((int)((this.getValues().size() / 2) - 1)) + this.getValues().get((int)(this.getValues().size() / 2))) / 2;
        }

        return (double) this.getValues().get((int)(this.getValues().size() / 2));
    }


    private List<Long> getValues() {
        return values;
    }


    private void setValues(final List<Long> values) {
        this.values = values;
    }


    private double getAverage() {
        return average;
    }


    private void setAverage(final double average) {
        this.average = average;
    }


    public long getMin() {
        return min;
    }


    private void setMin(final long min) {
        this.min = min;
    }


    private long getMax() {
        return max;
    }


    private void setMax(final long max) {
        this.max = max;
    }


    public long getCount() {
        return this.getValues().size();
    }
    
}