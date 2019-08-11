package net.wimpi.crowd.ldap;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.naming.InvalidNameException;
import net.wimpi.crowd.ldap.util.LRUCacheMap;
import net.wimpi.crowd.ldap.util.MemberOfSupport;
import net.wimpi.crowd.ldap.util.ServerConfiguration;
import net.wimpi.crowd.ldap.util.Utils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
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


/**
 * A partition that bridges to the CrowdClient/Crowd REST interface.
 * <p>
 * Currently this implementation is read only.
 *
 * @author Dieter Wimberger
 */
public class CrowdPartition
        extends SimpleReadOnlyPartition {

    private final Logger logger = LoggerFactory.getLogger(CrowdPartition.class);

    private final CrowdClient crowdClient;

    private final ServerConfiguration serverConfig;

    private List<Entry> crowdOneLevelList;
    private final Pattern UIDFilter = Pattern.compile("\\(0.9.2342.19200300.100.1.1=([^\\)]*)\\)");
    private final Pattern uidFilter = Pattern.compile("\\(uid=([^\\)]*)\\)");
    private final Pattern gidFilter = Pattern.compile("\\(gidNumber=([^\\)]*)\\)");

    private LRUCacheMap<String, Entry> entryCache;

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

        // Prepare list
        crowdOneLevelList = Collections.unmodifiableList(Arrays.asList(crowdGroupsEntry, crowdUsersEntry));

        // Add to cache
        entryCache.put(crowdDn.getName(), crowdEntry);
        entryCache.put(groupDn.getName(), crowdGroupsEntry);
        entryCache.put(usersDn.getName(), crowdUsersEntry);
        logger.debug("<== CrowdPartition::init");
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

    private boolean isCrowd(Dn dn) {

        return crowdEntry.getDn().equals(dn);
    }

    private boolean isCrowdGroups(Dn dn) {

        return crowdGroupsEntry.getDn().getName().equals(dn.getName());
    }

    private boolean isCrowdUsers(Dn dn) {

        return crowdUsersEntry.getDn().getName().equals(dn.getName());
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
            if (isCrowd(dn)) {
                entryCache.put(dn.getName(), crowdEntry);
                return true;
            }

            return false;
        }

        // two levels in DN
        if (dnSize == 2) {
            if (isCrowdGroups(dn)) {
                entryCache.put(dn.getName(), crowdGroupsEntry);
                return true;
            }
            if (isCrowdUsers(dn)) {
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

            if (isCrowdUsers(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String user = rdn.getNormValue();
                logger.debug("user={}", user);
                Entry userEntry = createUserEntry(dn);
                return (userEntry != null);
            }

            if (isCrowdGroups(prefix)) {
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

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context) {

        Dn dn = context.getDn();

        // 1. Try cache
        Entry se = entryCache.get(dn.getName());
        if (se == null) {
            // no object found
            return new EntryFilteringCursorImpl(new EmptyCursor<Entry>(), context, this.schemaManager);
        }

        String filterPreparation = context.getFilter().toString();
        if (filterPreparation.contains("(memberOf=")) { // memberOf filter is always threaded with AND condition
            EntryFilteringCursor cursorHelp = filterMemberOf(context, se, filterPreparation);
            if (cursorHelp != null) {
                return cursorHelp;
            }

            return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager); // return an empty result
        }
        return new EntryFilteringCursorImpl(new SingletonCursor<>(se), context, this.schemaManager);
    }

    private EntryFilteringCursor filterMemberOf(SearchOperationContext context, Entry se, String filterPreparation) {

        HashMap<String, String> parsedFilter = new HashMap<>();
        parsedFilter.put("cn", readValueFromFilter(filterPreparation, "2.5.4.3=")); // cn
        parsedFilter.put("ou", readValueFromFilter(filterPreparation, "2.5.4.11=")); // organizationalUnitName
        parsedFilter.put("dc", readValueFromFilter(filterPreparation, "0.9.2342.19200300.100.1.25=")); // domainComponent

        ArrayList<String> member = new ArrayList<>();
        String parsedRoles = se.get("memberof").toString();

        StringTokenizer tokenizer = new StringTokenizer(parsedRoles, System.getProperty("line.separator"));
        while (tokenizer.hasMoreTokens()) {
            member.add(tokenizer.nextToken());
        }

        for (String memberOf : member) {
            HashMap<String, String> eachLineCheck = new HashMap<String, String>(1);
            eachLineCheck.put("cn", readValueFromFilter(memberOf, "cn="));
            eachLineCheck.put("ou", readValueFromFilter(memberOf, "ou="));
            eachLineCheck.put("dc", readValueFromFilter(memberOf, "dc="));
            if (eachLineCheck.equals(parsedFilter)) {
                SingletonCursor<Entry> singletonCursor = new SingletonCursor<Entry>(se);
                return new EntryFilteringCursorImpl(singletonCursor, context, this.schemaManager);
            }
        }
        return null;
    }

    private String readValueFromFilter(String string, String identifier) {

        if (string.length() < identifier.length()) {
            return "";
        }

        Integer parseFrom = string.lastIndexOf(identifier) + identifier.length(); // start of parse
        String pomstring = string.substring(parseFrom);
        Integer parseTo = pomstring.indexOf(",");
        if (parseTo == -1) {
            parseTo = pomstring.indexOf(")");
        }
        if (parseTo == -1) {
            parseTo = pomstring.length();
        }
        if (pomstring.length() > 0 && parseTo >= 0 && parseTo <= string.length()) {
            return pomstring.substring(0, parseTo);
        } else return "";
    }

    @Nullable
    private Dn createGroupDn(String name) {

        try {
            return new Dn(schemaManager, String.format("uid=%s,%s", name, Utils.CROWD_GROUPS_DN));
        } catch (LdapInvalidDnException e) {
            logger.error("Cannot create group DN for {}", name, e);
            return null;
        }
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        Dn dn = context.getDn();
        Entry se = context.getEntry();

        if (se == null) {
            String name = dn.getRdn(0).getNormValue();
            logger.debug("Name={}", name);
            if ("crowd".equals(name)) {
                return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager);
            }
        }

        // 1. Organizational Units
        if (dn.getName().equals(crowdEntry.getDn().getName())) {
            return new EntryFilteringCursorImpl(
                    new ListCursor<>(crowdOneLevelList),
                    context,
                    this.schemaManager
            );
        }

        // 2. Groups
        if (dn.getName().equals(crowdGroupsEntry.getDn().getName())) {
            // Retrieve Filter
            List<Entry> l = new ArrayList<>();
            try {
                String searchedMember = getSearchedMember(context.getFilter());
                if (searchedMember != null) {
                    l = crowdClient.getGroupsForUser(searchedMember, 0, Integer.MAX_VALUE).stream()
                            .map(g -> createGroupEntry(createGroupDn(g.getName())))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } else {
                    l = crowdClient.searchGroupNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE).stream()
                            .map(gn -> createGroupEntry(createGroupDn(gn)))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            } catch (Exception ex) {
                logger.error("findOneLevel()", ex);
            }
            return new EntryFilteringCursorImpl(
                    new ListCursor<>(l),
                    context,
                    this.schemaManager
            );
        }

        // 3. Users
        if (dn.getName().equals(crowdUsersEntry.getDn().getName())) {
            // Retrieve Filter
            String filter = context.getFilter().toString();

            Matcher m = UIDFilter.matcher(filter);
            String uid = "";
            if (m.find()) {
                uid = m.group(1);
            }
            Matcher mm = uidFilter.matcher(filter);
            if (mm.find()) {
                uid = mm.group(1);
            }
            Matcher mmm = gidFilter.matcher(filter);
            if (mmm.find()) {
                // HACK: this is not implemented yet we need to search by custom attribute from m_crowdclient
                return null;
            }

            List<Entry> l = new ArrayList<>();
            try {
                SearchRestriction userName;
                if ("*".equals(uid)) {
                    // Contains * term restriction does not return any users, so use null one
                    userName = NullRestrictionImpl.INSTANCE;

                } else {
                    userName = new TermRestriction<String>(UserTermKeys.USERNAME, MatchMode.EXACTLY_MATCHES, uid);
                }
                List<String> list = crowdClient.searchUserNames(userName, 0, Integer.MAX_VALUE);
                for (String un : list) {
                    Dn udn = new Dn(this.schemaManager, String.format("uid=%s,%s", un, Utils.CROWD_USERS_DN));
                    l.add(createUserEntry(udn));
                }
            } catch (Exception ex) {
                logger.error("findOneLevel()", ex);
            }
            return new EntryFilteringCursorImpl(
                    new ListCursor<>(l),
                    context,
                    this.schemaManager
            );
        }

        // return an empty result
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, this.schemaManager);
    }

    private String getSearchedMember(ExprNode filter) {

        if (filter instanceof EqualityNode) {
            EqualityNode equalityNode = (EqualityNode) filter;
            if (SchemaConstants.MEMBER_AT.equals(equalityNode.getAttribute()) ||
                    SchemaConstants.MEMBER_AT_OID.equals(equalityNode.toString())) {
                String value = equalityNode.getValue().toString();
                if (value.startsWith(SchemaConstants.UID_AT_OID)) {
                    String[] parts = value.split(",");
                    return parts[0].substring(SchemaConstants.UID_AT_OID.length() + 1);
                }
            }
        } else if (filter instanceof OrNode) {
            OrNode orNode = (OrNode) filter;
            return orNode.getChildren().stream()
                    .map(this::getSearchedMember)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    @Override
    protected EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException {

        Dn dn = context.getDn();

        logger.debug("findSubTree()::dn={}", dn.getName());
        // Will only search at one level
        return findManyOnFirstLevel(context);
    }
}
