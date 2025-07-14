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
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.mia.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ContentType {
    js(Arrays.asList("application/javascript"), true, true),
    ogg(Arrays.asList("application/ogg"), false, false),
    pdf(Arrays.asList("application/pdf"), false, false),
    json(Arrays.asList("application/json"), true, true),
    zip(Arrays.asList("application/zip"), false, false),
    xml(Arrays.asList("application/xml", "text/xml"), true, true),
    gif(Arrays.asList("image/gif"), false, false),
    jpeg(Arrays.asList("image/jpeg"), false, false),
    png(Arrays.asList("image/png"), false, false),
    tiff(Arrays.asList("image/tiff"), false, false),
    svg(Arrays.asList("image/svg+xml"), false, false),
    doc(Arrays.asList("application/msword"), false, false),
    xls(Arrays.asList("application/vnd.ms-excel"), false, false),
    txt(Arrays.asList("text/plain"), true, true),
    undefined(Arrays.asList("undefined"), false, false);

    private List<String> mimeType;
    private boolean display;
    private boolean textFormat;

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
