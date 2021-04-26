package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.CommonLdapServer;
import com.aservo.ldap.adapter.Main;
import com.aservo.ldap.adapter.adapter.LdapUtils;
import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.backend.DirectoryBackend;
import com.aservo.ldap.adapter.backend.DirectoryBackendFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.junit.jupiter.api.*;


public abstract class AbstractTest {

    private static final Object lock = new Object();
    private static final Map<Integer, CommonLdapServer> servers = new HashMap<>();
    private static boolean initialised = false;
    private static int counter = 0;

    private static final String host = "localhost";
    public static final int MODE_NESTED_GROUPS_PORT = 10390;
    public static final int MODE_FLATTENING_PORT = 10391;

    private static final String dbUri1 = "file:./src/test/resources/com/aservo/ldap/adapter/db.json";
    private static final String dbUri2 = "file:./src/test/resources/com/aservo/ldap/adapter/db-cyclic-groups.json";

    protected DirectoryBackend directoryBackend;
    private final BackendConfig config;

    public AbstractTest(BackendConfig config) {

        this.config = config;
    }

    public AbstractTest() {

        this.config = BackendConfig.NONE;
    }

    @BeforeAll
    public static void setup()
            throws Exception {

        synchronized (lock) {

            counter++;

            if (!initialised) {

                Path testPath = Paths.get("./tmp");

                if (Files.exists(testPath))
                    FileUtils.deleteDirectory(testPath.toFile());

                Files.createDirectories(testPath);

                boot(MODE_NESTED_GROUPS_PORT, dbUri1, false);
                boot(MODE_FLATTENING_PORT, dbUri1, true);

                initialised = true;
            }
        }
    }

    @AfterAll
    public static void shutdown()
            throws Exception {

        synchronized (lock) {

            counter--;

            if (initialised && counter == 0) {

                servers.get(MODE_NESTED_GROUPS_PORT).shutdown();
                servers.get(MODE_FLATTENING_PORT).shutdown();
                servers.clear();
            }
        }
    }

    private static void boot(int port, String dbUri, boolean flattening)
            throws Exception {

        System.setProperty("directory-backend.permanent",
                "com.aservo.ldap.adapter.backend.JsonDirectoryBackend," +
                        "com.aservo.ldap.adapter.backend.CachedInMemoryDirectoryBackend");

        System.setProperty("directory-backend.session", "");

        System.setProperty("ds-cache-directory", "./tmp/" + port + "/cache");
        System.setProperty("bind.address", host + ":" + port);
        System.setProperty("mode.flattening", Boolean.toString(flattening));
        System.setProperty("db-uri", dbUri);

        CommonLdapServer server = Main.createServerInstance(Main.createConfiguration());

        // boot process
        server.startup();

        // wait 10 seconds for server boot
        for (int i = 0; i < 10; i++)
            if (server.isStarted())
                Thread.sleep(1000);

        // expect running server
        Assertions.assertTrue(server.isStarted());

        servers.put(port, server);
    }

    @BeforeEach
    public void begin()
            throws Exception {

        Properties serverProperties = new Properties();
        Properties backendProperties = new Properties();

        serverProperties.setProperty("directory-backend.permanent",
                "com.aservo.ldap.adapter.backend.JsonDirectoryBackend");

        serverProperties.setProperty("directory-backend.session", "");

        if (config == BackendConfig.NORMAL)
            backendProperties.setProperty("db-uri", dbUri1);
        else if (config == BackendConfig.CYCLIC_GROUPS)
            backendProperties.setProperty("db-uri", dbUri2);

        directoryBackend =
                new DirectoryBackendFactory(Main.createConfiguration(serverProperties, backendProperties))
                        .getPermanentDirectory();

        directoryBackend.startup();
    }

    @AfterEach
    public void end()
            throws Exception {

        directoryBackend.shutdown();
        directoryBackend = null;
    }

