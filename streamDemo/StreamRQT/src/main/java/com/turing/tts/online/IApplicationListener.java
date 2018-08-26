package com.turing.tts.online;

import com.turing.tts.ErrorMessage;

/**
 * TTS状态监听类
 * Created by brycezou on 7/31/17.
 */

public interface IApplicationListener {
    void onSynthesizeStart();
    void onSynthesizeComplete();
    void onPlayStart();
    void onPlayComplete();
    void onFirstBatchReceived();
    void onError(ErrorMessage msg);
    void onSystemReady();
}

