package test.api.helper;

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.database.Row;
import com.aservo.ldap.adapter.api.directory.DirectoryBackend;
import com.aservo.ldap.adapter.api.entity.ColumnNames;
import com.aservo.ldap.adapter.api.entity.EntityType;
import com.aservo.ldap.adapter.api.entity.GroupEntity;
import com.aservo.ldap.adapter.api.entity.UserEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.junit.jupiter.api.Assertions;


public class AssertionsLdap {

    private final boolean flattening;
    private final boolean abbreviateSn;
    private final boolean abbreviateGn;

    public AssertionsLdap(boolean flattening, boolean abbreviateSn, boolean abbreviateGn) {

        this.flattening = flattening;
        this.abbreviateSn = abbreviateSn;
        this.abbreviateGn = abbreviateGn;
    }

    public void assertCorrectEntry(DirectoryBackend directory, Attributes attributes, EntityType entityType, String id)
            throws Exception {

        Pair<EntityType, String> pair = getMeta(directory, attributes);

        Assertions.assertEquals(entityType, pair.getLeft());
        Assertions.assertEquals(id, pair.getRight());
    }

    public <T extends Row> void assertCorrectEntries(DirectoryBackend directory, NamingEnumeration results,
                                                     Set<T> entities)
            throws Exception {

        Set<String> domainEntities =
                entities.stream().filter(x ->
                        EntityType.fromString(x.apply(ColumnNames.TYPE, String.class)) == EntityType.DOMAIN
                )
                        .map(x -> x.apply(ColumnNames.ID, String.class))
                        .collect(Collectors.toSet());

        Set<String> groupUnitEntities =
                entities.stream().filter(x ->
                        EntityType.fromString(x.apply(ColumnNames.TYPE, String.class)) == EntityType.GROUP_UNIT
                )
                        .map(x -> x.apply(ColumnNames.ID, String.class))
                        .collect(Collectors.toSet());

        Set<String> userUnitEntities =
                entities.stream().filter(x ->
                        EntityType.fromString(x.apply(ColumnNames.TYPE, String.class)) == EntityType.USER_UNIT
                )
                        .map(x -> x.apply(ColumnNames.ID, String.class))
                        .collect(Collectors.toSet());

        Set<String> groupEntities =
                entities.stream().filter(x ->
                        EntityType.fromString(x.apply(ColumnNames.TYPE, String.class)) == EntityType.GROUP
                )
                        .map(x -> x.apply(ColumnNames.ID, String.class))
                        .collect(Collectors.toSet());

        Set<String> userEntities =
                entities.stream().filter(x ->
                        EntityType.fromString(x.apply(ColumnNames.TYPE, String.class)) == EntityType.USER
                )
                        .map(x -> x.apply(ColumnNames.ID, String.class))
                        .collect(Collectors.toSet());

        Assertions.assertEquals(entities.size(), domainEntities.size() + groupUnitEntities.size() +
                userUnitEntities.size() + groupEntities.size() + userEntities.size());

        Set<String> domainEntries = new HashSet<>();
        Set<String> groupUnitEntries = new HashSet<>();
        Set<String> userUnitEntries = new HashSet<>();
        Set<String> groupEntries = new HashSet<>();
        Set<String> userEntries = new HashSet<>();
        int count = 0;

        while (results.hasMore()) {

            Pair<EntityType, String> pair = getMeta(directory, ((SearchResult) results.next()).getAttributes());

            if (pair.getLeft() == EntityType.DOMAIN)
                domainEntries.add(pair.getRight());
            else if (pair.getLeft() == EntityType.GROUP_UNIT)
                groupUnitEntries.add(pair.getRight());
            else if (pair.getLeft() == EntityType.USER_UNIT)
                userUnitEntries.add(pair.getRight());
            else if (pair.getLeft() == EntityType.GROUP)
                groupEntries.add(pair.getRight());
            else if (pair.getLeft() == EntityType.USER)
                userEntries.add(pair.getRight());
            else
                Assertions.fail("Cannot handle unknown entity type.");

            count++;
        }

        Assertions.assertEquals(domainEntities, domainEntries);
        Assertions.assertEquals(groupUnitEntities, groupUnitEntries);
        Assertions.assertEquals(userUnitEntities, userUnitEntries);
        Assertions.assertEquals(groupEntities, groupEntries);
        Assertions.assertEquals(userEntities, userEntries);

        Assertions.assertEquals(entities.size(), count);
    }

