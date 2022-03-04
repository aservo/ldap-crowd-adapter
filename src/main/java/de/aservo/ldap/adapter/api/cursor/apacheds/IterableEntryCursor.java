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

package de.aservo.ldap.adapter.api.cursor.apacheds;

import de.aservo.ldap.adapter.api.cursor.Cursor;
import de.aservo.ldap.adapter.api.exception.InternalServerException;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorClosedException;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;

import java.io.IOException;


public class IterableEntryCursor
        extends AbstractCursor<Entry> {

    private final Logger logger;
    private final Cursor<Entry> entries;
    private boolean closed = false;

    public IterableEntryCursor(Logger logger, Cursor<Entry> entries) {

        this.logger = logger;
        this.entries = entries;
    }

    @Override
    public void before(Entry entry)
            throws LdapException, CursorException {

        // cursor pointer change not supported
    }

    @Override
    public void after(Entry entry)
            throws LdapException, CursorException {

        // cursor pointer change not supported
    }

    @Override
    public void beforeFirst()
            throws LdapException, CursorException {

        // cursor pointer change not supported
    }

    @Override
    public void afterLast()
            throws LdapException, CursorException {

        // cursor pointer change not supported
    }

    @Override
    public boolean first()
            throws LdapException, CursorException {

        // cursor pointer change not supported
        return true;
    }

    @Override
    public boolean last()
            throws LdapException, CursorException {

        // cursor pointer change not supported
        return false;
    }

    @Override
    public boolean previous()
            throws LdapException, CursorException {

        // cursor pointer change not supported
        return false;
    }

    @Override
    public boolean available() {

        // cursor pointer change not supported
        return true;
    }

    @Override
    public void close(Exception cause)
            throws IOException {

        super.close(cause);
        entries.close();

        logger.warn("An entry cursor was closed with an exception.", cause);
        logger.debug("[Thread ID {}] - An entry cursor is closed.", Thread.currentThread().getId());
    }

    @Override
    public void close()
            throws IOException {

        super.close();
        entries.close();

        logger.debug("[Thread ID {}] - An entry cursor was closed.", Thread.currentThread().getId());
    }

    @Override
    public boolean next() {

        try {

            if (closed)
                throw new CursorClosedException("Cannot perform next method on closed cursor.");

            return entries.next();

        } catch (Exception e) {

            logger.error("Caught an exception while accessing a cursor.", e);

            throw new InternalServerException("A cursor has detected an internal server error.");
        }
    }

    @Override
    public Entry get() {

        try {

            if (closed)
                throw new CursorClosedException("Cannot perform get method on closed cursor.");

            return entries.get();

        } catch (CursorClosedException e) {

            logger.error("Caught an exception while accessing a cursor.", e);

            throw new InternalServerException("A cursor has detected an internal server error.");
        }
    }
}
