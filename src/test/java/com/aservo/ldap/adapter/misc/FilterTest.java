package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.Utils;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FilterTest
        extends AbstractTest {

    public FilterTest() {

        super(BackendConfig.NORMAL);
    }

    @Test
    @Order(1)
    @DisplayName("it should list all entries")
    public void test001()
            throws Exception {

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.TOP_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes, false));
        }

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes, false));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(2)
    @DisplayName("it should show domain entry")
    public void test002()
            throws Exception {

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(3)
    @DisplayName("it should list all OU entries")
    public void test003()
            throws Exception {

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(4)
    @DisplayName("it should show group OU entry")
    public void test004()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(&" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OU_AT + "=" + Utils.OU_GROUPS + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(5)
    @DisplayName("it should show user OU entry")
    public void test005()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(&" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OU_AT + "=" + Utils.OU_USERS + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(6)
    @DisplayName("it should list OU and DC entries")
    public void test006()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(|" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(7)
    @DisplayName("it should handle nested filter expressions")
    public void test007()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(&" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC + "))" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + "))" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.GROUP_OF_NAMES_OC + "))" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes, false));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(8)
    @DisplayName("it should handle existence filter expressions correctly")
    public void test008()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(" + SchemaConstants.DESCRIPTION_AT + "=" + "*" + ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes, false));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(9)
    @DisplayName("it should filter by DN attribute values")
    public void test009()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(" + SchemaConstants.MEMBER_AT + "=" + "cn=UserB,ou=users,dc=json" + ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        Attributes attributes1 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals("GroupA", getAndCheckGroupEntry(attributes1, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes2 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals("GroupB", getAndCheckGroupEntry(attributes2, false));

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(10)
    @DisplayName("it should be able to process complex filter expressions")
    public void test010()
            throws Exception {

        String base = "ou=groups,dc=json";

        String filter =
                "(&" +
                        "(objectClass=groupOfNames)" +
                        "(|(cn=GroupB)(cn=GroupC)(cn=GroupD)(cn=GroupE))" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        Attributes attributes1 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals("GroupB", getAndCheckGroupEntry(attributes1, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes2 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals("GroupC", getAndCheckGroupEntry(attributes2, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes3 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals("GroupD", getAndCheckGroupEntry(attributes3, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes4 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals("GroupE", getAndCheckGroupEntry(attributes4, false));

        Assertions.assertFalse(results.hasMore());

        context.close();
    }
}
