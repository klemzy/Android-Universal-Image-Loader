package com.nostra13.universalimageloader.core.assist;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 15. 04. 14
 * Time: 16:14
 */

/**
 * User definable image serve parameters
 */
public class ImageServeParams
{
    private int blur = -1;

    private int quality = -1;

    private boolean recApp = false;

    private ImageFormat imageFormat = null;


    public int getBlur()
    {
        return blur;
    }

    public void setBlur(final int blur)
    {
        this.blur = blur;
    }

    public boolean isRecApp()
    {
        return recApp;
    }

    public void setRecApp(final boolean recApp)
    {
        this.recApp = recApp;
    }

    public void setQuality(final int quality)
    {
        this.quality = quality;
    }

    public int getQuality()
    {
        return quality;
    }

    public void setImageFormat(final ImageFormat imageFormat)
    {
        this.imageFormat = imageFormat;
    }

    public ImageFormat getImageFormat()
    {
        return imageFormat;
    }
}
