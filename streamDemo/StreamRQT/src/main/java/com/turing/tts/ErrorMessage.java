package com.turing.tts;

/**
 * 错误信息实体类
 * Created by brycezou on 7/31/17.
 */

public class ErrorMessage {
    /**
     * 错误码
     */
    public int errorCode;

    /**
     * 错误信息
     */
    public String errorMsg;

    /**
     * 构造方法
     * @param errorMsg 错误信息
     * @param errorCode 错误码
     */
    public ErrorMessage(String errorMsg, int errorCode) {
        this.errorMsg = errorMsg;
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
                "errorMsg='" + errorMsg + '\'' +
                ", errorCode=" + errorCode +
                '}';
    }
}
