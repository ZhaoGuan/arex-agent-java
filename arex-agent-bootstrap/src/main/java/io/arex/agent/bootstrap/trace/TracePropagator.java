package io.arex.agent.bootstrap.trace;

import java.util.Map;
import java.util.HashMap;

public class TracePropagator {
    public static Map<String, String> currentHeaders() {
        TraceContext.TraceSpan span = TraceContext.nextChild();
        Map<String, String> headers = new HashMap<>();
        headers.put(TraceHeaderNames.TRACE_ID, span.getTraceId());
        headers.put(TraceHeaderNames.SPAN_ID, span.getSpanId());
        if (span.getParentSpanId() != null) {
            headers.put(TraceHeaderNames.PARENT_SPAN_ID, span.getParentSpanId());
        }
        return headers;
    }
}
