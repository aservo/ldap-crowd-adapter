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

package com.aservo.ldap.adapter.util;

import com.aservo.ldap.adapter.backend.DirectoryBackend;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import org.apache.commons.io.FileUtils;


/**
 * The type Server configuration.
 */
public class ServerConfiguration {

    /**
     * The constant CONFIG_CACHE_DIR.
     */
    public static final String CONFIG_CACHE_DIR = "cache-directory";
    /**
     * The constant CONFIG_BIND_ADDRESS.
     */
    public static final String CONFIG_BIND_ADDRESS = "bind.address";
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
    /**
     * The constant CONFIG_READINESS_CHECK.
     */
    public static final String CONFIG_READINESS_CHECK = "readiness-check";
    /**
     * The constant CONFIG_SSL_ENABLED.
     */
    public static final String CONFIG_SSL_ENABLED = "ssl.enabled";
    /**
     * The constant CONFIG_SSL_KEY_STORE_FILE.
     */
    public static final String CONFIG_SSL_KEY_STORE_FILE = "ssl.key-store-file";
    /**
     * The constant CONFIG_SSL_KEY_STORE_PW.
     */
    public static final String CONFIG_SSL_KEY_STORE_PW = "ssl.key-store-password";
    /**
     * The constant CONFIG_MODE_FLATTENING.
     */
    public static final String CONFIG_MODE_FLATTENING = "mode.flattening";
    /**
     * The constant CONFIG_DIRECTORY_BACKEND.
     */
    public static final String CONFIG_DIRECTORY_BACKEND = "directory-backend";
    /**
     * The constant CONFIG_BASE_DN_DESCRIPTION.
     */
    public static final String CONFIG_BASE_DN_DESCRIPTION = "base-dn.description";
    /**
     * The constant CONFIG_BASE_DN_GROUPS_DESCRIPTION.
     */
    public static final String CONFIG_BASE_DN_GROUPS_DESCRIPTION = "base-dn-groups.description";
    /**
     * The constant CONFIG_BASE_DN_USERS_DESCRIPTION.
     */
    public static final String CONFIG_BASE_DN_USERS_DESCRIPTION = "base-dn-users.description";

    private final Path cacheDir;
    private final String host;
    private final int port;
    private final boolean entryCacheEnabled;
    private final int entryCacheMaxSize;
    private final Duration entryCacheMaxAge;
    private final boolean readinessCheck;
    private final boolean sslEnabled;
    private final Path keyStoreFile;
    private final String keyStorePassword;
    private final boolean flattening;
    private final DirectoryBackend directoryBackend;
    private final String baseDnDescription;
    private final String baseDnGroupsDescription;
    private final String baseDnUsersDescription;

