package de.aservo.ldap.adapter.api.cursor;

import de.aservo.ldap.adapter.api.database.util.UncheckedCloseable;

import java.io.IOException;


public interface Cursor<T>
        extends Iterable<T>, UncheckedCloseable {

    boolean next();

    T get();

    void close()
            throws IOException;
}
