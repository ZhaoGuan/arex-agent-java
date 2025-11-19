package io.arex.agent.bootstrap.model;

public class MockResult {
    public static final MockResult IGNORE_MOCK_RESULT = new MockResult(true, null, null);
    private final boolean ignoreMockResult;
    private final Object result;
    private final Throwable throwable;

    private MockResult(boolean ignoreMockResult, Object mockResult, Throwable throwable) {
        this.ignoreMockResult = ignoreMockResult;
        this.result = mockResult;
        this.throwable = throwable;
    }

    public boolean isIgnoreMockResult() {
        return ignoreMockResult;
    }

    public boolean notIgnoreMockResult() {
        return !isIgnoreMockResult();
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Object getResult() {
        return result;
    }

    public static MockResult success(boolean ignoreMockResult, Object mockResult) {
        // TODO 记录修改的内容 空结果返回 null 防止出现服务500
        if (mockResult == null) {
            return new MockResult(true, null, null);
        }
        if (mockResult instanceof Throwable) {
            return new MockResult(ignoreMockResult, null, (Throwable) mockResult);
        }
        return new MockResult(ignoreMockResult, mockResult, null);
    }

    public static MockResult success(Object mockResult) {
        return success(false, mockResult);
    }
}
