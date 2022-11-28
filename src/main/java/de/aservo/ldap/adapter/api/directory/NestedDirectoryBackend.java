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

package de.aservo.ldap.adapter.api.directory;

import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.entity.MembershipEntity;

import java.util.function.Supplier;


/**
 * The interface for all nested directory backends.
 */
public interface NestedDirectoryBackend
        extends DirectoryBackend {

    /**
     * Check if the caches need a reset.
     *
     * @param block the supplier object executed within the session
     * @return the result of the supplier code block
     */
    default <T> T withReadAccess(Supplier<T> block) {

        return block.get();
    }

    /**
     * Check if the caches need a reset.
     *
     * @param block the runnable object executed within the session
     */
    default void withReadAccess(Runnable block) {

        withReadAccess(() -> {

            block.run();
            return null;
        });
    }

    /**
     * Check if the caches need a reset.
     *
     * @param block the supplier object executed within the session
     * @return the result of the supplier code block
     */
    default <T> T withWriteAccess(Supplier<T> block) {

        return block.get();
    }

    /**
     * Check if the caches need a reset.
     *
     * @param block the runnable object executed within the session
     */
    default void withWriteAccess(Runnable block) {

        withWriteAccess(() -> {

            block.run();
            return null;
        });
    }

    /**
     * Check if the caches need a reset.
     *
     * @return the boolean
     */
    default boolean requireReset() {

        return false;
    }

    /**
     * To keep the cache up-to-date a group entity is updated or inserted.
     *
     * @param id the group ID
     */
    default void upsertGroup(String id) {
    }

    /**
     * To keep the cache up-to-date all group entities are updated or inserted.
     *
     * @param startIndex the start index for pagination
     * @param maxResults the maximum number of results for pagination
     * @return the number of handled entities
     */
    default int upsertAllGroups(int startIndex, int maxResults) {

        return 0;
    }

    /**
     * To keep the cache up-to-date all group entities are updated or inserted.
     *
     * @return the number of handled entities
     */
    default int upsertAllGroups() {

        return 0;
    }

    /**
     * To keep the cache up-to-date an user entity is updated or inserted.
     *
     * @param id the user ID
     */
    default void upsertUser(String id) {
    }

    /**
     * To keep the cache up-to-date an user entity is updated or inserted.
     * The user inherits the groups of another user
     *
     * @param id the user ID
     * @param idOther the user ID of an existing user
     */
    default void upsertUser(String id, String idOther) {
    }

    /**
     * To keep the cache up-to-date all user entities are updated or inserted.
     *
     * @param startIndex the start index for pagination
     * @param maxResults the maximum number of results for pagination
     * @return the number of handled entities
     */
    default int upsertAllUsers(int startIndex, int maxResults) {

        return 0;
    }

    /**
     * To keep the cache up-to-date all user entities are updated or inserted.
     *
     * @return the number of handled entities
     */
    default int upsertAllUsers() {

        return 0;
    }

    /**
     * To keep the cache up-to-date a membership entity is updated or inserted.
     *
     * @param membership the membership entity
     */
    default void upsertMembership(MembershipEntity membership) {
    }

    /**
     * To keep the cache up-to-date a group entity is dropped.
     *
     * @param id the group ID
     */
    default void dropGroup(String id) {
    }

    /**
     * To reset the cache all group entities are dropped.
     */
    default void dropAllGroups() {
    }

    /**
     * To keep the cache up-to-date an user entity is dropped.
     *
     * @param id the user ID
     */
    default void dropUser(String id) {
    }

    /**
     * To reset the cache all user entities are dropped.
     */
    default void dropAllUsers() {
    }

    /**
     * To keep the cache up-to-date a membership entity is dropped.
     *
     * @param membership the membership entity
     */
    default void dropMembership(MembershipEntity membership) {
    }

    /**
     * Returns a cursor for membership entities.
     *
     * @return the cursor with membership elements
     */
    MappableCursor<MembershipEntity> getMemberships();
}
