package com.turing.ttsdemo.zhiwa;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.turing.tts.ErrorMessage;
import com.turing.tts.online.IApplicationListener;
import com.turing.tts.online.OnlineTTS;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    /**语音合成器*/
    private OnlineTTS mTTS = OnlineTTS.getInstance();
    /**保存音频文件的路径*/
    private String mAudioPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/audio.pcm";
    /**输入待合成文本*/
    private EditText mText = null;
    /**显示当前TTS播放状态*/
    private TextView mTvInfo;
    private boolean mbSynthesisComplete = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    public void initViews() {
        mText = (EditText) findViewById(R.id.txtSynth);
        mText.setText("北京欢迎你");
        mTvInfo = (TextView) findViewById(R.id.tv_info);

        mTTS.initTTS();
        mTTS.setAppListener(new TTSListener());

        mbSynthesisComplete = false;
    }

    /**
     * 开始播放
     * @param view
     */
    public void startSpeak(View view) {
        String text = mText.getText().toString().trim();
        if(text.equals("")) {
            Toast.makeText(MainActivity.this, "空文本", Toast.LENGTH_SHORT).show();
            return;
        }
        switch(view.getId()) {
            case R.id.btnMixture:
                mTTS.synthesizeOnline(text, mAudioPath);
                mTTS.play();
                break;
            case R.id.btnTest:
                try {
                    String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/text_list.txt";
                    BufferedReader reader = new BufferedReader(new FileReader(filePath));
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        line = line.trim();
                        mbSynthesisComplete = false;
                        mTTS.synthesizeOnline(line, mAudioPath);
                        mTTS.play();
                        while (!mbSynthesisComplete) {
                            try {
                                Thread.sleep(50);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 停止播放
     * @param view
     */
    public void stopSpeak(View view){
        mTTS.stopTTS();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 更新当前tts状态信息
     * @param info
     */
    private void updateInfo(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvInfo.setText(info);
            }
        });
    }

    class TTSListener implements IApplicationListener {

        public long mStartTime = System.currentTimeMillis();

        @Override
        public void onSynthesizeStart() {
            mStartTime = System.currentTimeMillis();
            updateInfo("开始合成");
        }

        @Override
        public void onSynthesizeComplete() {
            updateInfo("合成结束");
            mbSynthesisComplete = true;
        }

        @Override
        public void onPlayStart() {
            updateInfo("开始播放");
        }

        @Override
        public void onPlayComplete() {
            updateInfo("播放结束");
        }

        @Override
        public void onFirstBatchReceived() {
            long time = System.currentTimeMillis();
            Log.e("OnlineTTS", "response time: "+(time-mStartTime));
        }

        @Override
        public void onError(ErrorMessage msg) {
            updateInfo("错误："+msg.toString());
        }

        @Override
        public void onSystemReady() {
            updateInfo("就绪");
        }
    }
}

