package com.turing.tts;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;

/**
 * Created by brycezou on 6/8/17.
 */

public class TuringTTS {

    private static final String TAG = "TuringTTS-JAR";
    // TTS native 方法可输入文本的最大长度
    private static final int mMAX_SENTENCE_LENGTH = 50;
    // TuringTTS类的单例
    private static volatile TuringTTS mInstance = null;
    // 待合成文本队列
    private static LinkedList<String> mTxtQueue = null;
    // 待播放音频队列
    private static LinkedList<byte[]> mAudioQueue = null;
    // 在线TTS,待合成的参数队列
    private static LinkedList<byte[]> mParamQueue = null;
    // 是否边合成边播放,否的话只保存PCM格式的音频文件而不播放
    private static boolean mbSpeakOut = true;
    // 是否运行音频播放线程
    private static boolean mbRunPlayThread = true;
    // 是否停止合成
    private static boolean mbStopSynthesize = false;
    // 音频播放器
    private static AudioTrack mAudioTrack = null;
    // 保存音频文件的路径
    private static String mAudioFilePath = "";
    // 语音合成完成的事件监听器
    private static ISynthesizeFinishListener mListener;
    // TTS当前状态
    // -1: 未初始化
    //  0: 空闲
    //  1: 正在合成
    private static int mStatus = -1;


    public interface ISynthesizeFinishListener {
        void onSynthesizeFinish();
    }

    public static void setListener(ISynthesizeFinishListener listener) {
        mListener = listener;
    }

    private TuringTTS() {
        // 设置TTS状态为未初始化
        mStatus = -1;
    }

    public static TuringTTS getInstance() {
        if(mInstance == null) {
            synchronized (TuringTTS.class) {
                if(mInstance == null) {
                    mInstance = new TuringTTS();
                }
            }
        }
        return mInstance;
    }

