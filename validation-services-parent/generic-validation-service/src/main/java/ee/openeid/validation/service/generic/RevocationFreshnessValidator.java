package ee.openeid.validation.service.generic;

import eu.europa.esig.dss.crl.CRLBinary;
import eu.europa.esig.dss.crl.CRLUtils;
import eu.europa.esig.dss.crl.CRLValidity;
import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPResponseBinary;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.reports.Reports;
import lombok.SneakyThrows;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class RevocationFreshnessValidator {

    private static final Duration REVOCATION_FRESHNESS_FIFTEEN_MINUTES_DIFFERENCE = Duration.ofMinutes(15);
    private static final Duration REVOCATION_FRESHNESS_DAY_DIFFERENCE = Duration.ofDays(1);

    private static final String REVOCATION_FRESHNESS_FAULT = "The revocation information is not considered as 'fresh'.";

    private final Reports reports;
    private final List<AdvancedSignature> signatures;

    public RevocationFreshnessValidator(Reports reports, List<AdvancedSignature> signatures) {
        this.reports = reports;
        this.signatures = signatures;
    }

    public void validate() {
        DiagnosticData diagnosticData = reports.getDiagnosticData();
        for (CertificateWrapper certificateWrapper : diagnosticData.getUsedCertificates()) {
            for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {

                if (signatureWrapper.getSigningCertificate() == null) {
                    return;
                }
                validate(certificateWrapper, signatureWrapper);
            }
        }
    }

    private void validate(CertificateWrapper certificateWrapper, SignatureWrapper signatureWrapper) {
        if (!certificateWrapper.getId().equals(signatureWrapper.getSigningCertificate().getId())
                || signatureWrapper.getTimestampList().isEmpty()) {
            return;
        }
        CRLValidity crlValidity = findCRLValidity(signatureWrapper.getId(), certificateWrapper.getId());
        Date ocspProducedAt = findOcspProducedAt(signatureWrapper.getId());

        if (ocspProducedAt == null && crlValidity == null) {
            return;
        }
        Date timestampProductionTime = getFirstTimestampProductionTime(signatureWrapper.getTimestampList());
        if (timestampProductionTime == null) {
            return;
        }

        invokeRevocationFreshnessCheckIfNeeded(signatureWrapper.getId(), ocspProducedAt, crlValidity, timestampProductionTime);
    }

    private CRLValidity findCRLValidity(String signatureId, String certificateId) {
        Optional<AdvancedSignature> advancedSignature = signatures.stream()
                .filter(signature -> signature.getId().equals(signatureId))
                .findFirst();
        if (advancedSignature.isEmpty() || advancedSignature.get().getCRLSource() == null) {
            return null;
        }

        Optional<CRLBinary> crlBinary = advancedSignature.get().getCRLSource().getCRLBinaryList().stream()
                .findFirst();

        Optional<CertificateToken> certificateToken = advancedSignature.get().getCertificates().stream()
                .filter(certificate -> certificate.getDSSIdAsString().equals(certificateId))
                .findFirst();
        if (crlBinary.isPresent() && certificateToken.isPresent()) {
            return buildCRLValidity(crlBinary.get(), certificateToken.get());
        }
        return null;
    }

    @SneakyThrows
    protected CRLValidity buildCRLValidity(CRLBinary crlBinary, CertificateToken certificateToken) {
        return CRLUtils.buildCRLValidity(crlBinary, certificateToken);
    }

    private Date findOcspProducedAt(String signatureId) {
        Optional<List<BasicOCSPResp>> basicOCSPResps = signatures.stream()
                .filter(signature -> signature.getId().equals(signatureId) && signature.getOCSPSource() != null)
                .findFirst()
                .map(signature -> signature.getOCSPSource().getOCSPResponsesList().stream()
                        .map(OCSPResponseBinary::getBasicOCSPResp)
                        .collect(Collectors.toList()));

        return basicOCSPResps.flatMap(
                ocspResps -> ocspResps.stream()
                        .map(BasicOCSPResp::getProducedAt)
                        .sorted()
                        .findFirst())
                .orElse(null);
    }

    private Date getFirstTimestampProductionTime(List<TimestampWrapper> timestamps) {
        TimestampWrapper timestampWrapper = Collections.min(timestamps, Comparator.comparing(TimestampWrapper::getProductionTime));
        if (timestampWrapper.getProductionTime() != null)
            return timestampWrapper.getProductionTime();
        return null;
    }

    private void invokeRevocationFreshnessCheckIfNeeded(String signatureId, Date ocspProducedAt, CRLValidity crlValidity, Date timeStampProductionTime) {
        boolean revocationFreshnessCheckInvokeError = isRevocationFreshnessCheckInvalid(ocspProducedAt, crlValidity, timeStampProductionTime);
        if (revocationFreshnessCheckInvokeError) {
            reports.getSimpleReport().getErrors(signatureId).add(REVOCATION_FRESHNESS_FAULT);
        } else {
            if (ocspProducedAt == null) {
                return;
            }
            boolean revocationFreshnessCheckInvokeWarning = areNotWithinRange(ocspProducedAt, timeStampProductionTime, REVOCATION_FRESHNESS_FIFTEEN_MINUTES_DIFFERENCE);
            if (revocationFreshnessCheckInvokeWarning) {
                reports.getSimpleReport().getWarnings(signatureId).add(REVOCATION_FRESHNESS_FAULT);
            }
        }
    }

    private boolean isRevocationFreshnessCheckInvalid(Date ocspProducedAt, CRLValidity crlValidity, Date timeStampProductionTime) {
        if (crlValidity != null) {
            return !(timeStampProductionTime.after(crlValidity.getThisUpdate()) && timeStampProductionTime.before(crlValidity.getNextUpdate()));
        }
        return areNotWithinRange(ocspProducedAt, timeStampProductionTime, REVOCATION_FRESHNESS_DAY_DIFFERENCE);

    }

    private static boolean areNotWithinRange(Date date1, Date date2, Duration range) {
        Duration timeBetweenDates = Duration.between(date1.toInstant(), date2.toInstant()).abs();
        return timeBetweenDates.compareTo(range) > 0;
    }
}
