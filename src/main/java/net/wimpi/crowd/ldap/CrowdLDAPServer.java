/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.wimpi.crowd.ldap;

import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.wimpi.crowd.ldap.util.ServerConfiguration;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CrowdLDAPServer {

    private final Logger logger = LoggerFactory.getLogger(CrowdLDAPServer.class);

    private final ServerConfiguration serverConfig;
    private final CrowdClient crowdClient;
    private final DirectoryService directoryService;

    public CrowdLDAPServer(ServerConfiguration serverConfig) {

        this.serverConfig = serverConfig;
        ClientProperties props = ClientPropertiesImpl.newInstanceFromProperties(serverConfig.getCrowdProperties());
        crowdClient = new RestCrowdClientFactory().newInstance(props);
        createNewLoaders();
        directoryService = initDirectoryService();
    }

    public ServerConfiguration getServerConfig() {

        return serverConfig;
    }

    public void start() {

        try {

            crowdClient.testConnection();
            directoryService.startup();

            LdapServer server = new LdapServer();

            Transport t = new TcpTransport(serverConfig.getHost(), serverConfig.getPort());

            // SSL Support
            if (serverConfig.isSslEnabled()) {

                t.setEnableSSL(true);
                server.setKeystoreFile(serverConfig.getKeyStore());
                server.setCertificatePassword(serverConfig.getCertificatePassword());
                server.addExtendedOperationHandler(new StartTlsHandler());
            }

            server.setTransports(t);
            server.setDirectoryService(directoryService);
            server.start();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private void copyStream(String resourcePath, File outputFile)
            throws IOException {

        if (outputFile.exists()) {
            return;
        }

        InputStream in = null;
        OutputStream out = null;

        try {

            in = getClass().getClassLoader().getResourceAsStream(resourcePath);
            out = new FileOutputStream(outputFile);

            // transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0)
                out.write(buf, 0, len);

        } finally {

            if (in != null)
                in.close();

            if (out != null)
                out.close();
        }
    }

    private void createNewLoaders() {

        try {

            // extract the schema on disk (a brand new one) and load the registries
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(serverConfig.getCacheDir());
            extractor.extractOrCopy(true);

            File attributeTypesDir =
                    new File(serverConfig.getCacheDir(), "schema/ou=schema/cn=other/ou=attributetypes");

            attributeTypesDir.mkdirs();

            // memberOf Support
            if (serverConfig.getMemberOfSupport().allowMemberOfAttribute()) {
                File memberOfLDIF = new File(attributeTypesDir, "m-oid=1.2.840.113556.1.2.102.ldif");
                copyStream("net/wimpi/crowd/ldap/memberof.ldif", memberOfLDIF);
            }

            File rf2307bisSchemaDir =
                    new File(serverConfig.getCacheDir(), "schema/ou=schema/cn=rfc2307bis/ou=attributetypes");

            rf2307bisSchemaDir.mkdirs();

            ArrayList<String> filenames = new ArrayList<String>();
            filenames.add("m-oid=1.3.6.1.1.1.1.0");
            filenames.add("m-oid=1.3.6.1.1.1.1.1");
            filenames.add("m-oid=1.3.6.1.1.1.1.2");
            filenames.add("m-oid=1.3.6.1.1.1.1.3");
            filenames.add("m-oid=1.3.6.1.1.1.1.4");

            for (String name : filenames) {
                File rf2307bisSchema = new File(attributeTypesDir, name + ".ldif");
                copyStream("net/wimpi/crowd/ldap/rfc2307/" + name + ".ldif", rf2307bisSchema);
            }

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }
    }

    private void initSchemaPartition(DirectoryService directoryService) {

        try {

            File schemaRepository = new File(serverConfig.getCacheDir(), "schema");

            SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
            SchemaManager schemaManager = new DefaultSchemaManager(loader);
            schemaManager.loadAllEnabled();
            directoryService.setSchemaManager(schemaManager);

            SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
            directoryService.setSchemaPartition(schemaPartition);

            // initialize the LdifPartition
            LdifPartition ldifPartition = new LdifPartition(directoryService.getSchemaManager(), directoryService.getDnFactory());
            ldifPartition.setPartitionPath(schemaRepository.toURI());

            schemaPartition.setWrappedPartition(ldifPartition);
            directoryService.setInstanceLayout(new InstanceLayout(serverConfig.getCacheDir()));

            // We have to load the schema now, otherwise we won't be able
            // to initialize the Partitions, as we won't be able to parse
            // and normalize their suffix DN
            schemaManager.loadAllEnabled();

            List<Throwable> errors = schemaManager.getErrors();

            if (errors.size() != 0) {
                throw new Exception(MessageFormat.format("Schema load failed: {0}", errors));
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private DirectoryService initDirectoryService() {

        try {

            // initialize the LDAP service
            DirectoryService directoryService = new DefaultDirectoryService();

            // first load the schema
            initSchemaPartition(directoryService);

            // then the system partition
            // this is a MANDATORY partition

            JdbmPartition partition = new JdbmPartition(directoryService.getSchemaManager(), directoryService.getDnFactory());
            partition.setId("system");
            partition.setPartitionPath(new File(serverConfig.getCacheDir(), "system").toURI());
            partition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));

            directoryService.setSystemPartition(partition);

            // disable the ChangeLog system
            directoryService.getChangeLog().setEnabled(false);
            directoryService.setDenormalizeOpAttrsEnabled(false);

            //disable Anonymous Access
            directoryService.setAllowAnonymousAccess(false);

            List<Interceptor> interceptors = directoryService.getInterceptors();

            for (Interceptor interceptor : interceptors) {
                if (interceptor instanceof AuthenticationInterceptor) {
                    logger.debug("Interceptor: {}", interceptor.getName());
                    AuthenticationInterceptor ai = (AuthenticationInterceptor) interceptor;
                    Set<Authenticator> auths = new HashSet<Authenticator>();
                    auths.add(new CrowdAuthenticator(crowdClient, directoryService.getSchemaManager()));
                    ai.setAuthenticators(auths);
                }
            }

            // add Crowd Partition
            addCrowdPartition(directoryService);

            return directoryService;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private void addCrowdPartition(DirectoryService directoryService) {

        try {

            CrowdPartition partition = new CrowdPartition(crowdClient, serverConfig);

            partition.setId("crowd");
            partition.setSchemaManager(directoryService.getSchemaManager());
            partition.initialize();

            directoryService.addPartition(partition);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}