package net.wimpi.crowd.ldap;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.group.GroupWithAttributes;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.GroupTermKeys;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.wimpi.crowd.ldap.util.*;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CrowdPartition
        extends SimpleReadOnlyPartition {

    private final Logger logger = LoggerFactory.getLogger(CrowdPartition.class);

    private final CrowdClient crowdClient;
    private final ServerConfiguration serverConfig;
    private LRUCacheMap<String, Entry> entryCache;
    private FilterMatcher filterProcessor;

    private Entry crowdEntry;
    private Entry crowdGroupsEntry;
    private Entry crowdUsersEntry;

    public CrowdPartition(CrowdClient crowdClient, ServerConfiguration serverConfig) {

        super("crowd");

        this.crowdClient = crowdClient;
        this.serverConfig = serverConfig;
        this.entryCache = new LRUCacheMap<>(300);
    }

    @Override
    protected void doInit()
            throws Exception {

        logger.debug("==> CrowdPartition::init");
        logger.info("Initializing {} with suffix {}", this.getClass().getSimpleName(), Utils.CROWD_DN);

        // Create LDAP Dn
        Dn crowdDn;
        try {
            crowdDn = new Dn(schemaManager, Utils.CROWD_DN);
        } catch (LdapInvalidDnException e) {
            logger.error("Cannot create crowd DN", e);
            return;
        }

        Rdn rdn = crowdDn.getRdn();

        // Create crowd entry
        // dn: dc=example,dc=com
        // objectclass: top
        // objectclass: domain
        // dc: crowd
        // description: Crowd Domain
        crowdEntry = new DefaultEntry(schemaManager, crowdDn);

        crowdEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.DOMAIN_OC);
        crowdEntry.put(SchemaConstants.DC_AT, rdn.getAva().getValue().toString());
        crowdEntry.put("description", "Crowd Domain");

        // Create group entry
        // dn: ou=groups, dc=crowd
        // objectClass: top
        // objectClass: organizationalUnit
        // ou: groups

        Dn groupDn;
        try {
            groupDn = new Dn(schemaManager, Utils.CROWD_GROUPS_DN);
        } catch (LdapInvalidDnException e) {
            logger.error("Cannot create group DN", e);
            return;
        }

        crowdGroupsEntry = new DefaultEntry(schemaManager, groupDn);
        crowdGroupsEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC);
        crowdGroupsEntry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
        crowdGroupsEntry.put("description", "Crowd Groups");

        // Create users entry
        // dn: ou=users, dc=crowd
        // objectClass: top
        // objectClass: organizationalUnit
        // ou: users
        Dn usersDn;
        try {
            usersDn = new Dn(this.schemaManager, Utils.CROWD_USERS_DN);
        } catch (LdapInvalidDnException e) {
            logger.error("Cannot create users DN", e);
            return;
        }

        crowdUsersEntry = new DefaultEntry(schemaManager, usersDn);
        crowdUsersEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC);
        crowdUsersEntry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
        crowdUsersEntry.put("description", "Crowd Users");

        // Add to cache
        entryCache.put(crowdDn.getName(), crowdEntry);
        entryCache.put(groupDn.getName(), crowdGroupsEntry);
        entryCache.put(usersDn.getName(), crowdUsersEntry);
        logger.debug("<== CrowdPartition::init");

        filterProcessor =
                new FilterMatcher() {

                    @Nullable
                    @Override
                    protected String getAttributeValue(String attribute, String entryId, OuType ouType) {

                        try {

                            if (ouType.equals(OuType.USER)) {

                                try {

                                    String userName = crowdClient.getUser(entryId).getName();
                                    UserWithAttributes attributes = crowdClient.getUserWithAttributes(userName);

                                    return attributes.getValue(attribute);

                                } catch (UserNotFoundException e) {
                                }

                            } else if (ouType.equals(OuType.GROUP)) {

                                try {

                                    String groupName = crowdClient.getGroup(entryId).getName();
                                    GroupWithAttributes attributes = crowdClient.getGroupWithAttributes(groupName);

                                    return attributes.getValue(attribute);

                                } catch (GroupNotFoundException e) {
                                }
                            }

                        } catch (OperationFailedException |
                                ApplicationPermissionException |
                                InvalidAuthenticationException e) {

                            logger.error("Cannot get value of user attribute.");
                        }

                        return null;
                    }
                };
    }

    @Override
    protected void doDestroy()
            throws Exception {

        logger.info("destroying partition");
        crowdClient.shutdown();
    }

    @Override
    public Dn getSuffixDn() {

        return crowdEntry.getDn();
    }

    private void enrichForActiveDirectory(String user, Entry userEntry)
            throws Exception {

        // ActiveDirectory emulation is not enabled
        if (!serverConfig.getMemberOfSupport().allowMemberOfAttribute())
            return;

        List<String> groups = crowdClient.getNamesOfGroupsForUser(user, 0, Integer.MAX_VALUE);

        if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.NESTED_GROUPS)) {

            List<String> result = crowdClient.getNamesOfGroupsForNestedUser(user, 0, Integer.MAX_VALUE);

            for (String g : result)
                if (!groups.contains(g))
                    groups.add(g);

        } else if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING)) {

            List<String> flatGroupList = new LinkedList<>(groups);

            for (String group : flatGroupList) {

                // works transitive
                List<String> result = crowdClient.getNamesOfParentGroupsForNestedGroup(group, 0, Integer.MAX_VALUE);

                for (String g : result)
                    if (!groups.contains(g))
                        groups.add(g);
            }
        }

        for (String group : groups) {

            Dn mdn = new Dn(schemaManager, String.format("cn=%s,%s", group, Utils.CROWD_GROUPS_DN));

            if (!userEntry.contains(Utils.MEMBER_OF_AT, mdn.getName()))
                userEntry.add(Utils.MEMBER_OF_AT, mdn.getName());
        }
    }

    @Nullable
    private Entry createUserEntry(Dn dn) {

        Entry userEntry = entryCache.get(dn.getName());
        if (userEntry != null) {
            return userEntry;
        }

        try {
            // 1. Obtain from Crowd
            Rdn rdn = dn.getRdn(0);
            String user = rdn.getNormValue();

            User u = crowdClient.getUser(user);
            UserWithAttributes uu = crowdClient.getUserWithAttributes(user);
            if (u == null || uu == null) {
                return null;
            }

            // 2. Create entry
            userEntry = new DefaultEntry(schemaManager, dn);
            userEntry.put(SchemaConstants.OBJECT_CLASS, SchemaConstants.INET_ORG_PERSON_OC);
            userEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.TOP_OC,
                    SchemaConstants.ORGANIZATIONAL_PERSON_OC,
                    SchemaConstants.PERSON_OC,
                    SchemaConstants.INET_ORG_PERSON_OC);
            userEntry.put(SchemaConstants.UID_AT, user);
            userEntry.put(SchemaConstants.CN_AT, u.getDisplayName());
            userEntry.put(SchemaConstants.COMMON_NAME_AT, u.getDisplayName());
            userEntry.put(SchemaConstants.GN_AT, u.getFirstName());
            userEntry.put(SchemaConstants.GIVENNAME_AT, u.getFirstName());
            userEntry.put(SchemaConstants.SN_AT, u.getLastName());
            userEntry.put(SchemaConstants.SURNAME_AT, u.getLastName());
            userEntry.put(SchemaConstants.MAIL_AT, u.getEmailAddress());
            userEntry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
            userEntry.put(SchemaConstants.UID_NUMBER_AT, Utils.calculateHash(user).toString());

            // Note: Emulate AD memberof attribute
            enrichForActiveDirectory(user, userEntry);

            logger.debug(userEntry.toString());

            entryCache.put(dn.getName(), userEntry);
        } catch (Exception ex) {
            logger.debug("createUserEntry()", ex);
        }
        return userEntry;
    }

    private Entry createGroupEntry(Dn dn) {

        Entry groupEntry = entryCache.get(dn.getName());
        if (groupEntry != null) {
            return groupEntry;
        }

        try {
            // 1. Obtain from crowd
            Rdn rdn = dn.getRdn(0);
            String group = rdn.getNormValue();

            Group g = crowdClient.getGroup(group);
            List<String> users = crowdClient.getNamesOfUsersOfGroup(group, 0, Integer.MAX_VALUE);

            groupEntry = new DefaultEntry(schemaManager, dn);
            groupEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_NAMES_OC);
            groupEntry.put(SchemaConstants.CN_AT, g.getName());
            groupEntry.put(SchemaConstants.DESCRIPTION_AT, g.getDescription());
            groupEntry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING)) {

                for (String gx : crowdClient.getNamesOfNestedChildGroupsOfGroup(group, 0, Integer.MAX_VALUE))
                    for (String ux : crowdClient.getNamesOfUsersOfGroup(gx, 0, Integer.MAX_VALUE))
                        if (!users.contains(ux))
                            users.add(ux);
            }

            for (String u : users) {
                Dn mdn = new Dn(this.schemaManager, String.format("uid=%s,%s", u, Utils.CROWD_USERS_DN));
                groupEntry.add(SchemaConstants.MEMBER_AT, mdn.getName());
            }
            entryCache.put(dn.getName(), groupEntry);
        } catch (Exception ex) {
            logger.debug("createGroupEntry()", ex);
        }
        return groupEntry;
    }

    @Override
    public ClonedServerEntry lookup(LookupOperationContext context) {

        Dn dn = context.getDn();
        Entry se = entryCache.get(context.getDn().getName());
        if (se == null) {
            //todo
            logger.debug("lookup()::No cached entry found for " + dn.getName());
            return null;
        } else {
            logger.debug("lookup()::Cached entry found for " + dn.getName());
            return new ClonedServerEntry(se);
        }
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext context)
            throws LdapException {

        Dn dn = context.getDn();

        if (entryCache.containsKey(context.getDn().getName())) {
            return true;
        }

        int dnSize = dn.size();

        // one level in DN
        if (dnSize == 1) {
            if (crowdEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), crowdEntry);
                return true;
            }

            return false;
        }

        // two levels in DN
        if (dnSize == 2) {
            if (crowdGroupsEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), crowdGroupsEntry);
                return true;
            }
            if (crowdUsersEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), crowdUsersEntry);
                return true;
            }
            return false;
        }

        // 3 levels in DN
        if (dnSize == 3) {
            Dn prefix = dn.getParent();
            try {
                prefix.apply(schemaManager);
            } catch (Exception ex) {
                logger.error("hasEntry()", ex);
            }
            logger.debug("Prefix={}", prefix);

            if (crowdUsersEntry.getDn().equals(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String user = rdn.getNormValue();
                logger.debug("user={}", user);
                Entry userEntry = createUserEntry(dn);
                return (userEntry != null);
            }

            if (crowdGroupsEntry.getDn().equals(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String group = rdn.getNormValue();
                logger.debug("group={}", group);
                Entry groupEntry = createGroupEntry(dn);
                return (groupEntry != null);
            }

            logger.debug("Prefix is neither users nor groups");
            logger.debug("Crowd Users = {}", crowdUsersEntry.getDn());
            logger.debug("Crowd Groups = {}", crowdGroupsEntry.getDn().toString());
            return false;
        }

        return false;
    }

    @Nullable
    private Dn createGroupDn(String groupId) {

        try {

            return new Dn(schemaManager, String.format("cn=%s,%s", groupId, Utils.CROWD_GROUPS_DN));

        } catch (LdapInvalidDnException e) {

            logger.error("Cannot create group DN.", e);
            return null;
        }
    }

    @Nullable
    private Dn createUserDn(String userId) {

        try {

            return new Dn(schemaManager, String.format("cn=%s,%s", userId, Utils.CROWD_USERS_DN));

        } catch (LdapInvalidDnException e) {

            logger.error("Cannot create user DN.", e);
            return null;
        }
    }

    private List<String> findGroups() {

        try {

            return crowdClient.searchGroupNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.debug("Cannot receive group information from Crowd.", e);

            return Collections.emptyList();
        }
    }

    @Nullable
    private String findGroup(String attribute, String value) {

        SearchRestriction restriction;
        List<String> result;

        try {

            switch (attribute) {

                case SchemaConstants.UID_AT:
                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    restriction = new TermRestriction<>(GroupTermKeys.NAME, MatchMode.EXACTLY_MATCHES, value);

                    result = crowdClient.searchGroupNames(restriction, 0, Integer.MAX_VALUE);

                    break;

                default:

                    logger.debug("Cannot handle unknown attribute : " + attribute);
                    return null;
            }

            if (result.size() > 1) {

                logger.error("Expect unique group for attribute: " + attribute);
                return null;
            }

            return result.stream().findAny().orElse(null);

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.error("Cannot receive user information from Crowd.", e);

            return null;
        }
    }

    private List<String> findUsers() {

        try {

            return crowdClient.searchUserNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.error("Cannot receive user information from Crowd.", e);

            return Collections.emptyList();
        }
    }

    @Nullable
    private String findUser(String attribute, String value) {

        SearchRestriction restriction;
        List<String> result;

        try {

            switch (attribute) {

                case SchemaConstants.UID_NUMBER_AT:
                case SchemaConstants.UID_NUMBER_AT_OID:

                    restriction = NullRestrictionImpl.INSTANCE;

                    result = crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE).stream()
                            .filter((x) -> Utils.calculateHash(x).toString().equals(value))
                            .collect(Collectors.toList());

                    break;

                case SchemaConstants.UID_AT:
                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    restriction = new TermRestriction<>(UserTermKeys.USERNAME, MatchMode.EXACTLY_MATCHES, value);

                    result = crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

                    break;

                case SchemaConstants.MAIL_AT:
                case SchemaConstants.MAIL_AT_OID:

                    restriction = new TermRestriction<>(UserTermKeys.EMAIL, MatchMode.EXACTLY_MATCHES, value);

                    result = crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

                    break;

                default:

                    logger.warn("Cannot handle unknown attribute : " + attribute);
                    return null;
            }

            if (result.size() > 1) {

                logger.error("Expect unique user for attribute: " + attribute);
                return null;
            }

            return result.stream().findAny().orElse(null);

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.error("Cannot receive user information from Crowd.", e);

            return null;
        }
    }

    private List<Entry> createGroupEntryList(List<String> groupIds, ExprNode filter) {

        return groupIds.stream()
                .filter((x) -> filterProcessor.match(filter, x, OuType.GROUP))
                .map(this::createGroupDn)
                .filter(Objects::nonNull)
                .map(this::createGroupEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Entry> createUserEntryList(List<String> userIds, ExprNode filter) {

        return userIds.stream()
                .filter((x) -> filterProcessor.match(filter, x, OuType.USER))
                .map(this::createUserDn)
                .filter(Objects::nonNull)
                .map(this::createUserEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context) {

        logger.debug("findOne()::dn={}", context.getDn().getName());

        if (context.getDn().getParent().equals(crowdGroupsEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntries = createGroupEntryList(groupIds, context.getFilter());

            return groupEntries.stream().findAny()
                    .map((entry) -> {

                        return new EntryFilteringCursorImpl(
                                new SingletonCursor<>(entry),
                                context,
                                schemaManager);
                    })
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));

        } else if (context.getDn().getParent().equals(crowdUsersEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return userEntries.stream().findAny()
                    .map((entry) -> {

                        return new EntryFilteringCursorImpl(
                                new SingletonCursor<>(entry),
                                context,
                                schemaManager);
                    })
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));

        } else if (context.getDn().getParent().equals(crowdEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();
            List<Entry> mergedEntries = new LinkedList<>();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));

            if (!userIds.isEmpty())
                mergedEntries.addAll(createUserEntryList(userIds, context.getFilter()));
            else {

                List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));

                if (!groupIds.isEmpty())
                    mergedEntries.addAll(createGroupEntryList(groupIds, context.getFilter()));
            }

            return mergedEntries.stream().findAny()
                    .map((entry) -> {

                        return new EntryFilteringCursorImpl(
                                new SingletonCursor<>(entry),
                                context,
                                schemaManager);
                    })
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));
        }

        // return an empty result
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        logger.debug("findManyOnFirstLevel()::dn={}", context.getDn().getName());

        if (context.getDn().equals(crowdGroupsEntry.getDn())) {

            List<Entry> groupEntryList = createGroupEntryList(findGroups(), context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(crowdGroupsEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntryList = createGroupEntryList(groupIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(crowdUsersEntry.getDn())) {

            List<Entry> userEntries = createUserEntryList(findUsers(), context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(crowdUsersEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(crowdEntry.getDn())) {

            List<Entry> groupEntries = createGroupEntryList(findGroups(), context.getFilter());
            List<Entry> userEntries = createUserEntryList(findUsers(), context.getFilter());

            List<Entry> mergedEntries =
                    Stream.concat(groupEntries.stream(), userEntries.stream())
                            .collect(Collectors.toList());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(crowdEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();
            List<Entry> mergedEntries = new LinkedList<>();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));

            if (!userIds.isEmpty())
                mergedEntries.addAll(createUserEntryList(userIds, context.getFilter()));
            else {

                List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));

                if (!groupIds.isEmpty())
                    mergedEntries.addAll(createGroupEntryList(groupIds, context.getFilter()));
            }

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );
        }

        // return an empty result
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException {

        logger.debug("findManyOnMultipleLevels()::dn={}", context.getDn().getName());

        // will only search at one level
        return findManyOnFirstLevel(context);
    }
}
