package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.exception.SecurityProblemException;
import com.aservo.ldap.adapter.helper.AbstractTest;
import com.aservo.ldap.adapter.util.DirectoryBackend;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JsonDirectoryBackendTest
        extends AbstractTest {

    private final List<String> indices = Arrays.asList("A", "B", "C", "D", "E");

    @Test
    public void test_001_getEntryInfo()
            throws Exception {

        for (String index : indices) {

            Assert.assertEquals("Group" + index,
                    directoryBackend.getGroupInfo("Group" + index).get(DirectoryBackend.GROUP_ID));

            Assert.assertEquals("Description of Group" + index + ".",
                    directoryBackend.getGroupInfo("Group" + index).get(DirectoryBackend.GROUP_DESCRIPTION));
        }

        for (String index : indices) {

            Assert.assertEquals("User" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_ID));

            Assert.assertEquals("FirstNameOfUser" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_FIRST_NAME));

            Assert.assertEquals("LastNameOfUser" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_LAST_NAME));

            Assert.assertEquals("DisplayNameOfUser" + index,
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_DISPLAY_NAME));

            Assert.assertEquals(index.toLowerCase() + ".user@email.com",
                    directoryBackend.getUserInfo("User" + index).get(DirectoryBackend.USER_EMAIL_ADDRESS));
        }
    }

    @Test
    public void test_002_authentication()
            throws Exception {

        for (String index : indices) {

            Assert.assertEquals("User" + index,
                    directoryBackend
                            .getInfoFromAuthenticatedUser("User" + index, "pw-user-" + index.toLowerCase())
                            .get(DirectoryBackend.USER_ID));
        }

        try {

            directoryBackend.getInfoFromAuthenticatedUser("UserA", "pw-incorrect");
            TestCase.fail();

        } catch (SecurityProblemException e) {
        }

        try {

            directoryBackend.getInfoFromAuthenticatedUser("non-existing-user", "pw");
            TestCase.fail();

        } catch (EntryNotFoundException e) {
        }
    }

    @Test
    public void test_003_getAllEntries()
            throws Exception {

        Assert.assertArrayEquals(indices.stream().map((x) -> "Group" + x).toArray(),
                directoryBackend.getAllGroups().toArray());

        Assert.assertArrayEquals(indices.stream().map((x) -> "User" + x).toArray(),
                directoryBackend.getAllUsers().toArray());
    }

    @Test
    public void test_004_getDirectGroupsOfUser()
            throws Exception {

        Assert.assertArrayEquals(new String[]{"GroupA", "GroupB"},
                directoryBackend.getDirectGroupsOfUser("UserA").toArray());

        Assert.assertArrayEquals(new String[]{"GroupA", "GroupB"},
                directoryBackend.getDirectGroupsOfUser("UserB").toArray());

        Assert.assertArrayEquals(new String[]{"GroupB"},
                directoryBackend.getDirectGroupsOfUser("UserC").toArray());

        Assert.assertArrayEquals(new String[]{"GroupA", "GroupC"},
                directoryBackend.getDirectGroupsOfUser("UserD").toArray());

        Assert.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getDirectGroupsOfUser("UserE").toArray());
    }

    @Test
    public void test_005_getDirectUsersOfGroup()
            throws Exception {

        Assert.assertArrayEquals(new String[]{"UserA", "UserB", "UserD"},
                directoryBackend.getDirectUsersOfGroup("GroupA").toArray());

        Assert.assertArrayEquals(new String[]{"UserA", "UserB", "UserC"},
                directoryBackend.getDirectUsersOfGroup("GroupB").toArray());

        Assert.assertArrayEquals(new String[]{"UserD"},
                directoryBackend.getDirectUsersOfGroup("GroupC").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getDirectUsersOfGroup("GroupD").toArray());

        Assert.assertArrayEquals(new String[]{"UserE"},
                directoryBackend.getDirectUsersOfGroup("GroupE").toArray());
    }

    @Test
    public void test_006_getDirectChildGroupsOfGroup()
            throws Exception {

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getDirectChildGroupsOfGroup("GroupA").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getDirectChildGroupsOfGroup("GroupB").toArray());

        Assert.assertArrayEquals(new String[]{"GroupA"},
                directoryBackend.getDirectChildGroupsOfGroup("GroupC").toArray());

        Assert.assertArrayEquals(new String[]{"GroupC"},
                directoryBackend.getDirectChildGroupsOfGroup("GroupD").toArray());

        Assert.assertArrayEquals(new String[]{"GroupD"},
                directoryBackend.getDirectChildGroupsOfGroup("GroupE").toArray());
    }

    @Test
    public void test_007_getDirectParentGroupsOfGroup()
            throws Exception {

        Assert.assertArrayEquals(new String[]{"GroupC"},
                directoryBackend.getDirectParentGroupsOfGroup("GroupA").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getDirectParentGroupsOfGroup("GroupB").toArray());

        Assert.assertArrayEquals(new String[]{"GroupD"},
                directoryBackend.getDirectParentGroupsOfGroup("GroupC").toArray());

        Assert.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getDirectParentGroupsOfGroup("GroupD").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getDirectParentGroupsOfGroup("GroupE").toArray());
    }

    @Test
    public void test_008_getTransitiveGroupsOfUser()
            throws Exception {

        Assert.assertArrayEquals(new String[]{"GroupA", "GroupB", "GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserA").toArray());

        Assert.assertArrayEquals(new String[]{"GroupA", "GroupB", "GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserB").toArray());

        Assert.assertArrayEquals(new String[]{"GroupB"},
                directoryBackend.getTransitiveGroupsOfUser("UserC").toArray());

        Assert.assertArrayEquals(new String[]{"GroupA", "GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserD").toArray());

        Assert.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getTransitiveGroupsOfUser("UserE").toArray());
    }

    @Test
    public void test_009_getTransitiveUsersOfGroup()
            throws Exception {

        Assert.assertArrayEquals(new String[]{"UserA", "UserB", "UserD"},
                directoryBackend.getTransitiveUsersOfGroup("GroupA").toArray());

        Assert.assertArrayEquals(new String[]{"UserA", "UserB", "UserC"},
                directoryBackend.getTransitiveUsersOfGroup("GroupB").toArray());

        Assert.assertArrayEquals(new String[]{"UserD", "UserA", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupC").toArray());

        Assert.assertArrayEquals(new String[]{"UserD", "UserA", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupD").toArray());

        Assert.assertArrayEquals(new String[]{"UserE", "UserD", "UserA", "UserB"},
                directoryBackend.getTransitiveUsersOfGroup("GroupE").toArray());
    }

    @Test
    public void test_010_getTransitiveChildGroupsOfGroup()
            throws Exception {

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupA").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupB").toArray());

        Assert.assertArrayEquals(new String[]{"GroupA"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupC").toArray());

        Assert.assertArrayEquals(new String[]{"GroupC", "GroupA"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupD").toArray());

        Assert.assertArrayEquals(new String[]{"GroupD", "GroupC", "GroupA"},
                directoryBackend.getTransitiveChildGroupsOfGroup("GroupE").toArray());
    }

    @Test
    public void test_011_getTransitiveParentGroupsOfGroup()
            throws Exception {

        Assert.assertArrayEquals(new String[]{"GroupC", "GroupD", "GroupE"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupA").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupB").toArray());

        Assert.assertArrayEquals(new String[]{"GroupD", "GroupE"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupC").toArray());

        Assert.assertArrayEquals(new String[]{"GroupE"},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupD").toArray());

        Assert.assertArrayEquals(new String[]{},
                directoryBackend.getTransitiveParentGroupsOfGroup("GroupE").toArray());
    }
}
