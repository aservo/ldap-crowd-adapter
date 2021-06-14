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

package com.aservo.ldap.adapter.sql.impl;

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.database.QueryDef;
import com.aservo.ldap.adapter.api.database.QueryDefFactory;
import com.aservo.ldap.adapter.api.entity.EntityType;
import com.aservo.ldap.adapter.api.query.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.SchemaManager;


public class QueryGenerator {

    private static final int GROUP_MEMBER_OF_FLAG = 1;
    private static final int USER_MEMBER_OF_FLAG = 2;
    private static final int GROUP_MEMBER_GROUP_FLAG = 4;
    private static final int GROUP_MEMBER_USER_FLAG = 8;

    private final SchemaManager schemaManager;
    private final String dcId;
    private final boolean flattening;
    private final boolean activeUsersOnly;
    private final boolean useMaterializedViews;

    public QueryGenerator(SchemaManager schemaManager, String dcId, boolean flattening, boolean activeUsersOnly,
                          boolean useMaterializedViews) {

        this.schemaManager = schemaManager;
        this.dcId = dcId;
        this.flattening = flattening;
        this.activeUsersOnly = activeUsersOnly;
        this.useMaterializedViews = useMaterializedViews;
    }

    public QueryDef generate(EntityType entityType, QueryDefFactory factory, QueryExpression expression) {

        if (entityType == EntityType.GROUP)
            expression = LdapUtils.preEvaluateExpressionForGroup(expression);
        else if (entityType == EntityType.USER)
            expression = LdapUtils.preEvaluateExpressionForUser(expression);
        else
            throw new IllegalArgumentException("Expect supported entity type.");

        expression = LdapUtils.removeNotExpressions(LdapUtils.removeValueExpressions(expression));

        StringBuilder builder = new StringBuilder();
        List<Object> arguments = new ArrayList<>();
        int joinPlan = createJoinPlan(entityType);

        generateSelectClause(builder, entityType, joinPlan, arguments);
        generateJoinClauses(builder, entityType, joinPlan);
        generateWhereClause(builder, entityType, joinPlan, expression, arguments);
        generateOrderByClause(builder, entityType);

        return factory.query(builder.toString()).on(arguments);
    }

    private int createJoinPlan(EntityType entityType) {

        int mask = 0;

        if (entityType == EntityType.GROUP)
            mask |= GROUP_MEMBER_USER_FLAG;

        if (entityType == EntityType.GROUP && !flattening)
            mask |= GROUP_MEMBER_OF_FLAG | GROUP_MEMBER_GROUP_FLAG;

        if (entityType == EntityType.USER)
            mask |= USER_MEMBER_OF_FLAG;

        return mask;
    }

    private void generateSelectClause(StringBuilder builder, EntityType entityType, int joinPlan,
                                      List<Object> arguments) {

        if (entityType == EntityType.GROUP) {

            builder.append("select ? as type, g.*");
            arguments.add(EntityType.GROUP.toString().toLowerCase());

            if ((joinPlan & GROUP_MEMBER_OF_FLAG) != 0)
                builder.append(", gp.name as parent_group_name");

            if ((joinPlan & GROUP_MEMBER_GROUP_FLAG) != 0)
                builder.append(", gc.name as member_group_name");

            if ((joinPlan & GROUP_MEMBER_USER_FLAG) != 0)
                builder.append(", u.username as member_user_username");

            builder.append(" from _Group g ");

        } else if (entityType == EntityType.USER) {

            builder.append("select ? as type, u.*");
            arguments.add(EntityType.USER.toString().toLowerCase());

            if ((joinPlan & USER_MEMBER_OF_FLAG) != 0)
                builder.append(", g.name as parent_group_name");

            builder.append(" from _User u ");
        }
    }

