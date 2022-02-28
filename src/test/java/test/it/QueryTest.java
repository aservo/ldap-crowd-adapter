package test.it;

import com.google.common.collect.Sets;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.directory.DirectoryBackend;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.api.AbstractServerTest;
import test.api.QueryTestPlan;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupNesting;

import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TEST_MODE", matches = "(unit-only)")
public class QueryTest
        extends AbstractServerTest {

    private final Logger logger = LoggerFactory.getLogger(QueryTest.class);

    public QueryTest() {

        super(new JsonWithGroupNesting(10935));
    }

    @Test
    @Order(1)
    @DisplayName("it should verify correctness by a list LDAP queries")
    public void test001()
            throws Exception {

        Path testPlan = Paths.get("./src/test/resources/de/aservo/ldap/adapter/ldap-query-test.json");

        List<QueryTestPlan.Element> elements = QueryTestPlan.createQueryTestPlan(testPlan.toFile());

        for (QueryTestPlan.Element element : elements) {

            if (element.isIgnored()) {

                logger.warn("This test ignored: {}", element.getDescription());

            } else {

                logger.info("Begin test execution: {}", element.getDescription());

                try {

                    getServer().getDirectoryBackendFactory()
                            .withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

                                InitialDirContext context = createContext("UserA", "pw-user-a");
                                SearchControls searchControls = new SearchControls();

                                if (element.getScope().equals("base"))
                                    searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
                                else if (element.getScope().equals("one"))
                                    searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
                                else if (element.getScope().equals("sub"))
                                    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                                else
                                    throw new IllegalArgumentException("Expect valid value (base|one|sub) for scope.");

                                NamingEnumeration results =
                                        context.search(element.getBase(), element.getFilter(), searchControls);

                                Set<Row> entities = Sets.newHashSet(element.getExpectations());

                                getLdapAssertions().assertCorrectEntries(directory, results, entities);

                                context.close();
                            });

                } catch (Throwable e) {

                    logger.error("This test failed: {}", element.getDescription());

                    throw e;
                }

                logger.info("End test execution: {}", element.getDescription());
            }
        }
    }
}
