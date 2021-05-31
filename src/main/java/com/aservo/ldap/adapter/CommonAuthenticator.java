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

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.entity.UserEntity;
import com.aservo.ldap.adapter.backend.DirectoryBackend;
import com.aservo.ldap.adapter.backend.DirectoryBackendFactory;
import com.aservo.ldap.adapter.backend.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.backend.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.exception.InternalServerException;
import java.nio.charset.StandardCharsets;
import javax.naming.AuthenticationException;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements ApacheDS authenticator to allow authentication by directory backend.
 */
public class CommonAuthenticator
        extends AbstractAuthenticator {

    private final Logger logger = LoggerFactory.getLogger(CommonAuthenticator.class);
    private final DirectoryBackendFactory directoryBackendFactory;
    private final SchemaManager schemaManager;

    /**
     * Instantiates a new authenticator.
     *
     * @param directoryBackendFactory the directory backend factory
     * @param schemaManager           the schema manager
     */
    public CommonAuthenticator(DirectoryBackendFactory directoryBackendFactory, SchemaManager schemaManager) {

        super(AuthenticationLevel.SIMPLE);
        this.directoryBackendFactory = directoryBackendFactory;
        this.schemaManager = schemaManager;
    }

    public LdapPrincipal authenticate(BindOperationContext context)
            throws Exception {

        DirectoryBackend directory = directoryBackendFactory.getPermanentDirectory();

        try {

            String userId = LdapUtils.getUserIdFromDn(schemaManager, directory, context.getDn().getName());

            if (userId == null)
                throw new LdapInvalidDnException("Cannot handle unexpected DN=" + context.getDn());

            String password = new String(context.getCredentials(), StandardCharsets.UTF_8);

            UserEntity user = directory.getAuthenticatedUser(userId, password);

            logger.info("[{}] - The user {} with DN={} has been successfully authenticated.",
                    context.getIoSession().getRemoteAddress(),
                    user.getId(),
                    context.getDn());

            return new LdapPrincipal(schemaManager, context.getDn(), AuthenticationLevel.SIMPLE);

        } catch (LdapInvalidDnException e) {

            logger.info("[{}] - Authentication with incorrect DN={} could not be performed.",
                    context.getIoSession().getRemoteAddress(),
                    context.getDn());

            logger.debug("Authentication failed.", e);

            throw new AuthenticationException(e.getMessage());

        } catch (DirectoryAccessFailureException |
                SecurityProblemException |
                EntityNotFoundException e) {

            logger.info("[{}] - Authentication with DN={} could not be performed.",
                    context.getIoSession().getRemoteAddress(),
                    context.getDn());

            logger.debug("Authentication failed.", e);

            throw new AuthenticationException(e.getMessage());

        } catch (Exception e) {

            logger.error("The authenticator caught an exception.", e);

            throw new InternalServerException("The authenticator has detected an internal server error.");
        }
    }
}
