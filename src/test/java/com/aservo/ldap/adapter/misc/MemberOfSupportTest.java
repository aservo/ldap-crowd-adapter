package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.MemberOfSupport;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemberOfSupportTest
        extends AbstractTest {

    @Test
    @Order(1)
    @DisplayName("it should show group attributes correctly in off mode")
    public void test001()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.OFF));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(2)
    @DisplayName("it should show user attributes correctly in off mode")
    public void test002()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.OFF));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(3)
    @DisplayName("it should show group attributes correctly in normal mode")
    public void test003()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NORMAL_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.NORMAL));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(4)
    @DisplayName("it should show user attributes correctly in normal mode")
    public void test004()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NORMAL_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.NORMAL));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(5)
    @DisplayName("it should show group attributes correctly in nested-groups mode")
    public void test005()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.NESTED_GROUPS));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(6)
    @DisplayName("it should show user attributes correctly in nested-groups mode")
    public void test006()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.NESTED_GROUPS));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(7)
    @DisplayName("it should show group attributes correctly in flattening mode")
    public void test007()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_FLATTENING_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.FLATTENING));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(8)
    @DisplayName("it should show user attributes correctly in flattening mode")
    public void test008()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_FLATTENING_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.FLATTENING));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }
}
