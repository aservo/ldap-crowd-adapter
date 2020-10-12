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

package com.aservo.ldap.adapter.adapter.query;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public abstract class LogicExpression
        implements FilterNode {

    private final List<FilterNode> children = new LinkedList<>();

    public LogicExpression(List<FilterNode> children) {

        this.children.addAll(children);
    }

    public List<FilterNode> getChildren() {

        return new LinkedList<>(children);
    }

    @Override
    public boolean equals(Object that) {

        if (this == that)
            return true;

        if (that == null)
            return false;

        if (!(that instanceof LogicExpression))
            return false;

        if (this.getClass() != that.getClass())
            return false;

        return this.getChildren().equals(((LogicExpression) that).getChildren());
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.getClass().getSimpleName(), this.getChildren());
    }
}
