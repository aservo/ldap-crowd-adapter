package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.CommonLdapServer;
import com.aservo.ldap.adapter.JsonDirectoryBackend;
import com.aservo.ldap.adapter.Main;
import com.aservo.ldap.adapter.util.DirectoryBackend;
import com.aservo.ldap.adapter.util.MemberOfSupport;
import com.aservo.ldap.adapter.util.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;


public abstract class AbstractTest {

    private static final Object lock = new Object();
    private static final Map<Integer, CommonLdapServer> servers = new HashMap<>();
    private static boolean initialised = false;
    private static int counter = 0;

    private static final String host = "localhost";
    private static final Path keyStoreFile = Paths.get("./tmp/local.keystore");
    private static final String keyStorePassword = "changeit";

    private static final String dbUri = "file:./src/test/resources/com/aservo/ldap/adapter/db.json";

    public static final int MODE_OFF_PORT = 10390;
    public static final int MODE_NORMAL_PORT = 10391;
    public static final int MODE_NESTED_GROUPS_PORT = 10392;
    public static final int MODE_FLATTENING_PORT = 10393;

    protected DirectoryBackend directoryBackend;

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

                System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
                System.setProperty("javax.net.ssl.trustStorePassword", keyStorePassword);

                Security.addProvider(new BouncyCastleProvider());

                KeyPair keyPair = CertificateGenerator.generateKeyPair();
                X509Certificate cert = CertificateGenerator.generateX509Certificate(keyPair, "CN=" + host, 10);

                CertificateGenerator.updateKeyStore(keyPair, cert, "self-signed", keyStoreFile.toFile(),
                        keyStorePassword);

                boot(MODE_OFF_PORT, "off");
                boot(MODE_NORMAL_PORT, "normal");
                boot(MODE_NESTED_GROUPS_PORT, "nested-groups");
                boot(MODE_FLATTENING_PORT, "flattening");

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

