package net.wimpi.crowd.ldap.util;

import java.util.function.BiFunction;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.filter.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class FilterMatcher {

    private final Logger logger = LoggerFactory.getLogger(FilterMatcher.class);

    public boolean match(ExprNode filter, String entryId, OuType ouType) {

        if (filter instanceof ObjectClassNode) {

            return true;

        } else if (filter instanceof AndNode) {

            AndNode node = (AndNode) filter;

            return node.getChildren().stream()
                    .allMatch((x) -> match(x, entryId, ouType));

        } else if (filter instanceof OrNode) {

            OrNode node = (OrNode) filter;

            return node.getChildren().stream()
                    .anyMatch((x) -> match(x, entryId, ouType));

        } else if (filter instanceof NotNode) {

            NotNode node = (NotNode) filter;

            return node.getChildren().stream()
                    .noneMatch((x) -> match(x, entryId, ouType));

        } else if (filter instanceof EqualityNode) {

            EqualityNode node = (EqualityNode) filter;

            return matchPairs(node.getAttribute(), node.getValue().toString(), entryId, ouType, this::compareEquality);

        } else if (filter instanceof PresenceNode) {

            PresenceNode node = (PresenceNode) filter;

            return matchPairs(node.getAttribute(), null, entryId, ouType, this::comparePresence);
        }

        logger.warn("Cannot process unsupported filter node " + filter.getClass().getName());
        return false;
    }

    private boolean matchPairs(String attribute,
                               @Nullable String value,
                               String entryId,
                               OuType ouType,
                               BiFunction<String, String, Boolean> compare) {

        switch (attribute) {

            case SchemaConstants.OBJECT_CLASS_AT:
            case SchemaConstants.OBJECT_CLASS_AT_OID:

                if (ouType.equals(OuType.GROUP) && (
                        compare.apply(value, SchemaConstants.GROUP_OF_NAMES_OC) ||
                                compare.apply(value, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC) ||
                                compare.apply(value, SchemaConstants.TOP_OC)))
                    return true;

                if (ouType.equals(OuType.USER) && (
                        compare.apply(value, SchemaConstants.INET_ORG_PERSON_OC) ||
                                compare.apply(value, SchemaConstants.ORGANIZATIONAL_PERSON_OC) ||
                                compare.apply(value, SchemaConstants.PERSON_OC) ||
                                compare.apply(value, SchemaConstants.TOP_OC)))
                    return true;

                break;

            case SchemaConstants.UID_NUMBER_AT:
            case SchemaConstants.UID_NUMBER_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, Utils.calculateHash(entryId).toString()))
                    return true;

                break;

            case SchemaConstants.UID_AT:
            case SchemaConstants.UID_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, entryId))
                    return true;

                break;

            case SchemaConstants.OU_AT:
            case SchemaConstants.OU_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(value, Utils.OU_GROUPS))
                    return true;

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, Utils.OU_USERS))
                    return true;

                break;

            case SchemaConstants.CN_AT:
            case SchemaConstants.CN_AT_OID:
            case SchemaConstants.COMMON_NAME_AT:

                if ((ouType.equals(OuType.GROUP) || ouType.equals(OuType.USER)) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.CN_AT, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.GN_AT:
            case SchemaConstants.GN_AT_OID:
            case SchemaConstants.GIVENNAME_AT:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.GN_AT, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.SN_AT:
            case SchemaConstants.SN_AT_OID:
            case SchemaConstants.SURNAME_AT:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.SN_AT, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.DISPLAY_NAME_AT:
            case SchemaConstants.DISPLAY_NAME_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.DISPLAY_NAME_AT, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.MAIL_AT:
            case SchemaConstants.MAIL_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.MAIL_AT, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.DESCRIPTION_AT:
            case SchemaConstants.DESCRIPTION_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.DESCRIPTION_AT, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.MEMBER_AT:
            case SchemaConstants.MEMBER_AT_OID:
            case SchemaConstants.UNIQUE_MEMBER_AT:
            case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(value, getAttributeValue(SchemaConstants.MEMBER_AT, entryId, ouType)))
                    return true;

                break;

            case Utils.MEMBER_OF_AT:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getAttributeValue(Utils.MEMBER_OF_AT, entryId, ouType)))
                    return true;

                break;

            default:
                break;
        }

        return false;
    }

    private boolean compareEquality(String a, String b) {

        return a != null && a.equalsIgnoreCase(b);
    }

    private boolean comparePresence(String a, String b) {

        return a == null && b != null;
    }

    @Nullable
    protected abstract String getAttributeValue(String attribute, String entryId, OuType ouType);
}
