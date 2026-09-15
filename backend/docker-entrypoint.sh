#!/bin/sh
set -eu

if [ -n "${AIVEN_CA_CERT_BASE64:-}" ]; then
    case "${SPRING_DATASOURCE_URL:-}" in
        jdbc:mysql://*) ;;
        *) echo "Aiven TLS requires a MySQL datasource URL." >&2; exit 1 ;;
    esac
    case "$SPRING_DATASOURCE_URL" in
        *sslMode=*|*trustCertificateKeyStore*|*useSSL=*|*verifyServerCertificate=*)
            echo "Aiven TLS configures SSL settings; remove them from the datasource URL." >&2
            exit 1
            ;;
    esac

    tls_directory=$(mktemp -d /tmp/crm-db-tls.XXXXXX)
    printf '%s' "$AIVEN_CA_CERT_BASE64" | base64 -d > "$tls_directory/ca.pem"
    # This store contains only a public CA certificate, not private keys.
    keytool -importcert -noprompt -alias aiven-ca \
        -file "$tls_directory/ca.pem" \
        -keystore "$tls_directory/truststore.p12" \
        -storetype PKCS12 -storepass changeit

    case "$SPRING_DATASOURCE_URL" in
        *\?*) separator='&' ;;
        *) separator='?' ;;
    esac
    SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}${separator}sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:${tls_directory}/truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=changeit&fallbackToSystemTrustStore=false"
    export SPRING_DATASOURCE_URL
    unset AIVEN_CA_CERT_BASE64
fi

exec java -jar /app/app.jar "$@"
