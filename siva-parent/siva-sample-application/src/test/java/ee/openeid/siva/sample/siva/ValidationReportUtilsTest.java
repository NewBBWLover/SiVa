/*
 * Copyright 2016 Riigi Infosüsteemide Amet
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package ee.openeid.siva.sample.siva;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationReportUtilsTest {

    @Test
    public void testConstructorIsPrivate() throws Exception {
        final Constructor<ValidationReportUtils> constructor = ValidationReportUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void documentNameJsonKeyPresentReturnsDocumentNameValue() throws Exception {
        final String json = "{\"documentName\":\"valid_value.bdoc\"}";
        assertEquals("valid_value.bdoc", ValidationReportUtils.getValidateFilename(json));
    }

    @Test
    public void documentNameJsonKeyNotPresentReturnsEmptyString() throws Exception {
        final String json = "{\"randomKey\": \"randomValue\"}";
        assertEquals("", ValidationReportUtils.getValidateFilename(json));
    }

    @Test
    public void documentNameJsonKeyIsNullReturnsEmptyString() throws Exception {
        final String json = "{\"documentName\": null}";
        assertEquals("", ValidationReportUtils.getValidateFilename(json));
    }

    @Test
    public void overallValidationRequiredJsonKeysArePresentReturnsValid() throws Exception {
        final String json = "{\"validSignaturesCount\": 1, \"signaturesCount\": 1}";
        assertEquals("VALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredJsonKeysPresentReturnsInvalid() throws Exception {
        final String json = "{\"validSignaturesCount\": 0, \"signaturesCount\": 1}";
        assertEquals("INVALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredJsonKeysWithValuesZeroReturnsInvalid() throws Exception {
        final String json = "{\"validSignaturesCount\": 0, \"signaturesCount\": 0}";
        assertEquals("INVALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredKeysPresentValuesNullReturnsInvalid() throws Exception {
        final String json = "{\"validSignaturesCount\": null, \"signaturesCount\": null}";
        assertEquals("ERROR", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredKeysNotPresentReturnsInvalid() throws Exception {
        final String json = "{\"randomKey\": \"randomValue\"}";
        assertEquals("INVALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void givenNullToValidateFilenameReturnsEmptyString() throws Exception {
        assertThat(ValidationReportUtils.getValidateFilename(null)).isEqualTo("");
    }

    @Test
    public void givenNullJSONToOverallValidationResultWillReturnERROR() throws Exception {
        assertThat(ValidationReportUtils.getOverallValidationResult(null)).isEqualTo("ERROR");
    }

    @Test
    public void whenHandleMissingJSONIsCalledErrorJSONWillBeReturned() throws Exception {
        assertThat(ValidationReportUtils.handleMissingJSON()).contains("errorMessage");
        assertThat(ValidationReportUtils.handleMissingJSON()).contains("No JSON found in SiVa API response");
    }

    @Test
    public void givenNullJSONStringReturnsTrue() throws Exception {
        assertThat(ValidationReportUtils.isJSONNull(null)).isTrue();
    }

    @Test
    public void givenStringToIsJSONNullReturnsFalse() throws Exception {
        assertThat(ValidationReportUtils.isJSONNull("random string")).isFalse();
    }
}