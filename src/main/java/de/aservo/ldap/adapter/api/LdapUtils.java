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

package de.aservo.ldap.adapter.api;

import de.aservo.ldap.adapter.api.entity.DomainEntity;
import de.aservo.ldap.adapter.api.entity.EntityType;
import de.aservo.ldap.adapter.api.entity.UnitEntity;
import de.aservo.ldap.adapter.api.exception.UnsupportedQueryExpressionException;
import de.aservo.ldap.adapter.api.query.*;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.*;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.NoOpNormalizer;
import org.apache.directory.server.core.api.interceptor.context.FilteringOperationContext;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
     * @param schemaManager the schema manager
     * @param entityType    the entry type
     * @return the DN
     */
    public static Dn createDn(SchemaManager schemaManager, EntityType entityType, String dcId) {

        try {

            switch (entityType) {

                case DOMAIN:
                    return new Dn(schemaManager, String.format("dc=%s", dcId));

                case GROUP_UNIT:
                    return new Dn(schemaManager, String.format("ou=%s,dc=%s", LdapUtils.OU_GROUPS, dcId));

                case USER_UNIT:
                    return new Dn(schemaManager, String.format("ou=%s,dc=%s", LdapUtils.OU_USERS, dcId));

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
     * @param schemaManager the schema manager
     * @param entityType    the entry type
     * @param name          the entry name
     * @return the DN
     */
    public static Dn createDn(SchemaManager schemaManager, EntityType entityType, String name, String dcId) {

        try {

            switch (entityType) {

                case GROUP:
                    return new Dn(schemaManager, String.format("cn=%s,ou=%s,dc=%s",
                            Rdn.escapeValue(name), LdapUtils.OU_GROUPS, dcId));

                case USER:
                    return new Dn(schemaManager, String.format("cn=%s,ou=%s,dc=%s",
                            Rdn.escapeValue(name), LdapUtils.OU_USERS, dcId));

                default:
                    return createDn(schemaManager, entityType, dcId);
            }

        } catch (LdapInvalidDnException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Gets group ID from DN.
     *
     * @param schemaManager the schema manager
     * @param dn            the DN
     * @return the group ID
     */
    public static String getGroupIdFromDn(SchemaManager schemaManager, String dn, String dcId) {

        try {

            Dn queryDn = new Dn(schemaManager, dn);
            String attribute = normalizeAttribute(queryDn.getRdn().getType());

            if ((queryDn.getParent().equals(createDn(schemaManager, EntityType.GROUP_UNIT, dcId)) ||
                    queryDn.getParent().equals(createDn(schemaManager, EntityType.DOMAIN, dcId))) &&
                    attribute.equals(SchemaConstants.CN_AT_OID)) {

                return queryDn.getRdn().getNormValue().toLowerCase();
            }

        } catch (LdapInvalidDnException e) {
        }

        return null;
    }

    /**
     * Gets user ID from DN.
     *
     * @param schemaManager the schema manager
     * @param dn            the DN
     * @return the user ID
     */
    public static String getUserIdFromDn(SchemaManager schemaManager, String dn, String dcId) {

        try {

            Dn queryDn = new Dn(schemaManager, dn);
            String attribute = normalizeAttribute(queryDn.getRdn().getType());

            if ((queryDn.getParent().equals(createDn(schemaManager, EntityType.USER_UNIT, dcId)) ||
                    queryDn.getParent().equals(createDn(schemaManager, EntityType.DOMAIN, dcId))) && (
                    attribute.equals(SchemaConstants.UID_AT_OID) ||
                            attribute.equals(SchemaConstants.CN_AT_OID))) {

                return queryDn.getRdn().getNormValue().toLowerCase();
            }

        } catch (LdapInvalidDnException e) {
        }

        return null;
    }

    /**
     * Creates an internal filter from ApacheDS filter.
     *
     * @param node the filter expression of ApacheDS
     * @return the internal query expression
     */
    public static QueryExpression createQueryExpression(ExprNode node) {

        if (node instanceof AndNode) {

            return new AndLogicExpression(
                    ((AndNode) node).getChildren().stream()
                            .map(LdapUtils::createQueryExpression)
                            .collect(Collectors.toList())
            );

        } else if (node instanceof OrNode) {

            return new OrLogicExpression(
                    ((OrNode) node).getChildren().stream()
                            .map(LdapUtils::createQueryExpression)
                            .collect(Collectors.toList())
            );

        } else if (node instanceof NotNode) {

            return new NotLogicExpression(
                    ((NotNode) node).getChildren().stream()
                            .map(LdapUtils::createQueryExpression)
                            .collect(Collectors.toList())
            );

        } else if (node instanceof EqualityNode) {

            EqualityNode n = (EqualityNode) node;

            return new EqualOperator(n.getAttribute(), n.getValue().toString());

        } else if (node instanceof PresenceNode) {

            PresenceNode n = (PresenceNode) node;

            return new PresenceOperator(n.getAttribute());

        } else if (node instanceof SubstringNode) {

            SubstringNode n = (SubstringNode) node;
            Pattern pattern;

            try {

                pattern = n.getRegex(new NoOpNormalizer());

            } catch (LdapException e) {

                throw new RuntimeException(e);
            }

            List<String> any = n.getAny();

            if (any == null)
                any = new ArrayList<>();

            return new WildcardOperator(n.getAttribute(), pattern, n.getInitial(), n.getFinal(), any);

        } else if (node instanceof ObjectClassNode) {

            return BooleanValue.trueValue();

        } else {

            throw new UnsupportedQueryExpressionException("Cannot evaluate unsupported operator.");
        }
    }

    /**
     * Remove all unnecessary boolean values from query expression.
     *
     * @param expression the query expression
     * @return the transformed query expression
     */
    public static QueryExpression removeValueExpressions(QueryExpression expression) {

        if (expression instanceof AndLogicExpression) {

            // Use logic of allMatch method
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#allMatch-java.util.function.Predicate-

            List<QueryExpression> childs =
                    ((AndLogicExpression) expression).getChildren().stream()
                            .map(LdapUtils::removeValueExpressions)
                            .filter(x -> !(x instanceof BooleanValue && ((BooleanValue) x).getValue()))
                            .collect(Collectors.toList());

            if (childs.isEmpty())
                return new BooleanValue(AndLogicExpression.EMPTY_SEQ_BOOLEAN);

            if (childs.stream().anyMatch(x -> x instanceof BooleanValue && !((BooleanValue) x).getValue()))
                return BooleanValue.falseValue();

            if (childs.size() == 1)
                return childs.get(0);

            return new AndLogicExpression(childs);

        } else if (expression instanceof OrLogicExpression) {

            // Use logic of anyMatch method
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#anyMatch-java.util.function.Predicate-

            List<QueryExpression> childs =
                    ((OrLogicExpression) expression).getChildren().stream()
                            .map(LdapUtils::removeValueExpressions)
                            .filter(x -> !(x instanceof BooleanValue && !((BooleanValue) x).getValue()))
                            .collect(Collectors.toList());

            if (childs.isEmpty())
                return new BooleanValue(OrLogicExpression.EMPTY_SEQ_BOOLEAN);

            if (childs.stream().anyMatch(x -> x instanceof BooleanValue && ((BooleanValue) x).getValue()))
                return BooleanValue.trueValue();

            if (childs.size() == 1)
                return childs.get(0);

            return new OrLogicExpression(childs);

        } else if (expression instanceof NotLogicExpression) {

            // Use logic of noneMatch method
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#noneMatch-java.util.function.Predicate-

            List<QueryExpression> childs =
                    ((NotLogicExpression) expression).getChildren().stream()
                            .map(LdapUtils::removeValueExpressions)
                            .filter(x -> !(x instanceof BooleanValue && !((BooleanValue) x).getValue()))
                            .collect(Collectors.toList());

            if (childs.isEmpty())
                return new BooleanValue(NotLogicExpression.EMPTY_SEQ_BOOLEAN);

            if (childs.stream().anyMatch(x -> x instanceof BooleanValue && ((BooleanValue) x).getValue()))
                return BooleanValue.falseValue();

            return new NotLogicExpression(childs);

        } else {

            return expression;
        }
    }

    /**
     * Remove all not-expressions values from query expression.
     *
     * @param expression the query expression
     * @return the transformed query expression
     */
    public static QueryExpression removeNotExpressions(QueryExpression expression) {

        if (expression instanceof AndLogicExpression) {

            if (((AndLogicExpression) expression).getChildren().size() == 1) {

                QueryExpression child = ((AndLogicExpression) expression).getChildren().get(0);

                return removeNotExpressions(child);

            } else {

                return new AndLogicExpression(
                        ((AndLogicExpression) expression).getChildren().stream()
                                .map(LdapUtils::removeNotExpressions)
                                .collect(Collectors.toList())
                );
            }

        } else if (expression instanceof OrLogicExpression) {

            if (((OrLogicExpression) expression).getChildren().size() == 1) {

                QueryExpression child = ((OrLogicExpression) expression).getChildren().get(0);

                return removeNotExpressions(child);

            } else {

                return new OrLogicExpression(
                        ((OrLogicExpression) expression).getChildren().stream()
                                .map(LdapUtils::removeNotExpressions)
                                .collect(Collectors.toList())
                );
            }

        } else if (expression instanceof NotLogicExpression) {

            if (((NotLogicExpression) expression).getChildren().size() == 1) {

                QueryExpression child = ((NotLogicExpression) expression).getChildren().get(0);

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

                    return removeNotExpressions(new OrLogicExpression(
                            ((NotLogicExpression) child).getChildren()
                    ));

                } else if (child instanceof BooleanValue) {

                    return ((BooleanValue) child).negate();

                } else if (child instanceof OperatorExpression) {

                    return ((OperatorExpression) child).negate();

                } else {

                    return removeNotExpressions(child);
                }

            } else {

                return removeNotExpressions(new AndLogicExpression(
                        ((NotLogicExpression) expression).getChildren().stream()
                                .map(x -> new NotLogicExpression(Collections.singletonList(x)))
                                .collect(Collectors.toList())
                ));
            }

        } else {

            return expression;
        }
    }

    /**
     * Prepare a domain entity related query expression for a final execution.
     *
     * @param expression the query expression
     * @param entity     the entity
     * @return the boolean
     */
    public static QueryExpression preEvaluateExpression(QueryExpression expression, DomainEntity entity) {

        if (expression instanceof AndLogicExpression) {

            return new AndLogicExpression(((AndLogicExpression) expression).getChildren().stream()
                    .map(x -> preEvaluateExpression(x, entity))
                    .collect(Collectors.toList()));

        } else if (expression instanceof OrLogicExpression) {

            return new OrLogicExpression(((OrLogicExpression) expression).getChildren().stream()
                    .map(x -> preEvaluateExpression(x, entity))
                    .collect(Collectors.toList()));

        } else if (expression instanceof NotLogicExpression) {

            return new NotLogicExpression(((NotLogicExpression) expression).getChildren().stream()
                    .map(x -> preEvaluateExpression(x, entity))
                    .collect(Collectors.toList()));

        } else if (expression instanceof OperatorExpression) {

            OperatorExpression operator = (OperatorExpression) expression;

            switch (LdapUtils.normalizeAttribute(operator.getAttribute())) {

                case SchemaConstants.OBJECT_CLASS_AT_OID:

                    return new BooleanValue(operator.check(SchemaConstants.DOMAIN_OC) ||
                            operator.check(SchemaConstants.TOP_OC));

                case SchemaConstants.DC_AT:

                    return new BooleanValue(operator.check(entity.getId()));

                case SchemaConstants.DESCRIPTION_AT_OID:

                    return new BooleanValue(operator.check(entity.getDescription()));

                default:

                    if (operator instanceof PresenceOperator)
                        return new BooleanValue(operator.isNegated());

                    return BooleanValue.falseValue();
            }

        } else {

            return expression;
        }
    }

    /**
     * Prepare an unit entity related query expression for a final execution.
     *
     * @param expression the query expression
     * @param entity     the entity
     * @return the boolean
     */
    public static QueryExpression preEvaluateExpression(QueryExpression expression, UnitEntity entity) {

        if (expression instanceof AndLogicExpression) {

            return new AndLogicExpression(((AndLogicExpression) expression).getChildren().stream()
                    .map(x -> preEvaluateExpression(x, entity))
                    .collect(Collectors.toList()));

        } else if (expression instanceof OrLogicExpression) {

            return new OrLogicExpression(((OrLogicExpression) expression).getChildren().stream()
                    .map(x -> preEvaluateExpression(x, entity))
                    .collect(Collectors.toList()));

        } else if (expression instanceof NotLogicExpression) {

            return new NotLogicExpression(((NotLogicExpression) expression).getChildren().stream()
                    .map(x -> preEvaluateExpression(x, entity))
                    .collect(Collectors.toList()));

        } else if (expression instanceof OperatorExpression) {

            OperatorExpression operator = (OperatorExpression) expression;

            switch (LdapUtils.normalizeAttribute(operator.getAttribute())) {

                case SchemaConstants.OBJECT_CLASS_AT_OID:

                    return new BooleanValue(operator.check(SchemaConstants.ORGANIZATIONAL_UNIT_OC) ||
                            operator.check(SchemaConstants.TOP_OC));

                case SchemaConstants.OU_AT_OID:

                    return new BooleanValue(operator.check(entity.getId()));

                case SchemaConstants.DESCRIPTION_AT_OID:

                    return new BooleanValue(operator.check(entity.getDescription()));

                default:

                    if (operator instanceof PresenceOperator)
                        return new BooleanValue(operator.isNegated());

                    return BooleanValue.falseValue();
            }

        } else {

            return expression;
        }
    }

    /**
     * EPrepare a group related query expression for a final execution.
     *
     * @param expression the query expression
     * @return the boolean
     */
    public static QueryExpression preEvaluateExpressionForGroup(QueryExpression expression) {

        if (expression instanceof AndLogicExpression) {

            return new AndLogicExpression(((AndLogicExpression) expression).getChildren().stream()
                    .map(LdapUtils::preEvaluateExpressionForGroup)
                    .collect(Collectors.toList()));

        } else if (expression instanceof OrLogicExpression) {

            return new OrLogicExpression(((OrLogicExpression) expression).getChildren().stream()
                    .map(LdapUtils::preEvaluateExpressionForGroup)
                    .collect(Collectors.toList()));

        } else if (expression instanceof NotLogicExpression) {

            return new NotLogicExpression(((NotLogicExpression) expression).getChildren().stream()
                    .map(LdapUtils::preEvaluateExpressionForGroup)
                    .collect(Collectors.toList()));

        } else if (expression instanceof OperatorExpression) {

            OperatorExpression operator = (OperatorExpression) expression;

            switch (LdapUtils.normalizeAttribute(operator.getAttribute())) {

                case SchemaConstants.OBJECT_CLASS_AT_OID:

                    return new BooleanValue(operator.check(SchemaConstants.GROUP_OF_NAMES_OC) ||
                            operator.check(SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC) ||
                            operator.check(SchemaConstants.TOP_OC));

                case SchemaConstants.OU_AT_OID:

                    return new BooleanValue(operator.check(LdapUtils.OU_GROUPS));

                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.DESCRIPTION_AT_OID:

                    if (operator instanceof PresenceOperator)
                        return new BooleanValue(!operator.isNegated());

                    return operator;

                case SchemaConstants.MEMBER_AT_OID:
                case SchemaConstants.UNIQUE_MEMBER_AT_OID:
                case LdapUtils.MEMBER_OF_AT_OID:

                    return operator;

                default:

                    if (operator instanceof PresenceOperator)
                        return new BooleanValue(operator.isNegated());

                    return BooleanValue.falseValue();
            }

        } else {

            return expression;
        }
    }

    /**
     * Prepare an user related query expression for a final execution.
     *
     * @param expression the query expression
     * @return the boolean
     */
    public static QueryExpression preEvaluateExpressionForUser(QueryExpression expression) {

        if (expression instanceof AndLogicExpression) {

            return new AndLogicExpression(((AndLogicExpression) expression).getChildren().stream()
                    .map(LdapUtils::preEvaluateExpressionForUser)
                    .collect(Collectors.toList()));

        } else if (expression instanceof OrLogicExpression) {

            return new OrLogicExpression(((OrLogicExpression) expression).getChildren().stream()
                    .map(LdapUtils::preEvaluateExpressionForUser)
                    .collect(Collectors.toList()));

        } else if (expression instanceof NotLogicExpression) {

            return new NotLogicExpression(((NotLogicExpression) expression).getChildren().stream()
                    .map(LdapUtils::preEvaluateExpressionForUser)
                    .collect(Collectors.toList()));

        } else if (expression instanceof OperatorExpression) {

            OperatorExpression operator = (OperatorExpression) expression;

            switch (LdapUtils.normalizeAttribute(operator.getAttribute())) {

                case SchemaConstants.OBJECT_CLASS_AT_OID:

                    return new BooleanValue(operator.check(SchemaConstants.INET_ORG_PERSON_OC) ||
                            operator.check(SchemaConstants.ORGANIZATIONAL_PERSON_OC) ||
                            operator.check(SchemaConstants.PERSON_OC) ||
                            operator.check(SchemaConstants.TOP_OC));

                case SchemaConstants.OU_AT_OID:

                    return new BooleanValue(operator.check(LdapUtils.OU_USERS));

                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.SN_AT_OID:
                case SchemaConstants.GN_AT_OID:
                case SchemaConstants.DISPLAY_NAME_AT_OID:
                case SchemaConstants.MAIL_AT_OID:

                    if (operator instanceof PresenceOperator)
                        return new BooleanValue(!operator.isNegated());

                    return operator;

                case LdapUtils.MEMBER_OF_AT_OID:

                    return operator;

                default:

                    if (operator instanceof PresenceOperator)
                        return new BooleanValue(operator.isNegated());

                    return BooleanValue.falseValue();
            }

        } else {

            return expression;
        }
    }

    /**
     * Evaluate a query expression to a boolean value.
     *
     * @param expression the query expression
     * @return the boolean
     */
    public static boolean evaluateExpression(QueryExpression expression) {

        if (expression instanceof AndLogicExpression) {

            return ((AndLogicExpression) expression).getChildren().stream()
                    .allMatch(LdapUtils::evaluateExpression);

        } else if (expression instanceof OrLogicExpression) {

            return ((OrLogicExpression) expression).getChildren().stream()
                    .anyMatch(LdapUtils::evaluateExpression);

        } else if (expression instanceof NotLogicExpression) {

            return ((NotLogicExpression) expression).getChildren().stream()
                    .noneMatch(LdapUtils::evaluateExpression);

        } else if (expression instanceof BooleanValue) {

            return ((BooleanValue) expression).getValue();

        } else
            throw new IllegalArgumentException("Expression is not ready with element " +
                    expression.getClass().getSimpleName());
    }

    /**
     * Normalizes LDAP attributes.
     * Function is used for attributes of incoming queries.
     *
     * @param context the filtering operation context
     * @return the normalized attributes
     */
    public static Set<String> getAttributes(FilteringOperationContext context) {

        return Arrays.stream(context.getReturningAttributesString())
                .map(LdapUtils::normalizeAttribute)
                .collect(Collectors.toSet());
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
