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

import com.google.common.collect.Lists;
import com.google.gson.*;
import de.aservo.ldap.adapter.ServerConfiguration;
import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.directory.NestedDirectoryBackend;
import de.aservo.ldap.adapter.api.entity.MembershipEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;


public class MirroredCrowdDirectoryBackend
        extends ProxyDirectoryBackend {

    /**
     * The constant CONFIG_APP_NAME.
     */
    public static final String CONFIG_APP_NAME = "application.name";
    /**
     * The constant CONFIG_REST_USERNAME.
     */
    public static final String CONFIG_REST_USERNAME = "rest.username";
    /**
     * The constant CONFIG_REST_USER_PW.
     */
    public static final String CONFIG_REST_USER_PW = "rest.user-password";
    /**
     * The constant CONFIG_REST_BASE_URL.
     */
    public static final String CONFIG_REST_BASE_URL = "rest.base-url";
    /**
     * The constant CONFIG_SYNC_PAGE_SIZE.
     */
    public static final String CONFIG_SYNC_PAGE_SIZE = "mirror.sync.page-size";
    /**
     * The constant CONFIG_AUDIT_LOG_PAGE_SIZE.
     */
    public static final String CONFIG_AUDIT_LOG_PAGE_SIZE = "mirror.audit-log.page-size";
    /**
     * The constant CONFIG_AUDIT_LOG_PAGE_LIMIT.
     */
    public static final String CONFIG_AUDIT_LOG_PAGE_LIMIT = "mirror.audit-log.page-limit";
    /**
     * The constant CONFIG_SYNC_INITIAL_DELAY.
     */
    public static final String CONFIG_SYNC_INITIAL_DELAY = "mirror.sync.initialdelay";
    /**
     * The constant CONFIG_SYNC_INITIAL_PERIOD.
     */
    public static final String CONFIG_SYNC_PERIOD = "mirror.sync.period";
    /**
     * The constant CONFIG_SYNC_USEDBLOCK.
     */
    public static final String CONFIG_SYNC_USEDBLOCK = "mirror.sync.usebdlock";
    /**
     * The constant CONFIG_SYNC_LOCKID.
     */
    public static final String CONFIG_SYNC_LOCKID = "mirror.sync.lockid";
    /**
     * The constant CONFIG_FORCE_FULL_SYNC_ON_BOOT.
     */
    public static final String CONFIG_FORCE_FULL_SYNC_ON_BOOT = "mirror.force-full-sync-on-boot";

    private final Logger logger = LoggerFactory.getLogger(MirroredCrowdDirectoryBackend.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final MirrorStrategy mirrorStrategy;
    private final AuditLogProcessor auditLogProcessor;

    /**
     * Instantiates a new directory backend.
     *
     * @param config           config the config instance of the server
     * @param directoryBackend the directory backend
     */
    public MirroredCrowdDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        super(config, directoryBackend);

        Properties properties = config.getBackendProperties();

        String appName = properties.getProperty(CONFIG_APP_NAME);
        String restUsername = properties.getProperty(CONFIG_REST_USERNAME);
        String restUserPassword = properties.getProperty(CONFIG_REST_USER_PW);
        String restBaseUrl = properties.getProperty(CONFIG_REST_BASE_URL);
        String syncPageSizeValue = properties.getProperty(CONFIG_SYNC_PAGE_SIZE);
        String auditLogPageSizeValue = properties.getProperty(CONFIG_AUDIT_LOG_PAGE_SIZE);
        String auditLogPageLimitValue = properties.getProperty(CONFIG_AUDIT_LOG_PAGE_LIMIT);
        String syncInitialDelayValue = properties.getProperty(CONFIG_SYNC_INITIAL_DELAY);
        String syncPeriodValue = properties.getProperty(CONFIG_SYNC_PERIOD);
        String syncUseDblockValue = properties.getProperty(CONFIG_SYNC_USEDBLOCK);
        String syncLockIdValue = properties.getProperty(CONFIG_SYNC_LOCKID);
        String forceFullSyncOnBootValue = properties.getProperty(CONFIG_FORCE_FULL_SYNC_ON_BOOT);

        if (appName == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_APP_NAME);

        if (restUsername == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_REST_USERNAME);

        if (restUserPassword == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_REST_USER_PW);

        if (restBaseUrl == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_REST_BASE_URL);

        if (syncPageSizeValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_SYNC_PAGE_SIZE);

        if (auditLogPageSizeValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_AUDIT_LOG_PAGE_SIZE);

        if (auditLogPageLimitValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_AUDIT_LOG_PAGE_LIMIT);

        if (syncInitialDelayValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_SYNC_INITIAL_DELAY);

        if (syncPeriodValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_SYNC_PERIOD);

        if (syncUseDblockValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_SYNC_USEDBLOCK);

        if (syncLockIdValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_SYNC_LOCKID);

        if (forceFullSyncOnBootValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_FORCE_FULL_SYNC_ON_BOOT);

        int syncPageSize = Integer.parseInt(syncPageSizeValue);
        int auditLogPageSize = Integer.parseInt(auditLogPageSizeValue);
        int auditLogPageLimit = Integer.parseInt(auditLogPageLimitValue);
        int syncInitialDelay = Integer.parseInt(syncInitialDelayValue);
        int syncPeriod = Integer.parseInt(syncPeriodValue);
        boolean syncUseDblock = Boolean.parseBoolean(syncUseDblockValue);
        int syncLockId = Integer.parseInt(syncLockIdValue);
        boolean forceFullSyncOnBoot = Boolean.parseBoolean(forceFullSyncOnBootValue);

        if (syncPageSize < 1)
            throw new IllegalArgumentException("The page size cannot be less than one.");

        if (auditLogPageSize < 1)
            throw new IllegalArgumentException("The page limit cannot be less than one.");

        if (auditLogPageLimit < 1)
            throw new IllegalArgumentException("The page limit cannot be less than one.");

        auditLogProcessor =
                new AuditLogProcessor(appName, restUsername, restUserPassword, restBaseUrl,
                        auditLogPageLimit, auditLogPageSize);

        mirrorStrategy = new MirrorStrategy(syncPageSize, forceFullSyncOnBoot, syncInitialDelay, syncPeriod, syncUseDblock, syncLockId);
    }

    @Override
    public void startup() {

        super.startup();

        scheduler.scheduleAtFixedRate(mirrorStrategy, mirrorStrategy.syncInitialDelay, mirrorStrategy.syncPeriod, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {

        scheduler.shutdown();

        try {

            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS))
                scheduler.shutdownNow();

        } catch (InterruptedException e) {

            logger.error("Could not complete all synchronization tasks.", e);

            Thread.currentThread().interrupt();
        }

        super.shutdown();
    }

    private enum UpdateType {

        GROUP_VALIDATE, GROUP_INVALIDATE, USER_VALIDATE, USER_INVALIDATE, MEMBERSHIP_VALIDATE, MEMBERSHIP_INVALIDATE;
    }

    private enum AuditLogState {

        FULL_UPDATE_REQUIRED, DELTA_UPDATE_REQUIRED, UP_TO_DATE, CON_ISSUE, UNDEFINED,
    }

    private enum SyncState {

        NO_SYNC, FOREIGN_SYNC, SYNC_START, SYNC_STOP, SYNC_COMPLETE,
    }

    private enum AuditLogEntry {

        SYNC_START, SYNC_STOP, SYNC_COMPLETE
    }

    @Override
    public <T> T withReadAccess(Supplier<T> block) {

        try {

            latch.await();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        return super.withReadAccess(block);
    }

    @Override
    public void withReadAccess(Runnable block) {

        withReadAccess(() -> {

            block.run();
            return null;
        });
    }

    @Override
    public <T> T withWriteAccess(Supplier<T> block) {

        try {

            latch.await();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        return super.withWriteAccess(block);
    }

    @Override
    public void withWriteAccess(Runnable block) {

        withWriteAccess(() -> {

            block.run();
            return null;
        });
    }

    private class MirrorStrategy
            implements Runnable {

        private final int pageSize;
        private boolean forceFullSync;
        private boolean resetToggle = false;
        private int syncInitialDelay;
        private int syncPeriod;
        private boolean syncUseDblock = false;
        private int syncLockId;

        public MirrorStrategy(int pageSize, boolean forceFullSync, int syncInitialDelay, int syncPeriod,
                              boolean syncUseDblock, int syncLockId) {

            this.pageSize = pageSize;
            this.forceFullSync = forceFullSync;
            this.syncInitialDelay = syncInitialDelay;
            this.syncPeriod = syncPeriod;
            this.syncUseDblock = syncUseDblock;
            this.syncLockId = syncLockId;
        }

        public void run() {

            directoryBackend.withWriteAccess(() -> {
                boolean gotLock = false;
                try {

                    if (syncUseDblock) {

                        logger.info("Trying to acquire syncdblock.");

                        gotLock = directoryBackend.acquireDbLock(this.syncLockId);
                        if (!gotLock) {
                            logger.info("Did not get the syncdblock, run canceled.");
                            return;
                        }
                        logger.info("Successfully acquired syncdblock.");

                    }
                    if (directoryBackend.requireReset() && !resetToggle) {

                        forceFullSync = true;
                        resetToggle = true;
                    }

                    if (forceFullSync) {

                        logger.info("Start forced synchronization of a full copy.");
                        performFullUpdate();
                        logger.info("End forced synchronization of a full copy.");

                        forceFullSync = false;
                        latch.countDown();
                        return;
                    }

                    AuditLogState state = auditLogProcessor.getAuditLogState(true);

                    if (state.equals(AuditLogState.FULL_UPDATE_REQUIRED)) {

                        logger.info("Start synchronization of a full copy.");
                        performFullUpdate();
                        logger.info("End synchronization of a full copy.");

                    } else if (state.equals(AuditLogState.DELTA_UPDATE_REQUIRED)) {

                        logger.info("Start incremental synchronization.");
                        performDeltaUpdate();
                        logger.info("End incremental synchronization.");
                    }

                    latch.countDown();

                } catch (Exception e) {

                    logger.error("An error occurred during synchronization.", e);
                } finally {
                    if (syncUseDblock && gotLock) {
                        directoryBackend.releaseDbLock(this.syncLockId);
                    }
                }
            });
        }

        private void performFullUpdate() {

            auditLogProcessor.updateConcurrent(() -> {

                directoryBackend.dropAllGroups();
                directoryBackend.dropAllUsers();

                MappableCursor<MembershipEntity> memberships = directoryBackend.getMemberships();
                int groupPage = 0;
                int userPage = 0;
                int groupCount = 0;
                int userCount = 0;

                while (groupPage != -1 || userPage != -1) {

                    if (groupPage >= 0) {

                        int groups = directoryBackend.upsertAllGroups(groupPage++ * pageSize, pageSize);
                        groupCount += groups;
                        if ( groups < pageSize)
                            groupPage = -1;
                    }

                    if (userPage >= 0) {

                        int users = directoryBackend.upsertAllUsers(userPage++ * pageSize, pageSize); 
                        userCount += users;
                        if (users < pageSize)
                            userPage = -1;
                    }
                }
                logger.info("FullUpdate: received {} users and {} groups", userCount, groupCount);

                while (memberships.next())
                    directoryBackend.upsertMembership(memberships.get());

                return false;
            });
        }

        private void performDeltaUpdate() {

            List<Pair<UpdateType, Object>> deltaUpdateList = new LinkedList<>();

            AuditLogState state = auditLogProcessor.updateConcurrent(() -> {

                boolean committed = false;
                boolean lastPageDone = false;
                int page = 0;

                deltaUpdateList.clear();

                while (!lastPageDone) {

                    JsonObject result;

                    try {

                        result = auditLogProcessor.queryAuditLog(page, pageSize);
                        page++;

                    } catch (IOException e) {

                        logger.error("Cannot call REST endpoint to query audit log for delta update.", e);

                        return true;
                    }

                    lastPageDone = result.getAsJsonObject().get("isLastPage").getAsBoolean();

                    for (JsonElement valueElement : result.getAsJsonArray("values")) {

                        String eventType = valueElement.getAsJsonObject().get("eventType").getAsString();
                        SyncState syncState = auditLogProcessor.getSynchronizationState(valueElement);

                        if (syncState == SyncState.SYNC_COMPLETE) {

                            committed = true;

                        } else if (syncState == SyncState.SYNC_STOP && committed) {

                            lastPageDone = true;
                            break;

                        } else if (syncState == SyncState.NO_SYNC) {

                            if (eventType.matches("(GROUP|USER)_(CREATED|UPDATED|DELETED)")) {

                                for (JsonElement entity : valueElement.getAsJsonObject().getAsJsonArray("entities")) {

                                    String type = entity.getAsJsonObject().get("type").getAsString();
                                    String name = entity.getAsJsonObject().get("name").getAsString();

                                    if (type.equals("GROUP")) {

                                        if (eventType.equals("GROUP_CREATED") || eventType.equals("GROUP_UPDATED"))
                                            deltaUpdateList.add(Pair.of(UpdateType.GROUP_VALIDATE, name));
                                        else if (eventType.equals("GROUP_DELETED"))
                                            deltaUpdateList.add(Pair.of(UpdateType.GROUP_INVALIDATE, name));

                                    } else if (type.equals("USER")) {

                                        String alias = auditLogProcessor.resolveToAlias(name);

                                        if (eventType.equals("USER_UPDATED")) {

                                            Optional<JsonObject> property =
                                                    StreamSupport.stream(valueElement.getAsJsonObject().getAsJsonArray("entries").spliterator(), false)
                                                            .map(JsonElement::getAsJsonObject)
                                                            .filter(x -> x.get("propertyName").getAsString().equalsIgnoreCase("username"))
                                                            .findAny();

                                            // username is changed
                                            property.ifPresent(x -> {

                                                String nameOld = x.get("oldValue").getAsString();
                                                String aliasOld = auditLogProcessor.resolveToAlias(nameOld);

                                                // remove the old user entity
                                                deltaUpdateList.add(Pair.of(UpdateType.USER_INVALIDATE, aliasOld));

                                                // add the new user entity
                                                deltaUpdateList.add(Pair.of(UpdateType.USER_VALIDATE, Pair.of(alias, aliasOld)));
                                            });

                                            if (property.isEmpty())
                                                deltaUpdateList.add(Pair.of(UpdateType.USER_VALIDATE, alias));

                                        } else if (eventType.equals("USER_CREATED"))
                                            deltaUpdateList.add(Pair.of(UpdateType.USER_VALIDATE, alias));
                                        else if (eventType.equals("USER_DELETED"))
                                            deltaUpdateList.add(Pair.of(UpdateType.USER_INVALIDATE, alias));
                                    }
                                }

                            } else if (eventType.matches("(ADDED_TO|REMOVED_FROM)_GROUP")) {

                                String parentGroupId = null;
                                Set<String> childGroupIds = new HashSet<>();
                                Set<String> userIds = new HashSet<>();

                                for (JsonElement entity : valueElement.getAsJsonObject().getAsJsonArray("entities")) {

                                    String type = entity.getAsJsonObject().get("type").getAsString();
                                    String name = entity.getAsJsonObject().get("name").getAsString();
                                    boolean primary = entity.getAsJsonObject().get("primary").getAsBoolean();

                                    if (primary && type.equals("GROUP"))
                                        parentGroupId = name;
                                    else if (type.equals("GROUP"))
                                        childGroupIds.add(name);
                                    else if (type.equals("USER"))
                                        userIds.add(auditLogProcessor.resolveToAlias(name));
                                }

                                if (parentGroupId == null)
                                    logger.warn("Cannot find parent group to create membership object.");
                                else {

                                    MembershipEntity membership =
                                            new MembershipEntity(parentGroupId, childGroupIds, userIds);

                                    if (eventType.equals("ADDED_TO_GROUP"))
                                        deltaUpdateList.add(Pair.of(UpdateType.MEMBERSHIP_VALIDATE, membership));
                                    else if (eventType.equals("REMOVED_FROM_GROUP"))
                                        deltaUpdateList.add(Pair.of(UpdateType.MEMBERSHIP_INVALIDATE, membership));
                                }
                            }
                        }
                    }
                }

                return false;
            });

            if (state.equals(AuditLogState.CON_ISSUE))
                return;

            downloadEntities(deltaUpdateList);
        }

        private void downloadEntities(List<Pair<UpdateType, Object>> deltaUpdateList) {

            for (Pair<UpdateType, Object> x : Lists.reverse(deltaUpdateList)) {

                if (x.getLeft().equals(UpdateType.GROUP_VALIDATE) &&
                        x.getRight() instanceof String) {

                    directoryBackend.upsertGroup(((String) x.getRight()).toLowerCase());

                } else if (x.getLeft().equals(UpdateType.GROUP_INVALIDATE) &&
                        x.getRight() instanceof String) {

                    directoryBackend.dropGroup(((String) x.getRight()).toLowerCase());

                } else if (x.getLeft().equals(UpdateType.USER_VALIDATE) &&
                        x.getRight() instanceof String) {

                    directoryBackend.upsertUser(((String) x.getRight()).toLowerCase());

                } else if (x.getLeft().equals(UpdateType.USER_VALIDATE) &&
                        x.getRight() instanceof Pair) {

                    String newName = ((Pair<?, ?>) x.getRight()).getLeft().toString();
                    String oldName = ((Pair<?, ?>) x.getRight()).getRight().toString();

                    directoryBackend.upsertUser(newName.toLowerCase(), oldName.toLowerCase());

                } else if (x.getLeft().equals(UpdateType.USER_INVALIDATE) &&
                        x.getRight() instanceof String) {

                    directoryBackend.dropUser(((String) x.getRight()).toLowerCase());

                } else if (x.getLeft().equals(UpdateType.MEMBERSHIP_VALIDATE) &&
                        x.getRight() instanceof MembershipEntity) {

                    directoryBackend.upsertMembership((MembershipEntity) x.getRight());

                } else if (x.getLeft().equals(UpdateType.MEMBERSHIP_INVALIDATE) &&
                        x.getRight() instanceof MembershipEntity) {

                    directoryBackend.dropMembership((MembershipEntity) x.getRight());
                }
            }
        }
    }

    private class AuditLogProcessor {

        private final Random random = new Random();
        private final String appName;
        private final String restUsername;
        private final String restUserPassword;
        private final String restBaseUrl;
        private final int pageLimit;
        private final int pageSize;

        public AuditLogProcessor(String appName, String restUsername, String restUserPassword, String restBaseUrl,
                                 int pageLimit, int pageSize) {

            this.appName = appName;
            this.restUsername = restUsername;
            this.restUserPassword = restUserPassword;
            this.restBaseUrl = restBaseUrl;
            this.pageLimit = pageLimit;
            this.pageSize = pageSize;
        }

        public AuditLogState updateConcurrent(Supplier<Boolean> supplier) {

            boolean requireRetry = true;
            boolean aborted = false;

            while (requireRetry && !aborted) {

                requireRetry = false;

                setSynchronizationMarker(AuditLogEntry.SYNC_START);
                aborted = supplier.get();

                if (!aborted) {

                    setSynchronizationMarker(AuditLogEntry.SYNC_STOP);

                    AuditLogState state = getAuditLogState(false);

                    if (state.equals(AuditLogState.CON_ISSUE))
                        return AuditLogState.CON_ISSUE;

                    if (state.equals(AuditLogState.UP_TO_DATE)) {

                        setSynchronizationMarker(AuditLogEntry.SYNC_COMPLETE);

                    } else {

                        logger.info("Retry synchronization.");
                        requireRetry = true;

                        waitBackoff(1000, 3000);
                    }
                }
            }

            if (aborted)
                return AuditLogState.UNDEFINED;

            return AuditLogState.UP_TO_DATE;
        }

        public AuditLogState getAuditLogState(boolean expectCommitted) {

            return repeatableRead(() -> {

                boolean committed = !expectCommitted;
                boolean changesFound = false;
                boolean startedMarkerFound = false;
                boolean finishedMarkerFound = false;
                boolean lastPageDone = false;
                int page = 0;

                while (!lastPageDone && page < pageLimit) {

                    JsonObject result;

                    try {

                        result = auditLogProcessor.queryAuditLog(page, pageSize);
                        page++;

                    } catch (IOException e) {

                        logger.error("Cannot call REST endpoint to query audit log for pagination.", e);

                        return AuditLogState.CON_ISSUE;
                    }

                    lastPageDone = result.getAsJsonObject().get("isLastPage").getAsBoolean();

                    for (JsonElement valueElement : result.getAsJsonArray("values")) {

                        SyncState syncState = getSynchronizationState(valueElement);

                        if (syncState == SyncState.SYNC_COMPLETE) {

                            committed = true;
                            continue;

                        } else if (syncState == SyncState.NO_SYNC) {

                            changesFound = true;
                            startedMarkerFound = false;
                            finishedMarkerFound = false;
                            continue;

                        } else if (syncState == SyncState.FOREIGN_SYNC || !committed)
                            continue;

                        if (syncState == SyncState.SYNC_START) {

                            startedMarkerFound = true;

                        } else if (syncState == SyncState.SYNC_STOP) {

                            startedMarkerFound = false;
                            finishedMarkerFound = true;
                        }

                        if (startedMarkerFound && finishedMarkerFound) {

                            if (changesFound)
                                return AuditLogState.DELTA_UPDATE_REQUIRED;
                            else
                                return AuditLogState.UP_TO_DATE;
                        }
                    }
                }

                return AuditLogState.UNDEFINED;
            });
        }

        public SyncState getSynchronizationState(JsonElement valueElement) {

            String eventType = valueElement.getAsJsonObject().get("eventType").getAsString();

            if (eventType.matches("(SYNCHRONIZATION_(STARTED|FINISHED))|(COMPLETED)")) {

                JsonArray entities = valueElement.getAsJsonObject().getAsJsonArray("entities");

                if (entities.size() != 1)
                    return SyncState.FOREIGN_SYNC;

                JsonObject author = valueElement.getAsJsonObject().getAsJsonObject("author");
                JsonObject entity = entities.get(0).getAsJsonObject();

                if (!author.get("name").getAsString().equals(appName))
                    return SyncState.FOREIGN_SYNC;

                if (!author.get("type").getAsString().equals("APPLICATION"))
                    return SyncState.FOREIGN_SYNC;

                if (!entity.get("name").getAsString().equals("synchronization"))
                    return SyncState.FOREIGN_SYNC;

                if (!entity.get("type").getAsString().equals("APPLICATION"))
                    return SyncState.FOREIGN_SYNC;

                if (eventType.equals("SYNCHRONIZATION_STARTED"))
                    return SyncState.SYNC_START;

                if (eventType.equals("SYNCHRONIZATION_FINISHED"))
                    return SyncState.SYNC_STOP;

                if (eventType.equals("COMPLETED"))
                    return SyncState.SYNC_COMPLETE;
            }

            return SyncState.NO_SYNC;
        }

        private AuditLogState repeatableRead(Supplier<AuditLogState> supplier) {

            boolean requireRetry = true;
            Long auditLogId;

            try {

                auditLogId = getLastAuditLogId();

                if (auditLogId == null)
                    return AuditLogState.FULL_UPDATE_REQUIRED;

                while (requireRetry) {

                    requireRetry = false;

                    AuditLogState result = supplier.get();

                    if (!result.equals(AuditLogState.UNDEFINED))
                        return result;

                    Long currentAuditLogId = getLastAuditLogId();

                    if (currentAuditLogId == null)
                        return AuditLogState.FULL_UPDATE_REQUIRED;

                    if (!auditLogId.equals(currentAuditLogId)) {

                        auditLogId = currentAuditLogId;
                        requireRetry = true;
                    }

                    waitBackoff(1000, 2000);
                }

            } catch (IOException e) {

                logger.error("Cannot call REST endpoint to query audit log for last entry.", e);

                return AuditLogState.CON_ISSUE;
            }

            return AuditLogState.FULL_UPDATE_REQUIRED;
        }

        private void waitBackoff(int minMillis, int maxMillis) {

            if (minMillis >= maxMillis)
                throw new IllegalArgumentException("Expect maximum greater than minimum.");

            if (minMillis <= 0)
                throw new IllegalArgumentException("Expect minimum greater than zero.");

            int duration = random.nextInt((maxMillis - minMillis) + 1) + minMillis;

            logger.debug("Waiting a backoff time of {} milliseconds.", duration);

            try {

                Thread.sleep(1000);

            } catch (InterruptedException e) {

                logger.error("The backoff waiting time was interrupted.", e);

                Thread.currentThread().interrupt();
            }
        }

        private void setSynchronizationMarker(AuditLogEntry entry) {

            JsonObject node = new JsonObject();
            JsonObject author = new JsonObject();

            if (entry == AuditLogEntry.SYNC_START) {

                node.add("eventType", new JsonPrimitive("SYNCHRONIZATION_STARTED"));
                node.add("eventMessage", new JsonPrimitive("Started synchronization with application"));

            } else if (entry == AuditLogEntry.SYNC_STOP) {

                node.add("eventType", new JsonPrimitive("SYNCHRONIZATION_FINISHED"));
                node.add("eventMessage", new JsonPrimitive("Finished synchronization with application"));

            } else if (entry == AuditLogEntry.SYNC_COMPLETE) {

                node.add("eventType", new JsonPrimitive("COMPLETED"));
                node.add("eventMessage", new JsonPrimitive("Committed last synchronization with application"));
            }

            node.add("entityType", new JsonPrimitive("APPLICATION"));
            node.add("entityName", new JsonPrimitive("synchronization"));
            node.add("author", author);

            author.add("name", new JsonPrimitive(appName));
            author.add("type", new JsonPrimitive("APPLICATION"));

            String route = "/rest/admin/1.0/auditlog";

            try {

                postRestApi(route, node, false);

            } catch (IOException e) {

                logger.error("Cannot call REST endpoint to query audit log.", e);
                throw new UncheckedIOException(e);
            }
        }

        private Long getLastAuditLogId()
                throws IOException {

            JsonObject node = new JsonObject();
            JsonArray actions = new JsonArray();

            node.add("actions", actions);
            actions.add("USER_CREATED");
            actions.add("USER_UPDATED");
            actions.add("USER_DELETED");
            actions.add("GROUP_CREATED");
            actions.add("GROUP_UPDATED");
            actions.add("GROUP_DELETED");
            actions.add("ADDED_TO_GROUP");
            actions.add("REMOVED_FROM_GROUP");

            String queryString = "?start=0&limit=1";
            String route = "/rest/admin/1.0/auditlog/query" + queryString;

            JsonObject result = postRestApi(route, node, true).get();
            JsonArray array = result.getAsJsonArray("values");

            if (array.size() != 1)
                return null;

            return array.get(0).getAsJsonObject().get("id").getAsLong();
        }

        private JsonObject queryAuditLog(int page, int pageSize)
                throws IOException {

            JsonObject node = new JsonObject();
            JsonArray actions = new JsonArray();

            node.add("actions", actions);
            actions.add("USER_CREATED");
            actions.add("USER_UPDATED");
            actions.add("USER_DELETED");
            actions.add("GROUP_CREATED");
            actions.add("GROUP_UPDATED");
            actions.add("GROUP_DELETED");
            actions.add("ADDED_TO_GROUP");
            actions.add("REMOVED_FROM_GROUP");
            actions.add("SYNCHRONIZATION_STARTED");
            actions.add("SYNCHRONIZATION_FINISHED");
            actions.add("COMPLETED");

            String queryString = "?start=" + (page * pageSize) + "&limit=" + pageSize;
            String route = "/rest/admin/1.0/auditlog/query" + queryString;

            return postRestApi(route, node, true).get();
        }

        public String resolveToAlias(String username) {

            JsonObject applications;
            JsonObject aliases;
            String appId = null;

            try {

                applications = getRestApi("/rest/appmanagement/1/application", true).get();
                aliases = getRestApi("/rest/appmanagement/1/aliases?user=" + username, true).get();

            } catch (IOException e) {

                throw new UncheckedIOException(e);
            }

            for (JsonElement element : applications.getAsJsonObject().getAsJsonArray("applications")) {

                JsonObject entry = element.getAsJsonObject().getAsJsonObject("ApplicationEntity");

                String id = entry.get("id").getAsString();
                String name = entry.get("name").getAsString();

                if (name.equals(appName))
                    appId = id;
            }

            if (appId == null || !aliases.getAsJsonObject().has(appId))
                return username;

            String alias = aliases.getAsJsonObject().get(appId).getAsString();

            logger.debug("Resolve username {} to alias {}: ", username, alias);

            return alias;
        }

        private Optional<JsonObject> getRestApi(String route, boolean expectResult)
                throws IOException {

            CloseableHttpClient httpclient =
                    HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

            Gson gson = new Gson();

            String credentials =
                    new String(Base64.getEncoder().encode((restUsername + ":" + restUserPassword)
                            .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

            HttpGet request = new HttpGet(restBaseUrl + route);

            request.setHeader("Authorization", "Basic " + credentials);
            request.setHeader("Accept", "application/json");

            HttpResponse response = httpclient.execute(request);

            if (!expectResult)
                return Optional.empty();

            String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name());

            try {

                return Optional.of(gson.fromJson(result, JsonObject.class));

            } catch (JsonSyntaxException e) {

                logger.error("Cannot parse JSON object. Status code: {}; Result:\n {}",
                        response.getStatusLine().getStatusCode(), result);

                throw e;
            }
        }

        private Optional<JsonObject> postRestApi(String route, JsonObject node, boolean expectResult)
                throws IOException {

            CloseableHttpClient httpclient =
                    HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

            Gson gson = new Gson();

            String credentials =
                    new String(Base64.getEncoder().encode((restUsername + ":" + restUserPassword)
                            .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

            HttpPost request = new HttpPost(restBaseUrl + route);

            request.setHeader("Authorization", "Basic " + credentials);
            request.setHeader("Accept", "application/json");

            if (node != null)
                request.setEntity(new StringEntity(gson.toJson(node), ContentType.APPLICATION_JSON));

            HttpResponse response = httpclient.execute(request);

            if (!expectResult)
                return Optional.empty();

            String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name());

            try {

                return Optional.of(gson.fromJson(result, JsonObject.class));

            } catch (JsonSyntaxException e) {

                logger.error("Cannot parse JSON object. Status code: {}; Result:\n {}",
                        response.getStatusLine().getStatusCode(), result);

                throw e;
            }
        }
    }
}
