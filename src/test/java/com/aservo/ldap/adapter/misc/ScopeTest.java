package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.MemberOfSupport;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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
public class ScopeTest
        extends AbstractTest {

    @Test
    public void test_001_browseGroupEntry()
            throws Exception {

        final String userName = "GroupE";

        Consumer<NamingEnumeration> consumer =
                results -> {

                    try {

                        Assert.assertTrue(results.hasMore());
                        Attributes attributes = ((SearchResult) results.next()).getAttributes();
                        Assert.assertEquals(userName, getAndCheckGroupEntry(attributes, MemberOfSupport.OFF));
                        Assert.assertFalse(results.hasMore());

                    } catch (Exception e) {

                        throw new RuntimeException(e);
                    }
                };

        List<String> baseList =
                Arrays.asList(
                        "cn=" + userName + ",dc=json",
                        "cn=" + userName + ",ou=groups,dc=json"
                );

        List<SearchControls> searchControlsList =
                Arrays.asList(new SearchControls(), new SearchControls(), new SearchControls());

        searchControlsList.get(0).setSearchScope(SearchControls.OBJECT_SCOPE);
        searchControlsList.get(1).setSearchScope(SearchControls.ONELEVEL_SCOPE);
        searchControlsList.get(2).setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "objectClass=*";

        for (String base : baseList) {

            for (SearchControls sc : searchControlsList) {

                InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

                consumer.accept(context.search(base, filter, sc));

                context.close();
            }
        }
    }

    @Test
    public void test_002_browseUserEntry()
            throws Exception {

        final String userName = "UserE";

        Consumer<NamingEnumeration> consumer =
                results -> {

                    try {

                        Assert.assertTrue(results.hasMore());
                        Attributes attributes = ((SearchResult) results.next()).getAttributes();
                        Assert.assertEquals(userName, getAndCheckUserEntry(attributes, MemberOfSupport.OFF));
                        Assert.assertFalse(results.hasMore());

                    } catch (Exception e) {

                        throw new RuntimeException(e);
                    }
                };

        List<String> baseList =
                Arrays.asList(
                        "cn=" + userName + ",dc=json",
                        "cn=" + userName + ",ou=users,dc=json"
                );

        List<SearchControls> searchControlsList =
                Arrays.asList(new SearchControls(), new SearchControls(), new SearchControls());

        searchControlsList.get(0).setSearchScope(SearchControls.OBJECT_SCOPE);
        searchControlsList.get(1).setSearchScope(SearchControls.ONELEVEL_SCOPE);
        searchControlsList.get(2).setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "objectClass=*";

        for (String base : baseList) {

            for (SearchControls sc : searchControlsList) {

                InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

                consumer.accept(context.search(base, filter, sc));

                context.close();
            }
        }
    }

    @Test
    public void test_003_browseRootEntryGetOne()
            throws Exception {

        String base = "dc=json";
        String filter = "objectClass=*";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_004_browseGroupsEntryGetOne()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_005_browseUsersEntryGetOne()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assert.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assert.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    public void test_006_browseRootEntryGetAll()
            throws Exception {

        String base = "dc=json";
        String filter = "objectClass=*";

        InitialDirContext context1 = createContext("UserA", "pw-user-a", MODE_OFF_PORT);
        InitialDirContext context2 = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc1 = new SearchControls();
        sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        SearchControls sc2 = new SearchControls();
        sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results1 = context1.search(base, filter, sc1);
        NamingEnumeration results2 = context1.search(base, filter, sc2);

        Assert.assertTrue(results1.hasMore() && results2.hasMore());
        checkRootEntry(((SearchResult) results1.next()).getAttributes());
        checkRootEntry(((SearchResult) results2.next()).getAttributes());

        Assert.assertTrue(results1.hasMore() && results2.hasMore());
        checkGroupsEntry(((SearchResult) results1.next()).getAttributes());
        checkGroupsEntry(((SearchResult) results2.next()).getAttributes());

        Assert.assertTrue(results1.hasMore() && results2.hasMore());
        checkUsersEntry(((SearchResult) results1.next()).getAttributes());
        checkUsersEntry(((SearchResult) results2.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes1, MemberOfSupport.OFF));
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes2, MemberOfSupport.OFF));
        }

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes1, MemberOfSupport.OFF));
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes2, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results1.hasMore() || results2.hasMore());

        context1.close();
        context2.close();
    }

    @Test
    public void test_007_browseGroupsEntryGetAll()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context1 = createContext("UserA", "pw-user-a", MODE_OFF_PORT);
        InitialDirContext context2 = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc1 = new SearchControls();
        sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        SearchControls sc2 = new SearchControls();
        sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results1 = context1.search(base, filter, sc1);
        NamingEnumeration results2 = context1.search(base, filter, sc2);

        Assert.assertTrue(results1.hasMore() && results2.hasMore());
        checkGroupsEntry(((SearchResult) results1.next()).getAttributes());
        checkGroupsEntry(((SearchResult) results2.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assert.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes1, MemberOfSupport.OFF));
            Assert.assertEquals(entry, getAndCheckGroupEntry(attributes2, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results1.hasMore() || results2.hasMore());

        context1.close();
        context2.close();
    }

    @Test
    public void test_008_browseUsersEntryGetAll()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context1 = createContext("UserA", "pw-user-a", MODE_OFF_PORT);
        InitialDirContext context2 = createContext("UserA", "pw-user-a", MODE_OFF_PORT);

        SearchControls sc1 = new SearchControls();
        sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        SearchControls sc2 = new SearchControls();
        sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results1 = context1.search(base, filter, sc1);
        NamingEnumeration results2 = context1.search(base, filter, sc2);

        Assert.assertTrue(results1.hasMore() && results2.hasMore());
        checkUsersEntry(((SearchResult) results1.next()).getAttributes());
        checkUsersEntry(((SearchResult) results2.next()).getAttributes());

        for (String entry : directoryBackend.getAllUsers()) {

            Assert.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes1, MemberOfSupport.OFF));
            Assert.assertEquals(entry, getAndCheckUserEntry(attributes2, MemberOfSupport.OFF));
        }

        Assert.assertFalse(results1.hasMore() || results2.hasMore());

        context1.close();
        context2.close();
    }
}
