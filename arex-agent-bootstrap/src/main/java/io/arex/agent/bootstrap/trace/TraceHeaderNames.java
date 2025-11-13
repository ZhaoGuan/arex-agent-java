package io.arex.agent.bootstrap.trace;

public interface TraceHeaderNames {
    String TRACE_ID = "X-Arex-TraceId";
    String SPAN_ID = "X-Arex-SpanId";
    String PARENT_SPAN_ID = "X-Arex-ParentSpanId";
}
