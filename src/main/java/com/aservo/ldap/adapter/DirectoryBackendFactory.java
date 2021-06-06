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

package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.api.directory.DirectoryBackend;
import com.aservo.ldap.adapter.api.directory.NestedDirectoryBackend;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public class DirectoryBackendFactory {

    private final ServerConfiguration config;
    private final NestedDirectoryBackend directoryBackend;

    public DirectoryBackendFactory(ServerConfiguration config) {

        this.config = config;

        List<String> directoryBackendClasses = config.getPermanentDirectoryBackendClasses();
        NestedDirectoryBackend innerDirectoryBackend;

        if (directoryBackendClasses.isEmpty())
            throw new IndexOutOfBoundsException("Expect non empty sequence of directory backend classes.");

        try {

            innerDirectoryBackend =
                    (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(0))
                            .getConstructor(ServerConfiguration.class)
                            .newInstance(config);

            for (int i = 1; i < directoryBackendClasses.size(); i++) {

                innerDirectoryBackend =
                        (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(i))
                                .getConstructor(ServerConfiguration.class, NestedDirectoryBackend.class)
                                .newInstance(config, innerDirectoryBackend);
            }

            directoryBackend = innerDirectoryBackend;

        } catch (ClassNotFoundException e) {

            throw new IllegalArgumentException("Missing class in permanent directory backend definition", e);

        } catch (Exception e) {

            throw new RuntimeException("Cannot instantiate permanent directory backend.", e);
        }
    }

    public DirectoryBackend getPermanentDirectory() {

        return directoryBackend;
    }

    public <T> T withSession(Function<DirectoryBackend, T> block) {

        NestedDirectoryBackend directory = createSessionSpecificDirectory();

        return directory.withReadAccess(() -> block.apply(directory));
    }

    public void withSession(Consumer<DirectoryBackend> block) {

        withSession(x -> {

            block.accept(x);
            return null;
        });
    }

    public void startup() {

        directoryBackend.startup();
    }

    public void shutdown() {

        directoryBackend.shutdown();
    }

    private NestedDirectoryBackend createSessionSpecificDirectory() {

        List<String> directoryBackendClasses = config.getSessionDirectoryBackendClasses();
        NestedDirectoryBackend innerDirectoryBackend = directoryBackend;

        try {

            for (int i = 0; i < directoryBackendClasses.size(); i++) {

                innerDirectoryBackend =
                        (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(i))
                                .getConstructor(ServerConfiguration.class, NestedDirectoryBackend.class)
                                .newInstance(config, innerDirectoryBackend);
            }

        } catch (ClassNotFoundException e) {

            throw new IllegalArgumentException("Missing class in session specific directory backend definition", e);

        } catch (Exception e) {

            throw new RuntimeException("Cannot instantiate session specific directory backend.", e);
        }

        return innerDirectoryBackend;
    }
}