    private void generateJoinClauses(StringBuilder builder, EntityType entityType, int joinPlan) {

        if (entityType == EntityType.GROUP) {

            if ((joinPlan & GROUP_MEMBER_OF_FLAG) != 0) {

                if (flattening)
                    if (useMaterializedViews)
                        builder.append("left outer join _Group_Membership_Transitive mp ");
                    else
                        builder.append("left outer join _Group_Membership_Transitive_Non_Materialized mp ");
                else
                    builder.append("left outer join _Group_Membership mp ");

                builder.append("on mp.member_group_id = g.id ");

                builder.append("left outer join _Group gp on gp.id = mp.parent_group_id ");
            }

            if ((joinPlan & GROUP_MEMBER_GROUP_FLAG) != 0) {

                if (flattening)
                    if (useMaterializedViews)
                        builder.append("left outer join _Group_Membership_Transitive mc ");
                    else
                        builder.append("left outer join _Group_Membership_Transitive_Non_Materialized mc ");
                else
                    builder.append("left outer join _Group_Membership mc ");

                builder.append("on mc.parent_group_id = g.id ");

                builder.append("left outer join _Group gc on gc.id = mc.member_group_id ");
            }

            if ((joinPlan & GROUP_MEMBER_USER_FLAG) != 0) {

                if (flattening)
                    if (useMaterializedViews)
                        builder.append("left outer join _User_Membership_Transitive mu ");
                    else
                        builder.append("left outer join _User_Membership_Transitive_Non_Materialized mu ");
                else
                    builder.append("left outer join _User_Membership mu ");

                builder.append("on mu.parent_group_id = g.id ");

                builder.append("left outer join _User u on u.id = mu.member_user_id ");
            }

        } else if (entityType == EntityType.USER) {

            if ((joinPlan & USER_MEMBER_OF_FLAG) != 0) {

                if (flattening)
                    if (useMaterializedViews)
                        builder.append("left outer join _User_Membership_Transitive mu ");
                    else
                        builder.append("left outer join _User_Membership_Transitive_Non_Materialized mu ");
                else
                    builder.append("left outer join _User_Membership mu ");

                builder.append("on mu.member_user_id = u.id ");

                builder.append("left outer join _Group g on g.id = mu.parent_group_id ");
            }
        }
    }

    private void generateWhereClause(StringBuilder builder, EntityType entityType, int joinPlan,
                                     QueryExpression expression, List<Object> arguments) {

        builder.append("where ");

        if ((joinPlan & (USER_MEMBER_OF_FLAG | GROUP_MEMBER_USER_FLAG)) != 0) {

            builder.append("( ");
        }

        transformQueryExpressionToSql(builder, entityType, joinPlan, expression, arguments);

        if ((joinPlan & (USER_MEMBER_OF_FLAG | GROUP_MEMBER_USER_FLAG)) != 0) {

            builder.append(") and ( u.active is null or u.active or ? = 'false' ) ");
            arguments.add(activeUsersOnly);
        }
    }

    private void generateOrderByClause(StringBuilder builder, EntityType entityType) {

        if (entityType == EntityType.GROUP)
            builder.append("order by g.id");
        else if (entityType == EntityType.USER)
            builder.append("order by u.id");
    }

    private void transformQueryExpressionToSql(StringBuilder builder, EntityType entityType, int joinPlan,
                                               QueryExpression expression, List<Object> arguments) {

        if (expression instanceof BooleanValue) {

            builder.append("? = 'true' ");
            arguments.add(((BooleanValue) expression).getValue());

        } else if (expression instanceof AndLogicExpression) {

            Iterator<QueryExpression> iter = ((AndLogicExpression) expression).getChildren().iterator();

            if (!iter.hasNext()) {

                builder.append("? = 'true' ");
                arguments.add(AndLogicExpression.EMPTY_SEQ_BOOLEAN);

            } else {

                QueryExpression first = iter.next();

                if (!iter.hasNext()) {

                    transformQueryExpressionToSql(builder, entityType, joinPlan, first, arguments);

                } else {

                    builder.append("( ");
                    transformQueryExpressionToSql(builder, entityType, joinPlan, first, arguments);

                    while (iter.hasNext()) {

                        builder.append(" and ");
                        transformQueryExpressionToSql(builder, entityType, joinPlan, iter.next(), arguments);
                    }

                    builder.append(") ");
                }
            }

        } else if (expression instanceof OrLogicExpression) {

            Iterator<QueryExpression> iter = ((OrLogicExpression) expression).getChildren().iterator();

            if (!iter.hasNext()) {

                builder.append("? = 'true' ");
                arguments.add(OrLogicExpression.EMPTY_SEQ_BOOLEAN);

            } else {

                QueryExpression first = iter.next();

                if (!iter.hasNext()) {

                    transformQueryExpressionToSql(builder, entityType, joinPlan, first, arguments);

                } else {

                    builder.append("( ");
                    transformQueryExpressionToSql(builder, entityType, joinPlan, first, arguments);

                    while (iter.hasNext()) {

                        builder.append(" or ");
                        transformQueryExpressionToSql(builder, entityType, joinPlan, iter.next(), arguments);
                    }

                    builder.append(") ");
                }
            }

        } else if (expression instanceof NotLogicExpression) {

            Iterator<QueryExpression> iter = ((NotLogicExpression) expression).getChildren().iterator();

            if (!iter.hasNext()) {

                builder.append("? = 'true' ");
                arguments.add(NotLogicExpression.EMPTY_SEQ_BOOLEAN);

            } else {

                QueryExpression first = iter.next();

                if (!iter.hasNext()) {

                    builder.append("not ");
                    transformQueryExpressionToSql(builder, entityType, joinPlan, first, arguments);

                } else {

                    // De Morgan's laws:
                    // ¬x ∧ ¬y ≡ ¬(x ∨ y)
                    // ¬x ∨ ¬y ≡ ¬(x ∧ y)

                    builder.append("not ( ");
                    transformQueryExpressionToSql(builder, entityType, joinPlan, first, arguments);

                    while (iter.hasNext()) {

                        builder.append(" or ");
                        transformQueryExpressionToSql(builder, entityType, joinPlan, iter.next(), arguments);
                    }

                    builder.append(") ");
                }
            }

        } else if (expression instanceof OperatorExpression) {

            processOperator(builder, arguments, entityType, joinPlan, ((OperatorExpression) expression));

        } else
            throw new IllegalArgumentException("Cannot process unexpected query expression " +
                    expression.getClass().getName());
    }

