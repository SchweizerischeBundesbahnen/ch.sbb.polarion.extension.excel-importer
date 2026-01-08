package ch.sbb.polarion.extension.excel_importer.utils;

import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.stream.Stream;

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

    public static Stream<Arguments> provideHostnames() {
        return Stream.of(
                Arguments.of("http://polarion.url", "http://polarion.url"),
                Arguments.of("https://polarion.url", "https://polarion.url"),
                Arguments.of("polarion.url", "http://polarion.url"),
                Arguments.of("", "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostnames")
    void testEnrichByProtocolPrefix_withAlreadyExistingProtocol(String hostname, String expected) {
        String result = PolarionUtils.enrichByProtocolPrefix(hostname);
        assertEquals(expected, result);
    }

    @Test
    void testGetAbsoluteUrl_withAbsoluteUrl() throws MalformedURLException {
        String relativeUrl = "http://polarion.url/some/path";
        URL absoluteUrl = PolarionUtils.getAbsoluteUrl(relativeUrl);
        assertEquals(URI.create("http://polarion.url/some/path").toURL(), absoluteUrl);
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
