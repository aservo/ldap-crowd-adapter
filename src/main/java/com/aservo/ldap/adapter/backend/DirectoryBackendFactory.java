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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DirectoryBackendFactory {

    private final Logger logger = LoggerFactory.getLogger(DirectoryBackendFactory.class);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ServerConfiguration config;
    private final DirectoryBackend directoryBackend;

    public DirectoryBackendFactory(ServerConfiguration config) {

        this.config = config;

        List<String> directoryBackendClasses = config.getDirectoryBackendClasses();
        NestedDirectoryBackend innerDirectoryBackend;

        if (directoryBackendClasses.isEmpty())
            throw new IndexOutOfBoundsException("Expect non empty sequence of directory backend classes.");

        try {

            innerDirectoryBackend =
                    (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(0))
                            .getConstructor(ServerConfiguration.class, Locking.class)
                            .newInstance(config, new Locking());

            for (int i = 1; i < directoryBackendClasses.size(); i++) {

                innerDirectoryBackend =
                        (NestedDirectoryBackend) Class.forName(directoryBackendClasses.get(i))
                                .getConstructor(ServerConfiguration.class, Locking.class, NestedDirectoryBackend.class)
                                .newInstance(config, new Locking(), innerDirectoryBackend);
            }

            directoryBackend = innerDirectoryBackend;

        } catch (ClassNotFoundException e) {

            throw new IllegalArgumentException("Missing class in directory backend definition", e);

        } catch (Exception e) {

            throw new RuntimeException("Cannot instantiate directory backend.", e);
        }
    }

    public DirectoryBackend getPermanentDirectory() {

        return directoryBackend;
    }

    public <T> T withSession(Function<DirectoryBackend, T> block) {

        long start = Instant.now().toEpochMilli();
        T result;

        rwLock.readLock().lock();

        try {

            result = block.apply(directoryBackend);

        } finally {

            rwLock.readLock().unlock();
        }

        long end = Instant.now().toEpochMilli();

        logger.debug("A read only session was performed in {} ms.", end - start);

        return result;
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

    public class Locking {

        public <T> T withWriteAccess(Supplier<T> block) {

            long start = Instant.now().toEpochMilli();
            T result;

            rwLock.writeLock().lock();

            try {

                result = block.get();

            } finally {

                rwLock.writeLock().unlock();
            }

            long end = Instant.now().toEpochMilli();

            logger.debug("A writing session was performed in {} ms.", end - start);

            return result;
        }

        public void withWriteAccess(Runnable block) {

            withWriteAccess(() -> {

                block.run();
                return null;
            });
        }
    }
}
