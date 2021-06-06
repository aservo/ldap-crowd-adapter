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

package com.aservo.ldap.adapter.api.query;


import java.util.Objects;

public final class BooleanValue
        implements QueryExpression {

    private final boolean value;

    public BooleanValue(boolean value) {

        this.value = value;
    }

    public boolean getValue() {

        return value;
    }

    public BooleanValue negate() {

        return new BooleanValue(!value);
    }

    @Override
    public boolean equals(Object that) {

        if (this == that)
            return true;

        if (that == null)
            return false;

        if (this.getClass() != that.getClass())
            return false;

        return this.getValue() == ((BooleanValue) that).getValue();
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.getClass().getSimpleName(), value);
    }

    public static BooleanValue trueValue() {

        return new BooleanValue(true);
    }

    public static BooleanValue falseValue() {

        return new BooleanValue(false);
    }
}
