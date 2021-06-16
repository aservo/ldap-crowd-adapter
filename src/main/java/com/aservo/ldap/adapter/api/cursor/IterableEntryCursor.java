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

package com.aservo.ldap.adapter.api.cursor;

import com.aservo.ldap.adapter.api.exception.InternalServerException;
import com.aservo.ldap.adapter.backend.CrowdDirectoryBackend;
import java.io.IOException;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorClosedException;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IterableEntryCursor
        extends AbstractCursor<Entry>
        implements Cursor<Entry> {

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);
    private final Cursor<Entry> entries;

    public IterableEntryCursor(Cursor<Entry> entries) {

        this.entries = entries;
    }

    @Override
    public void close(Exception cause)
            throws IOException {

        entries.close();
        super.close(cause);

        logger.warn("An entry cursor was closed with an exception.", cause);
        logger.debug("[Thread ID {}] - An entry cursor is closed.", Thread.currentThread().getId());
    }

    @Override
    public void close()
            throws IOException {

        entries.close();
        super.close();

        logger.debug("[Thread ID {}] - An entry cursor was closed.", Thread.currentThread().getId());
    }

    @Override
    public boolean available() {

        return false;
    }

    @Override
    public void before(Entry attributes)
            throws LdapException, CursorException {
    }

    @Override
    public void after(Entry attributes)
            throws LdapException, CursorException {
    }

    @Override
    public void beforeFirst()
            throws LdapException, CursorException {
    }

    @Override
    public void afterLast()
            throws LdapException, CursorException {
    }

    @Override
    public boolean first()
            throws LdapException, CursorException {

        throw new UnsupportedOperationException("The cursor operation first() is not supported.");
    }

    @Override
    public boolean last()
            throws LdapException, CursorException {

        throw new UnsupportedOperationException("The cursor operation last() is not supported.");
    }

    @Override
    public boolean previous()
            throws LdapException, CursorException {

        throw new UnsupportedOperationException("The cursor operation previous() is not supported.");
    }

    @Override
    public boolean next() {

        try {

            this.checkNotClosed("next()");
            return entries.next();

        } catch (Exception e) {

            logger.error("Caught an exception while accessing a cursor.", e);

            throw new InternalServerException("A cursor has detected an internal server error.");
        }
    }

    @Override
    public Entry get() {

        try {

            this.checkNotClosed("get()");
            return entries.get();

        } catch (CursorClosedException e) {

            logger.error("Caught an exception while accessing a cursor.", e);

            throw new InternalServerException("A cursor has detected an internal server error.");
        }
    }
}
