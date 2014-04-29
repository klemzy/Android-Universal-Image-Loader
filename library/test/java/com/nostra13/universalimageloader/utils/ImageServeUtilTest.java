package com.nostra13.universalimageloader.utils;

import android.os.Build;
import com.nostra13.universalimageloader.core.assist.ImageFormat;
import com.nostra13.universalimageloader.core.assist.ImageServeParams;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 28. 04. 14
 * Time: 13:50
 */
@RunWith(RobolectricTestRunner.class)
public class ImageServeUtilTest
{

    private static final String imageUri = "http://images.backend.iddiction.com/imageserve?v=201404141704&out=webp&q=70&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&thumb=114";

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

    @Test
    public void testProcessedImageUri()
    {
        String uri = "lol";
        String processedUri = ImageServeUtil.getProcessedImageUri(uri, -1, -1, false, null, null);
        Assert.assertTrue(processedUri.equals(uri));

        //Image format will be calculated based Android version (Robolectric settings)
        uri = imageUri;
        processedUri = ImageServeUtil.getProcessedImageUri(uri, -1, -1, false, null, null);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&q=70&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&thumb=114&out=webp"));

        //Image format set explicitly
        uri = imageUri;
        ImageServeParams params = new ImageServeParams();
        params.setImageFormat(ImageFormat.JPEG);
        processedUri = ImageServeUtil.getProcessedImageUri(uri, -1, -1, false, params, null);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&q=70&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&thumb=114&out=jpeg"));


        //ViewScaleType, width and height have to be set to append fit/fill, otherwise original params are preserved
        uri = imageUri;
        processedUri = ImageServeUtil.getProcessedImageUri(uri, 50, -1, false, null, ViewScaleType.FIT_INSIDE);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&q=70&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&thumb=114&out=webp"));

        //Image has to fit inside area so take larger edge
        uri = imageUri;
        processedUri = ImageServeUtil.getProcessedImageUri(uri, 150, 50, false, null, ViewScaleType.FIT_INSIDE);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&q=70&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&fit=150&out=webp"));

        //Image has to fill whole area so take smaller edge
        uri = imageUri;
        processedUri = ImageServeUtil.getProcessedImageUri(uri, 150, 45, false, null, ViewScaleType.CROP);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&q=70&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&fill=45&out=webp"));

        //If q parameter is missing q=100 is added
        uri = "http://images.backend.iddiction.com/imageserve?v=201404141704&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&thumb=114&out=webp";
        processedUri = ImageServeUtil.getProcessedImageUri(uri, -1, -1, false, null, null);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&q=100&thumb=114&out=webp"));


        uri = "http://images.backend.iddiction.com/imageserve?v=201404141704&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&thumb=114&out=webp&blur=50&q=20&recapp=false";
        params = new ImageServeParams();
        params.setBlur(30);
        params.setQuality(90);
        params.setRecApp(true);
        processedUri = ImageServeUtil.getProcessedImageUri(uri, -1, -1, false, params, null);
        Assert.assertTrue(processedUri.equals("http://images.backend.iddiction.com/imageserve?v=201404141704&blob-key=AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA&blur=30&q=90&recapp=true&thumb=114&out=webp"));
    }

    @Test
    public void testParseImageServeParams()
    {
        Map<String, String> paramsMap = ImageServeUtil.parseImageServeParams(imageUri);

        Assert.assertTrue(paramsMap.containsKey("key"));
        Assert.assertTrue(paramsMap.get("key").equals("AMIfv940OG-zgQtYE4nQAXMKAp7SvKDK8Q3ZTSDEiE5J_e6Ia05MPAy_thkQ5v0OdiQ7fNFhwRJ4xw9o4zJDcRMZv0EkD1z-r3nmqhLDdAMBgNdukofl31tzRKlbVWx8Drlij4ZVwRNKmSp_akQ1OsblYm9lf20UQA"));

        Assert.assertTrue(paramsMap.containsKey("v"));
        Assert.assertTrue(paramsMap.get("v").equals("201404141704"));

        Assert.assertTrue(paramsMap.containsKey("out"));
        Assert.assertTrue(paramsMap.get("out").equals("webp"));

        Assert.assertTrue(paramsMap.containsKey("q"));
        Assert.assertTrue(paramsMap.get("q").equals("70"));

        Assert.assertTrue(paramsMap.containsKey("thumb"));
        Assert.assertTrue(paramsMap.get("thumb").equals("114"));

        paramsMap = ImageServeUtil.parseImageServeParams("http://images.backend.iddiction.com/imageserve");
        Assert.assertTrue(paramsMap.size() == 0);

        paramsMap = ImageServeUtil.parseImageServeParams("http://images.backend.iddiction.com/imageserve?");
        Assert.assertTrue(paramsMap.size() == 0);

        paramsMap = ImageServeUtil.parseImageServeParams("http://images.backend.iddiction.com/imageserve?q=&blur=20");
        Assert.assertTrue(paramsMap.size() == 1);
    }
}
