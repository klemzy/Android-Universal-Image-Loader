package com.nostra13.universalimageloader.core.assist;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 15. 04. 14
 * Time: 16:49
 */
public abstract class ImageRequest
{

    private String uri;

    private DisplayImageOptions displayImageOptions = null;

    private ImageLoadingListener loadingListener = null;

    private boolean transparent;

    private int blur;

    private int imageQuality;

    private boolean recommendedApps;

    private ImageFormat imageFormat;

    public ImageRequest(String uri)
    {
        this.uri = uri;
        this.transparent = false;
        this.blur = -1;
        this.imageQuality = -1;
        this.recommendedApps = false;
        this.imageFormat = null;
    }

    public void setDisplayImageOptions(final DisplayImageOptions displayImageOptions)
    {
        this.displayImageOptions = displayImageOptions;
    }

    public void setLoadingListener(final ImageLoadingListener loadingListener)
    {
        this.loadingListener = loadingListener;
    }


    public String getUri()
    {
        return uri;
    }

    public DisplayImageOptions getDisplayImageOptions()
    {
        return displayImageOptions;
    }

    public ImageLoadingListener getLoadingListener()
    {
        return loadingListener;
    }

    public void setTransparent(final boolean transparent)
    {
        this.transparent = transparent;
    }

    public boolean isTransparent()
    {
        return transparent;
    }

    public void setBlur(final int blur)
    {
        this.blur = blur;
    }

    public int getBlur()
    {
        return blur;
    }

    public void setImageQuality(final int imageQuality)
    {
        this.imageQuality = imageQuality;
    }

    public int getImageQuality()
    {
        return imageQuality;
    }

    public void setRecommendedApps(final boolean recommendedApps)
    {
        this.recommendedApps = recommendedApps;
    }

    public boolean isRecommendedApps()
    {
        return recommendedApps;
    }

    public abstract ImageAware getImageAware();

    public void setImageFormat(final ImageFormat imageFormat)
    {
        this.imageFormat = imageFormat;
    }

    public ImageFormat getImageFormat()
    {
        return imageFormat;
    }
}
