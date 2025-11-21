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
        // TODO 有自定义参数的时候 这里才对 mockResult==null 返回 null
        String isAlwaysReplay = System.getProperty("arex.isAlwaysReplay");
        if (isAlwaysReplay != null && isAlwaysReplay.equals("true") && mockResult == null) {
            return null;
        }
        // 原逻辑
        if (mockResult instanceof Throwable) {
            return new MockResult(ignoreMockResult, null, (Throwable) mockResult);
        }
        return new MockResult(ignoreMockResult, mockResult, null);
    }

    public static MockResult success(Object mockResult) {
        return success(false, mockResult);
    }
}
