package com.turing.tts.online;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.util.Log;

import com.turing.tts.ErrorCode;
import com.turing.tts.ErrorMessage;

import java.io.FileOutputStream;
import java.util.LinkedList;

/**
 * Created by brycezou on 3/8/18.
 */

public class OnlineTTS {

    private static final String TAG = "OnlineTTS";
    private static volatile OnlineTTS mInstance = null;
    private static AudioTrack mAudioTrack = null;
    private static LinkedList<byte[]> mAudioQueue = null;
    private static LoudnessEnhancer mLoudnessEnhancer = null;
    private static int mSampleRate = 24000;
    private static int mTargetGain = 0;
    // 保存音频文件的路径
    private static String mAudioFilePath = "";
    // 是否运行音频播放线程
    private static boolean mbPlayAudio = false;
    private static IApplicationListener mAppListener = null;


    private static void resetAudioQueue() {
        if(mAudioQueue == null) {
            mAudioQueue = new LinkedList<>();
        }
        mAudioQueue.clear();
    }

    private static void resetAudioTrack() {
        if(mAudioTrack == null) {
            final int SAMPLE_RATE = mSampleRate;
            int MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER_SIZE * 2, AudioTrack.MODE_STREAM);
            try {
                if(mLoudnessEnhancer == null) {
                    mLoudnessEnhancer = new LoudnessEnhancer(mAudioTrack.getAudioSessionId());
                    mLoudnessEnhancer.setTargetGain(mTargetGain);
                    mLoudnessEnhancer.setEnabled(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private OnlineTTS() {
        OnlineSequentialRequest.initSequentialRequest();
        OnlineSequentialRequest.setResultListener(new IResultListener() {
            @Override
            public void onPcmCompleted(byte[] pcm, int code) {
                if(mAudioQueue != null) {
                    if(code == -1) {  // reach the end
                        mAudioQueue.add(null);
                    } else {
                        mAudioQueue.add(pcm);

                        // 如果所给文件名不为null,则尝试写文件;否则就不写
                        if(mAudioFilePath != null) {
                            try {
                                FileOutputStream f_out = new FileOutputStream(mAudioFilePath, true);
                                f_out.write(pcm);
                                f_out.close();
                            } catch (Exception e) {
                                if(mAppListener != null) {
                                    mAppListener.onError(new ErrorMessage("写入文件失败", ErrorCode.ERROR_WRITE_FILE));
                                }
                                Log.e(TAG, "write file failed: " + mAudioFilePath);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onUpdateOnlineStatus(int status) {
                switch (status) {
                    case OnlineTTStatus.START:
                        // 如果所给文件名不为null,则尝试创建文件
                        if(mAudioFilePath != null) {
                            try {
                                FileOutputStream f_out = new FileOutputStream(mAudioFilePath);
                                f_out.close();
                            } catch (Exception e) {
                                if(mAppListener != null) {
                                    mAppListener.onError(new ErrorMessage("创建文件失败", ErrorCode.ERROR_CREATE_FILE));
                                }
                                Log.e(TAG, "create file failed: " + mAudioFilePath);
                                e.printStackTrace();
                            }
                        }
                        if(mAppListener != null) {
                            mAppListener.onSynthesizeStart();
                            Log.i(TAG, "开始合成");
                        }
                        break;
                    case OnlineTTStatus.FINISH:
                        if(mAppListener != null) {
                            mAppListener.onSynthesizeComplete();
                            Log.i(TAG, "合成结束");
                        }
                        break;
                    case OnlineTTStatus.FINISH_WITH_ERROR_EMPTY_INPUT:
                        if(mAppListener != null) {
                            mAppListener.onError(new ErrorMessage("输入文本为空", ErrorCode.ERROR_INPUT_EMPTY));
                        }
                        break;
                    case OnlineTTStatus.FINISH_WITH_ERROR_ONLINE_FAILED:
                        if(mAppListener != null) {
                            mAppListener.onError(new ErrorMessage("在线合成失败", ErrorCode.ERROR_ONLINE_FAIL));
                        }
                        break;
                    case OnlineTTStatus.FINISH_WITH_ERROR_BUSY_SYNTHESIZING:
                        if(mAppListener != null) {
                            mAppListener.onError(new ErrorMessage("当前正在合成", ErrorCode.ERROR_IN_BUSY));
                            Log.e(TAG, "已经有任务正在合成");
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onFirstBatchReceived() {
                if(mAppListener != null) {
                    mAppListener.onFirstBatchReceived();
                }
            }

            @Override
            public void onSystemReady() {
                if(mAppListener != null) {
                    mAppListener.onSystemReady();
                }
            }
        });
    }

    public static OnlineTTS getInstance() {
        if(mInstance == null) {
            synchronized (OnlineTTS.class) {
                if(mInstance == null) {
                    mInstance = new OnlineTTS();
                }
            }
        }
        return mInstance;
    }

    public void initTTS() {
        resetAudioQueue();
        resetAudioTrack();
        mbPlayAudio = false;
        if(!mPlayAudioThread.isAlive()) {
            mPlayAudioThread.start();
        }
    }

    private static void sleepMillisecond(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Thread mPlayAudioThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                if(mbPlayAudio) {
                    if(mAudioQueue != null && !mAudioQueue.isEmpty()) {
                        byte[] wav_buff;
                        try {
                            wav_buff = mAudioQueue.removeFirst();
                        } catch (Exception e) {
                            continue;
                        }

                        if(wav_buff == null && mAudioQueue != null && mAudioQueue.isEmpty()) {
                            mbPlayAudio = false;
                            try {
                                if(mAudioTrack != null) {
                                    mAudioTrack.stop();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if(mAppListener != null) {
                                mAppListener.onPlayComplete();
                                Log.i(TAG, "播放结束");
                            }
                            continue;
                        } else if(wav_buff == null) {
                            continue;
                        }

                        try {
                            if(mbPlayAudio) {
                                mAudioTrack.write(wav_buff, 0, wav_buff.length);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        sleepMillisecond(10);
                    }
                } else {
                    sleepMillisecond(10);
                }
            }
        }
    });

    public void synthesizeOnline(String text, String audioPath) {
        if(OnlineSequentialRequest.mOnlineStatus != OnlineTTStatus.IDLE) {
            if(mAppListener != null) {
                mAppListener.onError(new ErrorMessage("当前正在合成", ErrorCode.ERROR_IN_BUSY));
                Log.e(TAG, "已经有任务正在合成");
            }
            return;
        }
        mAudioFilePath = audioPath;
        resetAudioQueue();
        OnlineSequentialRequest.synthesize(text);
    }

    public void play() {
        if(mbPlayAudio) {
            mbPlayAudio = true;
        } else {
            mbPlayAudio = true;
            try {
                mAudioTrack.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(mAppListener != null) {
                mAppListener.onPlayStart();
                Log.i(TAG, "开始播放");
            }
        }
    }

    public void stopTTS() {
        // 停止合成
        OnlineSequentialRequest.interruptSequentialRequest();

        if(mbPlayAudio) {
            mbPlayAudio = false;
            if(mAppListener != null) {
                mAppListener.onPlayComplete();
                Log.i(TAG, "播放结束");
            }
        }

        try {
            Thread.sleep(100);
            if(mAudioTrack != null) {
                mAudioTrack.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resetAudioQueue();
        }
    }

    public void setAppListener(IApplicationListener listener) {
        mAppListener = listener;
    }

    public void setSampleRate(int sr) {
        mSampleRate = sr;
    }

    public void setTargetGain(int gain) {
        mTargetGain = gain;
    }

    public void setRequestNumber(int num) {
        OnlineSequentialRequest.setRequestNumber(num);
    }

    public void setTimeoutTime(int connTimeout, int readTimeout) {
        OnlineRequest.setRequestTimeout(connTimeout, readTimeout);
    }

    public void setAudioSpeed(double speed) {
        OnlineRequest.setAudioSpeed(speed);
    }

    public void setVolume(double volume) {
        OnlineRequest.setVolume(volume);
    }

    public void setLf0Bias(double lf0_bias) {
        OnlineRequest.setLf0Bias(lf0_bias);
    }

    public void setArousal(double arousal) {
        OnlineRequest.setArousal(arousal);
    }

    public void setUrl(String url) {
        OnlineRequest.setUrl(url);
    }
}
