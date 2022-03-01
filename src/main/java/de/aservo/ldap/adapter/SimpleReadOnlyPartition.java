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

package de.aservo.ldap.adapter;

import de.aservo.ldap.adapter.api.exception.InternalServerException;
import de.aservo.ldap.adapter.api.exception.UnsupportedQueryExpressionException;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.*;
import org.apache.directory.server.core.api.partition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The base type ApacheDS partition for simplified read only access.
 */
public abstract class SimpleReadOnlyPartition
        extends AbstractPartition {

    private final Logger logger = LoggerFactory.getLogger(SimpleReadOnlyPartition.class);

    private static final String MODIFICATION_NOT_ALLOWED_MSG = "This simple partition does not allow modification.";

    @Override
    public EntryFilteringCursor search(SearchOperationContext context)
            throws LdapException {

        // disable internal requests
        if (context.getSession().getClientAddress() == null)
            return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager);

        logger.info("[{}] - Query: DN={} filter={} scope={}",
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

        } catch (UnsupportedQueryExpressionException e) {

            logger.debug("The partition caught an exception because of an unsupported filter expression.", e);

            throw e;

        } catch (Exception e) {

            logger.error("The partition caught an exception.", e);

            throw new InternalServerException("The partition has detected an internal server error.");

        } finally {

            logger.debug("[{}] - Cursor created by partition {}",
                    context.getSession().getClientAddress(),
                    this.getClass().getSimpleName());
        }

        return cursor;
    }

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

    /**
     * Run destruction process.
     *
     * @throws LdapException the ldap exception
     */
    protected abstract void doDestroy()
            throws LdapException;

    @Override
    protected void doDestroy(PartitionTxn partitionTxn)
            throws LdapException {

        doDestroy();
    }

    @Override
    protected void doRepair()
            throws LdapException {
    }

    @Override
    public PartitionReadTxn beginReadTransaction() {

        return null;
    }

    @Override
    public PartitionWriteTxn beginWriteTransaction() {

        return null;
    }

    @Override
    public void unbind(UnbindOperationContext unbindOperationContext)
            throws LdapException {
    }

    @Override
    public void saveContextCsn(PartitionTxn partitionTxn)
            throws LdapException {
    }

    @Override
    public Subordinates getSubordinates(PartitionTxn partitionTxn, Entry entry)
            throws LdapException {

        return null;
    }

    @Override
    public void add(AddOperationContext addOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    @Override
    public void modify(ModifyOperationContext modifyOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    @Override
    public Entry delete(DeleteOperationContext deleteOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    @Override
    public void move(MoveOperationContext moveOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    @Override
    public void rename(RenameOperationContext renameOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }

    @Override
    public void moveAndRename(MoveAndRenameOperationContext moveAndRenameOperationContext)
            throws LdapException {

        throw new LdapException(MODIFICATION_NOT_ALLOWED_MSG);
    }
}
