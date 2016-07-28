package ee.openeid.validation.service.bdoc.signature.policy;

import ee.openeid.siva.validation.service.signature.policy.SignaturePolicyService;
import ee.openeid.siva.validation.service.signature.policy.properties.SignaturePolicyProperties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;

public class BDOCSignaturePolicyService extends SignaturePolicyService {

    public BDOCSignaturePolicyService(SignaturePolicyProperties signaturePolicyProperties) {
        super(signaturePolicyProperties);
    }

    public String getAbsolutePath(String policy) {
        LOGGER.debug("creating policy file from path {}", policy);
        InputStream inputStream = StringUtils.isEmpty(policy) ?
                new ByteArrayInputStream(defaultPolicy) :
                getPolicyDataStreamFromPolicy(policy);

        try {
            final File file = File.createTempFile(policy + "_constraint", "xml");
            file.deleteOnExit();
            final OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            LOGGER.error("Unable to create temporary file from bdoc policy resource", e);
            throw new BdocPolicyFileCreationException(e);
        }
    }

    private class BdocPolicyFileCreationException extends RuntimeException {
        public BdocPolicyFileCreationException(Exception e) {
            super(e);
        }
    }

}
