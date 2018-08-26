package com.turing.tts.online;

/**
 * Created by brycezou on 3/8/18.
 */

public class OnlineTTSRunnable implements Runnable {

    private String mText;
    private int mIndex;

    public void setValue(String text, int idx) {
        mText = text;
        mIndex = idx;
    }

    public String getText() {
        return mText;
    }

    public int getIndex() {
        return mIndex;
    }

    @Override
    public void run() {

    }
}