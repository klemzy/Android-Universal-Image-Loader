package com.nostra13.universalimageloader.core;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 23. 04. 14
 * Time: 14:28
 */
public class ImageServeInfo extends ImageLoadingInfo
{

    final Map<String, String> imageServeParams;

    private boolean download;

    public ImageServeInfo(String uri, ImageAware imageAware, ImageSize targetSize, String memoryCacheKey,
                          DisplayImageOptions options, ImageLoadingListener listener,
                          ImageLoadingProgressListener progressListener, ReentrantLock loadFromUriLock, Map<String, String> imageServeParams)
    {
        super(uri, imageAware, targetSize, memoryCacheKey, options, listener, progressListener, loadFromUriLock);
        this.imageServeParams = imageServeParams;
        this.download = true;
    }

    public void setDownload(final boolean download)
    {
        this.download = download;
    }

    public boolean isDownload()
    {
        return download;
    }
}
