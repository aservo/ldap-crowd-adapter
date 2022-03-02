package de.aservo.ldap.adapter;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.CompareRequest;
import org.apache.directory.api.ldap.model.message.CompareResponse;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;


public class CompareRequestHandler
        extends LdapRequestHandler<CompareRequest> {

    public CompareRequestHandler() {
    }

    public void handle(LdapSession session, CompareRequest compareRequest) {

        CompareResponse compareResponse = (CompareResponse) compareRequest.getResultResponse();
        LdapResult result = compareRequest.getResultResponse().getLdapResult();

        try {
            if (compare(session, compareRequest)) {
                result.setResultCode(ResultCodeEnum.COMPARE_TRUE);
            } else {
                result.setResultCode(ResultCodeEnum.COMPARE_FALSE);
            }

            result.setMatchedDn(compareRequest.getName());
            session.getIoSession().write(compareResponse);

        } catch (Exception var6) {

            this.handleException(session, compareRequest, compareResponse, var6);
        }
    }

    public boolean compare(LdapSession session, CompareRequest compareRequest)
            throws LdapException {

        CompareOperationContext compareContext = new CompareOperationContext(session.getCoreSession(), compareRequest);
        DirectoryService directoryService = session.getCoreSession().getDirectoryService();
        Partition partition = directoryService.getPartitionNexus().getPartition(compareRequest.getName());

        try {

            if (partition instanceof SimpleReadOnlyPartition)
                return ((SimpleReadOnlyPartition) partition).compare(compareContext);

            throw new UnsupportedOperationException("Compare action requires partition type " +
                    SimpleReadOnlyPartition.class.getName());

        } finally {

            compareRequest.getResultResponse().addAllControls(compareContext.getResponseControls());
        }
    }
}
