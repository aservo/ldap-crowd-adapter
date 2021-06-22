package test.it;

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.directory.DirectoryBackend;
import com.aservo.ldap.adapter.api.entity.EntityType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import test.api.AbstractServerTest;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupFlattening;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TEST_MODE", matches = "(unit-only)")
public class ScopeTest
        extends AbstractServerTest {

    public ScopeTest() {

        super(new JsonWithGroupFlattening(10934));
    }

    @Test
    @Order(1)
    @DisplayName("it should be able to find a single group")
    public void test001()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            final String name = "GroupE+,";

            Consumer<NamingEnumeration> consumer =
                    results -> {

                        try {

                            Assertions.assertTrue(results.hasMore());

                            getLdapAssertions().assertCorrectEntry(directory,
                                    ((SearchResult) results.next()).getAttributes(),
                                    EntityType.GROUP, name.toLowerCase());

                            Assertions.assertFalse(results.hasMore());

                        } catch (Exception e) {

                            throw new RuntimeException(e);
                        }
                    };

            List<String> baseList =
                    Arrays.asList(
                            "cn=" + Rdn.escapeValue(name) + ",dc=json",
                            "cn=" + Rdn.escapeValue(name) + ",ou=groups,dc=json"
                    );

            List<SearchControls> searchControlsList =
                    Arrays.asList(new SearchControls(), new SearchControls(), new SearchControls());

            searchControlsList.get(0).setSearchScope(SearchControls.OBJECT_SCOPE);
            searchControlsList.get(1).setSearchScope(SearchControls.ONELEVEL_SCOPE);
            searchControlsList.get(2).setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = "objectClass=*";

            for (String base : baseList) {

                for (SearchControls sc : searchControlsList) {

                    InitialDirContext context = createContext("UserA", "pw-user-a");

                    consumer.accept(context.search(base, filter, sc));

                    context.close();
                }
            }
        });
    }

    @Test
    @Order(2)
    @DisplayName("it should be able to find a single user")
    public void test002()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            final String name = "UserE+,";

            Consumer<NamingEnumeration> consumer =
                    results -> {

                        try {

                            Assertions.assertTrue(results.hasMore());

                            getLdapAssertions().assertCorrectEntry(directory,
                                    ((SearchResult) results.next()).getAttributes(),
                                    EntityType.USER, name.toLowerCase());

                            Assertions.assertFalse(results.hasMore());

                        } catch (Exception e) {

                            throw new RuntimeException(e);
                        }
                    };

            List<String> baseList =
                    Arrays.asList(
                            "cn=" + Rdn.escapeValue(name) + ",dc=json",
                            "cn=" + Rdn.escapeValue(name) + ",ou=users,dc=json"
                    );

            List<SearchControls> searchControlsList =
                    Arrays.asList(new SearchControls(), new SearchControls(), new SearchControls());

            searchControlsList.get(0).setSearchScope(SearchControls.OBJECT_SCOPE);
            searchControlsList.get(1).setSearchScope(SearchControls.ONELEVEL_SCOPE);
            searchControlsList.get(2).setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = "objectClass=*";

            for (String base : baseList) {

                for (SearchControls sc : searchControlsList) {

                    InitialDirContext context = createContext("UserA", "pw-user-a");

                    consumer.accept(context.search(base, filter, sc));

                    context.close();
                }
            }
        });
    }

    @Test
    @Order(3)
    @DisplayName("it should be able to find the root DC entry")
    public void test003()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "dc=json";
            String filter = "objectClass=*";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            Assertions.assertTrue(results.hasMore());

            getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                    EntityType.DOMAIN, directory.getId());

            Assertions.assertFalse(results.hasMore());

            context.close();
        });
    }

    @Test
    @Order(4)
    @DisplayName("it should be able to find the groups OU entry")
    public void test004()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=groups,dc=json";
            String filter = "objectClass=*";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            Assertions.assertTrue(results.hasMore());

            getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                    EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

            Assertions.assertFalse(results.hasMore());

            context.close();
        });
    }

    @Test
    @Order(5)
    @DisplayName("it should be able to find the users OU entry")
    public void test005()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=users,dc=json";
            String filter = "objectClass=*";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            Assertions.assertTrue(results.hasMore());

            getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                    EntityType.USER_UNIT, LdapUtils.OU_USERS);

            Assertions.assertFalse(results.hasMore());

            context.close();
        });
    }

    @Test
    @Order(6)
    @DisplayName("it should be able to browse the root DC entry")
    public void test006()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "dc=json";
            String filter = "objectClass=*";

            InitialDirContext context1 = createContext("UserA", "pw-user-a");
            InitialDirContext context2 = createContext("UserA", "pw-user-a");

            SearchControls sc1 = new SearchControls();
            sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

            SearchControls sc2 = new SearchControls();
            sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results1 = context1.search(base, filter, sc1);
            NamingEnumeration results2 = context1.search(base, filter, sc2);

            for (NamingEnumeration results : Arrays.asList(results1, results2)) {

                Assertions.assertTrue(results.hasMore());

                getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                        EntityType.DOMAIN, directory.getId());

                Assertions.assertTrue(results.hasMore());

                getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                        EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

                Assertions.assertTrue(results.hasMore());

                getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                        EntityType.USER_UNIT, LdapUtils.OU_USERS);

                getLdapAssertions().assertCorrectEntries(directory, results,
                        Stream.concat(directory.getAllGroups().stream(), directory.getAllUsers().stream())
                                .collect(Collectors.toSet()));
            }

            context1.close();
            context2.close();
        });
    }

    @Test
    @Order(7)
    @DisplayName("it should be able to browse the groups OU entry")
    public void test007()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=groups,dc=json";
            String filter = "objectClass=*";

            InitialDirContext context1 = createContext("UserA", "pw-user-a");
            InitialDirContext context2 = createContext("UserA", "pw-user-a");

            SearchControls sc1 = new SearchControls();
            sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

            SearchControls sc2 = new SearchControls();
            sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results1 = context1.search(base, filter, sc1);
            NamingEnumeration results2 = context1.search(base, filter, sc2);

            for (NamingEnumeration results : Arrays.asList(results1, results2)) {

                Assertions.assertTrue(results.hasMore());

                getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                        EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

                getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllGroups());
            }

            context1.close();
            context2.close();
        });
    }

    @Test
    @Order(8)
    @DisplayName("it should be able to browse the users OU entry")
    public void test008()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=users,dc=json";
            String filter = "objectClass=*";

            InitialDirContext context1 = createContext("UserA", "pw-user-a");
            InitialDirContext context2 = createContext("UserA", "pw-user-a");

            SearchControls sc1 = new SearchControls();
            sc1.setSearchScope(SearchControls.ONELEVEL_SCOPE);

            SearchControls sc2 = new SearchControls();
            sc2.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results1 = context1.search(base, filter, sc1);
            NamingEnumeration results2 = context1.search(base, filter, sc2);

            for (NamingEnumeration results : Arrays.asList(results1, results2)) {

                Assertions.assertTrue(results.hasMore());

                getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                        EntityType.USER_UNIT, LdapUtils.OU_USERS);

                getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllUsers());
            }

            context1.close();
            context2.close();
        });
    }
}
