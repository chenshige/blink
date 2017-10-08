package com.blink.browser.util;

import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 基于AES和RSA的加密解密器，使用AES首先通过构造函数传入一个随机密钥，然后调用encrypt和decrypt可以加密解密
 * 使用RSA加密直接使用静态方法encrytpByRsa，不能解密
 * <p/>
 */
public class EncryptorUtil {
    private static final int KEY_LENGTH = 128;
    private static final int ITERATIONS = 17;
    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String TRANSFORMATION = "AES/OFB/NoPadding";
    private static final byte[] DEFAULT_SALT = {(byte) 0xD9, (byte) 0xAB, (byte) 0xD8, (byte) 0x42, (byte) 0x66, (byte) 0x45, (byte) 0xF3, (byte) 0x13};
    private static final byte[] DEFAULT_IV = {(byte) 0xB9, (byte) 0xA0, (byte) 0xE8, (byte) 0x62, (byte) 0x96, (byte) 0xA5, (byte) 0x13, (byte) 0xF3};

    /**
     * 生成的秘钥
     */
    private final SecretKey secretKey;
    /**
     * 初始化向量
     */
    private final IvParameterSpec iv;

    /**
     * 使用默认的salt{@link #DEFAULT_SALT}构建AES加密实例
     *
     * @param passPhrase 密码串，不可以为null或者空字符串
     */
    public EncryptorUtil(String passPhrase) {
        this(passPhrase, DEFAULT_SALT);
    }

    /**
     * @param passPhrase 密码串，不可以为null或者空字符串
     * @param salt       指定的干扰盐
     */
    public EncryptorUtil(String passPhrase, byte[] salt) {
        this.secretKey = genKey(passPhrase, salt);
        byte[] ivb = new byte[16];
        for (int i = 0; i < ivb.length; i++) {
            ivb[i] = DEFAULT_IV[i % DEFAULT_IV.length];
        }
        iv = new IvParameterSpec(ivb);

    }


    /**
     * 加密
     *
     * @param data 待加密内容
     */
    public byte[] encrypt(byte[] data) {
        try {
            SecretKeySpec key = cloneSecretKeySpec();
            Cipher ecipher = Cipher.getInstance(TRANSFORMATION);
            ecipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] enc = ecipher.doFinal(data);
            return enc;
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     *
     * @param str 待解密内容
     */
    public String decrypt(String str) {
        try {
            SecretKeySpec key = cloneSecretKeySpec();
            Cipher dcipher = Cipher.getInstance(TRANSFORMATION);
            dcipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] dec = Base64.decode(str, Base64.DEFAULT);
            byte[] utf8 = dcipher.doFinal(dec);
            return new String(utf8, "UTF8");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param passPhrase
     * @param salt
     * @return
     */
    private SecretKey genKey(String passPhrase, byte[] salt) {
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(passPhrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKey tmp = secretKeyFactory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 由于不清楚this.secretKey是否线程安全,这里clone一个出来
     */
    private SecretKeySpec cloneSecretKeySpec() {
        return new SecretKeySpec(this.secretKey.getEncoded(), this.secretKey.getAlgorithm());
    }


    //RSA public key after base 64
    private static final String RSA_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLZHQn5vl6TRT55OdRHQTi+qKdjP/wH3K9Dr8kPefdin1Yx+tyhhtSaJrOQLkV6Ky8s/2ctJaFQJGcgCwkOmUfDBNrAhXB0/7LJyBY6jUPb7ygbKc8wP27g4BZFCtK105svTIoOhlcJBahfPKcTbqZaSnhF2tVL2xtep0U4V/wOwIDAQAB";

    //RSA
    static PublicKey mPublicKey = null;

    private static boolean loadKey() {
        try {
            //The public key in base64
            byte[] buffer = Base64.decode(RSA_PUBLIC_KEY, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            mPublicKey = keyFactory.generatePublic(keySpec);
            return true;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] encryptByRsa(byte[] data) {
        if (null == mPublicKey) {
            loadKey();
        }

        if (null == mPublicKey) {
            throw new IllegalStateException();
        }

        try {
            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, mPublicKey);
            byte[] enKey = rsaCipher.doFinal(data);
            return enKey;
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