    protected InitialDirContext createContext(String userId, String password, int port)
            throws Exception {

        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "cn=" + userId + ",ou=users,dc=json");
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port);

        return new InitialDirContext(env);
    }

    protected void checkRootEntry(Attributes attributes)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.DOMAIN_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.DC_AT).getAll();

        Assertions.assertEquals(directoryBackend.getId().toLowerCase(), ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

        Assertions.assertFalse(ne2.next().toString().isEmpty());
        Assertions.assertFalse(ne2.hasMore());
    }

    protected void checkGroupsEntry(Attributes attributes)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_UNIT_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.OU_AT).getAll();

        Assertions.assertEquals(LdapUtils.OU_GROUPS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

        Assertions.assertFalse(ne2.next().toString().isEmpty());
        Assertions.assertFalse(ne2.hasMore());
    }

    protected void checkUsersEntry(Attributes attributes)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_UNIT_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.OU_AT).getAll();

        Assertions.assertEquals(LdapUtils.OU_USERS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

        Assertions.assertFalse(ne2.next().toString().isEmpty());
        Assertions.assertFalse(ne2.hasMore());
    }

    protected String getAndCheckGroupEntry(Attributes attributes, boolean flattening)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.GROUP_OF_NAMES_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.OU_AT).getAll();

        Assertions.assertEquals(LdapUtils.OU_GROUPS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.CN_AT).getAll();

        String entry = ne2.next().toString();
        GroupEntity group = directoryBackend.getGroup(entry);

        Assertions.assertFalse(ne2.hasMore());

        NamingEnumeration ne3 = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

        Assertions.assertEquals(group.getDescription(), ne3.next());
        Assertions.assertFalse(ne3.hasMore());

        if (flattening) {

            List<UserEntity> userMembers = directoryBackend.getTransitiveUsersOfGroup(entry);

            if (userMembers.isEmpty()) {

                Assertions.assertNull(attributes.get(SchemaConstants.MEMBER_AT));

            } else {

                NamingEnumeration ne4 = attributes.get(SchemaConstants.MEMBER_AT).getAll();

                for (UserEntity x : userMembers)
                    Assertions.assertEquals("cn=" + Rdn.escapeValue(x.getId()) + ",ou=users,dc=json", ne4.next());

                Assertions.assertFalse(ne4.hasMore());
            }

            Assertions.assertNull(attributes.get(LdapUtils.MEMBER_OF_AT));

        } else {

            Set<String> members =
                    Stream.concat(
                            directoryBackend.getDirectUsersOfGroup(entry).stream()
                                    .map(x -> "cn=" + Rdn.escapeValue(x.getId()) + ",ou=users,dc=json"),
                            directoryBackend.getDirectChildGroupsOfGroup(entry).stream()
                                    .map(x -> "cn=" + Rdn.escapeValue(x.getId()) + ",ou=groups,dc=json"))
                            .collect(Collectors.toSet());

            Set<String> memberOf =
                    directoryBackend.getDirectParentGroupsOfGroup(entry).stream()
                            .map(x -> "cn=" + Rdn.escapeValue(x.getId()) + ",ou=groups,dc=json")
                            .collect(Collectors.toSet());

            Attribute membersResultAttribute = attributes.get(SchemaConstants.MEMBER_AT);
            List<String> membersResult = new ArrayList<>();

            if (membersResultAttribute != null) {

                NamingEnumeration ne4 = membersResultAttribute.getAll();

                while (ne4.hasMore())
                    membersResult.add(ne4.next().toString());
            }

            Attribute memberOfResultAttribute = attributes.get(LdapUtils.MEMBER_OF_AT);
            List<String> memberOfResult = new ArrayList<>();

            if (memberOfResultAttribute != null) {

                NamingEnumeration ne5 = memberOfResultAttribute.getAll();

                while (ne5.hasMore())
                    memberOfResult.add(ne5.next().toString());
            }

            Assertions.assertEquals(members.size(), membersResult.size());
            Assertions.assertEquals(memberOf.size(), memberOfResult.size());

            Assertions.assertEquals(members, new HashSet<>(membersResult));
            Assertions.assertEquals(memberOf, new HashSet<>(memberOfResult));
        }

        return entry;
    }

    protected String getAndCheckUserEntry(Attributes attributes, boolean flattening)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.PERSON_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_PERSON_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.INET_ORG_PERSON_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.OU_AT).getAll();

        Assertions.assertEquals(LdapUtils.OU_USERS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.UID_AT).getAll();

        String entry = ne2.next().toString();
        UserEntity user = directoryBackend.getUser(entry);

        Assertions.assertFalse(ne2.hasMore());

        NamingEnumeration ne3 = attributes.get(SchemaConstants.CN_AT).getAll();

        Assertions.assertEquals(user.getId(), ne3.next());
        Assertions.assertFalse(ne3.hasMore());

        NamingEnumeration ne4 = attributes.get(SchemaConstants.SURNAME_AT).getAll();

        Assertions.assertEquals(user.getLastName(), ne4.next());
        Assertions.assertFalse(ne4.hasMore());

        NamingEnumeration ne5 = attributes.get(SchemaConstants.GIVENNAME_AT).getAll();

        Assertions.assertEquals(user.getFirstName(), ne5.next());
        Assertions.assertFalse(ne5.hasMore());

        NamingEnumeration ne6 = attributes.get(SchemaConstants.DISPLAY_NAME_AT).getAll();

        Assertions.assertEquals(user.getDisplayName(), ne6.next());
        Assertions.assertFalse(ne6.hasMore());

        NamingEnumeration ne7 = attributes.get(SchemaConstants.MAIL_AT).getAll();

        Assertions.assertEquals(user.getEmail(), ne7.next());
        Assertions.assertFalse(ne7.hasMore());

        if (flattening) {

            List<GroupEntity> memberOfGroups = directoryBackend.getTransitiveGroupsOfUser(entry);

            NamingEnumeration ne9 = attributes.get(LdapUtils.MEMBER_OF_AT).getAll();

            for (GroupEntity x : memberOfGroups)
                Assertions.assertEquals("cn=" + Rdn.escapeValue(x.getId()) + ",ou=groups,dc=json", ne9.next());

            Assertions.assertFalse(ne9.hasMore());

        } else {

            List<GroupEntity> memberOfGroups = directoryBackend.getDirectGroupsOfUser(entry);

            NamingEnumeration ne9 = attributes.get(LdapUtils.MEMBER_OF_AT).getAll();

            for (GroupEntity x : memberOfGroups)
                Assertions.assertEquals("cn=" + Rdn.escapeValue(x.getId()) + ",ou=groups,dc=json", ne9.next());

            Assertions.assertFalse(ne9.hasMore());
        }

        return entry;
    }

    public enum BackendConfig {
        NORMAL, CYCLIC_GROUPS, NONE
    }
}
