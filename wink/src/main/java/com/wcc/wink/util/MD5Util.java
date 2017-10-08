package com.wcc.wink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
    public static String digest(String rawString) {
        try {
            return compute(new String(rawString.getBytes("UTF8"), "latin1"));
        } catch (Exception e) {
            return "";
        }
    }

    public static String digest(Object... objs) {
        if (objs == null || objs.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("");
        for (Object param : objs) {
            if (param != null) {
                if ((param instanceof String
                        || param instanceof Integer
                        || param instanceof Long
                        || param instanceof Short
                        || param instanceof Byte
                        || param instanceof Float
                        || param instanceof Double
                        || param instanceof Character
                        || param instanceof Boolean)) {
                    sb.append(param);
                } else {
                    sb.append(param.toString());
                }
            }
        }

        return digest(sb.toString());
    }

    /**
     * Computes the MD5 fingerprint of a string.
     *
     * @param str 字符串
     * @return the MD5 digest of the input <code>String</code>
     */
    private static String compute(String str) throws Exception {
        // convert input String to a char[]
        // convert that char[] to byte[]
        // get the md5 digest as byte[]
        // bit-wise AND that byte[] with 0xff
        // prepend "0" to the output StringBuffer to make sure that we don't end
        // up with
        // something like "e21ff" instead of "e201ff"
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);

        return toHexString(md5Bytes);
    }

    public static byte[] encode16(String origin, String enc)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        if (origin == null || origin.length() == 0)
        {
            return null;
        }

        MessageDigest md = MessageDigest.getInstance("MD5");
        if (md == null)
        {
            throw new IllegalAccessError("no md5 algorithm");
        }

        byte[] bytes = md.digest(origin.getBytes(enc));
        byte[] dstBytes = new byte[8];
        System.arraycopy(bytes, 4, dstBytes, 0, 8);
        bytes = null;
        return dstBytes;
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        StringBuilder hexValue = new StringBuilder();
        for (byte b : bytes) {
            int val = ((int) b) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    /**
     * 获取单个本地文件的MD5值！
     *
     * @param path 本地文件路径
     * @return 返回文件的MD5值
     */
    public static String getFileMD5(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return null;
        }

        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return toHexString(digest.digest());
    }
}
