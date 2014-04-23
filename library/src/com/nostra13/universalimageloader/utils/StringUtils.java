package com.nostra13.universalimageloader.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 17. 04. 14
 * Time: 11:55
 */
public class StringUtils
{


    public static String sha256(String text)
    {
        try
        {
            return hash(text.getBytes("UTF-8"), "SHA-256");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("The device lacks UTF-8 support!", e);
        }
    }

    private static String hash(byte[] data, String algorithm)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.reset();
            digest.update(data);
            byte[] sha256bytes = digest.digest();
            return toHex(sha256bytes);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("This device has no SHA-256 algorithm available!");
        }
    }

    public static String toHex(byte[] bytes)
    {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format(Locale.US, "%0" + (bytes.length << 1) + "X", bi).toLowerCase(Locale.US);
    }
}