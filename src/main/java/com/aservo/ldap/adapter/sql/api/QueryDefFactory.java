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

package com.aservo.ldap.adapter.sql.api;


/**
 * The factory class for query definitions.
 */
public interface QueryDefFactory {

    /**
     * Creates a query by an ID.
     *
     * @param clauseId the clause ID
     * @return the query definition object
     */
    QueryDef queryById(String clauseId);

    /**
     * Creates a query from a string.
     *
     * @param clause the clause as string
     * @return the query definition object
     */
    QueryDef query(String clause);
}
