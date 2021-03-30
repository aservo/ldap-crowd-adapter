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

import java.util.Optional;


/**
 * A converter to map complex types for comfortable database access.
 */
public interface Converter {

    /**
     * Converts an argument to a database type.
     *
     * @param value the value which should be converted
     * @param clazz the expected target type
     * @return the optional result of a conversion
     */
    <T> Optional<T> read(Object value, Class<T> clazz);

    /**
     * Converts from a database type.
     *
     * @param value the value which should be converted
     * @return the optional result of a conversion
     */
    <T> Optional<T> write(T value);
}
