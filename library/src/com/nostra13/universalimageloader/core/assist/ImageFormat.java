package com.nostra13.universalimageloader.core.assist;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 16. 04. 14
 * Time: 16:25
 */
public enum ImageFormat
{
    JPEG("jpeg"),
    WEBP("webp"),
    PNG("png");

    private final String format;

    private ImageFormat(String format)
    {
        this.format = format;
    }

    @Override
    public String toString()
    {
        return format;
    }
}
