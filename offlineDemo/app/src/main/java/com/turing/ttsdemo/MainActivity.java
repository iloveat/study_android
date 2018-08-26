package com.turing.ttsdemo;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.turing.tts.TuringTTS;
import com.turing.util.FileUtil;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private TuringTTS mTTS = TuringTTS.getInstance();
    private boolean mbInitSuccessful = false;
    private AudioTrack mAudioTrack = null;

    private EditText mTxt2Synthesize = null;
    private String mAudioPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/audio.pcm";
    private String mTotalText = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    public void initViews() {
        mTxt2Synthesize = (EditText) findViewById(R.id.txtSynth);
        mTxt2Synthesize.setText("北京欢迎你");
        mbInitSuccessful = true;
        if(!mTTS.initTTS(true)) {
            FileUtil.showToast(MainActivity.this, "TTS初始化失败");
            mTxt2Synthesize.setText("");
            mTxt2Synthesize.setEnabled(false);
            mbInitSuccessful = false;
        } else {
            // 语音合成结束后运行
            TuringTTS.ISynthesizeFinishListener listener = new TuringTTS.ISynthesizeFinishListener() {
                @Override
                public void onSynthesizeFinish() {
                    Message msg = Message.obtain();
                    msg.what = 101;
                    mHandler.sendMessage(msg);
                }
            };
            TuringTTS.setListener(listener);
        }
        final int SAMPLE_RATE = 16000;
        int MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER_SIZE * 2, AudioTrack.MODE_STREAM);
/*
        try {
            FileInputStream fin = new FileInputStream("/sdcard/16K_60.params");
            DataInputStream din = new DataInputStream(fin);
            int file_size = fin.available();
            byte param[] = new byte[file_size];
            din.read(param);
            mTTS.synthFromParams(param, param.length);
        } catch (Exception e) {

        }
*/
    }

    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.btnOffline:
            case R.id.btnStop:
                mTotalText = mTxt2Synthesize.getText().toString().trim();
                if(mTotalText.equals("")) {
                    FileUtil.showToast(MainActivity.this, "空文本");
                    return;
                }

                if(view.getId() == R.id.btnOffline) {
                    mTTS.synthesizeAudioOffline(mTotalText, mAudioPath);
                } else if(view.getId() == R.id.btnStop) {
                    mTTS.stopTTS();
                }
                break;
            case R.id.btnLoad:
                showTextFileChooser();
                break;
            case R.id.btnReplay:
                FileUtil.showToast(MainActivity.this, "正在播放...");
                playAudioFile(mAudioPath);
                break;
            default:
                break;
        }
    }

    public void playAudioFile(final String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileInputStream fin;
                byte [] buffer;
                try {
                    fin = new FileInputStream(filePath);
                    buffer = new byte[fin.available()];
                    fin.read(buffer);
                    fin.close();
                } catch (Exception e) {
                    FileUtil.showToast(MainActivity.this, "读取音频文件 "+filePath+" 失败");
                    e.printStackTrace();
                    return;
                }
                if(mAudioTrack != null && buffer != null) {
                    mAudioTrack.play();
                    mAudioTrack.write(buffer, 0, buffer.length);
                }
            }
        }).start();
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 101) {
                FileUtil.showToast(MainActivity.this, "完成");
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 202:
                if(resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        mTotalText = FileUtil.readTextFromUri(MainActivity.this, uri);
                    } catch (IOException e) {
                        mTotalText = "";
                        FileUtil.showToast(MainActivity.this, "读文件失败");
                    }
                    mTxt2Synthesize.setText(mTotalText);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showTextFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "请选择文本文件"), 202);
        } catch (android.content.ActivityNotFoundException ex) {
            FileUtil.showToast(MainActivity.this, "请安装文件管理器");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mbInitSuccessful) {
            mTTS.freeTTS();
        }
        if(mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

}
