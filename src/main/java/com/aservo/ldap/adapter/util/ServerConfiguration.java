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
import com.aservo.ldap.adapter.backend.NestedDirectoryBackend;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * The type Server configuration.
 */
public class ServerConfiguration {

    /**
     * The constant CONFIG_DS_CACHE_DIR.
     */
    public static final String CONFIG_DS_CACHE_DIR = "ds-cache-directory";
    /**
     * The constant CONFIG_BIND_ADDRESS.
     */
    public static final String CONFIG_BIND_ADDRESS = "bind.address";
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
     * The constant CONFIG_UNDEFINED_FILTER_EXPRESSION_RESULT.
     */
    public static final String CONFIG_UNDEFINED_FILTER_EXPRESSION_RESULT = "mode.undefined-filter-expression-result";
    /**
     * The constant CONFIG_RESPONSE_MAX_SIZE_LIMIT.
     */
    public static final String CONFIG_RESPONSE_MAX_SIZE_LIMIT = "mode.response-max-size-limit";
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

    private final Properties backendProperties;
    private final Path cacheDir;
    private final String host;
    private final int port;
    private final boolean sslEnabled;
    private final Path keyStoreFile;
    private final String keyStorePassword;
    private final boolean flattening;
    private final boolean undefFilterExprResult;
    private final int responseMaxSizeLimit;
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

        this.backendProperties = backendProperties;

        cacheDir = Paths.get(serverProperties.getProperty(CONFIG_DS_CACHE_DIR, "./cache")).toAbsolutePath().normalize();

        String bindAddressValue = serverProperties.getProperty(CONFIG_BIND_ADDRESS, "localhost:10389");

        String[] bindAddressParts = bindAddressValue.split(":");

        if (bindAddressParts.length != 2 || bindAddressParts[0].isEmpty() || bindAddressParts[1].isEmpty())
            throw new IllegalArgumentException("Cannot parse value for " + CONFIG_BIND_ADDRESS);

        host = bindAddressParts[0];
        port = Integer.parseInt(bindAddressParts[1]);

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

        undefFilterExprResult =
                Boolean.parseBoolean(serverProperties.getProperty(CONFIG_UNDEFINED_FILTER_EXPRESSION_RESULT, "false"));

        responseMaxSizeLimit = Integer.parseInt(serverProperties.getProperty(CONFIG_RESPONSE_MAX_SIZE_LIMIT, "100"));

        if (responseMaxSizeLimit <= 0)
            throw new IllegalArgumentException("Expect value for " +
                    CONFIG_RESPONSE_MAX_SIZE_LIMIT + " greater than zero.");

        String directoryBackendClassesValue = serverProperties.getProperty(CONFIG_DIRECTORY_BACKEND);

        if (directoryBackendClassesValue == null || directoryBackendClassesValue.isEmpty())
            throw new IllegalArgumentException("Missing value for " + CONFIG_DIRECTORY_BACKEND);

        List<String> directoryBackendClasses =
                Arrays.stream(directoryBackendClassesValue.split(","))
                        .map(x -> x.trim())
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toList());

        NestedDirectoryBackend innerDirectoryBackend;

        try {

            innerDirectoryBackend =
                    (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(0))
                            .getConstructor(ServerConfiguration.class)
                            .newInstance(this);

            for (int i = 1; i < directoryBackendClasses.size(); i++) {

                innerDirectoryBackend =
                        (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(i))
                                .getConstructor(ServerConfiguration.class, NestedDirectoryBackend.class)
                                .newInstance(this, innerDirectoryBackend);
            }

            directoryBackend = innerDirectoryBackend;

        } catch (ClassNotFoundException e) {

            throw new IllegalArgumentException("Cannot find classes for directory backend definition: " +
                    directoryBackendClassesValue, e);

        } catch (Exception e) {

            throw new RuntimeException("Cannot instantiate directory backend.", e);
        }

        baseDnDescription = serverProperties.getProperty(CONFIG_BASE_DN_DESCRIPTION, "");
        baseDnGroupsDescription = serverProperties.getProperty(CONFIG_BASE_DN_GROUPS_DESCRIPTION, "");
        baseDnUsersDescription = serverProperties.getProperty(CONFIG_BASE_DN_USERS_DESCRIPTION, "");
    }

    /**
     * Gets the backend properties.
     *
     * @return the properties
     */
    public Properties getBackendProperties() {

        return backendProperties;
    }

    /**
     * Gets cache dir.
     *
     * @return the cache dir
     */
    public Path getDsCacheDir() {

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
    public boolean isFlatteningEnabled() {

        return flattening;
    }

    /**
     * Gets the boolean value for undefined filter expressions.
     *
     * @return the boolean
     */
    public boolean evaluateUndefinedFilterExprSuccessfully() {

        return undefFilterExprResult;
    }

    /**
     * Gets the maximum size limit for responses.
     *
     * @return the maximum number of entities
     */
    public int getResponseMaxSizeLimit() {

        return responseMaxSizeLimit;
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
