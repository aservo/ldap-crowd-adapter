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

package com.aservo.ldap.adapter.api.database.result;

import com.aservo.ldap.adapter.api.database.Row;
import java.util.function.Function;


/**
 * Result for a single value.
 */
public interface SingleResult
        extends EnrichedResult {

    /**
     * Transforms rows to expected types.
     *
     * @param f the function used to map a single row
     * @return the value of an expected type
     */
    <T> T transform(Function<Row, T> f);
}
