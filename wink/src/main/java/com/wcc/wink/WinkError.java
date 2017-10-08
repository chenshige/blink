package com.wcc.wink;

import java.net.HttpURLConnection;

/**
 * Created by wenbiao.xie on 2015/11/5.
 */
public class WinkError {

    /**
     * 网络错误类，定义了各种常见的网络错误
     * @author devilxie
     * @version 1.0
     *
     */
    public interface NetworkError {
        /**
         * 网络成功
         */
        int SUCCESS = 0;
        /**
         * 网络未知错误
         */
        int FAIL_UNKNOWN = -1;
        /**
         * 连接超时错误
         */
        int FAIL_CONNECT_TIMEOUT = -2;
        /**
         * 资源未找到错误
         */
        int FAIL_NOT_FOUND = -3;
        /**
         * 网络读写错误
         */
        int FAIL_IO_ERROR = -4;
        /**
         * 用户中断
         */
        int CANCEL = -5;

        /**
         * 无网络
         */
        int NO_AVAILABLE_NETWORK = -6;

        /**
         * SOCKET 读写超时
         */
        int SOCKET_TIMEOUT = -7;

        /**
         * status code, 403: Forbidden
         */
        int AUTH_EXPIRED = -8;

        /**
         * status code, 416: RANGE OUT
         */
        int RANGE_INVALID = -9;

        int SCHEME_NOT_SUPPORT = -10;

    }

    public static int toNetworkErrorWithHttpStatus(int code) {
        if (code == HttpURLConnection.HTTP_ACCEPTED || code == HttpURLConnection.HTTP_OK ||
                code == HttpURLConnection.HTTP_PARTIAL)
            return NetworkError.SUCCESS;

        if (code >= 400 && code < 500) {
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return NetworkError.FAIL_NOT_FOUND;
            }

            else if (code == HttpURLConnection.HTTP_FORBIDDEN
                    || code == HttpURLConnection.HTTP_BAD_REQUEST
                    || code == HttpURLConnection.HTTP_UNAUTHORIZED
                    || code == HttpURLConnection.HTTP_BAD_METHOD
                    || code == HttpURLConnection.HTTP_GONE)
                return NetworkError.AUTH_EXPIRED;
            else if (code == 416) {
                return NetworkError.FAIL_IO_ERROR;
            }
            return NetworkError.FAIL_NOT_FOUND;

        } else if (code >= 500) {
            return NetworkError.SOCKET_TIMEOUT;
        }

        return NetworkError.FAIL_UNKNOWN;
    }

    /**
     * 无错误
     */
    public final static int SUCCESS = 0;

    /**
     * 任务已经完成, 无继续
     */
    public static final int ALREADY_COMPLETED = 0x1000;

    /**
     * 任务已经存在，处于暂停或失败状态
     */
    public static final int EXIST = 0x1001;


    /**
     * 任务参数无效
     */
    public static final int INVALID = 0x1002;

    /**
     * 存储空间不够，任务下载\添加失败
     */
    public static final int INSUFFICIENT_SPACE = 0x1003;

    /**
     * 文件已删除
     */
    public static final int FILE_NOT_EXIST = 0x1004;

    /**
     * 删除类错误码
     */
    public static final int DELETE_RANGE_START = 0x2000;
    public static final int DELETE_RANGE_MAX = 0x2020;
}
