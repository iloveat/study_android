package com.turing.tts.online;

/**
 * Created by brycezou on 3/8/18.
 */

public interface IResultListener {
    void onPcmCompleted(byte[] pcm, int code);
    void onUpdateOnlineStatus(int status);
    void onFirstBatchReceived();
    void onSystemReady();
}















