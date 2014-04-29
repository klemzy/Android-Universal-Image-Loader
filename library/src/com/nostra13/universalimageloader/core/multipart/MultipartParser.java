package com.nostra13.universalimageloader.core.multipart;


import android.annotation.TargetApi;
import android.os.Build;
import com.nostra13.universalimageloader.utils.L;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 22. 04. 14
 * Time: 10:35
 */
public class MultipartParser
{
    private InputStream inputStream;

    private String contentType;

    public MultipartParser(InputStream inputStream, String contentType)
    {
        this.inputStream = inputStream;
        this.contentType = contentType;
    }

    /**
     * Parse the InputStream from our DataSource, constructing the
     * appropriate MimeBodyParts.  The <code>parsed</code> flag is
     * set to true, and if true on entry nothing is done.  This
     * method is called by all other methods that need data for
     * the body parts, to make sure the data has been parsed.
     *
     * @since JavaMail 1.2
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public synchronized List<BodyPart> parse() throws IOException
    {
        boolean complete = true;
        List<BodyPart> bodyParts = new ArrayList<BodyPart>();

        if(inputStream == null || contentType == null)
        {
            L.e("InputStream has been null or missing Content-Type in multipart response");
            return bodyParts;
        }

        if (!(inputStream instanceof ByteArrayInputStream) &&
                !(inputStream instanceof BufferedInputStream))
            inputStream = new BufferedInputStream(inputStream);

        ContentType cType = new ContentType(contentType);
        String boundary = null;
        String bp = cType.getParameter("boundary");
        if (bp != null)
            boundary = "--" + bp;

        try
        {
            // Skip and save the preamble
            LineInputStream lin = new LineInputStream(inputStream);
            StringBuffer preamblesb = null;
            String line;
            String lineSeparator = null;
            while ((line = lin.readLine()) != null)
            {
                /*
                * Strip trailing whitespace.  Can't use trim method
                * because it's too aggressive.  Some bogus MIME
                * messages will include control characters inputStream the
                * boundary string.
                */
                int i;
                for (i = line.length() - 1; i >= 0; i--)
                {
                    char c = line.charAt(i);
                    if (!(c == ' ' || c == '\t'))
                        break;
                }
                line = line.substring(0, i + 1);
                if (boundary != null)
                {
                    if (line.equals(boundary))
                        break;
                }
                else
                {
                    /*
                    * Boundary hasn't been defined, does this line
   		            * look like a boundary?  If so, assume it is
   		            * the boundary and save it.
   		            */
                    if (line.startsWith("--"))
                    {
                        boundary = line;
                        break;
                    }
                }

