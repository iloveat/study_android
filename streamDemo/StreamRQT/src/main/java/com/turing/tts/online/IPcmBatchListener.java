package com.turing.tts.online;

/**
 * Created by brycezou on 5/22/18.
 */

public interface IPcmBatchListener {
    void onPcmArrivalCallback(int status, byte[] pcm, int text_index, int pcm_index, String text);
}







