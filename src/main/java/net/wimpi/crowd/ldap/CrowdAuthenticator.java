package net.wimpi.crowd.ldap;

import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import java.text.MessageFormat;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements {@class AbstractAuthenticator} to authenticate against using
 * a CrowdClient.
 *
 * @author Dieter Wimberger (dieter at wimpi dot net)
 */
public class CrowdAuthenticator
        extends AbstractAuthenticator {

    private final Logger logger = LoggerFactory.getLogger(CrowdAuthenticator.class);

    private CrowdClient m_CrowdClient;
    private DirectoryService service;

    public CrowdAuthenticator(CrowdClient client, DirectoryService service) {
        super(AuthenticationLevel.SIMPLE);
        m_CrowdClient = client;
        this.service = service;
    }

    public LdapPrincipal authenticate(BindOperationContext ctx) throws Exception {
        String user = ctx.getDn().getRdn(0).getNormValue();
        String pass = new String(ctx.getCredentials(), "utf-8");

        try {
            User u = m_CrowdClient.authenticateUser(user, pass);
            if (u == null) {
                logger.debug("CrowdAuthenticator() :: Authentication failed ()::Authentication failed");
                throw new javax.naming.AuthenticationException("Invalid credentials for user: " + user);
            } else {
                logger.debug(MessageFormat.format("CrowdAuthenticator() :: User={0}", u.toString()));
                return new LdapPrincipal(this.service.getSchemaManager(), ctx.getDn(), AuthenticationLevel.SIMPLE);
            }
        } catch (Exception ex) {
            logger.debug("CrowdAuthenticator() :: Authentication failed()::Authentication failed: ", ex);
            throw new javax.naming.NamingException("Unable to perform authentication: " + ex);
        }
    }
}
