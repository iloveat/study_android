package com.turing.util;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by brycezou on 6/16/17.
 */

public class FileUtil {

    public static void showToast(Context ctx, String txt2show) {
        Toast.makeText(ctx, txt2show, Toast.LENGTH_SHORT).show();
    }

    public static String readTextFromUri(Context ctx, Uri uri) throws IOException {
        InputStream inputStream = ctx.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        return stringBuilder.toString();
    }

}
