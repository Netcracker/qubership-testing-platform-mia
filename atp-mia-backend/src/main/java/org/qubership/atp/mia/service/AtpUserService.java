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

package org.qubership.atp.mia.service;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtpUserService {

    private final Provider<UserInfo> userInfoProvider;

    /**
     * Get user full name.
     *
     * @return user full name
     */
    public String getAtpUser() {
        return userInfoProvider != null && userInfoProvider.get() != null
                ? userInfoProvider.get().getFullName()
                : "UserNotDefined";
    }

    /**
     * Gets user info by id.
     *
     * @param token user token
     * @return user info
     */
    public UUID getUserIdFromToken(String token) {
        UUID userId = null;
        if (StringUtils.isNotBlank(token)) {
            try {
                token = token.split(" ")[1];
                JsonParser parser = JsonParserFactory.getJsonParser();
                Map<String, ?> tokenData = parser.parseMap(JwtHelper.decode(token).getClaims());
                userId = UUID.fromString(tokenData.get("sub").toString());
            } catch (Exception e) {
                log.warn("Cannot parse token with error: ", e);
            }
        }
        return userId;
    }
}
