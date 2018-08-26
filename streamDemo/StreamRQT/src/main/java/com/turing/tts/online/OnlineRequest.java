package com.turing.tts.online;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import android.util.Base64;

/**
 * Created by brycezou on 3/8/18.
 */

public class OnlineRequest {

    private static final String TAG = "OnlineTTS";
    private static String mTTSUrl = "http://zhiwa2-tts.tuling123.com/tts/getText";
    private static int mConnTimeout = 800;
    private static int mReadTimeout = 3000;
    private static double mSpeed = 1.0;
    private static double mLoudness = 1.0;
    private static double mLf0Bias = 1.0;
    private static double mArousal = 1.0;
    private static IPcmBatchListener mPcmBatchListener = null;
    private static boolean mbStopSynthesize = false;
    private static boolean mbStopped = false;
    private static boolean mbRunning = false;
    private static String mEncodeFmt = "base64";


    private static void sendMessageAndDataBack(int status, byte[] pcm, int text_index, int pcm_index, String text) {
        if(mPcmBatchListener != null) {
            mPcmBatchListener.onPcmArrivalCallback(status, pcm, text_index, pcm_index, text);
        }
    }

    public static void interruptRequest() {
        mbStopSynthesize = true;
        while(mbRunning) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(!mbStopped) {
            sendMessageAndDataBack(PcmBatchStatus.BATCH_FINISH, null, -1, -1, null);
            mbStopped = true;
        }
    }

    /***
     * return:
     * 1) empty list: when response code != 200
     * 2) list with at least 2 elements and the last is null: when everything is ok
     * 3) otherwise: when exception occurred during receiving data
     */
    public static LinkedList<byte[]> postRequest4PCM(String strContent, int txtIndex) {
        mbRunning = true;
        BufferedReader reader = null;
        DataOutputStream output = null;
        HttpURLConnection connection = null;
        LinkedList<byte[]> pcm_list = new LinkedList<>();
        mbStopSynthesize = false;
        mbStopped = false;

        try {
            URL tts_url = new URL(mTTSUrl);
            connection = (HttpURLConnection) tts_url.openConnection();
            connection.setConnectTimeout(mConnTimeout);
            connection.setReadTimeout(mReadTimeout);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");

            String strJson = concatRequestString(strContent);
            output = new DataOutputStream(connection.getOutputStream());
            output.write(strJson.getBytes("utf-8"));
            output.flush();
            output.close();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            // if the response failed
            if(connection.getResponseCode() != 200) {
                try {
                    reader.close();
                    reader = null;
                    connection.disconnect();
                    connection = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e(TAG+txtIndex, "response code != 200");
                sendMessageAndDataBack(PcmBatchStatus.BATCH_RESPONSE_NOT200, null, txtIndex, -1, strContent);
                mbRunning = false;
                // empty list
                return pcm_list;
            }

            String line;
            int pcmIndex = 0;
            while ((line = reader.readLine()) != null) {
                byte[] pcm = parseString2PCM(line);
                if (pcm == null) {
                    Log.e(TAG+txtIndex, "error occurred when parsing pcm: "+strContent);
                    sendMessageAndDataBack(PcmBatchStatus.BATCH_ERROR, null, txtIndex, -1, strContent);
                    break;
                } else if (pcm.length == 0) {
                    pcm_list.add(null);
                    Log.i(TAG, String.format("%03d, %s", txtIndex, "received completely"));
                    sendMessageAndDataBack(PcmBatchStatus.BATCH_FINISH, null, txtIndex, -1, strContent);
                    break;
                } else {
                    pcm_list.add(pcm);
                    sendMessageAndDataBack(PcmBatchStatus.BATCH_NORMAL, pcm, txtIndex, pcmIndex, strContent);
                }
                if (mbStopSynthesize) {
                    throw new Exception();
                }
                pcmIndex++;
            }

        } catch (Exception e) {
            if (mbStopSynthesize) {
                pcm_list.add(null);
                Log.i(TAG, String.format("%03d, %s", txtIndex, "stop synthesize"));
                sendMessageAndDataBack(PcmBatchStatus.BATCH_FINISH, null, txtIndex, -1, strContent);
                mbStopped = true;
            } else {
                Log.e(TAG + txtIndex, strContent);
                Log.e(TAG + txtIndex, e.toString());
                sendMessageAndDataBack(PcmBatchStatus.BATCH_EXCEPTION, null, txtIndex, -1, strContent);
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (output != null) {
                    output.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mbRunning = false;
        return pcm_list;
    }

    public static void setEncodeFmt(String fmt) {
        if(fmt == null) {
            mEncodeFmt = "csv";
        } else {
            mEncodeFmt = fmt;
        }
    }

    public static void setUrl(String url) {
        mTTSUrl = url;
    }

    public static void setRequestTimeout(int connTimeout, int readTimeout) {
        mConnTimeout = connTimeout;
        mReadTimeout = readTimeout;
    }

    public static void setAudioSpeed(double speed) {
        if(speed < 0.5) {
            mSpeed = 0.5;
        } else if(speed > 2.0) {
            mSpeed = 2.0;
        } else {
            mSpeed = speed;
        }
    }

    public static void setVolume(double loudness) {
        if(loudness < 0.1) {
            mLoudness = 0.1;
        } else if(loudness > 2.5) {
            mLoudness = 2.5;
        } else {
            mLoudness = loudness;
        }
    }

    public static void setLf0Bias(double lf0_bias) {
        if(lf0_bias < 0.2) {
            mLf0Bias = 0.2;
        } else if(lf0_bias > 3.0) {
            mLf0Bias = 3.0;
        } else {
            mLf0Bias = lf0_bias;
        }
    }

    public static void setArousal(double arousal) {
        if(arousal < 0.0) {
            mArousal = 0.0;
        } else if(arousal > 4.0) {
            mArousal = 4.0;
        } else {
            mArousal = arousal;
        }
    }

    public static void setPcmBatchListener(IPcmBatchListener listener) {
        mPcmBatchListener = listener;
    }

    private static String concatRequestString(String strContent) {
        String result = "{}";
        try {
            JSONObject jObj = new JSONObject();
            jObj.put("text_str", strContent);
            jObj.put("f0_bias", mLf0Bias);
            jObj.put("speed", mSpeed);
            jObj.put("loudness", mLoudness);
            jObj.put("arousal", mArousal);
            jObj.put("encode_fmt", mEncodeFmt);
            jObj.put("stream", 1);
            jObj.put("fast_mode", 1);
            result = jObj.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void putShort(byte b[], short s, int index) {
        b[index + 1] = (byte) (s >> 8);
        b[index] = (byte) (s);
    }

    private static byte[] parseString2PCM(String strJson) {
        try {
            JSONObject jObj = new JSONObject(strJson);
            int ret = jObj.getInt("ret");
            // error occurred
            if(ret < 0) {
                return null;
            }

            int index = jObj.getInt("index");
            // reach the end
            if(index == -1) {
                return new byte[0];
            }

            int len = jObj.getInt("len");
            String data = jObj.getString("data");
            // data string and its length don't match
            if(data.length() != len) {
                return null;
            }

            if(mEncodeFmt.equals("base64")) {
                return Base64.decode(data, Base64.DEFAULT);
            } else {
                // csv format string to byte array
                String[] strArray = data.split(",");
                byte[] byteArray = new byte[strArray.length * 2];
                for (int i = 0; i < strArray.length; i++) {
                    putShort(byteArray, Short.valueOf(strArray[i]), 2 * i);
                }
                return byteArray;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
