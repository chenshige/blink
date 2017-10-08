package com.blink.browser.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5转换类
 *
 */
public class MD5Utils {
    /**
     * 使用md5的算法进行加密
     */
    public static String md5(String plainText) {
        byte[] secretBytes = getMD5(plainText.getBytes()) ;
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
    }

    public static byte[] getMD5(byte[] data) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            hash = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }

}
