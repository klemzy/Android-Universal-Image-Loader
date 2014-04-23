package com.nostra13.universalimageloader.core.multipart;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 22. 04. 14
 * Time: 13:59
 */
public class BodyPart
{

    private InternetHeaders headers;

    private byte[] data;

    public BodyPart(byte[] data)
    {
        this.data = data;
    }

    public BodyPart(InternetHeaders headers, byte[] data)
    {
        this.headers = headers;
        this.data = data;
    }

    public byte[] getData()
    {
        return data;
    }

    public InternetHeaders getHeaders()
    {
        return headers;
    }
}
