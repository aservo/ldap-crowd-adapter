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

package de.aservo.ldap.adapter.api.database.util;

import de.aservo.ldap.adapter.api.database.exception.UncheckedSQLException;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;


public interface UncheckedCloseable
        extends Closeable {

    default void closeUnchecked() {

        try {

            close();

        } catch (IOException e) {

            if (e.getCause() instanceof SQLException)
                throw new UncheckedSQLException(e);

            throw new UncheckedIOException(e);
        }
    }
}
