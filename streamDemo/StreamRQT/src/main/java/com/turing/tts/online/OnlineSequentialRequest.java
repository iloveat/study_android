package com.turing.tts.online;

import android.util.Log;

import java.util.LinkedList;

/**
 * Created by brycezou on 3/8/18.
 */

public class OnlineSequentialRequest {

    private static final String TAG = "OnlineTTS";
    private static LinkedList[] mPcmListArray = null;
    private static Thread[] mThread = null;
    private static int[] mThreadStatus = null;
    public static int mOnlineStatus = OnlineTTStatus.IDLE;
    private static boolean mbStopSynthesize = false;
    private static int mRequestNumber = 5;
    private static IResultListener mResultListener = null;
    private static int mMaxTextAndPcmIndex = -1;


    private static IPcmBatchListener mPcmBatchListener = new IPcmBatchListener() {
        @Override
        public void onPcmArrivalCallback(int status, byte[] pcm, int text_index, int pcm_index, String text) {
            if (mbStopSynthesize) {
                if (status == PcmBatchStatus.BATCH_FINISH) {
                    mMaxTextAndPcmIndex = -1;
                    if (text_index >= 0) {
                        updateOnlineStatus(OnlineTTStatus.FINISH);
                    }
                    mOnlineStatus = OnlineTTStatus.IDLE;
                    if (mResultListener != null) {
                        mResultListener.onSystemReady();
                    }
                }
                return;
            }
            switch (status) {
                case PcmBatchStatus.BATCH_ERROR:
                    break;
                case PcmBatchStatus.BATCH_EXCEPTION:
                    break;
                case PcmBatchStatus.BATCH_RESPONSE_NOT200:
                    break;
                case PcmBatchStatus.BATCH_FINISH:
                    break;
                case PcmBatchStatus.BATCH_NORMAL:
                    int cur_index = text_index * 10000 + pcm_index;
                    if (cur_index <= mMaxTextAndPcmIndex) {
                        return;
                    }
                    mResultListener.onPcmCompleted(pcm, 1);
                    if (text_index == 0 && pcm_index == 0) {
                        mResultListener.onFirstBatchReceived();
                    }
                    //Log.i(TAG, String.format("%03d_%03d, pcm part", text_index, pcm_index));
                    if (cur_index > mMaxTextAndPcmIndex) {
                        mMaxTextAndPcmIndex = cur_index;
                    }
                    if (mbStopSynthesize) {
                        mMaxTextAndPcmIndex = -1;
                        updateOnlineStatus(OnlineTTStatus.FINISH);
                        mOnlineStatus = OnlineTTStatus.IDLE;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public static void initSequentialRequest() {
        OnlineRequest.setPcmBatchListener(mPcmBatchListener);
    }

    private static LinkedList<byte[]> synthesizeOnce(String text, int index) {
        LinkedList<byte[]> pcm_list = OnlineRequest.postRequest4PCM(text, index);
        if(pcm_list == null || pcm_list.size() == 0 || pcm_list.peekLast() != null) {
            return null;
        }
        return pcm_list;
    }

    private static synchronized int prepareThreads(String text) {
        LinkedList<String> txt_queue = TextPreprocessor.splitText(text);
        int batch_num = txt_queue.size();
        if(batch_num > 0) {
            if(mPcmListArray != null) {
                for(int i = 0; i < mPcmListArray.length; i++) {
                    if(mPcmListArray[i] != null) {
                        mPcmListArray[i].clear();
                        mPcmListArray[i] = null;
                    }
                }
            }
            mPcmListArray = null;
            mPcmListArray = new LinkedList[batch_num];
            mThread = null;
            mThread = new Thread[batch_num];
            mThreadStatus = null;
            mThreadStatus = new int[batch_num];

            for(int i = 0; i < batch_num; i++) {
                OnlineTTSRunnable runnable = new OnlineTTSRunnable() {
                    @Override
                    public void run() {
                        int idx = this.getIndex();
                        String text = this.getText();
                        LinkedList<byte[]> pcmList = synthesizeOnce(text, idx);
                        try {
                            if(mThreadStatus != null && mPcmListArray != null && idx < mThreadStatus.length && idx < mPcmListArray.length) {
                                if (pcmList != null) {
                                    mThreadStatus[idx] = 1;  //线程执行成功
                                } else {
                                    mThreadStatus[idx] = -1; //线程执行失败
                                }
                                mPcmListArray[idx] = pcmList;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                runnable.setValue(txt_queue.get(i), i);
                Thread thd = new Thread(runnable);
                mThread[i] = thd;
                mThreadStatus[i] = 0;  //线程还未开启
            }
            return 1;
        }
        return -1;
    }

    private static void updateOnlineStatus(int status) {
        if(mResultListener != null) {
            mResultListener.onUpdateOnlineStatus(status);
        }
    }

    /***
     * return:
     * -1: error occurred
     * -2: failed
     * -3: exception
     *  1: succeed
     *  2: stop synthesize
     */
    private static synchronized int waitForResponseComplete(int index) {
        try {
            if(mThreadStatus == null || index >= mThreadStatus.length || index < 0) {
                return -1;
            }
            // 等待第i句话的合成结果
            int timeout = 0;
            while(mThreadStatus[index] == 0 && timeout < 2000) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timeout++;
                if(mbStopSynthesize) {
                    return 2;
                }
            }
            if(mThreadStatus[index] == 0) {
                mThreadStatus[index] = -1;
            }
            if(mThreadStatus[index] == -1) {
                return -2;
            }

            // mThreadStatus[index] must be 1
            if(mPcmListArray[index] != null) {
                mPcmListArray[index].clear();
                mPcmListArray[index] = null;
            }
            Log.i(TAG, String.format("%03d, %s", index, "pcm complete"));
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return -3;
        }
    }

    /***
     * return:
     * -1: stop synthesize
     * -2: failed
     *  1: succeed
     */
    private static int tryRequestManyTimes(int idx) {
        int result = 1;
        try {
            mThread[idx].start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (waitForResponseComplete(idx) < 0) {
            for (int kkk = 1; kkk < mRequestNumber; kkk++) {
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(mbStopSynthesize) {
                    result = -1;
                    break;
                }
                try {
                    mThreadStatus[idx] = 0;  // 线程还未开启
                    mThread[idx].run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (waitForResponseComplete(idx) < 0) {
                    result = -2;
                } else {
                    result = 1;
                    break;
                }
            }
        }
        return result;
    }

    public static void synthesize(final String text) {
        // check if in busy
        if(mOnlineStatus != OnlineTTStatus.IDLE) {
            updateOnlineStatus(OnlineTTStatus.FINISH_WITH_ERROR_BUSY_SYNTHESIZING);
            return;
        }
        // 设置TTS状态为正在合成
        mOnlineStatus = OnlineTTStatus.SYNTHESIZING;
        mbStopSynthesize = false;
        mMaxTextAndPcmIndex = -1;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateOnlineStatus(OnlineTTStatus.START);

                    if(prepareThreads(text) < 0) {
                        updateOnlineStatus(OnlineTTStatus.FINISH_WITH_ERROR_EMPTY_INPUT);
                        mOnlineStatus = OnlineTTStatus.IDLE;
                        return;
                    }

                    int batch_num = mThread.length;
                    for(int i = 0; i < batch_num; i++) {
                        if(mbStopSynthesize) {
                            break;
                        }
                        // 拷贝第i句话的结果
                        int code = tryRequestManyTimes(i);
                        if(code == -1) {  // stop synthesize
                            break;
                        } else if(code == -2) {
                            updateOnlineStatus(OnlineTTStatus.FINISH_WITH_ERROR_ONLINE_FAILED);
                            mOnlineStatus = OnlineTTStatus.IDLE;
                            return;
                        } else {  // succeed

                        }
                    }
                    mResultListener.onPcmCompleted(null, -1);

                    if(!mbStopSynthesize) {
                        updateOnlineStatus(OnlineTTStatus.FINISH);
                        mOnlineStatus = OnlineTTStatus.IDLE;
                        if (mResultListener != null) {
                            mResultListener.onSystemReady();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void setRequestNumber(int num) {
        if(num < 2) {
            mRequestNumber = 2;
        } else if(num > 8) {
            mRequestNumber = 8;
        } else {
            mRequestNumber = num;
        }
    }

    public static void setResultListener(IResultListener listener) {
        mResultListener = listener;
    }

    public static void interruptSequentialRequest() {
        mbStopSynthesize = true;
        OnlineRequest.interruptRequest();
    }

}













