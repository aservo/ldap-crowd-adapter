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


public final class EqualOperator
        extends BinaryOperator<EqualOperator> {

    private final String value;

    public EqualOperator(String attribute, String value, boolean negated, boolean ignoreCase) {

        super(attribute, negated, ignoreCase);
        this.value = value;
    }

    public EqualOperator(String attribute, String value) {

        this(attribute, value, false, true);
    }

    public String getValue() {

        return value;
    }

    public EqualOperator negate() {

        return new EqualOperator(getAttribute(), getValue(), !isNegated(), isIgnoreCase());
    }

    public boolean check(String value) {

        if (isIgnoreCase())
            return getValue().equalsIgnoreCase(value) != isNegated();

        return getValue().equals(value) != isNegated();
    }
}
