package com.wcc.wink.request;

import java.io.IOException;

/**
 * Created by wenbiao.xie on 2016/6/15.
 */
public class NetworkIOException extends IOException {

    private int code;

    public NetworkIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkIOException() {
    }

    public NetworkIOException(Throwable cause) {
        super(cause);
    }

    public NetworkIOException(String detailMessage, int code) {
        super(detailMessage);
        this.code = code;
    }

    public int code() {return code;}
}
