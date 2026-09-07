package com.crm.backend.webhook;

import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class WebhookEndpointValidator {

    private static final int MAX_URL_LENGTH = 2048;

    private final WebhookSecurityProperties properties;
    private final WebhookHostResolver hostResolver;

    public WebhookEndpointValidator(
            WebhookSecurityProperties properties,
            WebhookHostResolver hostResolver
    ) {
        this.properties = properties;
        this.hostResolver = hostResolver;
    }

    public String validateAndNormalize(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw invalid("Webhook endpoint URL is required");
        }

        String trimmedUrl = endpointUrl.trim();
        if (trimmedUrl.length() > MAX_URL_LENGTH) {
            throw invalid("Webhook endpoint URL is too long");
        }

        URI uri = parse(trimmedUrl);
        String scheme = normalizedScheme(uri);
        String host = normalizedHost(uri);
        boolean localDevelopmentEndpoint = isLocalDevelopmentEndpoint(
                scheme,
                host
        );

        validateStructure(uri);
        validateSchemeAndPort(uri, scheme, localDevelopmentEndpoint);
        validateResolvedAddresses(host, localDevelopmentEndpoint);

        String normalizedUrl = uri.normalize().toASCIIString();
        if (normalizedUrl.length() > MAX_URL_LENGTH) {
            throw invalid("Webhook endpoint URL is too long");
        }
        return normalizedUrl;
    }

    private URI parse(String endpointUrl) {
        try {
            return new URI(endpointUrl);
        } catch (URISyntaxException exception) {
            throw invalid("Webhook endpoint URL is invalid");
        }
    }

    private String normalizedScheme(URI uri) {
        if (uri.getScheme() == null) {
            throw invalid("Webhook endpoint URL must be absolute");
        }
        return uri.getScheme().toLowerCase(Locale.ROOT);
    }

    private String normalizedHost(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw invalid("Webhook endpoint URL must contain a valid host");
        }

        String unwrapped = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        if (unwrapped.endsWith(".")) {
            unwrapped = unwrapped.substring(0, unwrapped.length() - 1);
        }

        try {
            return unwrapped.contains(":")
                    ? unwrapped.toLowerCase(Locale.ROOT)
                    : IDN.toASCII(unwrapped).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw invalid("Webhook endpoint URL contains an invalid host");
        }
    }

    private void validateStructure(URI uri) {
        if (!uri.isAbsolute()) {
            throw invalid("Webhook endpoint URL must be absolute");
        }
        if (uri.getUserInfo() != null) {
            throw invalid("Webhook endpoint URL must not contain user info");
        }
        if (uri.getRawFragment() != null) {
            throw invalid("Webhook endpoint URL must not contain a fragment");
        }
    }

    private void validateSchemeAndPort(
            URI uri,
            String scheme,
            boolean localDevelopmentEndpoint
    ) {
        if (!"https".equals(scheme) && !localDevelopmentEndpoint) {
            throw invalid("Webhook endpoint URL must use HTTPS");
        }

        int port = uri.getPort();
        if (port > 65535) {
            throw invalid("Webhook endpoint URL contains an invalid port");
        }
        if ("https".equals(scheme) && port != -1 && port != 443) {
            throw invalid("Webhook HTTPS endpoints must use port 443");
        }
    }

    private boolean isLocalDevelopmentEndpoint(
            String scheme,
            String host
    ) {
        return properties.allowLocalHttp()
                && "http".equals(scheme)
                && "localhost".equals(host);
    }

    private void validateResolvedAddresses(
            String host,
            boolean localDevelopmentEndpoint
    ) {
        InetAddress[] addresses;
        try {
            addresses = hostResolver.resolve(host);
        } catch (UnknownHostException exception) {
            throw invalid("Webhook endpoint host could not be resolved");
        }

        if (addresses == null || addresses.length == 0) {
            throw invalid("Webhook endpoint host could not be resolved");
        }

        for (InetAddress address : addresses) {
            if (localDevelopmentEndpoint && address.isLoopbackAddress()) {
                continue;
            }
            if (isForbiddenAddress(address)) {
                throw invalid(
                        "Webhook endpoint must not resolve to a private or reserved address"
                );
            }
        }
    }

    private boolean isForbiddenAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return isForbiddenIpv4(bytes);
        }
        if (address instanceof Inet6Address) {
            return isForbiddenIpv6(bytes);
        }
        return true;
    }

    private boolean isForbiddenIpv4(byte[] address) {
        int first = Byte.toUnsignedInt(address[0]);
        int second = Byte.toUnsignedInt(address[1]);
        int third = Byte.toUnsignedInt(address[2]);

        return first == 0
                || first == 10
                || first == 127
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 0 && third == 0)
                || (first == 192 && second == 0 && third == 2)
                || (first == 192 && second == 168)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113)
                || first >= 224;
    }

    private boolean isForbiddenIpv6(byte[] address) {
        int first = Byte.toUnsignedInt(address[0]);
        int second = Byte.toUnsignedInt(address[1]);

        boolean uniqueLocal = (first & 0xfe) == 0xfc;
        boolean documentation = first == 0x20
                && second == 0x01
                && Byte.toUnsignedInt(address[2]) == 0x0d
                && Byte.toUnsignedInt(address[3]) == 0xb8;
        return uniqueLocal || documentation;
    }

    private InvalidWebhookEndpointException invalid(String message) {
        return new InvalidWebhookEndpointException(message);
    }
}
