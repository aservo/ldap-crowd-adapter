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

package com.aservo.ldap.adapter.sql.impl;

import com.aservo.ldap.adapter.sql.api.Converter;
import java.util.Optional;


/**
 * The no-operation converter is used by default.
 */
public class NoopConverter
        implements Converter {

    public <T> Optional<T> read(Object value, Class<T> clazz) {

        if (value == null)
            return null;

        return Optional.of((T) value);
    }

    public <T> Optional<T> write(T value) {

        if (value == null)
            return null;

        return Optional.of(value);
    }
}
