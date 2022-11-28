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

package de.aservo.ldap.adapter.backend;

import de.aservo.ldap.adapter.ServerConfiguration;
import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.directory.NestedDirectoryBackend;
import de.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import de.aservo.ldap.adapter.api.entity.EntityType;
import de.aservo.ldap.adapter.api.entity.GroupEntity;
import de.aservo.ldap.adapter.api.entity.MembershipEntity;
import de.aservo.ldap.adapter.api.entity.UserEntity;
import de.aservo.ldap.adapter.api.query.QueryExpression;
import org.apache.directory.api.ldap.model.schema.SchemaManager;

import java.util.Set;
import java.util.function.Supplier;


public abstract class ProxyDirectoryBackend
        implements NestedDirectoryBackend {

    protected final ServerConfiguration config;
    protected final NestedDirectoryBackend directoryBackend;

    protected ProxyDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        this.config = config;
        this.directoryBackend = directoryBackend;
    }

    @Override
    public <T> T withReadAccess(Supplier<T> block) {

        return directoryBackend.withReadAccess(block);
    }

    @Override
    public void withReadAccess(Runnable block) {

        directoryBackend.withReadAccess(block);
    }

    @Override
    public <T> T withWriteAccess(Supplier<T> block) {

        return directoryBackend.withWriteAccess(block);
    }

    @Override
    public void withWriteAccess(Runnable block) {

        directoryBackend.withWriteAccess(block);
    }

    @Override
    public String getId() {

        return directoryBackend.getId();
    }

    @Override
    public void startup() {

        directoryBackend.startup();
    }

    @Override
    public void shutdown() {

        directoryBackend.shutdown();
    }

    @Override
    public boolean requireReset() {

        return directoryBackend.requireReset();
    }

    @Override
    public void upsertGroup(String id) {

        directoryBackend.upsertGroup(id);
    }

    @Override
    public int upsertAllGroups(int startIndex, int maxResults) {

        return directoryBackend.upsertAllGroups(startIndex, maxResults);
    }

    @Override
    public int upsertAllGroups() {

        return directoryBackend.upsertAllGroups();
    }

    @Override
    public void upsertUser(String id) {

        directoryBackend.upsertUser(id);
    }

    @Override
    public void upsertUser(String id, String idOther) {

        directoryBackend.upsertUser(id, idOther);
    }

    @Override
    public int upsertAllUsers(int startIndex, int maxResults) {

        return directoryBackend.upsertAllUsers(startIndex, maxResults);
    }

    @Override
    public int upsertAllUsers() {

        return directoryBackend.upsertAllUsers();
    }

    @Override
    public void upsertMembership(MembershipEntity membership) {

        directoryBackend.upsertMembership(membership);
    }

    @Override
    public void dropGroup(String id) {

        directoryBackend.dropGroup(id);
    }

    @Override
    public void dropAllGroups() {

        directoryBackend.dropAllGroups();
    }

    @Override
    public void dropUser(String id) {

        directoryBackend.dropUser(id);
    }

    @Override
    public void dropAllUsers() {

        directoryBackend.dropAllUsers();
    }

    @Override
    public void dropMembership(MembershipEntity membership) {

        directoryBackend.dropMembership(membership);
    }

    @Override
    public MappableCursor<Row> runQueryExpression(String txId, SchemaManager schemaManager, QueryExpression expression,
                                                  EntityType entityType) {

        return directoryBackend.runQueryExpression(txId, schemaManager, expression, entityType);
    }

    @Override
    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getGroup(id);
    }

    @Override
    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getUser(id);
    }

    @Override
    public UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException {

        return directoryBackend.getAuthenticatedUser(id, password);
    }

    @Override
    public Set<GroupEntity> getAllGroups() {

        return directoryBackend.getAllGroups();
    }

    @Override
    public Set<GroupEntity> getAllGroups(int startIndex, int maxResults) {

        return directoryBackend.getAllGroups(startIndex, maxResults);
    }

    @Override
    public Set<UserEntity> getAllUsers() {

        return directoryBackend.getAllUsers();
    }

    @Override
    public Set<UserEntity> getAllUsers(int startIndex, int maxResults) {

        return directoryBackend.getAllUsers(startIndex, maxResults);
    }

    @Override
    public Set<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectUsersOfGroup(id);
    }

    @Override
    public Set<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectGroupsOfUser(id);
    }

    @Override
    public Set<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveUsersOfGroup(id);
    }

    @Override
    public Set<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveGroupsOfUser(id);
    }

    @Override
    public Set<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectChildGroupsOfGroup(id);
    }

    @Override
    public Set<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectParentGroupsOfGroup(id);
    }

    @Override
    public Set<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveChildGroupsOfGroup(id);
    }

    @Override
    public Set<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveParentGroupsOfGroup(id);
    }

    @Override
    public MappableCursor<MembershipEntity> getMemberships() {

        return directoryBackend.getMemberships();
    }
}
