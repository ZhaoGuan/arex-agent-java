package io.arex.inst.httpclient.resttemplate;

import io.arex.agent.bootstrap.model.MockResult;
import io.arex.agent.bootstrap.trace.TracePropagator;
import io.arex.inst.extension.MethodInstrumentation;
import io.arex.inst.extension.TypeInstrumentation;
import io.arex.inst.runtime.context.ContextManager;
import io.arex.inst.runtime.context.RepeatedCollectManager;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.bytebuddy.asm.Advice.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class RestTemplateInstrumentation extends TypeInstrumentation {

    @Override
    protected ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.springframework.web.client.RestTemplate");
    }

    @Override
    public List<MethodInstrumentation> methodAdvices() {
        return Collections.singletonList(new MethodInstrumentation(isMethod().and(isProtected()).and(named("doExecute")).and(takesArguments(4)), ExecuteAdvice.class.getName()));
    }

    public static class ExecuteAdvice {

        @OnMethodEnter(skipOn = OnNonDefaultValue.class, suppress = Throwable.class)
        public static boolean onEnter(@Argument(0) URI uri, @Argument(1) HttpMethod httpMethod, @Argument(2) RequestCallback requestCallback, @Local("extractor") RestTemplateExtractor extractor, @Local("mockResult") MockResult mockResult) {
            if (ContextManager.needRecordOrReplay()) {
                // 包装原来的 RequestCallback，注入 Trace Header
                RequestCallback originalCallback = requestCallback;
                requestCallback = clientHttpRequest -> {
                    // 先调用原来的回调
                    if (originalCallback != null) {
                        originalCallback.doWithRequest(clientHttpRequest);
                    }
                    // 注入 Trace Header
                    Map<String, String> headers = TracePropagator.currentHeaders();
                    HttpHeaders httpHeaders = clientHttpRequest.getHeaders();
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null && !httpHeaders.containsKey(entry.getKey())) {
                            httpHeaders.add(entry.getKey(), entry.getValue());
                        }
                    }
                };
                // 原有逻辑...
                extractor = new RestTemplateExtractor(uri, httpMethod, requestCallback);
                if (ContextManager.needReplay()) {
                    mockResult = extractor.replay();
                    return mockResult != null && mockResult.notIgnoreMockResult();
                }
                RepeatedCollectManager.enter();
            }
            return false;
        }

        @OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(@Return(readOnly = false, typing = Typing.DYNAMIC) Object result, @Thrown(readOnly = false) Throwable throwable, @Local("extractor") RestTemplateExtractor extractor, @Local("mockResult") MockResult mockResult) {
            if (mockResult != null && mockResult.notIgnoreMockResult()) {
                if (mockResult.getThrowable() != null) {
                    throwable = mockResult.getThrowable();
                } else {
                    result = mockResult.getResult();
                }
                return;
            }
            if (ContextManager.needRecord() && RepeatedCollectManager.exitAndValidate() && extractor != null) {
                extractor.record(result, throwable);
            }
        }
    }
}
