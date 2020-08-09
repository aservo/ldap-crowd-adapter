package com.aservo.ldap.adapter.misc;

import com.aservo.ldap.adapter.helper.AbstractTest;
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

        Assertions.assertArrayEquals(new String[]{"UserA"},
                directoryBackend.getTransitiveUsersOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{"UserB", "UserC"},
                directoryBackend.getTransitiveUsersOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"UserC", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupC").toArray());
    }

    @Test
    @Order(2)
    @DisplayName("it should resolve transitive groups by group user correctly")
    public void test002()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{"GroupA"},
                directoryBackend.getTransitiveGroupsOfUser("UserA").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupB", "GroupC"},
                directoryBackend.getTransitiveGroupsOfUser("UserB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupC", "GroupB"},
                directoryBackend.getTransitiveGroupsOfUser("UserC").toArray());
    }

    @Test
    @Order(3)
    @DisplayName("it should resolve transitive child groups correctly")
    public void test003()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupC"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupB"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupC").toArray());
    }

    @Test
    @Order(4)
    @DisplayName("it should resolve transitive parent groups correctly")
    public void test004()
            throws Exception {

        Assertions.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupA").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupC"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupB").toArray());

        Assertions.assertArrayEquals(new String[]{"GroupB"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupC").toArray());
    }
}
