package com.nostra13.universalimageloader.core.download.extra;

import com.nostra13.universalimageloader.core.listener.ConnectionListener;

import java.util.HashMap;
import java.util.Map;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 15. 04. 14
 * Time: 15:42
 */
public class DownloadExtra
{

    private Map<String, String> headers = new HashMap<String, String>();

    private ConnectionListener listener;

    public void setListener(final ConnectionListener listener)
    {
        this.listener = listener;
    }

    public ConnectionListener getListener()
    {
        return listener;
    }

    public void setHeader(String headerName, String headerValue)
    {
        headers.put(headerName, headerValue);
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }
}
