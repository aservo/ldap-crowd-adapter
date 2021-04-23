/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aservo.ldap.adapter.adapter;

import com.aservo.ldap.adapter.adapter.entity.Entity;
import com.aservo.ldap.adapter.adapter.entity.EntityType;
import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.UndefinedNode;
import com.aservo.ldap.adapter.adapter.query.*;
import com.aservo.ldap.adapter.backend.DirectoryBackend;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.*;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.jetbrains.annotations.Nullable;


/**
 * A helper for LDAP handling.
 */
public class LdapUtils {

    private LdapUtils() {
    }

    /**
     * The constant OU_GROUPS.
     */
    public static final String OU_GROUPS = "groups";
    /**
     * The constant OU_USERS.
     */
    public static final String OU_USERS = "users";

    /**
     * The constant MEMBER_OF_AT.
     */
    public static final String MEMBER_OF_AT = "memberOf";

    /**
     * The constant MEMBER_OF_AT_OID.
     */
    public static final String MEMBER_OF_AT_OID = "1.2.840.113556.1.2.102";

    /**
     * Creates a DN with suffix.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the related directory backend
     * @param entityType       the entry type
     * @return the DN
     */
    public static Dn createDn(SchemaManager schemaManager, DirectoryBackend directoryBackend, EntityType entityType) {

        try {

            switch (entityType) {

                case DOMAIN:
                    return new Dn(schemaManager, String.format("dc=%s",
                            directoryBackend.getId()));

                case GROUP_UNIT:
                    return new Dn(schemaManager, String.format("ou=%s,dc=%s",
                            LdapUtils.OU_GROUPS, directoryBackend.getId()));

                case USER_UNIT:
                    return new Dn(schemaManager, String.format("ou=%s,dc=%s",
                            LdapUtils.OU_USERS, directoryBackend.getId()));

                default:
                    throw new IllegalArgumentException("Cannot create DN from unknown entity.");
            }

        } catch (LdapInvalidDnException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a DN with suffix.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the related directory backend
     * @param entityType       the entry type
     * @param id               the entry ID
     * @return the DN
     */
    public static Dn createDn(SchemaManager schemaManager, DirectoryBackend directoryBackend, EntityType entityType,
                              String id) {

        try {

            switch (entityType) {

                case GROUP:
                    return new Dn(schemaManager, String.format("cn=%s,ou=%s,dc=%s",
                            Rdn.escapeValue(id), LdapUtils.OU_GROUPS, directoryBackend.getId()));

                case USER:
                    return new Dn(schemaManager, String.format("cn=%s,ou=%s,dc=%s",
                            Rdn.escapeValue(id), LdapUtils.OU_USERS, directoryBackend.getId()));

                default:
                    return createDn(schemaManager, directoryBackend, entityType);
            }

        } catch (LdapInvalidDnException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a DN with suffix.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the related directory backend
     * @param entity           the entry
     * @return the DN
     */
    public static Dn createDn(SchemaManager schemaManager, DirectoryBackend directoryBackend, Entity entity) {

        return createDn(schemaManager, directoryBackend, entity.getEntityType(), entity.getId());
    }

    /**
     * Gets group from DN.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the related directory backend
     * @param dn               the DN
     * @return the group ID
     */
    @Nullable
    public static String getGroupIdFromDn(SchemaManager schemaManager, DirectoryBackend directoryBackend, String dn) {

        try {

            Dn queryDn = new Dn(schemaManager, dn);
            String attribute = normalizeAttribute(queryDn.getRdn().getType());

            if ((queryDn.getParent().equals(createDn(schemaManager, directoryBackend, EntityType.GROUP_UNIT)) ||
                    queryDn.getParent().equals(createDn(schemaManager, directoryBackend, EntityType.DOMAIN))) &&
                    attribute.equals(SchemaConstants.CN_AT_OID)) {

                return queryDn.getRdn().getNormValue();
            }

        } catch (LdapInvalidDnException e) {
        }

        return null;
    }

    /**
     * Gets user from DN.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the related directory backend
     * @param dn               the DN
     * @return the user ID
     */
    @Nullable
    public static String getUserIdFromDn(SchemaManager schemaManager, DirectoryBackend directoryBackend, String dn) {

        try {

            Dn queryDn = new Dn(schemaManager, dn);
            String attribute = normalizeAttribute(queryDn.getRdn().getType());

            if ((queryDn.getParent().equals(createDn(schemaManager, directoryBackend, EntityType.USER_UNIT)) ||
                    queryDn.getParent().equals(createDn(schemaManager, directoryBackend, EntityType.DOMAIN))) && (
                    attribute.equals(SchemaConstants.UID_AT_OID) ||
                            attribute.equals(SchemaConstants.CN_AT_OID))) {

                return queryDn.getRdn().getNormValue();
            }

        } catch (LdapInvalidDnException e) {
        }

        return null;
    }

    /**
     * Check user and child group membership by DN.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the directory backend
     * @param group            the group entity
     * @param dn               the DN
     * @param flattening       the flattening enabled flag
     * @param negated          the flag to negate the check
     * @return the boolean
     */
    public static boolean isGroupMember(SchemaManager schemaManager, DirectoryBackend directoryBackend,
                                        GroupEntity group, String dn, boolean flattening, boolean negated) {

        String id = LdapUtils.getUserIdFromDn(schemaManager, directoryBackend, dn);

        if (id != null) {

            if (flattening)
                return directoryBackend.isUserTransitiveGroupMember(id, group.getId()) != negated;

            return directoryBackend.isUserDirectGroupMember(id, group.getId()) != negated;

        } else {

            id = LdapUtils.getGroupIdFromDn(schemaManager, directoryBackend, dn);

            if (id != null) {

                if (flattening)
                    return directoryBackend.isGroupTransitiveGroupMember(id, group.getId()) != negated;

                return directoryBackend.isGroupDirectGroupMember(id, group.getId()) != negated;
            }
        }

        return false;
    }

    /**
     * Check group membership by DN.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the directory backend
     * @param group            the group entity
     * @param dn               the DN
     * @param flattening       the flattening enabled flag
     * @param negated          the flag to negate the check
     * @return the boolean
     */
    public static boolean isMemberOfGroup(SchemaManager schemaManager, DirectoryBackend directoryBackend,
                                          GroupEntity group, String dn, boolean flattening, boolean negated) {

        String id = LdapUtils.getGroupIdFromDn(schemaManager, directoryBackend, dn);

        if (id != null) {

            if (flattening)
                return directoryBackend.isGroupTransitiveGroupMember(id, group.getId()) != negated;

            return directoryBackend.isGroupDirectGroupMember(id, group.getId()) != negated;
        }

        return false;
    }

    /**
     * Check user membership by DN.
     *
     * @param schemaManager    the schema manager
     * @param directoryBackend the directory backend
     * @param user             the user entity
     * @param dn               the DN
     * @param flattening       the flattening enabled flag
     * @param negated          the flag to negate the check
     * @return the boolean
     */
    public static boolean isMemberOfGroup(SchemaManager schemaManager, DirectoryBackend directoryBackend,
                                          UserEntity user, String dn, boolean flattening, boolean negated) {

        String id = LdapUtils.getGroupIdFromDn(schemaManager, directoryBackend, dn);

        if (id != null) {

            if (flattening)
                return directoryBackend.isUserTransitiveGroupMember(user.getId(), id) != negated;

            return directoryBackend.isUserDirectGroupMember(user.getId(), id) != negated;
        }

        return false;
    }

    /**
     * Find all IDs of group members.
     *
     * @param directoryBackend the directory backend
     * @param groupId          the group ID
     * @param flattening       the flattening enabled flag
     * @return the list of group IDs
     */
    public static List<String> findGroupMembers(DirectoryBackend directoryBackend, String groupId, boolean flattening)
            throws EntityNotFoundException {

        if (!flattening)
            return directoryBackend.getDirectChildGroupIdsOfGroup(groupId);

        return Collections.emptyList();
    }

    /**
     * Find all IDs of user members.
     *
     * @param directoryBackend the directory backend
     * @param groupId          the group ID
     * @param flattening       the flattening enabled flag
     * @return the list of user IDs
     */
    public static List<String> findUserMembers(DirectoryBackend directoryBackend, String groupId, boolean flattening)
            throws EntityNotFoundException {

        if (flattening)
            return directoryBackend.getTransitiveUserIdsOfGroup(groupId);

        return directoryBackend.getDirectUserIdsOfGroup(groupId);
    }

    /**
     * Find all IDs of reverse group members.
     *
     * @param directoryBackend the directory backend
     * @param groupId          the group ID
     * @param flattening       the flattening enabled flag
     * @return the list of group IDs
     */
    public static List<String> findGroupMembersReverse(DirectoryBackend directoryBackend, String groupId, boolean flattening)
            throws EntityNotFoundException {

        if (!flattening)
            return directoryBackend.getDirectParentGroupIdsOfGroup(groupId);

        return Collections.emptyList();
    }

    /**
     * Find all IDs of reverse user members.
     *
     * @param directoryBackend the directory backend
     * @param userId           the user ID
     * @param flattening       the flattening enabled flag
     * @return the list of user IDs
     */
    public static List<String> findUserMembersReverse(DirectoryBackend directoryBackend, String userId, boolean flattening)
            throws EntityNotFoundException {

        if (flattening)
            return directoryBackend.getTransitiveGroupIdsOfUser(userId);

        return directoryBackend.getDirectGroupIdsOfUser(userId);
    }

    /**
     * Creates an internal filter from ApacheDS filter.
     *
     * @param node the filter expression of ApacheDS
     * @return the internal filter expression
     */
    public static FilterNode createInternalFilterNode(ExprNode node) {

        if (node instanceof AndNode) {

            return new AndLogicExpression(
                    ((AndNode) node).getChildren().stream()
                            .map(LdapUtils::createInternalFilterNode)
                            .collect(Collectors.toList())
            );

        } else if (node instanceof OrNode) {

            return new OrLogicExpression(
                    ((OrNode) node).getChildren().stream()
                            .map(LdapUtils::createInternalFilterNode)
                            .collect(Collectors.toList())
            );

        } else if (node instanceof NotNode) {

            return new NotLogicExpression(
                    ((NotNode) node).getChildren().stream()
                            .map(LdapUtils::createInternalFilterNode)
                            .collect(Collectors.toList())
            );

        } else if (node instanceof EqualityNode) {

            return new EqualOperator(((EqualityNode) node).getAttribute(), ((EqualityNode) node).getValue().toString());

        } else if (node instanceof PresenceNode) {

            return new PresenceOperator(((PresenceNode) node).getAttribute());

        } else if (node instanceof ObjectClassNode) {

            return new NullNode();

        } else {

            return new UndefinedNode();
        }
    }

    /**
     * Remove null values from internal filter expression.
     *
     * @param filterNode the filter expression
     * @return the transformed filter expression
     */
    public static FilterNode removeNotExpressions(FilterNode filterNode) {

        if (filterNode instanceof AndLogicExpression) {

            if (((AndLogicExpression) filterNode).getChildren().size() == 1) {

                FilterNode child = ((AndLogicExpression) filterNode).getChildren().get(0);

                return removeNotExpressions(child);

            } else {

                return new AndLogicExpression(
                        ((AndLogicExpression) filterNode).getChildren().stream()
                                .map(LdapUtils::removeNotExpressions)
                                .collect(Collectors.toList())
                );
            }

        } else if (filterNode instanceof OrLogicExpression) {

            if (((OrLogicExpression) filterNode).getChildren().size() == 1) {

                FilterNode child = ((OrLogicExpression) filterNode).getChildren().get(0);

                return removeNotExpressions(child);

            } else {

                return new OrLogicExpression(
                        ((OrLogicExpression) filterNode).getChildren().stream()
                                .map(LdapUtils::removeNotExpressions)
                                .collect(Collectors.toList())
                );
            }

        } else if (filterNode instanceof NotLogicExpression) {

            if (((NotLogicExpression) filterNode).getChildren().size() == 1) {

                FilterNode child = ((NotLogicExpression) filterNode).getChildren().get(0);

                if (child instanceof AndLogicExpression) {

                    return removeNotExpressions(new OrLogicExpression(
                            ((AndLogicExpression) child).getChildren().stream()
                                    .map(x -> new NotLogicExpression(Collections.singletonList(x)))
                                    .collect(Collectors.toList())
                    ));

                } else if (child instanceof OrLogicExpression) {

                    return removeNotExpressions(new AndLogicExpression(
                            ((OrLogicExpression) child).getChildren().stream()
                                    .map(x -> new NotLogicExpression(Collections.singletonList(x)))
                                    .collect(Collectors.toList())
                    ));

                } else if (child instanceof NotLogicExpression) {

                    return removeNotExpressions(new AndLogicExpression(
                            ((NotLogicExpression) child).getChildren()
                    ));

                } else if (child instanceof EqualOperator) {

                    return removeNotExpressions(new NotEqualOperator(
                            ((EqualOperator) child).getAttribute(),
                            ((EqualOperator) child).getValue()
                    ));

                } else if (child instanceof NotEqualOperator) {

                    return removeNotExpressions(new EqualOperator(
                            ((NotEqualOperator) child).getAttribute(),
                            ((NotEqualOperator) child).getValue()
                    ));

                } else if (child instanceof PresenceOperator) {

                    return removeNotExpressions(new NoPresenceOperator(
                            ((PresenceOperator) child).getAttribute()
                    ));

                } else if (child instanceof NoPresenceOperator) {

                    return removeNotExpressions(new PresenceOperator(
                            ((NoPresenceOperator) child).getAttribute()
                    ));

                } else {

                    return removeNotExpressions(child);
                }

            } else {

                return removeNotExpressions(new OrLogicExpression(
                        ((NotLogicExpression) filterNode).getChildren().stream()
                                .map(x -> new NotLogicExpression(Collections.singletonList(x)))
                                .collect(Collectors.toList())
                ));
            }

        } else {

            return filterNode;
        }
    }

    /**
     * Normalizes LDAP attributes.
     * Function is used for attributes of incoming queries.
     *
     * @param attribute the attribute
     * @return the normalized attribute
     */
    public static String normalizeAttribute(String attribute) {

        if (attribute.equalsIgnoreCase(SchemaConstants.DC_AT) ||
                attribute.equalsIgnoreCase(SchemaConstants.DOMAIN_COMPONENT_AT) ||
                attribute.equals(SchemaConstants.DOMAIN_COMPONENT_AT_OID))
            return SchemaConstants.DC_AT;

        if (attribute.equals(SchemaConstants.OBJECT_CLASS_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.OBJECT_CLASS_AT))
            return SchemaConstants.OBJECT_CLASS_AT_OID;

        if (attribute.equals(SchemaConstants.OU_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.OU_AT) ||
                attribute.equalsIgnoreCase(SchemaConstants.ORGANIZATIONAL_UNIT_NAME_AT))
            return SchemaConstants.OU_AT_OID;

        if (attribute.equals(SchemaConstants.UID_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.UID_AT) ||
                attribute.equalsIgnoreCase(SchemaConstants.USER_ID_AT))
            return SchemaConstants.UID_AT_OID;

        if (attribute.equals(SchemaConstants.CN_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.CN_AT) ||
                attribute.equalsIgnoreCase(SchemaConstants.COMMON_NAME_AT))
            return SchemaConstants.CN_AT_OID;

        if (attribute.equals(SchemaConstants.SN_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.SN_AT) ||
                attribute.equalsIgnoreCase(SchemaConstants.SURNAME_AT))
            return SchemaConstants.SN_AT_OID;

        if (attribute.equals(SchemaConstants.GN_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.GN_AT) ||
                attribute.equalsIgnoreCase(SchemaConstants.GIVENNAME_AT))
            return SchemaConstants.GN_AT_OID;

        if (attribute.equals(SchemaConstants.DISPLAY_NAME_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.DISPLAY_NAME_AT))
            return SchemaConstants.DISPLAY_NAME_AT_OID;

        if (attribute.equals(SchemaConstants.MAIL_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.MAIL_AT))
            return SchemaConstants.MAIL_AT_OID;

        if (attribute.equals(SchemaConstants.DESCRIPTION_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.DESCRIPTION_AT))
            return SchemaConstants.DESCRIPTION_AT_OID;

        if (attribute.equals(SchemaConstants.MEMBER_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.MEMBER_AT))
            return SchemaConstants.MEMBER_AT_OID;

        if (attribute.equals(SchemaConstants.UNIQUE_MEMBER_AT_OID) ||
                attribute.equalsIgnoreCase(SchemaConstants.UNIQUE_MEMBER_AT))
            return SchemaConstants.UNIQUE_MEMBER_AT_OID;

        if (attribute.equals(MEMBER_OF_AT_OID) ||
                attribute.equalsIgnoreCase(MEMBER_OF_AT))
            return MEMBER_OF_AT_OID;

        return attribute;
    }
}
