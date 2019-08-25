/*
 * Initiator:
 * Copyright (c) 2012 Dieter Wimberger
 * http://dieter.wimpi.net
 *
 * Maintenance:
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

package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.util.DirectoryBackend;
import com.aservo.ldap.adapter.util.LdapHelper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.naming.AuthenticationException;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonAuthenticator
        extends AbstractAuthenticator {

    private final Logger logger = LoggerFactory.getLogger(CommonAuthenticator.class);

    private final DirectoryBackend directoryBackend;
    private final SchemaManager schemaManager;
    private final Dn rootDn;
    private final Dn usersDn;

    public CommonAuthenticator(DirectoryBackend directoryBackend, SchemaManager schemaManager)
            throws LdapInvalidDnException {

        super(AuthenticationLevel.SIMPLE);
        this.directoryBackend = directoryBackend;
        this.schemaManager = schemaManager;

        this.rootDn = new Dn(schemaManager, directoryBackend.getRootDnString());
        this.usersDn = new Dn(schemaManager, directoryBackend.getUserDnString());
    }

    public LdapPrincipal authenticate(BindOperationContext context)
            throws Exception {

        String userId = LdapHelper.getUserFromDn(rootDn, usersDn, context.getDn());

        if (userId != null) {

            String password = new String(context.getCredentials(), StandardCharsets.UTF_8);

            try {

                Map<String, String> userInfo = directoryBackend.getInfoFromAuthenticatedUser(userId, password);

                logger.debug("The user {} has been successfully authenticated.",
                        userInfo.get(DirectoryBackend.USER_ID));

                return new LdapPrincipal(schemaManager, context.getDn(), AuthenticationLevel.SIMPLE);

            } catch (Exception e) {

                logger.debug("Authentication could not be performed.", e);
                throw e;
            }

        } else {

            AuthenticationException error =
                    new AuthenticationException("Cannot handle unexpected DN : " + context.getDn());

            logger.debug("Authentication could not be performed with an incorrect DN.", error);
            throw error;
        }
    }
}
