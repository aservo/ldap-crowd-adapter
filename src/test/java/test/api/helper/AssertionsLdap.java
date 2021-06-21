package test.api.helper;

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.directory.DirectoryBackend;
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
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.junit.jupiter.api.Assertions;


public class AssertionsLdap {

    public static String correctEntry(
            Attributes attributes,
            EntityType entityType,
            DirectoryBackend directory,
            boolean flattening,
            boolean abbreviateSn,
            boolean abbreviateGn)
            throws Exception {

        String id = null;

        if (entityType == EntityType.DOMAIN) {

            id = directory.getId().toLowerCase();

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

                Assertions.assertEquals(SchemaConstants.TOP_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.DOMAIN_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

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
                NamingEnumeration ne = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

                Assertions.assertEquals(SchemaConstants.TOP_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_UNIT_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertEquals(id, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

                Assertions.assertFalse(ne.next().toString().isEmpty());
                Assertions.assertFalse(ne.hasMore());
            }

        } else if (entityType == EntityType.USER_UNIT) {

            id = LdapUtils.OU_USERS;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

                Assertions.assertEquals(SchemaConstants.TOP_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_UNIT_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertEquals(id, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

                Assertions.assertFalse(ne.next().toString().isEmpty());
                Assertions.assertFalse(ne.hasMore());
            }

        } else if (entityType == EntityType.GROUP) {

            GroupEntity entity;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

                Assertions.assertEquals(SchemaConstants.TOP_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.GROUP_OF_NAMES_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertEquals(LdapUtils.OU_GROUPS, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.CN_AT).getAll();

                String entry = ne.next().toString();
                entity = directory.getGroup(entry);
                id = entity.getId();

                Assertions.assertEquals(entry.toLowerCase(), entity.getId());
                Assertions.assertEquals(entry, entity.getName());
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

                    Assertions.assertEquals(member.size(), memberResult.size());
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

                    Assertions.assertEquals(member.size(), memberResult.size());
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

                    Assertions.assertEquals(memberOf.size(), memberOfResult.size());
                    Assertions.assertEquals(memberOf, new HashSet<>(memberOfResult));
                }
            }

        } else if (entityType == EntityType.USER) {

            UserEntity entity;

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

                Assertions.assertEquals(SchemaConstants.TOP_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.PERSON_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_PERSON_OC, ne.next());
                Assertions.assertEquals(SchemaConstants.INET_ORG_PERSON_OC, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.OU_AT).getAll();

                Assertions.assertEquals(LdapUtils.OU_USERS, ne.next());
                Assertions.assertFalse(ne.hasMore());
            }

            {
                NamingEnumeration ne = attributes.get(SchemaConstants.UID_AT).getAll();

                String entry = ne.next().toString();
                entity = directory.getUser(entry);
                id = entity.getId();

                Assertions.assertEquals(entry, entity.getId());
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

                Assertions.assertEquals(memberOf.size(), memberOfResult.size());
                Assertions.assertEquals(memberOf, new HashSet<>(memberOfResult));
            }
        }

        Assertions.assertNotNull(id);

        return id;
    }
}