    private void processOperator(StringBuilder builder, List<Object> arguments, EntityType entityType, int joinPlan,
                                 OperatorExpression expression) {

        switch (LdapUtils.normalizeAttribute(expression.getAttribute())) {

            case SchemaConstants.UID_AT_OID:

                if (entityType == EntityType.USER) {

                    builder.append("u.id ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.CN_AT_OID:

                if (entityType == EntityType.GROUP) {

                    builder.append("lower(g.name) ");
                    handleOperator(builder, arguments, expression);

                } else if (entityType == EntityType.USER) {

                    builder.append("lower(u.username) ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.SN_AT_OID:

                if (entityType == EntityType.USER) {

                    builder.append("lower(u.last_name) ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.GN_AT_OID:

                if (entityType == EntityType.USER) {

                    builder.append("lower(u.first_name) ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.DISPLAY_NAME_AT_OID:

                if (entityType == EntityType.USER) {

                    builder.append("lower(u.display_name) ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.MAIL_AT_OID:

                if (entityType == EntityType.USER) {

                    builder.append("lower(u.email) ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.DESCRIPTION_AT_OID:

                if (entityType == EntityType.GROUP) {

                    builder.append("lower(g.description) ");
                    handleOperator(builder, arguments, expression);
                }

                break;

            case SchemaConstants.MEMBER_AT_OID:
            case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                if (entityType == EntityType.GROUP) {

                    if (expression instanceof EqualOperator) {

                        String groupId = null;
                        String userId = null;

                        if ((joinPlan & GROUP_MEMBER_GROUP_FLAG) != 0)
                            groupId =
                                    LdapUtils.getGroupIdFromDn(schemaManager,
                                            ((EqualOperator) expression).getValue(), dcId);

                        if ((joinPlan & GROUP_MEMBER_USER_FLAG) != 0)
                            userId =
                                    LdapUtils.getUserIdFromDn(schemaManager,
                                            ((EqualOperator) expression).getValue(), dcId);

                        if (groupId != null) {

                            if (expression.isNegated())
                                builder.append("g.id not in ( select parent_group_id ");
                            else
                                builder.append("g.id in ( select parent_group_id ");

                            if (flattening)
                                if (useMaterializedViews)
                                    builder.append("from _Group_Membership_Transitive ");
                                else
                                    builder.append("from _Group_Membership_Transitive_Non_Materialized ");
                            else
                                builder.append("from _Group_Membership ");

                            builder.append("where member_group_id = ? ");
                            arguments.add(groupId.toLowerCase());
                            builder.append(") ");
                        }

                        if (userId != null) {

                            if (expression.isNegated())
                                builder.append("g.id not in ( select parent_group_id ");
                            else
                                builder.append("g.id in ( select parent_group_id ");

                            if (flattening)
                                if (useMaterializedViews)
                                    builder.append("from _User_Membership_Transitive ");
                                else
                                    builder.append("from _User_Membership_Transitive_Non_Materialized ");
                            else
                                builder.append("from _User_Membership ");

                            builder.append("where member_user_id = ? ");
                            arguments.add(userId.toLowerCase());
                            builder.append(") ");
                        }

                        if (groupId == null && userId == null) {

                            builder.append("? = 'true' ");
                            arguments.add(false);
                        }

                    } else if (expression instanceof PresenceOperator) {

                        if (expression.isNegated())
                            builder.append("( mc.member_group_id is null and mu.member_user_id is null ) ");
                        else
                            builder.append("( mc.member_group_id is not null or mu.member_user_id is not null ) ");
                    }
                }

                break;

            case LdapUtils.MEMBER_OF_AT_OID:

                if (entityType == EntityType.GROUP) {

                    if (expression instanceof EqualOperator) {

                        String groupId = null;

                        if ((joinPlan & GROUP_MEMBER_OF_FLAG) != 0)
                            groupId =
                                    LdapUtils.getGroupIdFromDn(schemaManager,
                                            ((EqualOperator) expression).getValue(), dcId);

                        if (groupId == null) {

                            builder.append("? = 'true' ");
                            arguments.add(false);

                        } else {

                            if (expression.isNegated())
                                builder.append("g.id not in ( select member_group_id ");
                            else
                                builder.append("g.id in ( select member_group_id ");

                            if (flattening)
                                if (useMaterializedViews)
                                    builder.append("from _Group_Membership_Transitive ");
                                else
                                    builder.append("from _Group_Membership_Transitive_Non_Materialized ");
                            else
                                builder.append("from _Group_Membership ");

                            builder.append("where parent_group_id = ? ");
                            arguments.add(groupId.toLowerCase());
                            builder.append(") ");
                        }

                    } else if (expression instanceof PresenceOperator) {

                        builder.append("mp.parent_group_id ");
                        builder.append(getOperator(expression));
                    }

                } else if (entityType == EntityType.USER) {

                    if (expression instanceof EqualOperator) {

                        String groupId = null;

                        if ((joinPlan & USER_MEMBER_OF_FLAG) != 0)
                            groupId =
                                    LdapUtils.getGroupIdFromDn(schemaManager,
                                            ((EqualOperator) expression).getValue(), dcId);

                        if (groupId == null) {

                            builder.append("? = 'true' ");
                            arguments.add(false);

                        } else {

                            if (expression.isNegated())
                                builder.append("u.id not in ( select member_user_id ");
                            else
                                builder.append("u.id in ( select member_user_id ");

                            if (flattening)
                                if (useMaterializedViews)
                                    builder.append("from _User_Membership_Transitive ");
                                else
                                    builder.append("from _User_Membership_Transitive_Non_Materialized ");
                            else
                                builder.append("from _User_Membership ");

                            builder.append("where parent_group_id = ? ");
                            arguments.add(groupId.toLowerCase());
                            builder.append(") ");
                        }

                    } else if (expression instanceof PresenceOperator) {

                        builder.append("mu.parent_group_id ");
                        builder.append(getOperator(expression));
                    }
                }

                break;

            default:
                throw new IllegalArgumentException("Cannot process unexpected query expression " +
                        expression.getClass().getName());
        }
    }

    private void handleOperator(StringBuilder builder, List<Object> arguments, OperatorExpression expression) {

        builder.append(getOperator(expression));

        if (expression instanceof BinaryOperator) {

            builder.append("? ");

            if (expression instanceof WildcardOperator)
                arguments.add(((WildcardOperator) expression).getValue(WildcardOperator.Format.SQL));
            else
                arguments.add(((BinaryOperator) expression).getValue().toLowerCase());
        }
    }

    private String getOperator(OperatorExpression expression) {

        if (expression instanceof EqualOperator)
            if (expression.isNegated())
                return "<> ";
            else
                return "= ";

        if (expression instanceof WildcardOperator)
            if (expression.isNegated())
                return "not like ";
            else
                return "like ";

        if (expression instanceof PresenceOperator)
            if (expression.isNegated())
                return "is null ";
            else
                return "is not null ";

        throw new IllegalArgumentException("Cannot handle unexpected operator " +
                expression.getClass().getSimpleName());
    }
}
