package com.nostra13.universalimageloader.utils;

import android.os.Build;
import com.nostra13.universalimageloader.core.assist.ImageFormat;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 16. 04. 14
 * Time: 10:54
 */
public class ImageServeUtil
{

    static int apiVersion = Build.VERSION.SDK_INT;


    public static String getProcessedImageUri(String uri, int width, int height, boolean trasparent, int blur, int quality, boolean recommendedApp, ImageFormat imageFormat, ViewScaleType viewScaleType)
    {
        // Check if the path is as expected
        if (!(uri.contains("blob-key") && uri.contains("imageserve") && uri.contains("http"))) return uri;

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
            boolean sizeParamsHandled = false;

            String delimiter = isFirst ? "?" : "&";

            //If no scale type of width/height has been requested keep original params
            if (viewScaleType != null && width > 0 && height > 0)
            {
                sizeParamsHandled = true;
                isFirst = false;

                if (viewScaleType == ViewScaleType.CROP)
                {
                    //If crop then get fill picture and take smaller edge
                    String paramName = "fill";
                    String paramValue = String.valueOf(width > height ? height : width);
                    addParamToUrl(outUri, paramName, paramValue, delimiter);
                }
                else if (viewScaleType == ViewScaleType.FIT_INSIDE)
                {
                    //If fill then get fit picture and take larger edge
                    String paramName = "fit";
                    String paramValue = String.valueOf(width > height ? width : height);
                    addParamToUrl(outUri, paramName, paramValue, delimiter);
                }
            }

            boolean hasQuality = false;
            boolean hasBlur = false;

            if (blur != -1)
            {
                delimiter = isFirst ? "?" : "&";
                addParamToUrl(outUri, "blur", String.valueOf(blur), delimiter);
                hasBlur = true;
                isFirst = false;
            }

            if (quality != -1)
            {
                delimiter = isFirst ? "?" : "&";
                addParamToUrl(outUri, "q", String.valueOf(quality), delimiter);
                hasQuality = true;
                isFirst = false;
            }

            delimiter = isFirst ? "?" : "&";
            addParamToUrl(outUri, "recapp", String.valueOf(recommendedApp), delimiter);
            isFirst = false;

            delimiter = "&";

            for (NameValuePair param : URLEncodedUtils.parse(currentUri, "UTF-8"))
            {

                //Size params were not handled so apply params that were added originally
                if (param.getName().equals("fit") || param.getName().equals("fill") || param.getName().equals("thumb"))
                {
                    if (!sizeParamsHandled)
                    {
                        addParamToUrl(outUri, param.getName(), param.getValue(), delimiter);
                        isFirst = false;
                    }

                    continue;
                }

                //Out parameter is handled at the end, recapp is always handled
                if (param.getName().equals("out") || param.getName().equals("recapp")) continue;


                //If matching parameter hasnt been set yet from ImageParams then try to keep original
                if (param.getName().equals("blur"))
                {
                    if (!hasBlur)
                        addParamToUrl(outUri, param.getName(), param.getValue(), delimiter);
                }
                else if (param.getName().equals("q"))
                {
                    if (!hasQuality)
                        addParamToUrl(outUri, param.getName(), param.getValue(), delimiter);
                }
                else
                {
                    addParamToUrl(outUri, param.getName(), param.getValue(), delimiter);
                }
            }

            delimiter = isFirst ? "?" : "&";

            //If specific format hasnt been requested then calculate format based on transparency and Android API version
            String format = imageFormat != null ? imageFormat.toString() : getImageFormat(trasparent, apiVersion);
            addParamToUrl(outUri, "out", format, delimiter);

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

    static String getImageFormat(boolean transparent, int versionSDK)
    {
        String format;
        if (transparent)
        {
            // Transparent WebP support has appeared in Android 4.2.1+
            if (versionSDK >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                format = ImageFormat.WEBP.toString();
            }
            else
            {
                format = ImageFormat.PNG.toString();
            }
        }
        else
        {
            // Non-transparent WebP support has appeared in Android 4.0+
            if (versionSDK >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            {
                format = ImageFormat.WEBP.toString();
            }
            else
            {
                format = ImageFormat.JPEG.toString();
            }
        }

        return format;
    }

    //Parse params from url and return them in map
    public static Map<String, String> parseImageServeParams(String uri)
    {
        Map<String, String> paramsMap = new TreeMap<String, String>();

        int index = uri.indexOf("?");
        if (index == -1)
            return paramsMap;

        if ((index + 1) > (uri.length() - 1))
            return paramsMap;

        String allParams = uri.substring(index + 1);
        String[] params = allParams.split("&");
        for (String param : params)
        {
            String[] valueAndName = param.split("=");
            if (valueAndName.length != 2) continue;

            String valueName = valueAndName[0].equalsIgnoreCase("blob-key") ? "key" : valueAndName[0];
            paramsMap.put(valueName, valueAndName[1]);
        }

        return paramsMap;
    }

    private static void addParamToUrl(StringBuilder outUri, String paramName, String paramValue, String delimiter) throws UnsupportedEncodingException
    {
        outUri.append(delimiter);
        outUri.append(URLEncoder.encode(paramName, "UTF-8"));
        outUri.append("=");
        outUri.append(URLEncoder.encode(paramValue, "UTF-8"));
    }

}
