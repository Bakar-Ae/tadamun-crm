package com.crm.backend.webhook;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface WebhookHostResolver {

    InetAddress[] resolve(String host) throws UnknownHostException;
}
