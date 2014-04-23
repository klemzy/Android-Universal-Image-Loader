package com.nostra13.universalimageloader.utils;

import android.os.Build;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 16. 04. 14
 * Time: 10:54
 */
public class ImageServeUtil
{

    static int apiVersion = Build.VERSION.SDK_INT;


    public static String getProcessedImageUri(String uri, int width, int height, boolean trasparent, ViewScaleType viewScaleType)
    {
        // Check if the path is as expected
        if (!(uri.contains("blob-key") && uri.contains("imageserve") && uri.contains("http")))
        {
            return uri;
        }

        URI currentUri = null;
        StringBuilder outUri = new StringBuilder();

        try
        {
            currentUri = new URI(uri);

            String authority = currentUri.getAuthority();
            String path = currentUri.getPath();

            outUri.append("http://");
            outUri.append(authority);
            outUri.append(path);

            boolean isFirst = true;
            for (NameValuePair param : URLEncodedUtils.parse(currentUri, "UTF-8"))
            {
                // Ignore fit, fill, thumb and out parameters
                if (param.getName().equals("fit") || param.getName().equals("fill") || param.getName().equals("out") || param.getName().equals("thumb"))
                {
                    continue;
                }

                outUri.append(isFirst ? "?" : "&");
                isFirst = false;
                outUri.append(URLEncoder.encode(param.getName(), "UTF-8"));
                outUri.append("=");
                outUri.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }

            if (viewScaleType != null && width > 0 && height > 0)
            {
                String paramName = null;
                String paramValue = null;
                if (viewScaleType == ViewScaleType.CROP)
                {
                    //If crop then get fill picture and take smaller edge
                    paramName = "&fill=";
                    paramValue = String.valueOf(width > height ? height : width);
                }
                else if(viewScaleType == ViewScaleType.FIT_INSIDE)
                {
                    //If fill then get fit picture and take larger edge
                    paramName = "&fit=";
                    paramValue = String.valueOf(width > height ? width : height);
                }

                outUri.append(paramName);
                outUri.append(paramValue);
            }

            outUri.append("&out=");
            String format = getImageFormat(trasparent, apiVersion);
            outUri.append(format);
        }
        catch (URISyntaxException e)
        {
            return uri;
        }
        catch (UnsupportedEncodingException e)
        {
            return uri;
        }

        return outUri.toString();
    }

    private static String getImageFormat(boolean transparent, int versionSDK)
    {
        String format;
        if (transparent)
        {
            // Transparent WebP support has appeared in Android 4.2.1+
            if (versionSDK >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                format = "webp";
            }
            else
            {
                format = "png";
            }
        }
        else
        {
            // Non-transparent WebP support has appeared in Android 4.0+
            if (versionSDK >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            {
                format = "webp";
            }
            else
            {
                format = "jpeg";
            }
        }

        return format;
    }

}
