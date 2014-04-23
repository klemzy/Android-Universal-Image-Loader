package com.nostra13.universalimageloader.core.listener;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 17. 04. 14
 * Time: 14:49
 */
public interface ConnectionListener
{
    public void onConnected(HttpURLConnection connection) throws IOException;
}
