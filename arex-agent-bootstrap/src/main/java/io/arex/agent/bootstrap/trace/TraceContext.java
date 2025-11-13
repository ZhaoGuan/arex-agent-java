package io.arex.agent.bootstrap.trace;

import java.util.UUID;

public class TraceContext {
    private static final ThreadLocal<TraceSpan> CURRENT_SPAN = new ThreadLocal<>();

    public static TraceSpan currentSpan() {
        return CURRENT_SPAN.get();
    }

    public static TraceSpan createRoot() {
        TraceSpan span = new TraceSpan(generateTraceId(), "0", null);
        CURRENT_SPAN.set(span);
        return span;
    }

    public static TraceSpan nextChild() {
        TraceSpan parent = currentSpan();
        if (parent == null) {
            return createRoot();
        }
        TraceSpan child = new TraceSpan(parent.getTraceId(), nextSpanId(parent.getSpanId()), parent.getSpanId());
        CURRENT_SPAN.set(child);
        return child;
    }

    public static void set(TraceSpan span) {
        CURRENT_SPAN.set(span);
    }

    public static void clear() {
        CURRENT_SPAN.remove();
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String nextSpanId(String parentSpanId) {
        return parentSpanId == null ? "1" : parentSpanId + ".1";
    }

    public static class TraceSpan {
        private final String traceId;
        private final String spanId;
        private final String parentSpanId;

        public TraceSpan(String traceId, String spanId, String parentSpanId) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getSpanId() {
            return spanId;
        }

        public String getParentSpanId() {
            return parentSpanId;
        }
    }
}
