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

package com.aservo.ldap.adapter.adapter.entity;

import com.aservo.ldap.adapter.backend.DirectoryBackend;


/**
 * The domain entity.
 */
public class DomainEntity
        extends Entity
        implements DescribableEntity {

    private final String description;

    /**
     * Instantiates a new Domain.
     *
     * @param directoryBackend the related directory backend
     * @param description      the description
     */
    public DomainEntity(DirectoryBackend directoryBackend, String description) {

        super(directoryBackend.getId());
        this.description = description;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {

        return description;
    }

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public EntityType getEntityType() {

        return EntityType.DOMAIN;
    }
}
