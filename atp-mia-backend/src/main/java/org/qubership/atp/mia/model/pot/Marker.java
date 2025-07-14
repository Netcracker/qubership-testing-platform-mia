/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.atp.mia.model.pot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.mia.exceptions.responseerrors.MarkerRegexException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Marker implements Serializable {

    private static final long serialVersionUID = -8129637806434896234L;
    @Nullable
    private List<String> passedMarkerForLog;
    @Nullable
    private List<String> failedMarkersForLog;
    @Nullable
    private List<String> warnMarkersForLog;
    private boolean failWhenNoPassedMarkersFound;

    /**
     * Clones Marker.
     *
     * @return copy of Marker class.
     */
    public Marker clone() {
        MarkerBuilder builder = this.toBuilder();
        if (passedMarkerForLog != null) {
            builder.passedMarkerForLog(new ArrayList<>(passedMarkerForLog));
        }
        if (failedMarkersForLog != null) {
            builder.failedMarkersForLog(new ArrayList<>(failedMarkersForLog));
        }
        if (warnMarkersForLog != null) {
            builder.warnMarkersForLog(new ArrayList<>(warnMarkersForLog));
        }
        return builder.build();
    }

    /**
     * Check line for markers.
     * If any error then FAIL,
     * otherwise if any warning then WARNING
     * otherwise if any passed then PASSED
     * otherwise UNKNOWN.
     *
     * @param line line to check
     * @return {@link Statuses}
     */
    public Statuses checkLineForMarker(String line) {
        Statuses status = Statuses.UNKNOWN;
        if (failedMarkersForLog != null
                && failedMarkersForLog.stream().anyMatch(marker -> checkLineWithMarker(marker, line, "failed"))) {
            status = Statuses.FAIL;
        }
        if (!status.equals(Statuses.FAIL) && warnMarkersForLog != null
                && warnMarkersForLog.stream().anyMatch(marker -> checkLineWithMarker(marker, line, "warn"))) {
            status = Statuses.WARNING;
        }
        if (!status.equals(Statuses.FAIL) && !status.equals(Statuses.WARNING) && passedMarkerForLog != null
                && passedMarkerForLog.stream().anyMatch(marker -> checkLineWithMarker(marker, line, "passed"))) {
            status = Statuses.SUCCESS;
        }
        return status;
    }

    private boolean checkLineWithMarker(String marker, String line, String markerType) {
        if (!Strings.isBlank(marker)) {
            try {
                return Pattern.compile(marker).matcher(line).find();
            } catch (PatternSyntaxException e) {
                throw new MarkerRegexException(markerType, marker);
            }
        }
        return  false;
    }
}
