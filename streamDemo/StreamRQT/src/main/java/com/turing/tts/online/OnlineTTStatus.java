package com.turing.tts.online;

/**
 * Created by brycezou on 3/8/18.
 */

public class OnlineTTStatus {
    public static final int IDLE = 20001;
    public static final int START = 20002;
    public static final int FINISH = 20003;
    public static final int SYNTHESIZING = 20004;
    public static final int FINISH_WITH_ERROR_EMPTY_INPUT = -20001;
    public static final int FINISH_WITH_ERROR_ONLINE_FAILED = -20002;
    public static final int FINISH_WITH_ERROR_BUSY_SYNTHESIZING = -20003;
}













