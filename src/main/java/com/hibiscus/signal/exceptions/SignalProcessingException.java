package com.hibiscus.signal.exceptions;

/**
 * 信号处理过程中发生的异常类
 */
public class SignalProcessingException extends Exception {

    /**
     * 错误码，用于标识不同类型的异常
     */
    private final int errorCode;

    /**
     * 构造函数，使用指定的错误信息和错误码创建异常对象
     *
     * @param message   异常信息
     * @param errorCode 错误码
     */
    public SignalProcessingException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数，使用指定的错误信息、错误码和异常原因创建异常对象
     *
     * @param message   异常信息
     * @param errorCode 错误码
     * @param cause     异常原因
     */
    public SignalProcessingException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 返回异常的详细信息，包括错误码、消息和原因（如果存在）
     *
     * @return 异常信息字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SignalProcessingException [errorCode=").append(errorCode)
                .append(", message=").append(getMessage());

        if (getCause() != null) {
            sb.append(", cause=").append(getCause().toString());
        }

        sb.append("]");
        return sb.toString();
    }
}
