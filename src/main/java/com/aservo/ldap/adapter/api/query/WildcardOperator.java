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

package com.aservo.ldap.adapter.api.query;


import java.util.List;
import java.util.regex.Pattern;

public final class WildcardOperator
        extends BinaryOperator<WildcardOperator> {

    private final Pattern pattern;
    private final String initialSegment;
    private final String finalSegment;
    private final List<String> middleSegments;

    public WildcardOperator(String attribute, Pattern pattern, String initialSegment, String finalSegment,
                            List<String> middleSegments, boolean negated, boolean ignoreCase) {

        super(attribute, negated, ignoreCase);
        this.pattern = pattern;
        this.initialSegment = initialSegment;
        this.finalSegment = finalSegment;
        this.middleSegments = middleSegments;
    }

    public WildcardOperator(String attribute, Pattern pattern, String initialSegment, String finalSegment,
                            List<String> middleSegments) {

        this(attribute, pattern, initialSegment, finalSegment, middleSegments, false, true);
    }

    public String getValue() {

        return pattern.pattern();
    }

    public String getValue(Format format) {

        if (format == Format.REGEX)
            return pattern.pattern();

        if (format == Format.LDAP) {

            StringBuilder builder = new StringBuilder();

            if (initialSegment == null)
                builder.append('*');
            else
                builder.append(initialSegment.replace("*", "\\2A")).append('*');

            for (String middleSegment : middleSegments) {

                builder.append(middleSegment.replace("*", "\\2A"));
                builder.append('*');
            }

            if (finalSegment != null) {
                builder.append(finalSegment.replace("*", "\\2A"));
            }

            return builder.toString();
        }

        if (format == Format.SQL) {

            StringBuilder builder = new StringBuilder();

            if (initialSegment == null)
                builder.append('%');
            else
                builder.append(initialSegment.replace("%", "\\%")).append('%');

            for (String middleSegment : middleSegments) {

                builder.append(middleSegment.replace("%", "\\%"));
                builder.append('%');
            }

            if (finalSegment != null) {
                builder.append(finalSegment.replace("%", "\\%"));
            }

            return builder.toString();
        }

        throw new IllegalArgumentException("Cannot handle unexpected format for wildcard pattern.");
    }

    public Pattern getPattern() {

        return pattern;
    }

    public WildcardOperator negate() {

        return new WildcardOperator(getAttribute(), pattern, initialSegment, finalSegment, middleSegments,
                !isNegated(),
                isIgnoreCase()
        );
    }

    public boolean check(String value) {

        if (isIgnoreCase())
            return pattern.matcher(value.toLowerCase()).matches();

        return pattern.matcher(value).matches();
    }

    public enum Format {

        REGEX, LDAP, SQL
    }
}
