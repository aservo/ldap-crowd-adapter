package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.api.entity.GroupEntity;
import com.aservo.ldap.adapter.api.entity.UserEntity;
import com.aservo.ldap.adapter.helper.AbstractTest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CyclicGroupsTest
        extends AbstractTest {

    public CyclicGroupsTest() {

        super(BackendConfig.CYCLIC_GROUPS);
    }

    @Test
    @Order(1)
    @DisplayName("it should resolve transitive users by group correctly")
    public void test001()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserA")),
                directoryBackend.getTransitiveUsersOfGroup("GroupA").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserB", "UserC")),
                directoryBackend.getTransitiveUsersOfGroup("GroupB").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("UserC", "UserB")),
                directoryBackend.getTransitiveUsersOfGroup("GroupC").stream()
                        .map(UserEntity::getUsername)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(2)
    @DisplayName("it should resolve transitive groups by group user correctly")
    public void test002()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupA")),
                directoryBackend.getTransitiveGroupsOfUser("UserA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB", "GroupC")),
                directoryBackend.getTransitiveGroupsOfUser("UserB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC", "GroupB")),
                directoryBackend.getTransitiveGroupsOfUser("UserC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(3)
    @DisplayName("it should resolve transitive child groups correctly")
    public void test003()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC")),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB")),
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }

    @Test
    @Order(4)
    @DisplayName("it should resolve transitive parent groups correctly")
    public void test004()
            throws Exception {

        Assertions.assertEquals(new HashSet<>(),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupA").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupC")),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupB").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(new HashSet<>(Arrays.asList("GroupB")),
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupC").stream()
                        .map(GroupEntity::getName)
                        .collect(Collectors.toSet()));
    }
}
