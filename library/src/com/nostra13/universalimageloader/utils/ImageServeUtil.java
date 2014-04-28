package com.nostra13.universalimageloader.utils;

import android.os.Build;
import com.nostra13.universalimageloader.core.assist.ImageFormat;
import com.nostra13.universalimageloader.core.assist.ImageServeParams;
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


    public static String getProcessedImageUri(String uri, int width, int height, boolean trasparent, ImageServeParams params, ViewScaleType viewScaleType)
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
            boolean hasQuality = false;
            for (NameValuePair param : URLEncodedUtils.parse(currentUri, "UTF-8"))
            {
                // Ignore fit, fill, thumb and out parameters
                if (param.getName().equals("fit") || param.getName().equals("fill") || param.getName().equals("out") || param.getName().equals("thumb"))
                    continue;

                if (param.getName().equals("q")) hasQuality = true;

                String delimiter = isFirst ? "?" : "&";
                isFirst = false;

                if (params != null && params.getBlur() != -1 && param.getName().equals("blur"))
                {
                    addParamToUrl(outUri, "blur", String.valueOf(params.getBlur()), delimiter);
                }
                else if (params != null && params.getQuality() != -1 && param.getName().equals("q"))
                {
                    addParamToUrl(outUri, "q", String.valueOf(params.getQuality()), delimiter);
                    hasQuality = true;
                }
                else if (params != null && params.isRecApp() && param.getName().equals("recapp"))
                {
                    addParamToUrl(outUri, "recapp", String.valueOf(params.isRecApp()), delimiter);
                }
                else
                {
                    addParamToUrl(outUri, param.getName(), param.getValue(), delimiter);
                }
            }

            //ImageServe returns q=100 if no quality speficied. Add q=100 if there isnt one already in original uri
            if (!hasQuality)
                addParamToUrl(outUri, "q", "100", "&");

            if (viewScaleType != null && width > 0 && height > 0)
            {
                String paramName = null;
                String paramValue = null;
                if (viewScaleType == ViewScaleType.CROP)
                {
                    //If crop then get fill picture and take smaller edge
                    paramName = "fill";
                    paramValue = String.valueOf(width > height ? height : width);
                }
                else if (viewScaleType == ViewScaleType.FIT_INSIDE)
                {
                    //If fill then get fit picture and take larger edge
                    paramName = "fit";
                    paramValue = String.valueOf(width > height ? width : height);
                }

                addParamToUrl(outUri, paramName, paramValue, "&");
            }

            //If specific format hasnt been requested then calculate format based on transparency and Android API version
            String format = params != null && params.getImageFormat() != null ? params.getImageFormat().toString() : getImageFormat(trasparent, apiVersion);
            addParamToUrl(outUri, "out", format, "&");

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
        String allParams = uri.substring(index + 1);
        String[] params = allParams.split("&");
        for (String param : params)
        {
            String[] valueAndName = param.split("=");
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
