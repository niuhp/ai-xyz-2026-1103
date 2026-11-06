package com.niuhp.aixyz.config;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * 禁止 HTTP 传输层隐式重放生成请求，由应用层决定是否重新生成。
 *
 * @author niuhaipeng
 */
final class OneShotModelRequestBody extends RequestBody {

    /** SDK 已序列化的原始请求体。 */
    private final RequestBody delegate;

    OneShotModelRequestBody(RequestBody delegate) {
        this.delegate = delegate;
    }

    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return delegate.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        delegate.writeTo(sink);
    }

    @Override
    public boolean isOneShot() {
        return true;
    }
}
