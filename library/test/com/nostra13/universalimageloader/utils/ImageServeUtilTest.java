package com.nostra13.universalimageloader.utils;

import android.os.Build;
import com.nostra13.universalimageloader.core.assist.ImageFormat;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 28. 04. 14
 * Time: 13:50
 */
@RunWith(RobolectricTestRunner.class)
public class ImageServeUtilTest
{

    @Test
    public void testImageFormat()
    {
        String format = ImageServeUtil.getImageFormat(true, Build.VERSION_CODES.JELLY_BEAN_MR1);
        Assert.assertEquals(format, ImageFormat.WEBP.toString());

        format = ImageServeUtil.getImageFormat(true, Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        Assert.assertEquals(format, ImageFormat.PNG.toString());

        format = ImageServeUtil.getImageFormat(false, Build.VERSION_CODES.JELLY_BEAN_MR1);
        Assert.assertEquals(format, ImageFormat.WEBP.toString());

        format = ImageServeUtil.getImageFormat(false, Build.VERSION_CODES.GINGERBREAD_MR1);
        Assert.assertEquals(format, ImageFormat.JPEG.toString());
    }
}
