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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LockingDirectoryBackend
        extends ProxyDirectoryBackend {

    private final Logger logger = LoggerFactory.getLogger(LockingDirectoryBackend.class);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public LockingDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        super(config, directoryBackend);
    }

    @Override
    public <T> T withReadAccess(Supplier<T> block) {

        long start = System.currentTimeMillis();
        T result;

        rwLock.readLock().lock();

        try {

            result = super.withReadAccess(block);

        } finally {

            rwLock.readLock().unlock();
        }

        long end = System.currentTimeMillis();

        logger.debug("[Thread ID {}] - A read only session was performed in {} ms.",
                Thread.currentThread().getId(), end - start == 0 ? 1 : end - start);

        return result;
    }

    @Override
    public void withReadAccess(Runnable block) {

        withReadAccess(() -> {

            block.run();
            return null;
        });
    }

    @Override
    public <T> T withWriteAccess(Supplier<T> block) {

        long start = System.currentTimeMillis();
        T result;

        rwLock.writeLock().lock();

        try {

            result = super.withWriteAccess(block);

        } finally {

            rwLock.writeLock().unlock();
        }

        long end = System.currentTimeMillis();

        logger.debug("[Thread ID {}] - A writing session was performed in {} ms.",
                Thread.currentThread().getId(), end - start == 0 ? 1 : end - start);

        return result;
    }

    @Override
    public void withWriteAccess(Runnable block) {

        withWriteAccess(() -> {

            block.run();
            return null;
        });
    }
}
