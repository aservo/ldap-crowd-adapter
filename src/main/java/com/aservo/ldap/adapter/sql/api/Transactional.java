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

package com.aservo.ldap.adapter.sql.api;

import java.util.function.Consumer;
import java.util.function.Function;


/**
 * The transactional access definition.
 */
public interface Transactional {

    /**
     * Creates a transaction for the lifetime of a code block.
     *
     * @param block the code block
     * @return the return value of the code block
     */
    <T> T withTransaction(Function<QueryDefFactory, T> block);

    /**
     * Creates a transaction for the lifetime of a code block.
     *
     * @param block the code block
     */
    void withTransaction(Consumer<QueryDefFactory> block);
}
