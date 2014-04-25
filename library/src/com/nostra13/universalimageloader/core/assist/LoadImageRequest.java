package com.nostra13.universalimageloader.core.assist;

import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 25. 04. 14
 * Time: 12:16
 */
public class LoadImageRequest extends ImageRequest
{
    private NonViewAware nonViewAware;

    public LoadImageRequest(final String uri, int width, int height, ViewScaleType viewScaleType)
    {
        super(uri);
        this.nonViewAware = new NonViewAware(new ImageSize(width, height), viewScaleType);
    }

    @Override
    public ImageAware getImageAware()
    {
        return nonViewAware;
    }
}
