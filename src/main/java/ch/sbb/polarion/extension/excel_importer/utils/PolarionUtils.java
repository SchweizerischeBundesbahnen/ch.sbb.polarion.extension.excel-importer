package ch.sbb.polarion.extension.excel_importer.utils;

import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.config.Configuration;
import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@UtilityClass
public class PolarionUtils {

    private static final String HTTP_PROTOCOL_PREFIX = "http://";
    private static final String HTTPS_PROTOCOL_PREFIX = "https://";
    private static final String LOCALHOST = "localhost";

    public String getBaseUrl() {
        String polarionBaseUrl = System.getProperty(PolarionProperties.BASE_URL, LOCALHOST);
        if (!polarionBaseUrl.contains(LOCALHOST)) {
            return enrichByProtocolPrefix(polarionBaseUrl);
        }
        String hostname = Configuration.getInstance().cluster().nodeHostname();
        return enrichByProtocolPrefix(hostname);
    }

    public String enrichByProtocolPrefix(String hostname) {
        if (StringUtils.isEmpty(hostname) || hostname.startsWith(HTTP_PROTOCOL_PREFIX) || hostname.startsWith(HTTPS_PROTOCOL_PREFIX)) {
            return hostname;
        } else {
            return HTTP_PROTOCOL_PREFIX + hostname;
        }
    }

    @SneakyThrows
    public static URL getAbsoluteUrl(String relativeUrl) {
        String absoluteUrl = getAbsoluteUrl(relativeUrl, getBaseUrl());
        return URI.create(absoluteUrl).toURL();
    }

    public static String getAbsoluteUrl(String relativeUrl, String baseUrl) throws MalformedURLException {
        try {
            URI uri = new URI(relativeUrl);
            if (uri.isAbsolute() && uri.getHost() != null) {
                return relativeUrl; // if URL is absolute, then return it as is
            }
        } catch (URISyntaxException e) {
            // looks like relative URL
        }

        // if relative URL, then create absolute with base URL
        try {
            URI baseUri = new URI(baseUrl);
            URI fullUri = baseUri.resolve(relativeUrl);
            return fullUri.toString();
        } catch (URISyntaxException e) {
            throw new MalformedURLException("Invalid base URL or relative URL");
        }
    }
}
