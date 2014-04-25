package com.nostra13.universalimageloader.core.assist;

import android.widget.ImageView;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.imageaware.ViewAware;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 25. 04. 14
 * Time: 12:12
 */
public class DisplayImageRequest extends ImageRequest
{

    private ViewAware viewAware;

    public DisplayImageRequest(final String uri, final ImageView imageView)
    {
        super(uri);
        this.viewAware = new ImageViewAware(imageView);
    }

    @Override
    public ImageAware getImageAware()
    {
        return viewAware;
    }
}
