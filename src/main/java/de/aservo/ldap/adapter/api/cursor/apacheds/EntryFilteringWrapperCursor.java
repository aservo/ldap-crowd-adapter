package de.aservo.ldap.adapter.api.cursor.apacheds;

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EntryFilteringWrapperCursor
        extends AbstractCursor<Entry>
        implements EntryFilteringCursor {

    private final Cursor<Entry> entries;
    private final SearchOperationContext operationContext;
    private final List<EntryFilter> filters;

    public EntryFilteringWrapperCursor(Cursor<Entry> entries, SearchOperationContext operationContext) {

        this.entries = entries;
        this.operationContext = operationContext;
        this.filters = new ArrayList<>();
    }

    @Override
    public boolean addEntryFilter(EntryFilter filter) {

        return filters.add(filter);
    }

    @Override
    public List<EntryFilter> getEntryFilters() {

        return Collections.unmodifiableList(filters);
    }

    @Override
    public SearchOperationContext getOperationContext() {

        return operationContext;
    }

    @Override
    public void before(Entry attributes)
            throws LdapException, CursorException {

        entries.before(attributes);
    }

    @Override
    public void after(Entry attributes)
            throws LdapException, CursorException {

        entries.after(attributes);
    }

    @Override
    public void beforeFirst()
            throws LdapException, CursorException {

        entries.beforeFirst();
    }

    @Override
    public void afterLast()
            throws LdapException, CursorException {

        entries.afterLast();
    }

    @Override
    public boolean first()
            throws LdapException, CursorException {

        return entries.first();
    }

    @Override
    public boolean last()
            throws LdapException, CursorException {

        return entries.last();
    }

    @Override
    public boolean previous()
            throws LdapException, CursorException {

        return entries.previous();
    }

    @Override
    public boolean available() {

        return entries.available();
    }

    @Override
    public void close()
            throws IOException {

        super.close();
        entries.close();
    }

    @Override
    public void close(Exception cause)
            throws IOException {

        super.close(cause);
        entries.close(cause);
    }

    @Override
    public boolean next() throws LdapException, CursorException {

        return entries.next();
    }

    @Override
    public Entry get()
            throws InvalidCursorPositionException {

        Entry entry;

        try {

            entry = entries.get();

        } catch (CursorException e) {

            throw new InvalidCursorPositionException(e.getMessage());
        }

        if (!(entry instanceof ClonedServerEntry))
            entry = new ClonedServerEntry(entry);

        return entry;
    }
}
