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

    private ImageServeParams params;

    private boolean transparent;


    public ImageRequest(String uri)
    {
        this.uri = uri;
        this.transparent = false;
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

    public void setParams(final ImageServeParams params)
    {
        this.params = params;
    }

    public ImageServeParams getParams()
    {
        return params;
    }

    public abstract ImageAware getImageAware();
}