                // save the preamble after skipping blank lines
                if (line.length() > 0)
                {
                    // if we haven't figured out what the line seprator
                    // is, do it now
                    if (lineSeparator == null)
                    {
                        try
                        {
                            lineSeparator =
                                    System.getProperty("line.separator", "\n");
                        }
                        catch (SecurityException ex)
                        {
                            lineSeparator = "\n";
                        }
                    }
                    // accumulate the preamble
                    if (preamblesb == null)
                        preamblesb = new StringBuffer(line.length() + 2);

                    preamblesb.append(line).append(lineSeparator);
                }
            }

            // save individual boundary bytes for comparison later
            byte[] bndbytes = ASCIIUtility.getBytes(boundary);
            int bl = bndbytes.length;

   	        /*
            * Compile Boyer-Moore parsing tables.
   	        */

            // initialize Bad Character Shift table
            int[] bcs = new int[256];
            for (int i = 0; i < bl; i++)
                bcs[bndbytes[i]] = i + 1;

            // initialize Good Suffix Shift table
            int[] gss = new int[bl];
            NEXT:
            for (int i = bl; i > 0; i--)
            {
                int j;    // the beginning index of the suffix being considered
                for (j = bl - 1; j >= i; j--)
                {
                    // Testing for good suffix
                    if (bndbytes[j] == bndbytes[j - i])
                    {
                        // bndbytes[j..len] is a good suffix
                        gss[j - 1] = i;
                    }
                    else
                    {
                        // No match. The array has already been
                        // filled up with correct values before.
                        continue NEXT;
                    }
                }
                while (j > 0)
                    gss[--j] = i;
            }
            gss[bl - 1] = 1;

   	        /*
               * Read and process body parts until we see the
   	        * terminating boundary line (or EOF).
   	        */
            boolean done = false;
            getparts:
            while (!done)
            {
                InternetHeaders headers = createInternetHeaders(inputStream);

                if (!inputStream.markSupported())
                    throw new IOException("Stream doesn't support mark");

                ByteArrayOutputStream buf = new ByteArrayOutputStream();

                int b;

                /*
                 * These buffers contain the bytes we're checking
                 * for a match.  inbuf is the current buffer and
                 * previnbuf is the previous buffer.  We need the
                 * previous buffer to check that we're preceeded
                 * by an EOL.
                 */
                // XXX - a smarter algorithm would use a sliding window
                //	 over a larger buffer
                byte[] inbuf = new byte[bl];
                byte[] previnbuf = new byte[bl];
                int inSize = 0;        // number of valid bytes inputStream inbuf
                int prevSize = 0;    // number of valid bytes inputStream previnbuf
                int eolLen;
                boolean first = true;

                /*
                 * Read and save the content bytes inputStream buf.
                 */
                for (; ; )
                {
                    inputStream.mark(bl + 4 + 1000); // bnd + "--\r\n" + lots of LWSP
                    eolLen = 0;
                    inSize = readFully(inputStream, inbuf, 0, bl);
                    if (inSize < bl)
                    {
                        complete = false;
                        done = true;
                        break;
                    }
                    // check whether inbuf contains a boundary string
                    int i;
                    for (i = bl - 1; i >= 0; i--)
                    {
                        if (inbuf[i] != bndbytes[i])
                            break;
                    }
                    if (i < 0)
                    {    // matched all bytes
                        eolLen = 0;
                        if (!first)
                        {
                            // working backwards, find out if we were preceeded
                            // by an EOL, and if so find its length
                            b = previnbuf[prevSize - 1];
                            if (b == '\r' || b == '\n')
                            {
                                eolLen = 1;
                                if (b == '\n' && prevSize >= 2)
                                {
                                    b = previnbuf[prevSize - 2];
                                    if (b == '\r')
                                        eolLen = 2;
                                }
                            }
                        }
                        if (first || eolLen > 0)
                        {
                            // matched the boundary, check for last boundary
                            int b2 = inputStream.read();
                            if (b2 == '-')
                            {
                                if (inputStream.read() == '-')
                                {
                                    complete = true;
                                    done = true;
                                    break;    // ignore trailing text
                                }
                            }
                            // skip linear whitespace
                            while (b2 == ' ' || b2 == '\t')
                                b2 = inputStream.read();
                            // check for end of line
                            if (b2 == '\n')
                                break;    // got it!  break out of the loop
                            if (b2 == '\r')
                            {
                                inputStream.mark(1);
                                if (inputStream.read() != '\n')
                                    inputStream.reset();
                                break;    // got it!  break out of the loop
                            }
                        }
                        i = 0;
                    }

                    /*
                     * Get here if boundary didn't match,
                     * wasn't preceeded by EOL, or wasn't
                     * followed by whitespace or EOL.
                     */

                    // compute how many bytes we can skip
                    int skip = Math.max(i + 1 - bcs[inbuf[i] & 0x7f], gss[i]);
                    // want to keep at least two characters
                    if (skip < 2)
                    {
                        // only skipping one byte, save one byte
                        // from previous buffer as well
                        // first, write out bytes we're done with
                        if (prevSize > 1)
                            buf.write(previnbuf, 0, prevSize - 1);
                        inputStream.reset();
                        skipFully(inputStream, 1);
                        if (prevSize >= 1)
                        {    // is there a byte to save?
                            // yes, save one from previous and one from current
                            previnbuf[0] = previnbuf[prevSize - 1];
                            previnbuf[1] = inbuf[0];
                            prevSize = 2;
                        }
                        else
                        {
                            // no previous bytes to save, can only save current
                            previnbuf[0] = inbuf[0];
                            prevSize = 1;
                        }
                    }
                    else
                    {
                        // first, write out data from previous buffer before
                        // we dump it
                        if (prevSize > 0)
                            buf.write(previnbuf, 0, prevSize);
                        // all the bytes we're skipping are saved inputStream previnbuf
                        prevSize = skip;
                        inputStream.reset();
                        skipFully(inputStream, prevSize);
                        // swap buffers
                        byte[] tmp = inbuf;
                        inbuf = previnbuf;
                        previnbuf = tmp;
                    }
                    first = false;
                }


                // write out data from previous buffer, not including EOL
                if (prevSize - eolLen > 0)
                    buf.write(previnbuf, 0, prevSize - eolLen);
                // if we didn't find a trailing boundary,
                // the current buffer has data we need too
                if (!complete && inSize > 0)
                    buf.write(inbuf, 0, inSize);

                /*
                 * Create a MimeBody element to represent this body part.
                 */
                BodyPart part = new BodyPart(headers, buf.toByteArray());
                bodyParts.add(part);
            }
        }
        catch (IOException ioex)
        {
            throw new IOException(ioex);
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (IOException cex)
            {
                // ignore
            }
        }

        return bodyParts;
    }

    /**
     * Read data from the input stream to fill the buffer starting
     * at the specified offset with the specified number of bytes.
     * If len is zero, return zero.  If at EOF, return -1.  Otherwise,
     * return the number of bytes read.  Call the read method on the
     * input stream as many times as necessary to read len bytes.
     *
     * @param in  InputStream to read from
     * @param buf buffer to read into
     * @param off offset in the buffer for first byte
     * @param len number of bytes to read
     * @return -1 on EOF, otherwise number of bytes read
     * @throws java.io.IOException on I/O errors
     */
    int readFully(InputStream in, byte[] buf, int off, int len)
            throws IOException
    {
        if (len == 0)
            return 0;
        int total = 0;
        while (len > 0)
        {
            int bsize = in.read(buf, off, len);
            if (bsize <= 0)    // should never be zero
                break;
            off += bsize;
            total += bsize;
            len -= bsize;
        }
        return total > 0 ? total : -1;
    }

    /**
     * Skip the specified number of bytes, repeatedly calling
     * the skip method as necessary.
     */
    void skipFully(InputStream in, long offset) throws IOException
    {
        while (offset > 0)
        {
            long cur = in.skip(offset);
            if (cur <= 0)
                throw new EOFException("can't skip");
            offset -= cur;
        }
    }

    InternetHeaders createInternetHeaders(InputStream is)
            throws IOException
    {
        return new InternetHeaders(is);
    }

}
