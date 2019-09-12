package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.MemberOfSupport;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MemberOfSupportTest
        extends AbstractTest {

    @Test
    public void test_001_checkGroupAttributesForOffMode()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_002_checkUserAttributesForOffMode()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_003_checkGroupAttributesForNormalMode()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NORMAL_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.NORMAL));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_004_checkUserAttributesForNormalMode()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NORMAL_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.NORMAL));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_005_checkGroupAttributesForNestedGroupsMode()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.NESTED_GROUPS));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_006_checkUserAttributesForNestedGroupsMode()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.NESTED_GROUPS));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_007_checkGroupAttributesForFlatteningMode()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=groupOfUniqueNames";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_FLATTENING_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.FLATTENING));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_008_checkUserAttributesForFlatteningMode()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_FLATTENING_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.FLATTENING));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }
}
