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

package com.aservo.ldap.adapter.backend.exception;


/**
 * The type Security problem exception.
 */
public class SecurityProblemException
        extends RuntimeException {

    /**
     * Instantiates a new Security problem exception.
     */
    public SecurityProblemException() {

        super();
    }

    /**
     * Instantiates a new Security problem exception.
     *
     * @param message the message
     */
    public SecurityProblemException(String message) {

        super(message);
    }

    /**
     * Instantiates a new Security problem exception.
     *
     * @param message   the message
     * @param throwable the throwable
     */
    public SecurityProblemException(String message, Throwable throwable) {

        super(message, throwable);
    }

    /**
     * Instantiates a new Security problem exception.
     *
     * @param throwable the throwable
     */
    public SecurityProblemException(Throwable throwable) {

        super(throwable);
    }
}
