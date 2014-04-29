package com.nostra13.universalimageloader.core.download;

import android.content.Context;
import android.net.Uri;
import com.nostra13.universalimageloader.core.assist.ContentLengthInputStream;
import com.nostra13.universalimageloader.core.download.extra.DownloadExtra;
import com.nostra13.universalimageloader.utils.IoUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 23. 04. 14
 * Time: 14:39
 */
public class ImageServeDownloader extends BaseImageDownloader
{

    public ImageServeDownloader(final Context context)
    {
        super(context);
    }

    @Override
    protected InputStream getStreamFromNetwork(final String imageUri, final Object extra) throws IOException
    {
        DownloadExtra downloadExtra = null;
        if(extra != null && extra instanceof DownloadExtra)
            downloadExtra = (DownloadExtra) extra;

        HttpURLConnection conn = createConnection(imageUri, downloadExtra);

        int redirectCount = 0;
        while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT)
        {
            conn = createConnection(conn.getHeaderField("Location"), downloadExtra);
            redirectCount++;
        }

        InputStream imageStream;
        try
        {
            imageStream = conn.getInputStream();
        }
        catch (IOException e)
        {
            // Read all data to allow reuse connection (http://bit.ly/1ad35PY)
            IoUtils.readAndCloseStream(conn.getErrorStream());
            throw e;
        }

        if(downloadExtra != null && downloadExtra.getListener() != null)
            downloadExtra.getListener().onConnected(conn);

        return new ContentLengthInputStream(new BufferedInputStream(imageStream, BUFFER_SIZE), conn.getContentLength());
    }

    protected HttpURLConnection createConnection(final String url, final DownloadExtra extra) throws IOException
    {
        String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
        HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();

        if (extra != null)
        {
            for (Map.Entry<String, String> header : extra.getHeaders().entrySet())
                conn.addRequestProperty(header.getKey(), header.getValue());
        }

        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }
}
