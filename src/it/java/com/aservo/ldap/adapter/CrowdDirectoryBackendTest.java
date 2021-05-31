package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.api.entity.Entity;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.backend.exception.SecurityProblemException;
import com.aservo.ldap.adapter.helper.AbstractIntegrationTest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrowdDirectoryBackendTest
        extends AbstractIntegrationTest {

    private final List<String> indices = Arrays.asList("A", "B", "C", "D", "E");

    @Test
    @Order(1)
    @DisplayName("it should be able to read entry info")
    public void test001()
            throws Exception {

        for (String index : indices) {

            Assertions.assertEquals("Group" + index,
                    directoryBackend.getGroup("Group" + index).getId());

            Assertions.assertEquals("Description of Group" + index + ".",
                    directoryBackend.getGroup("Group" + index).getDescription());
        }

        for (String index : indices) {

            Assertions.assertEquals("User" + index,
                    directoryBackend.getUser("User" + index).getId());

            Assertions.assertEquals("LastNameOfUser" + index,
                    directoryBackend.getUser("User" + index).getLastName());

            Assertions.assertEquals("FirstNameOfUser" + index,
                    directoryBackend.getUser("User" + index).getFirstName());

            Assertions.assertEquals("DisplayNameOfUser" + index,
                    directoryBackend.getUser("User" + index).getDisplayName());

            Assertions.assertEquals(index.toLowerCase() + ".user@email.com",
                    directoryBackend.getUser("User" + index).getEmail());
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
                            .getAuthenticatedUser("User" + index, "pw-user-" + index.toLowerCase())
                            .getId());
        }

        Assertions.assertThrows(SecurityProblemException.class, () -> {

            directoryBackend.getAuthenticatedUser("UserA", "pw-incorrect");
        });

        Assertions.assertThrows(EntityNotFoundException.class, () -> {

            directoryBackend.getAuthenticatedUser("non-existing-user", "pw");
        });
    }

    @Test
    @Order(3)
    @DisplayName("it should list all entries")
    public void test003()
            throws Exception {

        Assertions.assertEquals(indices.stream().map((x) -> "Group" + x).collect(Collectors.toSet()),
                directoryBackend.getAllGroups().stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(indices.stream().map((x) -> "User" + x).collect(Collectors.toSet()),
                directoryBackend.getAllUsers().stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(4)
    @DisplayName("it should list direct groups of an user")
    public void test004()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB")),
                directoryBackend.getDirectGroupsOfUser("UserA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB")),
                directoryBackend.getDirectGroupsOfUser("UserB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB")),
                directoryBackend.getDirectGroupsOfUser("UserC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupC")),
                directoryBackend.getDirectGroupsOfUser("UserD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE")),
                directoryBackend.getDirectGroupsOfUser("UserE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(5)
    @DisplayName("it should list direct users of a group")
    public void test005()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserD")),
                directoryBackend.getDirectUsersOfGroup("GroupA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserC")),
                directoryBackend.getDirectUsersOfGroup("GroupB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserD")),
                directoryBackend.getDirectUsersOfGroup("GroupC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getDirectUsersOfGroup("GroupD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserE")),
                directoryBackend.getDirectUsersOfGroup("GroupE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(6)
    @DisplayName("it should list direct child groups of a group")
    public void test006()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getDirectChildGroupsOfGroup("GroupA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getDirectChildGroupsOfGroup("GroupB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA")),
                directoryBackend.getDirectChildGroupsOfGroup("GroupC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC")),
                directoryBackend.getDirectChildGroupsOfGroup("GroupD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD")),
                directoryBackend.getDirectChildGroupsOfGroup("GroupE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(7)
    @DisplayName("it should list direct parent groups of a group")
    public void test007()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC")),
                directoryBackend.getDirectParentGroupsOfGroup("GroupA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getDirectParentGroupsOfGroup("GroupB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD")),
                directoryBackend.getDirectParentGroupsOfGroup("GroupC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE")),
                directoryBackend.getDirectParentGroupsOfGroup("GroupD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getDirectParentGroupsOfGroup("GroupE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(8)
    @DisplayName("it should list transitive groups of an user")
    public void test008()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB", "GroupC", "GroupD", "GroupE")),
                directoryBackend.getTransitiveGroupsOfUser("UserA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupB", "GroupC", "GroupD", "GroupE")),
                directoryBackend.getTransitiveGroupsOfUser("UserB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB")),
                directoryBackend.getTransitiveGroupsOfUser("UserC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA", "GroupC", "GroupD", "GroupE")),
                directoryBackend.getTransitiveGroupsOfUser("UserD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE")),
                directoryBackend.getTransitiveGroupsOfUser("UserE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(9)
    @DisplayName("it should list transitive users of a group")
    public void test009()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserD")),
                directoryBackend.getTransitiveUsersOfGroup("GroupA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA", "UserB", "UserC")),
                directoryBackend.getTransitiveUsersOfGroup("GroupB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserD", "UserA", "UserB")),
                directoryBackend.getTransitiveUsersOfGroup("GroupC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserD", "UserA", "UserB")),
                directoryBackend.getTransitiveUsersOfGroup("GroupD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserE", "UserD", "UserA", "UserB")),
                directoryBackend.getTransitiveUsersOfGroup("GroupE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(10)
    @DisplayName("it should list transitive child groups of a group")
    public void test010()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA")),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC", "GroupA")),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD", "GroupC", "GroupA")),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(11)
    @DisplayName("it should list transitive parent groups of a group")
    public void test011()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC", "GroupD", "GroupE")),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupA").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupB").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupD", "GroupE")),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupC").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupE")),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupD").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupE").stream()
                        .map(Entity::getId)
                        .collect(Collectors.toSet()));
    }
}
