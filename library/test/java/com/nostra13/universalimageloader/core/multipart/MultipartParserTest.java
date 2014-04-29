package com.nostra13.universalimageloader.core.multipart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 29. 04. 14
 * Time: 14:31
 */
@RunWith(RobolectricTestRunner.class)
public class MultipartParserTest
{

    @Test
    public void testMultipartResponseParsing()
    {
        InputStream inputStream = MultipartParserTest.class.getResourceAsStream("/multipart-response");
        String contentType = null;
    }

}
