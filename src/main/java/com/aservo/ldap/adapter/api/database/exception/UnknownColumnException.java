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

package com.aservo.ldap.adapter.api.database.exception;

/**
 * The exception type for unknown columns.
 */
public class UnknownColumnException
        extends UncheckedSQLException {

    /**
     * Instantiates a new unknown column exception.
     */
    public UnknownColumnException() {

        super();
    }

    /**
     * Instantiates a new unknown column exception.
     *
     * @param message the message
     */
    public UnknownColumnException(String message) {

        super(message);
    }

    /**
     * Instantiates a new unknown column exception.
     *
     * @param message   the message
     * @param throwable the throwable
     */
    public UnknownColumnException(String message, Throwable throwable) {

        super(message, throwable);
    }

    /**
     * Instantiates a new unknown column exception.
     *
     * @param throwable the throwable
     */
    public UnknownColumnException(Throwable throwable) {

        super(throwable);
    }
}
