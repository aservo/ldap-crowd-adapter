package test.it;

import de.aservo.ldap.adapter.api.directory.DirectoryBackend;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import test.api.AbstractServerTest;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupNesting;

import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TEST_MODE", matches = "(unit-only)")
public class GroupNestingTest
        extends AbstractServerTest {

    public GroupNestingTest() {

        super(new JsonWithGroupNesting(10932));
    }

    @Test
    @Order(1)
    @DisplayName("it should use the correct flags for attribute abbreviation")
    public void test001()
            throws Exception {

        Assertions.assertFalse(getServer().getServerConfig().isAbbreviateSnAttribute());
        Assertions.assertFalse(getServer().getServerConfig().isAbbreviateGnAttribute());
    }

    @Test
    @Order(2)
    @DisplayName("it should show group attributes correctly in nested-groups mode")
    public void test002()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=groups,dc=json";
            String filter = "objectClass=groupOfUniqueNames";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllGroups());

            context.close();
        });
    }

    @Test
    @Order(3)
    @DisplayName("it should show user attributes correctly in nested-groups mode")
    public void test003()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=users,dc=json";
            String filter = "objectClass=inetOrgPerson";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllUsers());

            context.close();
        });
    }
}
