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

package com.aservo.ldap.adapter.api.entity;

import com.aservo.ldap.adapter.api.database.Row;
import java.util.Objects;


/**
 * The parent class fo all entities.
 */
public abstract class Entity
        implements Row {

    private final String id;

    @Override
    public boolean equals(Object that) {

        if (this == that)
            return true;

        if (that == null)
            return false;

        if (this.getClass() != that.getClass())
            return false;

        return id.equalsIgnoreCase(((Entity) that).id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    /**
     * Instantiates a new Group.
     *
     * @param id the id of this object
     */
    public Entity(String id) {

        this.id = id;
    }

    /**
     * Gets the entity ID.
     *
     * @return the ID
     */
    public String getId() {

        return id;
    }

    /**
     * Gets an entity value by column name.
     *
     * @return the column value
     */
    public <T> T apply(String columnName, Class<T> clazz) {

        try {

            return (T) findColumn(columnName);

        } catch (ClassCastException e) {

            throw new IllegalArgumentException(
                    "Cannot perform a cast for column " + columnName +
                            " and with type [" + clazz.getName() + "].", e);
        }
    }

    protected abstract Object findColumn(String columnName);

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public abstract EntityType getEntityType();
}
