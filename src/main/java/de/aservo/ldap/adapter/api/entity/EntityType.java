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

package de.aservo.ldap.adapter.api.entity;


/**
 * The enumeration for all supported entity types.
 */
public enum EntityType {

    DOMAIN("DOMAIN"),
    GROUP_UNIT("GROUP_UNIT"),
    USER_UNIT("USER_UNIT"),
    GROUP("GROUP"),
    USER("USER");

    private final String name;

    EntityType(String name) {

        this.name = name;
    }

    public boolean equalsName(String name) {

        return this.name.equalsIgnoreCase(name);
    }

    public String toString() {

        return name;
    }

    public static EntityType fromString(String name) {

        for (EntityType x : EntityType.values())
            if (x.name.equalsIgnoreCase(name))
                return x;

        throw new IllegalArgumentException("Cannot create entity type from unexpected string.");
    }
}