    /**
     * Instantiates a new server configuration.
     *
     * @param serverProperties  the server properties
     * @param backendProperties the backend properties
     */
    public ServerConfiguration(Properties serverProperties, Properties backendProperties) {

        cacheDir = Paths.get(serverProperties.getProperty(CONFIG_CACHE_DIR, "./cache")).toAbsolutePath().normalize();

        try {

            if (Files.exists(cacheDir))
                FileUtils.deleteDirectory(cacheDir.toFile());

            Files.createDirectories(cacheDir);

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        String bindAddressValue = serverProperties.getProperty(CONFIG_BIND_ADDRESS, "localhost:10389");

        String[] bindAddressParts = bindAddressValue.split(":");

        if (bindAddressParts.length != 2 || bindAddressParts[0].isEmpty() || bindAddressParts[1].isEmpty())
            throw new IllegalArgumentException("Cannot parse value for " + CONFIG_BIND_ADDRESS);

        host = bindAddressParts[0];
        port = Integer.parseInt(bindAddressParts[1]);

        // entry-cache support
        entryCacheEnabled = Boolean.parseBoolean(serverProperties.getProperty(CONFIG_ENTRY_CACHE_ENABLED, "false"));
        entryCacheMaxSize = Integer.parseInt(serverProperties.getProperty(CONFIG_ENTRY_CACHE_MAX_SIZE, "300"));
        entryCacheMaxAge = Duration.parse(serverProperties.getProperty(CONFIG_ENTRY_CACHE_MAX_AGE, "PT1H"));

        if (entryCacheMaxSize <= 0)
            throw new IllegalArgumentException("Expect value greater than zero for " + CONFIG_ENTRY_CACHE_MAX_SIZE);

        if (entryCacheMaxAge.isNegative() || entryCacheMaxAge.isZero())
            throw new IllegalArgumentException("Expect value greater than zero for " + CONFIG_ENTRY_CACHE_MAX_AGE);

        // require connection check to backend at startup
        readinessCheck = Boolean.parseBoolean(serverProperties.getProperty(CONFIG_READINESS_CHECK, "true"));

        // SSL support
        sslEnabled = Boolean.parseBoolean(serverProperties.getProperty(CONFIG_SSL_ENABLED, "false"));

        if (sslEnabled) {

            String keyStoreFileValue = serverProperties.getProperty(CONFIG_SSL_KEY_STORE_FILE);

            if (keyStoreFileValue == null)
                throw new IllegalArgumentException("Missing value for " + CONFIG_SSL_KEY_STORE_FILE);

            keyStoreFile = Paths.get(keyStoreFileValue).toAbsolutePath().normalize();

            keyStorePassword = serverProperties.getProperty(CONFIG_SSL_KEY_STORE_PW);

            if (keyStorePassword == null)
                throw new IllegalArgumentException("Missing value for " + CONFIG_SSL_KEY_STORE_PW);

        } else {

            keyStoreFile = null;
            keyStorePassword = null;
        }

        flattening = Boolean.parseBoolean(serverProperties.getProperty(CONFIG_MODE_FLATTENING, "true"));

        String directoryBackendClassValue = serverProperties.getProperty(CONFIG_DIRECTORY_BACKEND);

        if (directoryBackendClassValue == null || directoryBackendClassValue.isEmpty())
            throw new IllegalArgumentException("Missing value for " + CONFIG_DIRECTORY_BACKEND);

        try {

            directoryBackend =
                    (DirectoryBackend) Class.forName(directoryBackendClassValue)
                            .getConstructor(Properties.class)
                            .newInstance(backendProperties);

        } catch (Exception e) {

            throw new IllegalArgumentException("Cannot handle incorrect directory backend: " +
                    directoryBackendClassValue, e);
        }

        baseDnDescription = serverProperties.getProperty(CONFIG_BASE_DN_DESCRIPTION, "");
        baseDnGroupsDescription = serverProperties.getProperty(CONFIG_BASE_DN_GROUPS_DESCRIPTION, "");
        baseDnUsersDescription = serverProperties.getProperty(CONFIG_BASE_DN_USERS_DESCRIPTION, "");
    }

    /**
     * Gets cache dir.
     *
     * @return the cache dir
     */
    public Path getCacheDir() {

        return cacheDir;
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {

        return host;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {

        return port;
    }

    /**
     * Is entry cache enabled boolean.
     *
     * @return the boolean
     */
    public boolean isEntryCacheEnabled() {

        return entryCacheEnabled;
    }

    /**
     * Gets entry cache maximum size.
     *
     * @return the entry cache max size
     */
    public int getEntryCacheMaxSize() {

        return entryCacheMaxSize;
    }

    /**
     * Gets entry cache maximum age.
     *
     * @return the entry cache max age
     */
    public Duration getEntryCacheMaxAge() {

        return entryCacheMaxAge;
    }

    /**
     * Check the connection to the backend.
     *
     * @return the boolean
     */
    public boolean requireReadinessCheck() {

        return readinessCheck;
    }

    /**
     * Is SSL enabled boolean.
     *
     * @return the boolean
     */
    public boolean isSslEnabled() {

        return sslEnabled;
    }

    /**
     * Gets key store file.
     *
     * @return the key store file
     */
    public Path getKeyStoreFile() {

        return keyStoreFile;
    }

    /**
     * Gets key store password.
     *
     * @return the key store password
     */
    public String getKeyStorePassword() {

        return keyStorePassword;
    }

    /**
     * Is flattening boolean.
     *
     * @return the boolean
     */
    public boolean isFlattening() {

        return flattening;
    }

    /**
     * Gets directory backend.
     *
     * @return the directory backend
     */
    public DirectoryBackend getDirectoryBackend() {

        return directoryBackend;
    }

    /**
     * Gets base DN description.
     *
     * @return the base dn description
     */
    public String getBaseDnDescription() {

        return baseDnDescription;
    }

    /**
     * Gets base DN groups description.
     *
     * @return the base dn groups description
     */
    public String getBaseDnGroupsDescription() {

        return baseDnGroupsDescription;
    }

    /**
     * Gets base DN users description.
     *
     * @return the base dn users description
     */
    public String getBaseDnUsersDescription() {

        return baseDnUsersDescription;
    }
}