    private Pair<EntityType, String> getMeta(DirectoryBackend directory, Attributes attributes)
            throws Exception {

        EntityType entityType = null;
        String id = null;

        {
            NamingEnumeration ne = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

            Assertions.assertTrue(ne.hasMore());
            Assertions.assertEquals(SchemaConstants.TOP_OC, ne.next());
            Assertions.assertTrue(ne.hasMore());

            String objectClass = (String) ne.next();

            if (SchemaConstants.DOMAIN_OC.equals(objectClass)) {

                Assertions.assertFalse(ne.hasMore());

                entityType = EntityType.DOMAIN;

            } else if (SchemaConstants.ORGANIZATIONAL_UNIT_OC.equals(objectClass)) {

                Assertions.assertFalse(ne.hasMore());

                NamingEnumeration neOu = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertTrue(neOu.hasMore());

                String ou = (String) neOu.next();

                Assertions.assertFalse(neOu.hasMore());

                if (LdapUtils.OU_GROUPS.equals(ou))
                    entityType = EntityType.GROUP_UNIT;
                else if (LdapUtils.OU_USERS.equals(ou))
                    entityType = EntityType.USER_UNIT;
                else
                    Assertions.fail("Cannot handle unknown ou attribute.");

            } else if (SchemaConstants.GROUP_OF_NAMES_OC.equals(objectClass)) {

                Assertions.assertTrue(ne.hasMore());
                Assertions.assertEquals(SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());

                entityType = EntityType.GROUP;

            } else if (SchemaConstants.PERSON_OC.equals(objectClass)) {

                Assertions.assertTrue(ne.hasMore());
                Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_PERSON_OC, ne.next());
                Assertions.assertTrue(ne.hasMore());
                Assertions.assertEquals(SchemaConstants.INET_ORG_PERSON_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());

                entityType = EntityType.USER;

            } else
                Assertions.fail("Cannot handle unknown objectClass attribute.");
        }

        Assertions.assertNotNull(entityType);

        if (entityType == EntityType.DOMAIN) {

            id = directory.getId().toLowerCase();

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DC_AT).getAll();

