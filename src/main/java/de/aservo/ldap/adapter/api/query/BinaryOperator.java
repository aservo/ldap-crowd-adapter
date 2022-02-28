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

package de.aservo.ldap.adapter.api.query;

import java.util.Objects;


public abstract class BinaryOperator<T extends BinaryOperator<?>>
        implements OperatorExpression<T> {

    private final String attribute;
    private final boolean negated;
    private final boolean ignoreCase;

    public BinaryOperator(String attribute, boolean negated, boolean ignoreCase) {

        this.attribute = attribute;
        this.negated = negated;
        this.ignoreCase = ignoreCase;
    }

    public String getAttribute() {

        return attribute;
    }

    public abstract String getValue();

    public boolean isNegated() {

        return negated;
    }

    public boolean isIgnoreCase() {

        return ignoreCase;
    }

    @Override
    public boolean equals(Object that) {

        if (this == that)
            return true;

        if (that == null)
            return false;

        if (this.getClass() != that.getClass())
            return false;

        if (ignoreCase)
            return getAttribute().equalsIgnoreCase(((BinaryOperator) that).getAttribute()) &&
                    getValue().equalsIgnoreCase(((BinaryOperator) that).getValue());

        return getAttribute().equals(((BinaryOperator) that).getAttribute()) &&
                getValue().equals(((BinaryOperator) that).getValue());
    }

    @Override
    public int hashCode() {

        if (ignoreCase)
            return Objects.hash(this.getClass().getSimpleName(), getAttribute().toLowerCase(),
                    getValue().toLowerCase());

        return Objects.hash(this.getClass().getSimpleName(), getAttribute(), getValue());
    }
}
