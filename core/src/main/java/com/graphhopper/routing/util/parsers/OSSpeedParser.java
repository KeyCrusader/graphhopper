/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.OSSpeedPeriod;
import com.graphhopper.storage.IntsRef;

import static com.graphhopper.routing.ev.MaxSpeed.UNSET_SPEED;


public class OSSpeedParser implements TagParser {
    private final DecimalEncodedValue speedEnc;
    private final OSSpeedPeriod period;

    public OSSpeedParser(DecimalEncodedValue speedEnc, OSSpeedPeriod period) {
        if (!speedEnc.isStoreTwoDirections())
            throw new IllegalArgumentException("EncodedValue for osSpeed must be able to store two directions");

        this.speedEnc = speedEnc;
        this.period = period;
    }

    /*
     * Takes the edge and deserialises the speed from it based on the period.
     */
    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        for (OSSpeedDirection direction : OSSpeedDirection.values()) {
            double speed = UNSET_SPEED;

            // TODO Optimise this absolute mess of string manipulations
            String tagString = way.getTag(direction.tag);
            if (tagString != null && !tagString.isEmpty()) {
                final int periodIndex = tagString.indexOf(this.period.tag);
                if (periodIndex > -1) {
                    final int endIndex = tagString.indexOf(";",periodIndex);
                    tagString = tagString.substring(0, endIndex);
                    final int startIndex = tagString.lastIndexOf(";");
                    tagString = tagString.substring(startIndex+1);
                    tagString = tagString.split("@")[0].trim();
                    speed = Double.parseDouble(tagString);
                }
            }
            speedEnc.setDecimal(direction.reverse, edgeId, edgeIntAccess, isValidSpeed(speed) ? speed : UNSET_SPEED);
        }
    }

    /*
     * This is copied from the OSMMaxSpeedParser
     */
    private boolean isValidSpeed(double speed) {
        return !Double.isNaN(speed);
    }
}

/*
 * The fields to read from and if they are forwards or backwards.
 * Done with an enum so that it can be looped easily.
 */

enum OSSpeedDirection {
    FORWARD("avgspeed:forward:conditional", false),
    BACKWARD("avgspeed:backward:conditional", true);

    final String tag;
    final boolean reverse;

    OSSpeedDirection(String tag, boolean reverse) {
        this.tag = tag;
        this.reverse = reverse;
    }
}