                Assertions.assertEquals(id, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

                Assertions.assertFalse(ne.next().toString().isEmpty());
                Assertions.assertFalse(ne.hasMore());
            }

        } else if (entityType == EntityType.GROUP_UNIT) {

            id = LdapUtils.OU_GROUPS;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

                Assertions.assertFalse(ne.next().toString().isEmpty());
                Assertions.assertFalse(ne.hasMore());
            }

        } else if (entityType == EntityType.USER_UNIT) {

            id = LdapUtils.OU_USERS;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

                Assertions.assertFalse(ne.next().toString().isEmpty());
                Assertions.assertFalse(ne.hasMore());
            }

        } else if (entityType == EntityType.GROUP) {

            GroupEntity entity;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertEquals(LdapUtils.OU_GROUPS, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.CN_AT).getAll();

                String name = ne.next().toString();
                id = name.toLowerCase();
                entity = directory.getGroup(id);

                Assertions.assertEquals(id, entity.getId());
                Assertions.assertEquals(name, entity.getName());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

                Assertions.assertEquals(entity.getDescription(), ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            if (flattening) {

                Set<String> member =
                        directory.getTransitiveUsersOfGroup(id).stream()
                                .map(x -> "cn=" + Rdn.escapeValue(x.getUsername()) + ",ou=users,dc=json")
                                .collect(Collectors.toSet());

                Attribute memberAttributes = attributes.get(SchemaConstants.MEMBER_AT);

                if (member.isEmpty()) {

                    Assertions.assertNull(memberAttributes);

                } else {

                    Assertions.assertNotNull(memberAttributes);

                    List<String> memberResult = new ArrayList<>();

                    {
                        NamingEnumeration ne = memberAttributes.getAll();

                        while (ne.hasMore())
                            memberResult.add(ne.next().toString());
                    }

                    Assertions.assertEquals(member, new HashSet<>(memberResult));
                }

                Assertions.assertNull(attributes.get(LdapUtils.MEMBER_OF_AT));

            } else {

                Set<String> member =
                        Stream.concat(
                                directory.getDirectUsersOfGroup(id).stream()
                                        .map(x -> "cn=" + Rdn.escapeValue(x.getUsername()) + ",ou=users,dc=json"),
                                directory.getDirectChildGroupsOfGroup(id).stream()
                                        .map(x -> "cn=" + Rdn.escapeValue(x.getName()) + ",ou=groups,dc=json"))
                                .collect(Collectors.toSet());

                Set<String> memberOf =
                        directory.getDirectParentGroupsOfGroup(id).stream()
                                .map(x -> "cn=" + Rdn.escapeValue(x.getName()) + ",ou=groups,dc=json")
                                .collect(Collectors.toSet());

                Attribute memberAttributes = attributes.get(SchemaConstants.MEMBER_AT);
                Attribute memberOfAttributes = attributes.get(LdapUtils.MEMBER_OF_AT);

                if (member.isEmpty()) {

                    Assertions.assertNull(memberAttributes);

                } else {

                    Assertions.assertNotNull(memberAttributes);

                    List<String> memberResult = new ArrayList<>();

                    {
                        NamingEnumeration ne = memberAttributes.getAll();

                        while (ne.hasMore())
                            memberResult.add(ne.next().toString());
                    }

                    Assertions.assertEquals(member, new HashSet<>(memberResult));
                }

                if (memberOf.isEmpty()) {

                    Assertions.assertNull(memberOfAttributes);

                } else {

                    Assertions.assertNotNull(memberOfAttributes);

                    List<String> memberOfResult = new ArrayList<>();

                    {
                        NamingEnumeration ne = memberOfAttributes.getAll();

                        while (ne.hasMore())
                            memberOfResult.add(ne.next().toString());
                    }

                    Assertions.assertEquals(memberOf, new HashSet<>(memberOfResult));
                }
            }

        } else if (entityType == EntityType.USER) {

            UserEntity entity;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertEquals(LdapUtils.OU_USERS, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.UID_AT).getAll();

                String name = ne.next().toString();
                id = name.toLowerCase();
                entity = directory.getUser(id);

                Assertions.assertEquals(id, entity.getId());
                Assertions.assertEquals(name, entity.getId());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.CN_AT).getAll();

                Assertions.assertEquals(entity.getUsername(), ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            if (abbreviateSn) {

                Assertions.assertNull(attributes.get(SchemaConstants.SURNAME_AT));

                NamingEnumeration ne = attributes.get(SchemaConstants.SN_AT).getAll();

                Assertions.assertEquals(entity.getLastName(), ne.next());
                Assertions.assertFalse(ne.hasMore());

            } else {

                Assertions.assertNull(attributes.get(SchemaConstants.SN_AT));

                NamingEnumeration ne = attributes.get(SchemaConstants.SURNAME_AT).getAll();

                Assertions.assertEquals(entity.getLastName(), ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            if (abbreviateGn) {

                Assertions.assertNull(attributes.get(SchemaConstants.GIVENNAME_AT));

                NamingEnumeration ne = attributes.get(SchemaConstants.GN_AT).getAll();

                Assertions.assertEquals(entity.getFirstName(), ne.next());
                Assertions.assertFalse(ne.hasMore());

            } else {

                Assertions.assertNull(attributes.get(SchemaConstants.GN_AT));

                NamingEnumeration ne = attributes.get(SchemaConstants.GIVENNAME_AT).getAll();

                Assertions.assertEquals(entity.getFirstName(), ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DISPLAY_NAME_AT).getAll();

                Assertions.assertEquals(entity.getDisplayName(), ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.MAIL_AT).getAll();

                Assertions.assertEquals(entity.getEmail(), ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            Set<String> memberOf;

            if (flattening) {

                memberOf =
                        directory.getTransitiveGroupsOfUser(id).stream()
                                .map(x -> "cn=" + Rdn.escapeValue(x.getName()) + ",ou=groups,dc=json")
                                .collect(Collectors.toSet());

            } else {

                memberOf =
                        directory.getDirectGroupsOfUser(id).stream()
                                .map(x -> "cn=" + Rdn.escapeValue(x.getName()) + ",ou=groups,dc=json")
                                .collect(Collectors.toSet());
            }

            Attribute memberOfAttributes = attributes.get(LdapUtils.MEMBER_OF_AT);

            if (memberOf.isEmpty()) {

                Assertions.assertNull(memberOfAttributes);

            } else {

                Assertions.assertNotNull(memberOfAttributes);

                List<String> memberOfResult = new ArrayList<>();

                {
                    NamingEnumeration ne = memberOfAttributes.getAll();

                    while (ne.hasMore())
                        memberOfResult.add(ne.next().toString());
                }

                Assertions.assertEquals(memberOf, new HashSet<>(memberOfResult));
            }
        }

        Assertions.assertNotNull(id);

        return Pair.of(entityType, id);
    }
}
