package com.nostra13.universalimageloader.core;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 29. 04. 14
 * Time: 16:08
 */
@RunWith(RobolectricTestRunner.class)
public class LoadAndDisplayMultiImageTaskTest
{

    private static final String X_IDDICTION_LIST_HEADER = "{\"list\":[{\"v\":\"201404141704\",\"fit\":\"120\",\"q\":\"70\",\"key\":\"AMIfv9462j1OBDfvvWk7k5tXNqEiHZKrUKnJ1lOcP5NgHdpKGc3pFqkqzR71pRnVocKHiOTs8DwNuaZ06QjDTER5qqx4O3dLTqONVYpoN8Khs7XsbA9i580zgOC3i8PvD1X59YkNxGzGeAcBUxivDvQMFURRbkmu3g\",\"out\":\"webp\"},{\"v\":\"201404141704\",\"fit\":\"120\",\"q\":\"70\",\"key\":\"AMIfv94fK38NmbWVfpx8irc9SoVSi-pcq8XLej5Y9lKyhYrl-BWR0BprMkBNX-4C4lWyRND2zTDWC6Mos9mkMKn-3utpDAWsW8cqSrSfthRSv0RK_FpgpMuH16YO5Jt5gmJ6lPGO3-pJ_1X5Y-_pW8NmeVX9snZ-Ow\",\"out\":\"webp\"},{\"v\":\"201404141704\",\"q\":\"100\",\"key\":\"AMIfv94vwDSek72zmqL9l9Gccowtt3NscAeTHhEP_uzNZ0h0Bt4__-kbLjSly8Kbf6PJGQY1sLRPv1igTCMiS7JZBFb74oTwIUoDRTo-v8096EMKFkV6DnBBqEc75ShYh_ZsDc7lwVAu2jOnxGCvKfcgKz-1igupPw\",\"out\":\"webp\"}]}";

    @Test
    public void convertToJsonStringTest() throws JSONException
    {
        List<ImageServeInfo> list = new ArrayList<ImageServeInfo>();
        Map<String, String> map = new TreeMap<String, String>();
        map.put("key", "AMIfv9462j1OBDfvvWk7k5tXNqEiHZKrUKnJ1lOcP5NgHdpKGc3pFqkqzR71pRnVocKHiOTs8DwNuaZ06QjDTER5qqx4O3dLTqONVYpoN8Khs7XsbA9i580zgOC3i8PvD1X59YkNxGzGeAcBUxivDvQMFURRbkmu3g");
        map.put("out", "webp");
        map.put("q", "70");
        map.put("v", "201404141704");
        map.put("fit", "120");

        ImageServeInfo serveInfo = new ImageServeInfo(null, null, null, null, null, null, null, null, map);
        list.add(serveInfo);

        map = new TreeMap<String, String>();
        map.put("key", "AMIfv94fK38NmbWVfpx8irc9SoVSi-pcq8XLej5Y9lKyhYrl-BWR0BprMkBNX-4C4lWyRND2zTDWC6Mos9mkMKn-3utpDAWsW8cqSrSfthRSv0RK_FpgpMuH16YO5Jt5gmJ6lPGO3-pJ_1X5Y-_pW8NmeVX9snZ-Ow");
        map.put("out", "webp");
        map.put("q", "70");
        map.put("v", "201404141704");
        map.put("fit", "120");

        serveInfo = new ImageServeInfo(null, null, null, null, null, null, null, null, map);
        list.add(serveInfo);

        map = new TreeMap<String, String>();
        map.put("key", "AMIfv94vwDSek72zmqL9l9Gccowtt3NscAeTHhEP_uzNZ0h0Bt4__-kbLjSly8Kbf6PJGQY1sLRPv1igTCMiS7JZBFb74oTwIUoDRTo-v8096EMKFkV6DnBBqEc75ShYh_ZsDc7lwVAu2jOnxGCvKfcgKz-1igupPw");
        map.put("out", "webp");
        map.put("q", "100");
        map.put("v", "201404141704");

        serveInfo = new ImageServeInfo(null, null, null, null, null, null, null, null, map);
        list.add(serveInfo);

        LoadAndDisplayMultiImageTask task = new LoadAndDisplayMultiImageTask(null, null, null, null, null, null, null, false, null);
        String jsonHeader = task.convertToJsonString(list);

        Assert.assertEquals(X_IDDICTION_LIST_HEADER, jsonHeader);
    }



}
