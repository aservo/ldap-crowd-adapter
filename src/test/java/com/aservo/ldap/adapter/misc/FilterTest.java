package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.MemberOfSupport;
import com.aservo.ldap.adapter.util.Utils;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FilterTest
        extends AbstractTest {

    @Test
    public void test_001_filterAll()
            throws Exception {

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.TOP_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.OFF));
        }

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_002_filterDomain()
            throws Exception {

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_003_filterOu()
            throws Exception {

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_004_filterOuAndGroups()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(&" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OU_AT + "=" + Utils.OU_GROUPS + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_005_filterOuAndUsers()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(&" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OU_AT + "=" + Utils.OU_USERS + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_006_filterOuOrDomain()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(|" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_007_filterUsersByNegation()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(&" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC + "))" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + "))" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.GROUP_OF_NAMES_OC + "))" +
                        ")";

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
    public void test_008_filterByDescriptionExistence()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(" + SchemaConstants.DESCRIPTION_AT + "=" + "*" + ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_009_filterByMember()
            throws Exception {

        String base = "dc=json";

        String filter =
                "(" + SchemaConstants.MEMBER_AT + "=" + "cn=UserB,ou=users,dc=json" + ")";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        Attributes attributes1 = ((SearchResult) results.next()).getAttributes();
        Assert.assertEquals("GroupA", getAndCheckGroupEntry(attributes1, MemberOfSupport.OFF));

        Assert.assertTrue(results.hasMore());
        Attributes attributes2 = ((SearchResult) results.next()).getAttributes();
        Assert.assertEquals("GroupB", getAndCheckGroupEntry(attributes2, MemberOfSupport.OFF));

        Assert.assertFalse(results.hasMore());

        context.close();
    }
}
