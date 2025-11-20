package io.arex.inst.runtime.context;

import io.arex.agent.bootstrap.TraceContextManager;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.inst.runtime.listener.ContextListener;
import io.arex.inst.runtime.log.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextManager {
    private static final Map<String, ArexContext> RECORD_MAP = new LatencyContextHashMap();
    private static final List<ContextListener> LISTENERS = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    /**
     * agent call this method
     */
    public static ArexContext currentContext() {
        return currentContext(false, null);
    }

    /**
     * agent will call this method
     * record scene: recordId is map key
     * replay scene: replayId is map key
     */
    public static ArexContext currentContext(boolean createIfAbsent, String recordId) {
        String traceId = TraceContextManager.get(createIfAbsent);
        if (StringUtil.isEmpty(traceId)) {
            return null;
        }
        if (createIfAbsent) {
            final ArexContext arexContext = createContext(recordId, traceId);
            publish(arexContext, true);
            RECORD_MAP.put(traceId, arexContext);
            return arexContext;
        }
        return RECORD_MAP.get(traceId);
    }

    /**
     * ArexContext.of(recordId, replayId)
     */
    private static ArexContext createContext(String recordId, String traceId) {
        // replay scene: traceId is replayId
        if (StringUtil.isNotEmpty(recordId)) {
            return ArexContext.of(recordId, traceId);
        }
        // record scene: traceId is recordId
        return ArexContext.of(traceId);
    }

    public static ArexContext getContext(String traceId) {
        return RECORD_MAP.get(traceId);
    }

    public static boolean needRecord() {
        ArexContext context = currentContext();
        LogManager.info("ContextManager needRecord", String.valueOf(context != null && !context.isReplay()));
        return context != null && !context.isReplay();
    }

    public static boolean needReplay() {
        String isAlwaysReplay = System.getProperty("arex.enable.isAlwaysReplay");
        LogManager.info("needReplay isAlwaysReplay: ", String.valueOf(isAlwaysReplay));
        if (isAlwaysReplay != null && isAlwaysReplay.equals("true")) {
            return true;
        }
        ArexContext context = currentContext();
        return context != null && context.isReplay();
    }

    public static boolean needRecordOrReplay() {
        String isAlwaysReplay = System.getProperty("arex.enable.isAlwaysReplay");
        LogManager.info("needRecordOrReplay isAlwaysReplay: ", String.valueOf(isAlwaysReplay));
        if (isAlwaysReplay != null && isAlwaysReplay.equals("true")) {
            return true;
        }
        return currentContext() != null;
    }

    public static void remove() {
        String caseId = TraceContextManager.remove();
        if (StringUtil.isEmpty(caseId)) {
            return;
        }
        ArexContext context = RECORD_MAP.remove(caseId);
        publish(context, false);
    }

    public static void registerListener(ContextListener listener) {
        LISTENERS.add(listener);
    }

    private static void publish(ArexContext context, boolean isCreate) {
        if (CollectionUtil.isNotEmpty(LISTENERS)) {
            LISTENERS.forEach(listener -> {
                if (isCreate) {
                    listener.onCreate(context);
                } else {
                    listener.onComplete(context);
                }
            });
        }
    }

    public static void setAttachment(String key, Object value) {
        ArexContext context = currentContext();
        if (context != null) {
            context.setAttachment(key, value);
        }
    }
}
