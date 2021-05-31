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

package com.aservo.ldap.adapter.backend;

import com.aservo.ldap.adapter.api.entity.MembershipEntity;
import java.util.List;
import java.util.function.Supplier;
import javax.naming.OperationNotSupportedException;
import org.apache.commons.lang3.tuple.Pair;


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
     * Gets all direct group relationships.
     *
     * @return the transitive group relationships
     */
    default List<Pair<String, String>> getAllDirectGroupRelationships() {

        throw new RuntimeException(new OperationNotSupportedException(
                "The method getAllDirectGroupRelationships is not supported."));
    }

    /**
     * Gets all direct user relationships.
     *
     * @return the transitive user relationships
     */
    default List<Pair<String, String>> getAllDirectUserRelationships() {

        throw new RuntimeException(new OperationNotSupportedException(
                "The method getAllDirectUserRelationships is not supported."));
    }

    /**
     * Gets all transitive group relationships.
     *
     * @return the transitive group relationships
     */
    default List<Pair<String, String>> getAllTransitiveGroupRelationships() {

        throw new RuntimeException(new OperationNotSupportedException(
                "The method getAllTransitiveGroupRelationships is not supported."));
    }

    /**
     * Gets all transitive user relationships.
     *
     * @return the transitive user relationships
     */
    default List<Pair<String, String>> getAllTransitiveUserRelationships() {

        throw new RuntimeException(new OperationNotSupportedException(
                "The method getAllTransitiveUserRelationships is not supported."));
    }

    /**
     * Returns an iterable sequence of membership entities.
     *
     * @return the iterable object with membership elements
     */
    default Iterable<MembershipEntity> getMemberships() {

        throw new RuntimeException(new OperationNotSupportedException(
                "The method getMemberships is not supported."));
    }
}
