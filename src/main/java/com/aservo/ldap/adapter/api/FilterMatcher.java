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

package com.aservo.ldap.adapter.api;

import com.aservo.ldap.adapter.api.entity.*;
import com.aservo.ldap.adapter.api.query.*;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;


/**
 * The handler for filter expressions.
 */
public abstract class FilterMatcher {

    /**
     * Make a match between entry and filter expression.
     *
     * @param entity     the entry
     * @param expression the filter expression
     * @return the boolean
     */
    public boolean matchEntity(Entity entity, QueryExpression expression) {

        if (expression instanceof BooleanValue) {

            return ((BooleanValue) expression).getValue();

        } else if (expression instanceof AndLogicExpression) {

            return ((AndLogicExpression) expression).getChildren().stream()
                    .allMatch(x -> matchEntity(entity, x));

        } else if (expression instanceof OrLogicExpression) {

            return ((OrLogicExpression) expression).getChildren().stream()
                    .anyMatch(x -> matchEntity(entity, x));

        } else if (expression instanceof NotLogicExpression) {

            return ((NotLogicExpression) expression).getChildren().stream()
                    .noneMatch(x -> matchEntity(entity, x));

        } else if (expression instanceof OperatorExpression) {

            return processOperator(entity, ((OperatorExpression) expression));

        } else
            throw new IllegalArgumentException("Cannot process unexpected filter expression " +
                    expression.getClass().getName());
    }

    private boolean processOperator(Entity entity, OperatorExpression expression) {

        switch (LdapUtils.normalizeAttribute(expression.getAttribute())) {

            case SchemaConstants.DC_AT:

                if (entity instanceof DomainEntity &&
                        expression.check(entity.getId()))
                    return true;

                break;

            case SchemaConstants.OBJECT_CLASS_AT_OID:

                if (entity instanceof DomainEntity &&
                        (expression.check(SchemaConstants.DOMAIN_OC) ||
                                expression.check(SchemaConstants.TOP_OC)))
                    return true;

                if (entity instanceof UnitEntity &&
                        (expression.check(SchemaConstants.ORGANIZATIONAL_UNIT_OC) ||
                                expression.check(SchemaConstants.TOP_OC)))
                    return true;

                if (entity instanceof GroupEntity &&
                        (expression.check(SchemaConstants.GROUP_OF_NAMES_OC) ||
                                expression.check(SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC) ||
                                expression.check(SchemaConstants.TOP_OC)))
                    return true;

                if (entity instanceof UserEntity &&
                        (expression.check(SchemaConstants.INET_ORG_PERSON_OC) ||
                                expression.check(SchemaConstants.ORGANIZATIONAL_PERSON_OC) ||
                                expression.check(SchemaConstants.PERSON_OC) ||
                                expression.check(SchemaConstants.TOP_OC)))
                    return true;

                break;

            case SchemaConstants.OU_AT_OID:

                if (entity instanceof UnitEntity &&
                        expression.check(entity.getId()))
                    return true;

                if (entity instanceof GroupEntity &&
                        expression.check(LdapUtils.OU_GROUPS))
                    return true;

                if (entity instanceof UserEntity &&
                        expression.check(LdapUtils.OU_USERS))
                    return true;

                break;

            case SchemaConstants.UID_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(entity.getId()))
                    return true;

                break;

            case SchemaConstants.CN_AT_OID:

                if ((entity instanceof GroupEntity || entity instanceof UserEntity) &&
                        expression.check(entity.getId()))
                    return true;

                break;

            case SchemaConstants.SN_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getLastName()))
                    return true;

                break;

            case SchemaConstants.GN_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getFirstName()))
                    return true;

                break;

            case SchemaConstants.DISPLAY_NAME_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getDisplayName()))
                    return true;

                break;

            case SchemaConstants.MAIL_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getEmail()))
                    return true;

                break;

            case SchemaConstants.DESCRIPTION_AT_OID:

                if (entity instanceof DescribableEntity &&
                        expression.check(((DescribableEntity) entity).getDescription()))
                    return true;

                break;

            case SchemaConstants.MEMBER_AT_OID:
            case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                if (entity instanceof GroupEntity) {

                    if (expression instanceof EqualOperator) {

                        EqualOperator expr = (EqualOperator) expression;

                        return isGroupMember((GroupEntity) entity, expr.getValue(), expr.isNegated());
                    }
                }

                break;

            case LdapUtils.MEMBER_OF_AT_OID:

                if (entity instanceof GroupEntity) {

                    if (expression instanceof EqualOperator) {

                        EqualOperator expr = (EqualOperator) expression;

                        return isMemberOfGroup((GroupEntity) entity, expr.getValue(), expr.isNegated());
                    }
                }

                if (entity instanceof UserEntity) {

                    if (expression instanceof EqualOperator) {

                        EqualOperator expr = (EqualOperator) expression;

                        return isMemberOfGroup((UserEntity) entity, expr.getValue(), expr.isNegated());
                    }
                }

                break;

            default:
                break;
        }

        return false;
    }

    protected abstract boolean isGroupMember(GroupEntity entity, String dn, boolean negated);

    protected abstract boolean isMemberOfGroup(GroupEntity entity, String dn, boolean negated);

    protected abstract boolean isMemberOfGroup(UserEntity entity, String dn, boolean negated);
}
