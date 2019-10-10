package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.exception.SecurityProblemException;
import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.DirectoryBackend;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JsonDirectoryBackendTest
        extends AbstractTest {

    private final List<String> indices = Arrays.asList("A", "B", "C", "D", "E");

    @Test
    @Order(1)
    @DisplayName("it should be able to read entry info")
    public void test001()
            throws Exception {

        for (String index : indices) {

            Assertions.assertEquals("Group" + index,
                    directoryBackend.getGroupInfo("Group" + index).get(DirectoryBackend.GROUP_ID));

            Assertions.assertEquals("Description of Group" + index + ".",
                    directoryBackend.getGroupInfo("Group" + index).get(DirectoryBackend.GROUP_DESCRIPTION));
        }

        for (String index : indices) {

            Assertions.assertEquals("User" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_ID));

            Assertions.assertEquals("FirstNameOfUser" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_FIRST_NAME));

            Assertions.assertEquals("LastNameOfUser" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_LAST_NAME));

            Assertions.assertEquals("DisplayNameOfUser" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_DISPLAY_NAME));

            Assertions.assertEquals(index.toLowerCase() + ".user@email.com",
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_EMAIL_ADDRESS));
        }
    }

    @Test
    @Order(2)
    @DisplayName("it should make an authentication correctly")
    public void test002()
            throws Exception {

        for (String index : indices) {

            Assertions.assertEquals("User" + index,
                    directoryBackend
                            .getInfoFromAuthenticatedUser("User" + index, "pw-user-" + index.toLowerCase())
                            .get(DirectoryBackend.USER_ID));
        }

        Assertions.assertThrows(SecurityProblemException.class, () -> {

            directoryBackend.getInfoFromAuthenticatedUser("UserA", "pw-incorrect");
        });

        Assertions.assertThrows(EntryNotFoundException.class, () -> {

            directoryBackend.getInfoFromAuthenticatedUser("non-existing-user", "pw");
        });
    }

    @Test
    @Order(3)
    @DisplayName("it should list all entries")
    public void test003()
            throws Exception {

        Assertions.assertArrayEquals(indices.stream().map((x) -> "Group" + x).toArray(),
                directoryBackend.getAllGroups().toArray());

        Assertions.assertArrayEquals(indices.stream().map((x) -> "User" + x).toArray(),
                directoryBackend.getAllUsers().toArray());
    }

    @Test
    @Order(4)
    @DisplayName("it should list direct groups of an user")
    public void test004()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"GroupA", "GroupB"},
                directoryBackend.getDirectGroupsOfUser("UserA").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupA", "GroupB"},
                directoryBackend.getDirectGroupsOfUser("UserB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupB"},
                directoryBackend.getDirectGroupsOfUser("UserC").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupA", "GroupC"},
                directoryBackend.getDirectGroupsOfUser("UserD").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getDirectGroupsOfUser("UserE").toArray());
    }

    @Test
    @Order(5)
    @DisplayName("it should list direct users of a group")
    public void test005()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"UserA", "UserB", "UserD"},
                directoryBackend.getDirectUsersOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{"UserA", "UserB", "UserC"},
                directoryBackend.getDirectUsersOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"UserD"},
                directoryBackend.getDirectUsersOfGroup("GroupC").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getDirectUsersOfGroup("GroupD").toArray());

        Assertions.assertArrayEquals(new String[]{"UserE"},
                directoryBackend.getDirectUsersOfGroup("GroupE").toArray());
    }

    @Test
    @Order(6)
    @DisplayName("it should list direct child groups of a group")
    public void test006()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getDirectChildGroupsOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getDirectChildGroupsOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupA"},
                directoryBackend.getDirectChildGroupsOfGroup("GroupC").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupC"},
                directoryBackend.getDirectChildGroupsOfGroup("GroupD").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupD"},
                directoryBackend.getDirectChildGroupsOfGroup("GroupE").toArray());
    }

    @Test
    @Order(7)
    @DisplayName("it should list direct parent groups of a group")
    public void test007()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"GroupC"},
                directoryBackend.getDirectParentGroupsOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getDirectParentGroupsOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupD"},
                directoryBackend.getDirectParentGroupsOfGroup("GroupC").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getDirectParentGroupsOfGroup("GroupD").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getDirectParentGroupsOfGroup("GroupE").toArray());
    }

    @Test
    @Order(8)
    @DisplayName("it should list transitive groups of an user")
    public void test008()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"GroupA", "GroupB", "GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserA").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupA", "GroupB", "GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupB"},
                directoryBackend.getTransitiveGroupsOfUser("UserC").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupA", "GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserD").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserE").toArray());
    }

    @Test
    @Order(9)
    @DisplayName("it should list transitive users of a group")
    public void test009()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"UserA", "UserB", "UserD"},
                directoryBackend.getTransitiveUsersOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{"UserA", "UserB", "UserC"},
                directoryBackend.getTransitiveUsersOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"UserD", "UserA", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupC").toArray());

        Assertions.assertArrayEquals(new String[]{"UserD", "UserA", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupD").toArray());

        Assertions.assertArrayEquals(new String[]{"UserE", "UserD", "UserA", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupE").toArray());
    }

    @Test
    @Order(10)
    @DisplayName("it should list transitive child groups of a group")
    public void test010()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupA"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupC").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupC", "GroupA"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupD").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupD", "GroupC", "GroupA"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupE").toArray());
    }

    @Test
    @Order(11)
    @DisplayName("it should list transitive parent groups of a group")
    public void test011()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupD", "GroupE"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupC").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupD").toArray());

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupE").toArray());
    }
}
