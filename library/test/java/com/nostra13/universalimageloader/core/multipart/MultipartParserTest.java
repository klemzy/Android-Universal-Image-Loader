package com.nostra13.universalimageloader.core.multipart;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 29. 04. 14
 * Time: 14:31
 */
@RunWith(RobolectricTestRunner.class)
public class MultipartParserTest
{

    private static final String CONTENT_TYPE_HEADER = "multipart/mixed; boundary=--------------------145ad5d512a";

    @Test
    public void testParseMultipartResponse() throws IOException
    {
        InputStream inputStream = null;
        String contentType = null;

        MultipartParser parser = new MultipartParser(inputStream, contentType);
        Assert.assertEquals(0, parser.parse().size());

        inputStream = MultipartParserTest.class.getResourceAsStream("/multipart-response");
        contentType = CONTENT_TYPE_HEADER;

        parser = new MultipartParser(inputStream, contentType);
        List<BodyPart> bodyParts = parser.parse();
        Assert.assertNotNull(bodyParts);
        Assert.assertEquals(15, bodyParts.size());
    }

    @Test
    public void testParseMultipartResponseCorruptBody() throws IOException
    {
        InputStream inputStream = MultipartParserTest.class.getResourceAsStream("/multipart-response-corrupt");

        MultipartParser parser = new MultipartParser(inputStream, CONTENT_TYPE_HEADER);
        List<BodyPart> bodyParts = parser.parse();
        Assert.assertEquals(0, bodyParts.size());
    }

    @Test
    public void testMultipartReponseMissingBoundary() throws IOException
    {
        //Parsing can figure out boundary even if there wasnt one passed in Content-Type header
        InputStream inputStream = MultipartParserTest.class.getResourceAsStream("/multipart-response");
        String contentType = "multipart/mixed";

        MultipartParser parser = new MultipartParser(inputStream, contentType);
        List<BodyPart> bodyParts = parser.parse();
        Assert.assertEquals(15, bodyParts.size());
    }

    @Test
    public void testMultipartResponseWrongBoundary() throws IOException
    {
        InputStream inputStream = MultipartParserTest.class.getResourceAsStream("/multipart-response");
        String contentType = "multipart/mixed; boundary=--------------------145ad5d";

        MultipartParser parser = new MultipartParser(inputStream, contentType);
        List<BodyPart> bodyParts = parser.parse();
        Assert.assertEquals(0, bodyParts.size());
    }

}
