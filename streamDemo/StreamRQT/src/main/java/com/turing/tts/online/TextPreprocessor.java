package com.turing.tts.online;

import java.util.LinkedList;

/**
 * Created by brycezou on 3/8/18.
 */

public class TextPreprocessor {

    private static final int mSERVER_MAX_SENTENCE_LENGTH = 38;
    private static final String mSeparator = "，。！？、；：…,!?;:";

    public static LinkedList<String> splitText(String text) {
        LinkedList<String> text_queue = new LinkedList<>();
        text = text.trim().replace('\n', ',').replace('\"', ',').replace('\'', ',').replace('．', ',').concat("。");
        int patch_idx = 0;
        while(!text.equals("")) {

            String strPatch;
            if(patch_idx == 0) {
                strPatch =text.substring(0, Math.min(text.length(), 25));
            } else {
                strPatch =text.substring(0, Math.min(text.length(), mSERVER_MAX_SENTENCE_LENGTH));
            }
            patch_idx++;

            int max_idx = -1;
            for(int i = 0; i < mSeparator.length(); i++) {
                int idx = strPatch.lastIndexOf(mSeparator.charAt(i));
                if(idx > max_idx) {
                    max_idx = idx;
                }
            }
            if(max_idx != -1) {
                if(max_idx > 5) {
                    strPatch = text.substring(0, max_idx + 1);
                }
            }

            boolean bLegal = false;
            for(int k = 0; k < strPatch.length(); k++) {
                char c2find = strPatch.charAt(k);
                boolean bFind = false;
                for(int g = 0; g < mSeparator.length(); g++) {
                    if(c2find == mSeparator.charAt(g)) {
                        bFind = true;
                        break;
                    }
                }
                if(!bFind) {
                    bLegal = true;
                    break;
                }
            }

            if(bLegal) {
                text_queue.add(strPatch);
            }

            text = text.substring(strPatch.length());
        }

        try {
            if (text_queue.size() > 1) {
                int last_idx = text_queue.size() - 1;
                String str_m1 = text_queue.get(last_idx);
                String str_m2 = text_queue.get(last_idx - 1);
                if (str_m1.length() < 5) {
                    text_queue.removeLast();
                    text_queue.removeLast();
                    text_queue.add(str_m2 + str_m1);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return text_queue;
    }
}
