package test.it;

import com.aservo.ldap.adapter.api.directory.DirectoryBackend;
import com.aservo.ldap.adapter.api.entity.EntityType;
import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import test.api.AbstractServerTest;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupFlatteningAndSsl;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TEST_MODE", matches = "(unit-only)")
public class SslConnectionTest
        extends AbstractServerTest {

    public SslConnectionTest() {

        super(new JsonWithGroupFlatteningAndSsl(10931));
    }

    @Test
    @Order(1)
    @DisplayName("it should be able to connect via SSL")
    public void test001()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "cn=UserA,ou=users,dc=json";
            String filter = "objectClass=inetOrgPerson";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            Assertions.assertTrue(results.hasMore());

            getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                    EntityType.USER, ("UserA").toLowerCase());

            Assertions.assertFalse(results.hasMore());

            context.close();
        });
    }
}
