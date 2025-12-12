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

package org.qubership.atp.mia.model;

import static org.qubership.atp.mia.model.exception.ErrorCodes.MIA_1410_REST_CONTENT_TYPE_NOT_SUPPORT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.mia.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ContentType {
    js(Collections.singletonList("application/javascript"), true, true),
    ogg(Collections.singletonList("application/ogg"), false, false),
    pdf(Collections.singletonList("application/pdf"), false, false),
    json(Collections.singletonList("application/json"), true, true),
    zip(Collections.singletonList("application/zip"), false, false),
    xml(Arrays.asList("application/xml", "text/xml"), true, true),
    gif(Collections.singletonList("image/gif"), false, false),
    jpeg(Collections.singletonList("image/jpeg"), false, false),
    png(Collections.singletonList("image/png"), false, false),
    tiff(Collections.singletonList("image/tiff"), false, false),
    svg(Collections.singletonList("image/svg+xml"), false, false),
    doc(Collections.singletonList("application/msword"), false, false),
    xls(Collections.singletonList("application/vnd.ms-excel"), false, false),
    txt(Collections.singletonList("text/plain"), true, true),
    undefined(Collections.singletonList("undefined"), false, false);

    private final List<String> mimeType;
    private final boolean display;
    private final boolean textFormat;

    ContentType(List<String> mimeType, boolean display, boolean textFormat) {
        this.mimeType = mimeType;
        this.display = display;
        this.textFormat = textFormat;
    }

    /**
     * Get content type.
     *
     * @param value type as string
     * @return ContentType
     */
    public static ContentType getType(String value) {
        for (ContentType type : ContentType.values()) {
            for (String mimeType : type.mimeType) {
                if (mimeType.equals(value) || !Strings.isEmpty(value)
                        && (mimeType.matches(".*?\\b" + value + "\\b.*?")
                        || value.matches(".*?\\b" + mimeType + "\\b.*?"))) {
                    return type;
                }
            }
        }
        Utils.error(log, MIA_1410_REST_CONTENT_TYPE_NOT_SUPPORT, value);
        return ContentType.undefined;
    }

    /**
     * get extension based on content type.
     */
    public String getExtension() {
        return "." + name();
    }

    /**
     * To display file with current content type.
     *
     * @return should be displayed or not
     */
    public boolean isDisplay() {
        return display;
    }

    /**
     * Has text format file with current content type.
     *
     * @return has text format or not
     */
    public boolean isTextFormat() {
        return textFormat;
    }
}
