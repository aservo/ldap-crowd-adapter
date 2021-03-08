package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.helper.AbstractIntegrationTest;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SslConnectionTest
        extends AbstractIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("it should be able to connect via SSL")
    public void test001()
            throws Exception {

        String base = "cn=UserA,ou=users,dc=crowd";
        String filter = "objectClass=inetOrgPerson";

        InitialDirContext context = createContext("UserA", "pw-user-a", MODE_NESTED_GROUPS_PORT);

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        Attributes attributes = ((SearchResult) results.next()).getAttributes();

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.PERSON_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_PERSON_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.INET_ORG_PERSON_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.CN_AT).getAll();

        Assertions.assertEquals(ne1.next(), "UserA");
        Assertions.assertFalse(ne1.hasMore());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }
}
