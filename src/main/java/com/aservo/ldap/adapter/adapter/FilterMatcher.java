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

import com.aservo.ldap.adapter.adapter.entity.*;
import com.aservo.ldap.adapter.adapter.query.*;
import com.aservo.ldap.adapter.backend.DirectoryBackend;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.SchemaManager;


/**
 * The handler for filter expressions.
 */
public abstract class FilterMatcher {

    /**
     * Make a match between entry and filter expression.
     *
     * @param entity     the entry
     * @param filterNode the filter expression
     * @return the boolean
     */
    public boolean matchEntity(Entity entity, FilterNode filterNode) {

        if (filterNode instanceof NullNode) {

            return true;

        } else if (filterNode instanceof AndLogicExpression) {

            return ((AndLogicExpression) filterNode).getChildren().stream()
                    .allMatch(x -> matchEntity(entity, x));

        } else if (filterNode instanceof OrLogicExpression) {

            return ((OrLogicExpression) filterNode).getChildren().stream()
                    .anyMatch(x -> matchEntity(entity, x));

        } else if (filterNode instanceof NotLogicExpression) {

            return ((NotLogicExpression) filterNode).getChildren().stream()
                    .noneMatch(x -> matchEntity(entity, x));

        } else if (filterNode instanceof Operator) {

            return processOperator(entity, ((Operator) filterNode));

        } else
            throw new IllegalArgumentException("Cannot process unsupported filter expression " +
                    filterNode.getClass().getName());
    }

    private boolean processOperator(Entity entity, Operator expression) {

        switch (LdapUtils.normalizeAttribute(expression.getAttribute())) {

            case SchemaConstants.OBJECT_CLASS_AT:
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

            case SchemaConstants.OU_AT:
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

            case SchemaConstants.DC_AT:
            case SchemaConstants.DOMAIN_COMPONENT_AT:
            case SchemaConstants.DOMAIN_COMPONENT_AT_OID:

                if (entity instanceof DomainEntity &&
                        expression.check(entity.getId()))
                    return true;

                break;

            case SchemaConstants.UID_AT:
            case SchemaConstants.UID_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(entity.getId()))
                    return true;

                break;

            case SchemaConstants.CN_AT:
            case SchemaConstants.CN_AT_OID:
            case SchemaConstants.COMMON_NAME_AT:

                if ((entity instanceof GroupEntity || entity instanceof UserEntity) &&
                        expression.check(entity.getId()))
                    return true;

                break;

            case SchemaConstants.SN_AT:
            case SchemaConstants.SN_AT_OID:
            case SchemaConstants.SURNAME_AT:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getLastName()))
                    return true;

                break;

            case SchemaConstants.GN_AT:
            case SchemaConstants.GN_AT_OID:
            case SchemaConstants.GIVENNAME_AT:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getFirstName()))
                    return true;

                break;

            case SchemaConstants.DISPLAY_NAME_AT:
            case SchemaConstants.DISPLAY_NAME_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getDisplayName()))
                    return true;

                break;

            case SchemaConstants.MAIL_AT:
            case SchemaConstants.MAIL_AT_OID:

                if (entity instanceof UserEntity &&
                        expression.check(((UserEntity) entity).getEmail()))
                    return true;

                break;

            case SchemaConstants.DESCRIPTION_AT:
            case SchemaConstants.DESCRIPTION_AT_OID:

                if (entity instanceof DomainEntity &&
                        expression.check(((DescribableEntity) entity).getDescription()))
                    return true;

                if (entity instanceof UnitEntity &&
                        expression.check(((DescribableEntity) entity).getDescription()))
                    return true;

                if (entity instanceof GroupEntity &&
                        expression.check(((DescribableEntity) entity).getDescription()))
                    return true;

                break;

            case SchemaConstants.MEMBER_AT:
            case SchemaConstants.MEMBER_AT_OID:
            case SchemaConstants.UNIQUE_MEMBER_AT:
            case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                if (entity instanceof GroupEntity) {

                    if (expression instanceof EqualOperator) {

                        EqualOperator expr = (EqualOperator) expression;

                        return LdapUtils.isGroupMember(getSchemaManager(), getDirectoryBackend(),
                                (GroupEntity) entity, expr.getValue(), isFlatteningEnabled(), false);

                    } else if (expression instanceof NotEqualOperator) {

                        NotEqualOperator expr = (NotEqualOperator) expression;

                        return LdapUtils.isGroupMember(getSchemaManager(), getDirectoryBackend(),
                                (GroupEntity) entity, expr.getValue(), isFlatteningEnabled(), true);
                    }
                }

                break;

            case LdapUtils.MEMBER_OF_AT:
            case LdapUtils.MEMBER_OF_AT_OID:

                if (entity instanceof GroupEntity) {

                    if (expression instanceof EqualOperator) {

                        EqualOperator expr = (EqualOperator) expression;

                        return LdapUtils.isMemberOfGroup(getSchemaManager(), getDirectoryBackend(),
                                (GroupEntity) entity, expr.getValue(), isFlatteningEnabled(), false);

                    } else if (expression instanceof NotEqualOperator) {

                        NotEqualOperator expr = (NotEqualOperator) expression;

                        return LdapUtils.isMemberOfGroup(getSchemaManager(), getDirectoryBackend(),
                                (GroupEntity) entity, expr.getValue(), isFlatteningEnabled(), true);
                    }
                }

                if (entity instanceof UserEntity) {

                    if (expression instanceof EqualOperator) {

                        EqualOperator expr = (EqualOperator) expression;

                        return LdapUtils.isMemberOfGroup(getSchemaManager(), getDirectoryBackend(),
                                (UserEntity) entity, expr.getValue(), isFlatteningEnabled(), false);

                    } else if (expression instanceof NotEqualOperator) {

                        NotEqualOperator expr = (NotEqualOperator) expression;

                        return LdapUtils.isMemberOfGroup(getSchemaManager(), getDirectoryBackend(),
                                (UserEntity) entity, expr.getValue(), isFlatteningEnabled(), true);
                    }
                }

                break;

            default:
                break;
        }

        return false;
    }

    protected abstract boolean isFlatteningEnabled();

    protected abstract DirectoryBackend getDirectoryBackend();

    protected abstract SchemaManager getSchemaManager();
}
