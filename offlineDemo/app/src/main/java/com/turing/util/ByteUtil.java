package com.turing.util;

/**
 * Created by brycezou on 7/18/17.
 */

public class ByteUtil {

    public static float bytes4ToFloat32(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

    public static double bytes8ToDouble64(byte[] b, int index) {
        long l;
        l = b[index+0];
        l &= 0xff;
        l |= ((long) b[index+1] << 8);
        l &= 0xffff;
        l |= ((long) b[index+2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index+3] << 24);
        l &= 0xffffffffl;
        l |= ((long) b[index+4] << 32);
        l &= 0xffffffffffl;
        l |= ((long) b[index+5] << 40);
        l &= 0xffffffffffffl;
        l |= ((long) b[index+6] << 48);
        l &= 0xffffffffffffffl;
        l |= ((long) b[index+7] << 56);
        return Double.longBitsToDouble(l);
    }

    public static byte[] float32ToBytes4(float x) {
        byte[] bb = new byte[4];
        int l = Float.floatToIntBits(x);
        for (int i = 0; i < 4; i++) {
            bb[i] = new Integer(l).byteValue();
            l = l >> 8;
        }
        return bb;
    }

    public static String string2Unicode(String str) {
        StringBuffer unicode = new StringBuffer();
        for(int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            unicode.append("\\u" + Integer.toHexString(c));
        }
        return unicode.toString();
    }
}
