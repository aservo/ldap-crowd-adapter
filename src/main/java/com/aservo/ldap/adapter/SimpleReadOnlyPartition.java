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

import java.io.IOException;
import java.io.OutputStream;
import javax.naming.OperationNotSupportedException;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.*;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.Subordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The base type ApacheDS partition for simplified read only access.
 */
public abstract class SimpleReadOnlyPartition
        implements Partition {

    private final Logger logger = LoggerFactory.getLogger(SimpleReadOnlyPartition.class);

    private static final String MODIFICATION_NOT_ALLOWED_MSG = "This simple partition does not allow modification.";

    /**
     * The partition ID.
     */
    protected final String id;
    /**
     * The schema manager.
     */
    protected SchemaManager schemaManager;
    private boolean initialized;

    /**
     * Instantiates a new read only partition.
     *
     * @param id the partition ID
     */
    protected SimpleReadOnlyPartition(String id) {

        this.id = id;
    }

    public final void initialize()
            throws LdapException {

        if (!initialized) {

            doInit();
            initialized = true;
        }
    }

    public final boolean isInitialized() {

        return initialized;
    }

    public final void destroy()
            throws Exception {

        try {

            doDestroy();

        } finally {

            initialized = false;
        }
    }

    public final String getId() {

        return this.id;
    }

    public final SchemaManager getSchemaManager() {

        return this.schemaManager;
    }

    public void setSchemaManager(SchemaManager schemaManager) {

        this.schemaManager = schemaManager;
    }

    public EntryFilteringCursor search(SearchOperationContext context)
            throws LdapException {

        // disable internal requests
        if (context.getSession().getClientAddress() == null)
            return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager);

        logger.info("[{}] - Access partition: DN={} filter={} scope={}",
                context.getSession().getClientAddress(),
                context.getDn().getName(),
                context.getFilter(),
                context.getScope());

        EntryFilteringCursor cursor;

        try {

            switch (context.getScope()) {

                // -base: the node itself
                case OBJECT:
                    cursor = findOne(context);
                    break;

                // -one: one level under the node
                case ONELEVEL:
                    cursor = findManyOnFirstLevel(context);
                    break;

                // -sub: all node under the node
                case SUBTREE:
                    cursor = findManyOnMultipleLevels(context);
                    break;

                // no scope defined
                default:
                    cursor = new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager);
                    break;
            }

        } finally {

            logger.info("[{}] - Cursor created by partition {}",
                    context.getSession().getClientAddress(),
                    this.getClass().getSimpleName());
        }

        return cursor;
    }

    /**
     * Run initialization process.
     *
     * @throws LdapException the ldap exception
     */
    protected abstract void doInit()
            throws LdapException;

    /**
     * Run destruction process.
     *
     * @throws LdapException the ldap exception
     */
    protected abstract void doDestroy()
            throws LdapException;

    /**
     * Find one entry cursor.
     *
     * @param context the context
     * @return the entry filtering cursor
     * @throws LdapException the ldap exception
     */
    protected abstract EntryFilteringCursor findOne(SearchOperationContext context)
            throws LdapException;

    /**
     * Find many entry cursors on first level.
     *
     * @param context the context
     * @return the entry filtering cursor
     * @throws LdapException the ldap exception
     */
    protected abstract EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException;

    /**
     * Find many entry cursors on multiple levels.
     *
     * @param context the context
     * @return the entry filtering cursor
     * @throws LdapException the ldap exception
     */
    protected abstract EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException;

    public void setId(String id) {

        throw new RuntimeException(new OperationNotSupportedException("Cannot set partition ID outside object."));
    }

    public void setSuffixDn(Dn dn)
            throws LdapInvalidDnException {

        throw new RuntimeException(new OperationNotSupportedException("Cannot set dn outside object."));
    }

    public Subordinates getSubordinates(Entry entry)
            throws LdapException {

        return null;
    }

    public void repair()
            throws Exception {

    }

    public void sync()
            throws Exception {

    }

    public void unbind(UnbindOperationContext unbindOperationContext)
            throws LdapException {

    }

    public void dumpIndex(OutputStream stream, String name)
            throws IOException {

        stream.write(Strings.getBytesUtf8("Nothing to dump for index " + name));
    }

    public void setCacheService(CacheService cacheService) {

    }

    public String getContextCsn() {
        return null;
    }

    public void saveContextCsn()
            throws Exception {

    }

    public void add(AddOperationContext addOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    public void modify(ModifyOperationContext modifyOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    public Entry delete(DeleteOperationContext deleteOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    public void move(MoveOperationContext moveOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    public void rename(RenameOperationContext renameOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    public void moveAndRename(MoveAndRenameOperationContext moveAndRenameOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }
}
