package de.aservo.ldap.adapter.backend;

import de.aservo.ldap.adapter.api.directory.DirectoryBackend;
import de.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import de.aservo.ldap.adapter.api.directory.exception.SecurityProblemException;
import de.aservo.ldap.adapter.api.entity.GroupEntity;
import de.aservo.ldap.adapter.api.entity.UserEntity;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import test.api.AbstractBackendTest;
import test.configuration.backend.JsonBackend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TEST_MODE", matches = "(it-only)|(it-offline-only)")
public class JsonDirectoryBackendTest
        extends AbstractBackendTest {

    private final List<String> indices =
            Arrays.asList("A", "B", "C", "D", "E+,", "F", "G", "H", "I");

    public JsonDirectoryBackendTest() {

        super(new JsonBackend());
    }

    @Test
    @Order(1)
    @DisplayName("it should be able to get entity attributes")
    public void test001()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        for (String index : indices) {

            Assertions.assertEquals(("Group" + index).toLowerCase(),
                    directory.getGroup("Group" + index).getId());

            Assertions.assertEquals("Group" + index,
                    directory.getGroup("Group" + index).getName());

            Assertions.assertEquals("Description of Group" + index + ".",
                    directory.getGroup("Group" + index).getDescription());
        }

        for (String index : indices) {

            Assertions.assertEquals(("User" + index).toLowerCase(),
                    directory.getUser("User" + index).getId());

            Assertions.assertEquals("User" + index,
                    directory.getUser("User" + index).getUsername());

            Assertions.assertEquals("LastNameOfUser" + index,
                    directory.getUser("User" + index).getLastName());

            Assertions.assertEquals("FirstNameOfUser" + index,
                    directory.getUser("User" + index).getFirstName());

            Assertions.assertEquals("DisplayNameOfUser" + index,
                    directory.getUser("User" + index).getDisplayName());

            Assertions.assertEquals(index.toLowerCase().charAt(0) + ".user@email.com",
                    directory.getUser("User" + index).getEmail());
        }
    }

    @Test
    @Order(2)
    @DisplayName("it should perform an authentication correctly")
    public void test002()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        for (String index : indices) {

            Assertions.assertEquals("User" + index,
                    directory
                            .getAuthenticatedUser("User" + index, "pw-user-" + index.toLowerCase().charAt(0))
                            .getUsername());
        }

        Assertions.assertThrows(SecurityProblemException.class, () -> {

            directory.getAuthenticatedUser("UserA", "pw-incorrect");
        });

        Assertions.assertThrows(EntityNotFoundException.class, () -> {

            directory.getAuthenticatedUser("non-existing-user", "pw");
        });
    }

    @Test
    @Order(3)
    @DisplayName("it should list all entities")
    public void test003()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(indices.stream().map((x) -> "Group" + x).collect(Collectors.toSet()),
                directory.getAllGroups().stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(indices.stream().map((x) -> "User" + x).collect(Collectors.toSet()),
                directory.getAllUsers().stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(4)
    @DisplayName("it should list direct groups of an user")
    public void test004()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB")),
                directory.getDirectGroupsOfUser("UserA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB")),
                directory.getDirectGroupsOfUser("UserB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB")),
                directory.getDirectGroupsOfUser("UserC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupC")),
                directory.getDirectGroupsOfUser("UserD").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE+,")),
                directory.getDirectGroupsOfUser("UserE+,").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupF")),
                directory.getDirectGroupsOfUser("UserF").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG")),
                directory.getDirectGroupsOfUser("UserG").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupH")),
                directory.getDirectGroupsOfUser("UserH").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectGroupsOfUser("UserI").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(5)
    @DisplayName("it should list direct users of a group")
    public void test005()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserD")),
                directory.getDirectUsersOfGroup("GroupA").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserC")),
                directory.getDirectUsersOfGroup("GroupB").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserD")),
                directory.getDirectUsersOfGroup("GroupC").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectUsersOfGroup("GroupD").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserE+,")),
                directory.getDirectUsersOfGroup("GroupE+,").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserF")),
                directory.getDirectUsersOfGroup("GroupF").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserG")),
                directory.getDirectUsersOfGroup("GroupG").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserH")),
                directory.getDirectUsersOfGroup("GroupH").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectUsersOfGroup("GroupI").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(6)
    @DisplayName("it should list direct child groups of a group")
    public void test006()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectChildGroupsOfGroup("GroupA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectChildGroupsOfGroup("GroupB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA")),
                directory.getDirectChildGroupsOfGroup("GroupC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC")),
                directory.getDirectChildGroupsOfGroup("GroupD").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD")),
                directory.getDirectChildGroupsOfGroup("GroupE+,").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectChildGroupsOfGroup("GroupF").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupH")),
                directory.getDirectChildGroupsOfGroup("GroupG").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG")),
                directory.getDirectChildGroupsOfGroup("GroupH").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectChildGroupsOfGroup("GroupI").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(7)
    @DisplayName("it should list direct parent groups of a group")
    public void test007()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC")),
                directory.getDirectParentGroupsOfGroup("GroupA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectParentGroupsOfGroup("GroupB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD")),
                directory.getDirectParentGroupsOfGroup("GroupC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE+,")),
                directory.getDirectParentGroupsOfGroup("GroupD").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectParentGroupsOfGroup("GroupE+,").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectParentGroupsOfGroup("GroupF").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupH")),
                directory.getDirectParentGroupsOfGroup("GroupG").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG")),
                directory.getDirectParentGroupsOfGroup("GroupH").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getDirectParentGroupsOfGroup("GroupI").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(8)
    @DisplayName("it should list transitive groups of an user")
    public void test008()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB", "GroupC", "GroupD", "GroupE+,")),
                directory.getTransitiveGroupsOfUser("UserA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB", "GroupC", "GroupD", "GroupE+,")),
                directory.getTransitiveGroupsOfUser("UserB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB")),
                directory.getTransitiveGroupsOfUser("UserC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupC", "GroupD", "GroupE+,")),
                directory.getTransitiveGroupsOfUser("UserD").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE+,")),
                directory.getTransitiveGroupsOfUser("UserE+,").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupF")),
                directory.getTransitiveGroupsOfUser("UserF").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG", "GroupH")),
                directory.getTransitiveGroupsOfUser("UserG").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG", "GroupH")),
                directory.getTransitiveGroupsOfUser("UserH").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveGroupsOfUser("UserI").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(9)
    @DisplayName("it should list transitive users of a group")
    public void test009()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserD")),
                directory.getTransitiveUsersOfGroup("GroupA").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserC")),
                directory.getTransitiveUsersOfGroup("GroupB").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserD", "UserA", "UserB")),
                directory.getTransitiveUsersOfGroup("GroupC").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserD", "UserA", "UserB")),
                directory.getTransitiveUsersOfGroup("GroupD").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserE+,", "UserD", "UserA", "UserB")),
                directory.getTransitiveUsersOfGroup("GroupE+,").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserF")),
                directory.getTransitiveUsersOfGroup("GroupF").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserG", "UserH")),
                directory.getTransitiveUsersOfGroup("GroupG").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserG", "UserH")),
                directory.getTransitiveUsersOfGroup("GroupH").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveUsersOfGroup("GroupI").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(10)
    @DisplayName("it should list transitive child groups of a group")

    public void test010()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveChildGroupsOfGroup("GroupA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveChildGroupsOfGroup("GroupB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA")),
                directory.getTransitiveChildGroupsOfGroup("GroupC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC", "GroupA")),
                directory.getTransitiveChildGroupsOfGroup("GroupD").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD", "GroupC", "GroupA")),
                directory.getTransitiveChildGroupsOfGroup("GroupE+,").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveChildGroupsOfGroup("GroupF").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupH")),
                directory.getTransitiveChildGroupsOfGroup("GroupG").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG")),
                directory.getTransitiveChildGroupsOfGroup("GroupH").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveChildGroupsOfGroup("GroupI").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(11)
    @DisplayName("it should list transitive parent groups of a group")
    public void test011()
            throws Exception {

        DirectoryBackend directory = getBackendFactory().getPermanentDirectory();

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC", "GroupD", "GroupE+,")),
                directory.getTransitiveParentGroupsOfGroup("GroupA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveParentGroupsOfGroup("GroupB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD", "GroupE+,")),
                directory.getTransitiveParentGroupsOfGroup("GroupC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE+,")),
                directory.getTransitiveParentGroupsOfGroup("GroupD").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveParentGroupsOfGroup("GroupE+,").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveParentGroupsOfGroup("GroupF").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupH")),
                directory.getTransitiveParentGroupsOfGroup("GroupG").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupG")),
                directory.getTransitiveParentGroupsOfGroup("GroupH").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directory.getTransitiveParentGroupsOfGroup("GroupI").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }
}