                servers.get(MODE_OFF_PORT).shutdown();
                servers.get(MODE_NORMAL_PORT).shutdown();
                servers.get(MODE_NESTED_GROUPS_PORT).shutdown();
                servers.get(MODE_FLATTENING_PORT).shutdown();
                servers.clear();
            }
        }
    }

    private static void boot(int port, String mode)
            throws Exception {

        System.setProperty("cache-directory", "./tmp/" + port + "/cache");
        System.setProperty("bind.address", host + ":" + port);
        System.setProperty("ssl.enabled", "true");
        System.setProperty("ssl.key-store-file", keyStoreFile.toString());
        System.setProperty("ssl.key-store-password", keyStorePassword);
        System.setProperty("support.member-of", mode);
        System.setProperty("db-uri", dbUri);

        CommonLdapServer server = Main.createServerInstance();

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

        Properties properties = new Properties();

        properties.setProperty("db-uri", dbUri);

        directoryBackend = new JsonDirectoryBackend(properties);

        directoryBackend.startup();
    }

    @AfterEach
    public void end()
            throws Exception {

        directoryBackend.shutdown();
    }

    protected InitialDirContext createContext(String userId, String password, int port)
            throws Exception {

        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "cn=" + userId + ",ou=users,dc=json");
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldaps://" + host + ":" + port);

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

        Assertions.assertEquals(Utils.OU_GROUPS, ne1.next());
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

        Assertions.assertEquals(Utils.OU_USERS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

        Assertions.assertFalse(ne2.next().toString().isEmpty());
        Assertions.assertFalse(ne2.hasMore());
    }

    protected String getAndCheckGroupEntry(Attributes attributes, MemberOfSupport memberOfSupport)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.GROUP_OF_NAMES_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.OU_AT).getAll();

        Assertions.assertEquals(Utils.OU_GROUPS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.CN_AT).getAll();

        String entry = ne2.next().toString();
        Map<String, String> info = directoryBackend.getGroupInfo(entry);

        Assertions.assertFalse(ne2.hasMore());

        NamingEnumeration ne3 = attributes.get(SchemaConstants.DESCRIPTION_AT).getAll();

        Assertions.assertEquals(info.get(DirectoryBackend.GROUP_DESCRIPTION), ne3.next());
        Assertions.assertFalse(ne3.hasMore());

        if (memberOfSupport.equals(MemberOfSupport.OFF) ||
                memberOfSupport.equals(MemberOfSupport.NORMAL)) {

            List<String> userMembers = directoryBackend.getDirectUsersOfGroup(entry);

            if (userMembers.isEmpty()) {

                Assertions.assertNull(attributes.get(SchemaConstants.MEMBER_AT));

            } else {

                NamingEnumeration ne4 = attributes.get(SchemaConstants.MEMBER_AT).getAll();

                for (String x : userMembers)
                    Assertions.assertEquals("cn=" + x + ",ou=users,dc=json", ne4.next());

                Assertions.assertFalse(ne4.hasMore());
            }

            Assertions.assertNull(attributes.get(Utils.MEMBER_OF_AT));

        } else if (memberOfSupport.equals(MemberOfSupport.NESTED_GROUPS)) {

            List<String> userMembers = directoryBackend.getDirectUsersOfGroup(entry);
            List<String> groupMembers = directoryBackend.getDirectChildGroupsOfGroup(entry);
            List<String> memberOfGroups = directoryBackend.getDirectParentGroupsOfGroup(entry);

            if (userMembers.isEmpty() && groupMembers.isEmpty()) {

                Assertions.assertNull(attributes.get(SchemaConstants.MEMBER_AT));

            } else {

                NamingEnumeration ne4 = attributes.get(SchemaConstants.MEMBER_AT).getAll();

                for (String x : userMembers)
                    Assertions.assertEquals("cn=" + x + ",ou=users,dc=json", ne4.next());

                for (String x : groupMembers)
                    Assertions.assertEquals("cn=" + x + ",ou=groups,dc=json", ne4.next());

                Assertions.assertFalse(ne4.hasMore());
            }

            if (memberOfGroups.isEmpty()) {

                Assertions.assertNull(attributes.get(Utils.MEMBER_OF_AT));

            } else {

                NamingEnumeration ne5 = attributes.get(Utils.MEMBER_OF_AT).getAll();

                for (String x : memberOfGroups)
                    Assertions.assertEquals("cn=" + x + ",ou=groups,dc=json", ne5.next());

                Assertions.assertFalse(ne5.hasMore());
            }

        } else if (memberOfSupport.equals(MemberOfSupport.FLATTENING)) {

            List<String> userMembers = directoryBackend.getTransitiveUsersOfGroup(entry);

            if (userMembers.isEmpty()) {

                Assertions.assertNull(attributes.get(SchemaConstants.MEMBER_AT));

            } else {

                NamingEnumeration ne4 = attributes.get(SchemaConstants.MEMBER_AT).getAll();

                for (String x : userMembers)
                    Assertions.assertEquals("cn=" + x + ",ou=users,dc=json", ne4.next());

                Assertions.assertFalse(ne4.hasMore());
            }

            Assertions.assertNull(attributes.get(Utils.MEMBER_OF_AT));
        }

        return entry;
    }

    protected String getAndCheckUserEntry(Attributes attributes, MemberOfSupport memberOfSupport)
            throws Exception {

        NamingEnumeration ne0 = attributes.get(SchemaConstants.OBJECT_CLASS_AT).getAll();

        Assertions.assertEquals(SchemaConstants.TOP_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.PERSON_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.ORGANIZATIONAL_PERSON_OC, ne0.next());
        Assertions.assertEquals(SchemaConstants.INET_ORG_PERSON_OC, ne0.next());
        Assertions.assertFalse(ne0.hasMore());

        NamingEnumeration ne1 = attributes.get(SchemaConstants.OU_AT).getAll();

        Assertions.assertEquals(Utils.OU_USERS, ne1.next());
        Assertions.assertFalse(ne1.hasMore());

        NamingEnumeration ne2 = attributes.get(SchemaConstants.UID_AT).getAll();

        String entry = ne2.next().toString();
        Map<String, String> info = directoryBackend.getUserInfo(entry);

        Assertions.assertFalse(ne2.hasMore());

        NamingEnumeration ne3 = attributes.get(SchemaConstants.CN_AT).getAll();

        Assertions.assertEquals(info.get(DirectoryBackend.USER_ID), ne3.next());
        Assertions.assertFalse(ne3.hasMore());

        NamingEnumeration ne4 = attributes.get(SchemaConstants.GN_AT).getAll();

        Assertions.assertEquals(info.get(DirectoryBackend.USER_FIRST_NAME), ne4.next());
        Assertions.assertFalse(ne4.hasMore());

        NamingEnumeration ne5 = attributes.get(SchemaConstants.SN_AT).getAll();

        Assertions.assertEquals(info.get(DirectoryBackend.USER_LAST_NAME), ne5.next());
        Assertions.assertFalse(ne5.hasMore());

        NamingEnumeration ne6 = attributes.get(SchemaConstants.DISPLAY_NAME_AT).getAll();

        Assertions.assertEquals(info.get(DirectoryBackend.USER_DISPLAY_NAME), ne6.next());
        Assertions.assertFalse(ne6.hasMore());

        NamingEnumeration ne7 = attributes.get(SchemaConstants.MAIL_AT).getAll();

        Assertions.assertEquals(info.get(DirectoryBackend.USER_EMAIL_ADDRESS), ne7.next());
        Assertions.assertFalse(ne7.hasMore());

        NamingEnumeration ne8 = attributes.get(SchemaConstants.UID_NUMBER_AT).getAll();

        Assertions.assertEquals(Integer.toString(Utils.calculateHash(entry)), ne8.next());
        Assertions.assertFalse(ne8.hasMore());

        if (memberOfSupport.equals(MemberOfSupport.OFF)) {

            Assertions.assertNull(attributes.get(Utils.MEMBER_OF_AT));

        } else if (memberOfSupport.equals(MemberOfSupport.FLATTENING)) {

            List<String> memberOfGroups = directoryBackend.getTransitiveGroupsOfUser(entry);

            NamingEnumeration ne9 = attributes.get(Utils.MEMBER_OF_AT).getAll();

            for (String x : memberOfGroups)
                Assertions.assertEquals("cn=" + x + ",ou=groups,dc=json", ne9.next());

            Assertions.assertFalse(ne9.hasMore());

        } else if (memberOfSupport.equals(MemberOfSupport.NORMAL) ||
                memberOfSupport.equals(MemberOfSupport.NESTED_GROUPS)) {

            List<String> memberOfGroups = directoryBackend.getDirectGroupsOfUser(entry);

            NamingEnumeration ne9 = attributes.get(Utils.MEMBER_OF_AT).getAll();

            for (String x : memberOfGroups)
                Assertions.assertEquals("cn=" + x + ",ou=groups,dc=json", ne9.next());

            Assertions.assertFalse(ne9.hasMore());
        }

        return entry;
    }
}
