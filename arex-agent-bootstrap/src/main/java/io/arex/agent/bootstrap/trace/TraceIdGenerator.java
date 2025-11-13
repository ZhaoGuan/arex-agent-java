package io.arex.agent.bootstrap.trace;

import java.util.UUID;

public class TraceIdGenerator {
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
