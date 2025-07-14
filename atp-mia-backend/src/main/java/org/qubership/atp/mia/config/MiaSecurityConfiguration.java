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

package org.qubership.atp.mia.config;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.qubership.atp.auth.springbootstarter.config.SecurityConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

@Order(1)
@KeycloakConfiguration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("default")
public class MiaSecurityConfiguration extends SecurityConfiguration {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http
                .headers()
                .frameOptions()
                .sameOrigin()
                .and()
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/rest/deployment/liveness", "/rest/deployment/readiness",
                        "/version", "/v3/api-docs/**", "/swagger-ui/**",
                        "/webjars/**", "/metrics")
                .permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/**")
                .authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}

