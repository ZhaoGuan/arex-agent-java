package io.arex.inst.netty.v4.server;

import io.arex.agent.bootstrap.model.Mocker;
import io.arex.inst.runtime.context.ContextManager;
import io.arex.inst.runtime.listener.CaseEvent;
import io.arex.inst.runtime.listener.CaseEventDispatcher;
import io.arex.inst.runtime.log.LogManager;
import io.arex.inst.runtime.model.ArexConstants;
import io.arex.inst.netty.v4.common.AttributeKey;
import io.arex.inst.netty.v4.common.NettyHelper;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.inst.runtime.util.MockUtils;
import io.arex.inst.runtime.util.TypeUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

import java.util.Map;

public class ResponseTracingHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!ContextManager.needRecordOrReplay()) {
            super.write(ctx, msg, promise);
            return;
        }

        try {
            if (msg instanceof LastHttpContent) {
                if (msg instanceof FullHttpResponse) {
                    processHeaders(ctx.channel(), (HttpResponse) msg);
                }
                invoke(ctx, (LastHttpContent) msg, promise);
            } else {
                if (msg instanceof HttpResponse) {
                    processHeaders(ctx.channel(), (HttpResponse) msg);
                }
            }
        } catch (Throwable e) {
            LogManager.warn("netty write error", e);
        } finally {
            super.write(ctx, msg, promise);
        }
    }

    void processHeaders(final Channel channel, final HttpResponse response) {
        Mocker mocker = channel.attr(AttributeKey.TRACING_MOCKER).get();
        if (mocker == null) {
            return;
        }
        Map<String, String> headers = NettyHelper.parseHeaders(response.headers());
        mocker.getTargetResponse().setAttribute("Headers", headers);
        channel.attr(AttributeKey.TRACING_MOCKER).set(mocker);
        appendHeader(response);
    }

    void appendHeader(HttpResponse response) {
        if (ContextManager.needRecord()) {
            response.headers().set(ArexConstants.RECORD_ID, ContextManager.currentContext().getCaseId());
            return;
        }

        if (ContextManager.needReplay()) {
            response.headers().set(ArexConstants.REPLAY_ID, ContextManager.currentContext().getReplayId());
        }
    }

    void invoke(ChannelHandlerContext ctx, LastHttpContent msg, ChannelPromise prm) {
        // compatible with VoidChannelPromise.java package not visible (< 4.1.0)
        if (prm.getClass().getName().contains("VoidChannelPromise")) {
            prm = ctx.newPromise();
        }

        String body = NettyHelper.parseBody(msg.content());
        Mocker mocker = ctx.channel().attr(AttributeKey.TRACING_MOCKER).get();
        Throwable throwable = prm.cause();
        Object response = throwable != null ? throwable : body;
        if (mocker == null || response == null) {
            return;
        }
        mocker.getTargetResponse().setBody(Serializer.serialize(response));
        mocker.getTargetResponse().setType(TypeUtil.getName(response));
        // TODO 录制和回放互斥
        if (ContextManager.needReplay()) {
            Object replayResult = MockUtils.replayMocker(mocker);
            // 这里需要判断上文的 replayMock结果 如果有 这里也不能执行 不然返回 entry 会被录制
            String isAlwaysReplay = System.getProperty("arex.isAlwaysReplay");
            if (replayResult == null && mocker.getReplayId() == null && isAlwaysReplay != null && isAlwaysReplay.equals("true")) {
                mocker.getTargetResponse().setBody(String.valueOf(System.currentTimeMillis()));
                mocker.getTargetResponse().setType(Long.class.getName());
                MockUtils.recordMocker(mocker);
            }
            // 原逻辑
            // MockUtils.replayMocker(mocker);
        } else {
            MockUtils.recordMocker(mocker);
        }
        CaseEventDispatcher.onEvent(CaseEvent.ofExitEvent());
    }
}