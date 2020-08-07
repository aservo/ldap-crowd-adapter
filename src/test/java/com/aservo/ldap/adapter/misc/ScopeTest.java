package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScopeTest
        extends AbstractTest {

    @Test
    @Order(1)
    @DisplayName("it should be able to find a single group")
    public void test001()
            throws Exception {

        final String userName = "GroupE";

        Consumer<NamingEnumeration> consumer =
                results -> {

                    try {

                        Assertions.assertTrue(results.hasMore());
                        Attributes attributes = ((SearchResult) results.next()).getAttributes();
                        Assertions.assertEquals(userName, getAndCheckGroupEntry(attributes, false));
                        Assertions.assertFalse(results.hasMore());

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

                InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

                consumer.accept(context.search(base, filter, sc));

                context.close();
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("it should be able to find a single user")
    public void test002()
            throws Exception {

        final String userName = "UserE";

        Consumer<NamingEnumeration> consumer =
                results -> {

                    try {

                        Assertions.assertTrue(results.hasMore());
                        Attributes attributes = ((SearchResult) results.next()).getAttributes();
                        Assertions.assertEquals(userName, getAndCheckUserEntry(attributes, false));
                        Assertions.assertFalse(results.hasMore());

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

                InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

                consumer.accept(context.search(base, filter, sc));

                context.close();
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("it should be able to find the root DC entry")
    public void test003()
            throws Exception {

        String base = "dc=json";
        String filter = "objectClass=*";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(4)
    @DisplayName("it should be able to find the groups OU entry")
    public void test004()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(5)
    @DisplayName("it should be able to find the users OU entry")
    public void test005()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(6)
    @DisplayName("it should be able to browse the root DC entry")
    public void test006()
            throws Exception {

        String base = "dc=json";
        String filter = "objectClass=*";

        InitialDirContext context1 = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);
        InitialDirContext context2 = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc1 = new SearchControls();
        sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        SearchControls sc2 = new SearchControls();
        sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results1 = context1.search(base, filter, sc1);
        NamingEnumeration results2 = context1.search(base, filter, sc2);

        Assertions.assertTrue(results1.hasMore() && results2.hasMore());
        checkRootEntry(((SearchResult) results1.next()).getAttributes());
        checkRootEntry(((SearchResult) results2.next()).getAttributes());

        Assertions.assertTrue(results1.hasMore() && results2.hasMore());
        checkGroupsEntry(((SearchResult) results1.next()).getAttributes());
        checkGroupsEntry(((SearchResult) results2.next()).getAttributes());

        Assertions.assertTrue(results1.hasMore() && results2.hasMore());
        checkUsersEntry(((SearchResult) results1.next()).getAttributes());
        checkUsersEntry(((SearchResult) results2.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes1, false));
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes2, false));
        }

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes1, false));
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes2, false));
        }

        Assertions.assertFalse(results1.hasMore() || results2.hasMore());

        context1.close();
        context2.close();
    }

    @Test
    @Order(7)
    @DisplayName("it should be able to browse the groups OU entry")
    public void test007()
            throws Exception {

        String base = "ou=groups,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context1 = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);
        InitialDirContext context2 = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc1 = new SearchControls();
        sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        SearchControls sc2 = new SearchControls();
        sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results1 = context1.search(base, filter, sc1);
        NamingEnumeration results2 = context1.search(base, filter, sc2);

        Assertions.assertTrue(results1.hasMore() && results2.hasMore());
        checkGroupsEntry(((SearchResult) results1.next()).getAttributes());
        checkGroupsEntry(((SearchResult) results2.next()).getAttributes());

        for (String entry : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes1, false));
            Assertions.assertEquals(entry, getAndCheckGroupEntry(attributes2, false));
        }

        Assertions.assertFalse(results1.hasMore() || results2.hasMore());

        context1.close();
        context2.close();
    }

    @Test
    @Order(8)
    @DisplayName("it should be able to browse the users OU entry")
    public void test008()
            throws Exception {

        String base = "ou=users,dc=json";
        String filter = "objectClass=*";

        InitialDirContext context1 = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);
        InitialDirContext context2 = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc1 = new SearchControls();
        sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        SearchControls sc2 = new SearchControls();
        sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results1 = context1.search(base, filter, sc1);
        NamingEnumeration results2 = context1.search(base, filter, sc2);

        Assertions.assertTrue(results1.hasMore() && results2.hasMore());
        checkUsersEntry(((SearchResult) results1.next()).getAttributes());
        checkUsersEntry(((SearchResult) results2.next()).getAttributes());

        for (String entry : directoryBackend.getAllUsers()) {

            Assertions.assertTrue(results1.hasMore() && results2.hasMore());
            Attributes attributes1 = ((SearchResult) results1.next()).getAttributes();
            Attributes attributes2 = ((SearchResult) results2.next()).getAttributes();
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes1, false));
            Assertions.assertEquals(entry, getAndCheckUserEntry(attributes2, false));
        }

        Assertions.assertFalse(results1.hasMore() || results2.hasMore());

        context1.close();
        context2.close();
    }
}
