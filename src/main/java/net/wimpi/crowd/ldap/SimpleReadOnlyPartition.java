package net.wimpi.crowd.ldap;

import java.io.IOException;
import java.io.OutputStream;
import javax.naming.OperationNotSupportedException;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.*;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.Subordinates;
import org.apache.directory.server.i18n.I18n;


public abstract class SimpleReadOnlyPartition
        implements Partition {

    private static final String MODIFICATION_NOT_ALLOWED_MSG = "This simple partition does not allow modification.";

    protected String id;
    protected SchemaManager schemaManager;
    private boolean initialized;

    public final void initialize()
            throws LdapException {

        if (!initialized) {

            try {

                doInit();
                initialized = true;

            } catch (Exception e) {

                throw new LdapOtherException(e.getMessage(), e);
            }
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

    public void setId(String id) {

        this.checkInitialized("id");
        this.id = id;
    }

    public final SchemaManager getSchemaManager() {

        return this.schemaManager;
    }

    public void setSchemaManager(SchemaManager schemaManager) {

        this.schemaManager = schemaManager;
    }

    protected void checkInitialized(String property) {

        if (this.initialized)
            throw new IllegalStateException(I18n.err(I18n.ERR_576, new Object[]{property}));
    }

    public EntryFilteringCursor search(SearchOperationContext context)
            throws LdapException {

        switch (context.getScope()) {

            // -base: the node itself
            case OBJECT:
                return findOne(context);

            // -one: one level under the node
            case ONELEVEL:
                return findManyOnFirstLevel(context);

            // -sub: all node under the node
            case SUBTREE:
                return findManyOnMultipleLevels(context);

            // no scope defined
            default:
                return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager);
        }
    }

    protected abstract void doInit()
            throws Exception;

    protected abstract void doDestroy()
            throws Exception;

    protected abstract EntryFilteringCursor findOne(SearchOperationContext context)
            throws LdapException;

    protected abstract EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException;

    protected abstract EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException;

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
