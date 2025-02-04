package ch.sbb.polarion.extension.excel_importer.utils;

import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

class PolarionUtilsTest {

    @BeforeEach
    void setUp() {
        System.clearProperty(PolarionProperties.BASE_URL);
    }

    @Test
    void testGetBaseUrl_withCustomBaseUrl() {
        System.setProperty(PolarionProperties.BASE_URL, "http://polarion.url");
        String baseUrl = PolarionUtils.getBaseUrl();
        assertEquals("http://polarion.url", baseUrl);
    }

    @Test
    void testGetBaseUrl_withLocalhost() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class, Answers.RETURNS_DEEP_STUBS)) {
            configurationMockedStatic.when(() -> Configuration.getInstance().cluster().nodeHostname()).thenReturn("myhostname");

            String baseUrl = PolarionUtils.getBaseUrl();
            assertEquals("http://myhostname", baseUrl);
        }
    }

    @Test
    void testEnrichByProtocolPrefix_withNoProtocol() {
        String hostname = "polarion.url";
        String result = PolarionUtils.enrichByProtocolPrefix(hostname);
        assertEquals("http://polarion.url", result);
    }

    @Test
    void testEnrichByProtocolPrefix_withHttpProtocol() {
        String hostname = "http://polarion.url";
        String result = PolarionUtils.enrichByProtocolPrefix(hostname);
        assertEquals("http://polarion.url", result);
    }

    @Test
    void testEnrichByProtocolPrefix_withHttpsProtocol() {
        String hostname = "https://polarion.url";
        String result = PolarionUtils.enrichByProtocolPrefix(hostname);
        assertEquals("https://polarion.url", result);
    }

    @Test
    void testEnrichByProtocolPrefix_withEmptyHostname() {
        String hostname = "";
        String result = PolarionUtils.enrichByProtocolPrefix(hostname);
        assertEquals("", result);
    }

    @Test
    void testGetAbsoluteUrl_withAbsoluteUrl() throws MalformedURLException {
        String relativeUrl = "http://polarion.url/some/path";
        URL absoluteUrl = PolarionUtils.getAbsoluteUrl(relativeUrl);
        assertEquals(new URL("http://polarion.url/some/path"), absoluteUrl);
    }

    @Test
    void testGetAbsoluteUrl_withRelativeUrl() throws MalformedURLException {
        String relativeUrl = "/some/path";
        String baseUrl = "http://polarion.url";
        String absoluteUrl = PolarionUtils.getAbsoluteUrl(relativeUrl, baseUrl);
        assertEquals("http://polarion.url/some/path", absoluteUrl);
    }

    @Test
    void testGetAbsoluteUrl_withInvalidUrl() {
        String invalidUrl = "invalid:url";
        assertThrows(MalformedURLException.class, () -> {
            PolarionUtils.getAbsoluteUrl(invalidUrl);
        });
    }
}
