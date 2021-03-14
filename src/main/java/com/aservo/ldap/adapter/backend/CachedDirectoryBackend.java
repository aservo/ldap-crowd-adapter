/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aservo.ldap.adapter.backend;

import com.aservo.ldap.adapter.util.ServerConfiguration;
import java.time.Duration;
import java.util.Properties;


public abstract class CachedDirectoryBackend
        implements DirectoryBackend {

    /**
     * The constant CONFIG_ENTRY_CACHE_ENABLED.
     */
    public static final String CONFIG_ENTRY_CACHE_ENABLED = "entry-cache.enabled";
    /**
     * The constant CONFIG_ENTRY_CACHE_MAX_SIZE.
     */
    public static final String CONFIG_ENTRY_CACHE_MAX_SIZE = "entry-cache.max-size";
    /**
     * The constant CONFIG_ENTRY_CACHE_MAX_AGE.
     */
    public static final String CONFIG_ENTRY_CACHE_MAX_AGE = "entry-cache.max-age";

    protected final ServerConfiguration config;
    protected final DirectoryBackend directoryBackend;
    protected final boolean entryCacheEnabled;
    protected final int entryCacheMaxSize;
    protected final Duration entryCacheMaxAge;

    protected CachedDirectoryBackend(ServerConfiguration config, DirectoryBackend directoryBackend) {

        Properties properties = config.getBackendProperties();

        this.config = config;
        this.directoryBackend = directoryBackend;

        entryCacheEnabled = Boolean.parseBoolean(properties.getProperty(CONFIG_ENTRY_CACHE_ENABLED, "false"));
        entryCacheMaxSize = Integer.parseInt(properties.getProperty(CONFIG_ENTRY_CACHE_MAX_SIZE, "300"));
        entryCacheMaxAge = Duration.parse(properties.getProperty(CONFIG_ENTRY_CACHE_MAX_AGE, "PT1H"));

        if (entryCacheMaxSize <= 0)
            throw new IllegalArgumentException("Expect value greater than zero for " + CONFIG_ENTRY_CACHE_MAX_SIZE);

        if (entryCacheMaxAge.isNegative() || entryCacheMaxAge.isZero())
            throw new IllegalArgumentException("Expect value greater than zero for " + CONFIG_ENTRY_CACHE_MAX_AGE);
    }

    @Override
    public String getId() {

        return directoryBackend.getId();
    }

    @Override
    public void startup() {

        directoryBackend.startup();
    }

    @Override
    public void shutdown() {

        directoryBackend.shutdown();
    }
}