    public boolean initTTS(boolean ifspeak) {
        // 设置TTS状态为未初始化
        mStatus = -1;
        // 初始化离线TTS
        boolean init_ret = (jniInit() > 0);

        // 如果初始化成功
        if(init_ret) {
            // 是否边合成边播放
            mbSpeakOut = ifspeak;
            // 初始化(离线)待合成文本缓冲区
            if(mTxtQueue == null) {
                mTxtQueue = new LinkedList<>();
            }
            mTxtQueue.clear();

            //初始化(在线)待合成参数缓冲区
            if(mParamQueue == null) {
                mParamQueue = new LinkedList<>();
            }
            mParamQueue.clear();

            // 如果边合成边播放(通用)
            if(mbSpeakOut) {
                // 初始化音频数据缓冲区
                if(mAudioQueue == null) {
                    mAudioQueue = new LinkedList<>();
                }
                mAudioQueue.clear();

                // 初始化音频播放器
                if(mAudioTrack == null) {
                    final int SAMPLE_RATE = 16000;
                    int MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER_SIZE * 2, AudioTrack.MODE_STREAM);
                    //mAudioTrack.play();
                }
            }

            // 设置TTS状态为空闲
            mStatus = 0;
        }
        return init_ret;
    }

    public boolean freeTTS() {
        // 停止合成和播放线程
        stopTTS();
        // 清空(离线)待合成文本缓冲区
        if(mTxtQueue != null) {
            mTxtQueue.clear();
            mTxtQueue = null;
        }
        // 清空(在线)待合成参数缓冲区
        if(mParamQueue != null) {
            mParamQueue.clear();
            mParamQueue = null;
        }
        // 如果边合成边播放(通用)
        if(mbSpeakOut) {
            // 清空音频数据缓冲区
            if(mAudioQueue != null) {
                mAudioQueue.clear();
                mAudioQueue = null;
            }
            // 释放音频播放器
            if(mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            }
        }
        // 设置TTS状态为未初始化
        mStatus = -1;
        // 释放离线TTS申请的空间
        return jniFree();
    }

    public void stopTTS() {
        // 停止合成
        mbStopSynthesize = true;
        // 停止播放
        mbRunPlayThread = false;
        // 打断离线TTS
        jniStop();
        // 等待合成器完成收尾工作
        try {
            Thread.sleep(800);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 设置TTS状态为空闲
        mStatus = 0;
    }

    // 开启离线TTS
    public void synthesizeAudioOffline(String text, String filePath) {
        if(mStatus != 0) {
            Log.e(TAG, "未初始化或正在合成");
            return;
        }
        // 设置TTS状态为正在合成
        mStatus = 1;

        mAudioFilePath = filePath;
        if(text == null || text.trim().equals("")) {
            Log.e(TAG, "输入文本为空");
            // 设置TTS状态为空闲
            mStatus = 0;
            return;
        }

        // 如果边合成边播放,则先清空音频缓冲区
        if(mbSpeakOut) {
            if(mAudioQueue != null) {
                mAudioQueue.clear();
            }
        }

        // 分割文本并加入待合成队列
        splitText(text);

        // 创建音频文件
        try {
            FileOutputStream fout = new FileOutputStream(mAudioFilePath);
            fout.close();
        } catch (Exception e) {
            Log.e(TAG, "创建文件失败: "+mAudioFilePath);
            e.printStackTrace();
            // 设置TTS状态为空闲
            mStatus = 0;
            return;
        }

        // 开启语音合成线程
        mbStopSynthesize = false;
        doTTSInNewThreadOffline();

        // 如果边合成边播放
        if(mbSpeakOut) {
            // 延迟开启播放线程
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mbRunPlayThread = true;
            playAudioThread();
        }
    }

    // 离线语音合成主线程
    private void doTTSInNewThreadOffline() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 设置TTS状态为正在合成
                mStatus = 1;
                // 如果待合成队列不为空,且用户没有停止合成
                Log.d(TAG, "synthesize start");
                while(!mbStopSynthesize && mTxtQueue != null && !mTxtQueue.isEmpty()) {
                    // 如果音频缓冲还不多,就继续合成;否则延时等待.
                    // 如果不等待,缓冲队列就会无限增长,直至OOM
                    if(mAudioQueue != null && mAudioQueue.size() < 3000) {
                        String text = mTxtQueue.removeFirst();
                        try {
                            jniSynthAudio(text, 1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(!mbSpeakOut) {
                    mListener.onSynthesizeFinish();
                    // 设置TTS状态为空闲
                    mStatus = 0;
                }
                Log.d(TAG, "synthesize finish");
            }
        }).start();
    }

    // 音频播放线程,边合成边播放时用
    private void playAudioThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int nEmpty = 0;
                Log.d(TAG, "play start");
                while(mbRunPlayThread && !mbStopSynthesize) {
                    // 如果播放队列有数据则准备播放;否则休息50毫秒
                    if(mAudioQueue != null && !mAudioQueue.isEmpty()) {
                        byte[] wav_buff = mAudioQueue.removeFirst();
                        mAudioTrack.play();
                        mAudioTrack.write(wav_buff, 0, wav_buff.length);
                        mAudioTrack.stop();
                        nEmpty = 0;
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        nEmpty++;
                        // 如果连续超过2000毫秒都无数据可播放，认为合成已结束
                        if(nEmpty > 40) {
                            mbRunPlayThread = false;
                        }
                    }
                }  // while end
                mListener.onSynthesizeFinish();
                // 设置TTS状态为空闲
                mStatus = 0;
                Log.d(TAG, "play finish");
            } // run end
        }).start();
    }

    // 分割文本并加入待合成队列(离线)
    private void splitText(String text) {
        if(text == null || text.trim().equals("")) {
            Log.e(TAG, "输入文本为空");
            return;
        }
        if(mTxtQueue != null) {
            mTxtQueue.clear();
        }
        while(!text.equals("")) {
            String strPatch = text.substring(0, Math.min(text.length(), mMAX_SENTENCE_LENGTH));
            mTxtQueue.add(strPatch);
            text = text.substring(strPatch.length());
        }
    }

    // 获取TTS当前状态
    public int getStatus() {
        return mStatus;
    }

    // 音频数据就绪回调方法
    private static void onAudioReadyCallback(byte[] buff, int size) {

        // 如果边合成边播放
        if(mbSpeakOut) {
            // 如果用户没有停止合成
            if(!mbStopSynthesize) {
                if(mAudioQueue != null) {
                    mAudioQueue.add(buff);
                }
            }
        }

        // 创建音频文件
        try {
            FileOutputStream fout = new FileOutputStream(mAudioFilePath, true);
            fout.write(buff);
            fout.close();
        } catch (Exception e) {
            Log.e(TAG, "写入文件失败: "+mAudioFilePath);
            e.printStackTrace();
        }
    }

    private native static int jniInit();
    private native static int jniInit2(Context ctx, String apikey, String keysec);
    private native static boolean jniFree();
    private native static boolean jniSynthAudio(String text, int syn_status);
    private native static void jniStop();
    private native static void jniSynthFromParams(byte [] params, int length);

    public void synthFromParams(byte [] params, int length) {
        jniSynthFromParams(params, length);
    }

    static {
        try {
            System.loadLibrary("turingtts");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "turing tts library not found.");
        }
    }

    public boolean initTTS2(boolean ifspeak, Context ctx, String apikey, String keysec) {
        // 设置TTS状态为未初始化
        mStatus = -1;
        // 初始化离线TTS
        boolean init_ret = (jniInit2(ctx, apikey, keysec) > 0);

        // 如果初始化成功
        if(init_ret) {
            // 是否边合成边播放
            mbSpeakOut = ifspeak;
            // 初始化(离线)待合成文本缓冲区
            if(mTxtQueue == null) {
                mTxtQueue = new LinkedList<>();
            }
            mTxtQueue.clear();

            //初始化(在线)待合成参数缓冲区
            if(mParamQueue == null) {
                mParamQueue = new LinkedList<>();
            }
            mParamQueue.clear();

            // 如果边合成边播放(通用)
            if(mbSpeakOut) {
                // 初始化音频数据缓冲区
                if(mAudioQueue == null) {
                    mAudioQueue = new LinkedList<>();
                }
                mAudioQueue.clear();

                // 初始化音频播放器
                if(mAudioTrack == null) {
                    final int SAMPLE_RATE = 16000;
                    int MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER_SIZE * 2, AudioTrack.MODE_STREAM);
                    //mAudioTrack.play();
                }
            }

            // 设置TTS状态为空闲
            mStatus = 0;
        }
        return init_ret;
    }
}
