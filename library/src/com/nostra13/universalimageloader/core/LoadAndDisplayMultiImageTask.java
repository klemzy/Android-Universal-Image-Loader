package com.nostra13.universalimageloader.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.SparseArray;
import com.nostra13.universalimageloader.core.assist.*;
import com.nostra13.universalimageloader.core.decode.ImageDecoder;
import com.nostra13.universalimageloader.core.decode.ImageDecodingInfo;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.download.extra.DownloadExtra;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ConnectionListener;
import com.nostra13.universalimageloader.core.multipart.BodyPart;
import com.nostra13.universalimageloader.core.multipart.InternetHeaders;
import com.nostra13.universalimageloader.core.multipart.MultipartParser;
import com.nostra13.universalimageloader.utils.IoUtils;
import com.nostra13.universalimageloader.utils.L;
import com.nostra13.universalimageloader.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 16. 04. 14
 * Time: 11:55
 */
public class LoadAndDisplayMultiImageTask implements Runnable, IoUtils.CopyListener
{

    private static final String IMAGE_URL = "http://images.idd-backend.appspot.com/imageserveV2";

    private static final String ACCEPT_HEADER = "multipart/mixed";

    private static final String IDDICTION_LIST_HEADER_NAME = "x-iddiction-list";


    private static final String LOG_WAITING_FOR_RESUME = "ImageLoader is paused. Waiting...  [%s]";

    private static final String LOG_RESUME_AFTER_PAUSE = ".. Resume loading [%s]";

    private static final String LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]";

    private static final String LOG_IMAGE_ALREADY_ADDED_TO_MULTIPART_REQUEST_LIST = "Image has already been added to multipart request list. Skipping...";

    private static final String LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING = "...Get cached bitmap from memory after waiting. [%s]";

    private static final String LOG_LOAD_IMAGE_FROM_NETWORK = "Load image from network [%s]";

    private static final String LOG_LOAD_IMAGE_FROM_DISC_CACHE = "Load image from disc cache [%s]";

    private static final String LOG_RESIZE_CACHED_IMAGE_FILE = "Resize image in disc cache [%s]";

    private static final String LOG_PREPROCESS_IMAGE = "PreProcess image before caching in memory [%s]";

    private static final String LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]";

    private static final String LOG_CACHE_IMAGE_IN_MEMORY = "Cache image in memory [%s]";

    private static final String LOG_CACHE_IMAGE_ON_DISC = "Cache image on disc [%s]";

    private static final String LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK = "Process image before cache on disc [%s]";

    private static final String LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]";

    private static final String LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]";

    private static final String LOG_TASK_INTERRUPTED = "Task was interrupted [%s]";

    private static final String ERROR_PRE_PROCESSOR_NULL = "Pre-processor returned null [%s]";

    private static final String ERROR_POST_PROCESSOR_NULL = "Post-processor returned null [%s]";

    private static final String ERROR_PROCESSOR_FOR_DISK_CACHE_NULL = "Bitmap processor for disc cache returned null [%s]";

    private final List<ImageServeInfo> loadingInfoList;

    private final ImageLoaderEngine engine;

    private final ImageLoaderConfiguration configuration;

    private final ImageDownloader downloader;

    private final ImageDownloader networkDeniedDownloader;

    private final ImageDownloader slowNetworkDownloader;

    private final ImageDecoder decoder;

    private final Handler handler;

    private final boolean writeLogs;

    private String contentType;


    private LoadedFrom loadedFrom = LoadedFrom.NETWORK;

    public LoadAndDisplayMultiImageTask(List<ImageServeInfo> loadingInfoList, ImageLoaderEngine engine, Handler handler)
    {
        this.loadingInfoList = loadingInfoList;
        this.engine = engine;

        this.configuration = engine.configuration;
        this.downloader = configuration.downloader;
        this.networkDeniedDownloader = configuration.networkDeniedDownloader;
        this.slowNetworkDownloader = configuration.slowNetworkDownloader;
        this.decoder = configuration.decoder;
        this.writeLogs = configuration.writeLogs;
        this.handler = handler;

    }

    @Override
    public void run()
    {
        waitIfPaused();

        for (Iterator<ImageServeInfo> iterator = loadingInfoList.iterator(); iterator.hasNext(); )
        {
            ImageServeInfo loadingInfo = iterator.next();

            //Check if view has already been GC'd
            String memoryCacheKey = loadingInfo.memoryCacheKey;

            if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey)) continue;

            //Check if image exists on disc or in memory
            Bitmap bmp;
            try
            {

                if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey)) continue;

                if (isTaskInterrupted(memoryCacheKey)) throw new TaskCancelledException();

                bmp = getBitmap(loadingInfo);

                if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey)) continue;

                if (isTaskInterrupted(memoryCacheKey)) throw new TaskCancelledException();

                //Bitmap has been found, display it and remove from list
                if (bmp != null)
                {
                    displayBitmapTask(bmp, loadingInfo);
                    iterator.remove();
                }
                else
                {
                    log(LOG_LOAD_IMAGE_FROM_NETWORK, loadingInfo.memoryCacheKey);
                }

            }
            catch (TaskCancelledException e)
            {
                fireCancelEvent(loadingInfo);
                return;
            }
        }

        if (loadingInfoList.size() == 0)
        {
            log("Nothing to download. Empty multipart image request");
            return;
        }


        try
        {
            String imageRequests = convertToJsonString(loadingInfoList);
            String hashString = StringUtils.sha256(ACCEPT_HEADER + "#" + imageRequests);
            downloadImagesAndParseResponse(IMAGE_URL + "?hash=" + URLEncoder.encode(hashString, "UTF-8"), imageRequests);
        }
        catch (JSONException e)
        {
            fireCancelEvent(loadingInfoList);
        }
        catch (UnsupportedEncodingException e)
        {
            fireCancelEvent(loadingInfoList);
        }
        catch (IOException e)
        {
            fireCancelEvent(loadingInfoList);
        }
        catch (TaskCancelledException e)
        {
            fireCancelEvent(loadingInfoList);
        }

    }

    Bitmap getBitmap(ImageServeInfo loadingInfo) throws TaskCancelledException
    {
        String memoryCacheKey = loadingInfo.memoryCacheKey;
        Bitmap bmp = configuration.memoryCache.get(memoryCacheKey);
        if (bmp == null)
        {
            long start = System.currentTimeMillis();
            bmp = tryLoadBitmapFromDisk(loadingInfo);
            log("TIME Load bitmap from disk: " + (System.currentTimeMillis() - start));

            if (bmp == null) return null;

            if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey))
            {
                return null;
            }

            if (isTaskInterrupted(memoryCacheKey)) throw new TaskCancelledException();

            tryPreprocessBitmap(bmp, loadingInfo);
            tryCacheBitmapInMemory(bmp, loadingInfo);
        }
        else
        {
            loadedFrom = LoadedFrom.MEMORY_CACHE;
            log(LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING, memoryCacheKey);
        }

        tryPostprocessBitmap(bmp, loadingInfo);

        return bmp;
    }

    void tryPreprocessBitmap(Bitmap bitmap, ImageServeInfo loadingInfo)
    {
        if (loadingInfo.options.shouldPreProcess())
        {
            log(LOG_PREPROCESS_IMAGE, loadingInfo.memoryCacheKey);
            bitmap = loadingInfo.options.getPreProcessor().process(bitmap);
            if (bitmap == null)
            {
                L.e(ERROR_PRE_PROCESSOR_NULL, loadingInfo.memoryCacheKey);
            }
        }
    }

    void tryCacheBitmapInMemory(Bitmap bitmap, ImageServeInfo loadingInfo)
    {
        if (bitmap != null && loadingInfo.options.isCacheInMemory())
        {
            log(LOG_CACHE_IMAGE_IN_MEMORY, loadingInfo.memoryCacheKey);
            configuration.memoryCache.put(loadingInfo.memoryCacheKey, bitmap);
        }
    }

    void tryPostprocessBitmap(Bitmap bitmap, ImageServeInfo loadingInfo)
    {
        if (bitmap != null && loadingInfo.options.shouldPostProcess())
        {
            log(LOG_POSTPROCESS_IMAGE, loadingInfo.memoryCacheKey);
            bitmap = loadingInfo.options.getPostProcessor().process(bitmap);
            if (bitmap == null)
            {
                L.e(ERROR_POST_PROCESSOR_NULL, loadingInfo.memoryCacheKey);
            }
        }
    }

    String convertToJsonString(List<ImageServeInfo> loadingInfoList) throws JSONException
    {
        Map<String, Map<String, String>> map = new TreeMap<String, Map<String, String>>();
        for (ImageServeInfo loadingInfo : loadingInfoList)
        {
            Map<String, String> params = loadingInfo.imageServeParams;
            String key = params.get("key");
            map.put(key, params);
        }

        JSONObject listObject = new JSONObject();
        JSONArray array = new JSONArray();


        for (Map.Entry<String, Map<String, String>> params : map.entrySet())
        {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, String> param : params.getValue().entrySet())
                object.put(param.getKey(), param.getValue());

            array.put(object);
        }

        listObject.put("list", array);

        return listObject.toString();
    }


    private void displayBitmapTask(Bitmap bitmap, ImageServeInfo loadingInfo)
    {
        DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bitmap, loadingInfo, engine, loadedFrom);
        displayBitmapTask.setLoggingEnabled(writeLogs);
        runTask(displayBitmapTask, loadingInfo.options.isSyncLoading(), handler, engine);
    }

    private Bitmap tryLoadBitmapFromDisk(ImageServeInfo loadingInfo) throws TaskCancelledException
    {
        File imageFile = configuration.diskCache.get(loadingInfo.uri);


        Bitmap bitmap = null;
        try
        {
            if (imageFile != null && imageFile.exists())
            {
                String cacheFileUri = ImageDownloader.Scheme.FILE.wrap(imageFile.getAbsolutePath());

                log(LOG_LOAD_IMAGE_FROM_DISC_CACHE, loadingInfo.memoryCacheKey);
                loadedFrom = LoadedFrom.DISC_CACHE;

                if (!isTaskNotActual(loadingInfo.imageAware, loadingInfo.memoryCacheKey))
                {
                    bitmap = decodeImage(loadingInfo, cacheFileUri);
                }
            }
        }
        catch (IllegalStateException e)
        {
            fireFailEvent(FailReason.FailType.NETWORK_DENIED, null, loadingInfo);
        }
        catch (IOException e)
        {
            L.e(e);
            fireFailEvent(FailReason.FailType.IO_ERROR, e, loadingInfo);
            if (imageFile.exists())
            {
                imageFile.delete();
            }
        }
        catch (OutOfMemoryError e)
        {
            L.e(e);
            fireFailEvent(FailReason.FailType.OUT_OF_MEMORY, e, loadingInfo);
        }
        catch (Throwable e)
        {
            L.e(e);
            fireFailEvent(FailReason.FailType.UNKNOWN, e, loadingInfo);
        }

        return bitmap;
    }


    /**
     * Download multiple images.
     * See https://github.com/Iddiction/backend/wiki/ImageServe-API for more details
     *
     * @param uri          Uri of service that serves multiple images
     * @param requestsJson Json of all images (uri and params) that should be returned
     * @throws IOException
     * @throws TaskCancelledException
     */
    void downloadImagesAndParseResponse(final String uri, String requestsJson) throws IOException, TaskCancelledException
    {
        final DownloadExtra downloadExtra = new DownloadExtra();
        downloadExtra.setHeader("Accept", ACCEPT_HEADER);
        downloadExtra.setHeader(IDDICTION_LIST_HEADER_NAME, requestsJson);


        downloadExtra.setListener(new ConnectionListener()
        {
            @Override
            public void onConnected(final HttpURLConnection connection) throws IOException
            {
                contentType = connection.getHeaderField("Content-Type");
            }
        });

        long start = System.currentTimeMillis();
        InputStream inputStream = getDownloader().getStream(uri, downloadExtra);
        log("TIME Load bitmaps from network: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        MultipartParser parser = new MultipartParser(inputStream, contentType);
        log("TIME Parse bitmaps from response: " + (System.currentTimeMillis() - start));

        List<BodyPart> bodyParts = parser.parse();

        log("Received " + bodyParts.size() + " of multipart/mixed parts");

        SparseArray<BodyPart> bodyPartMap = createBodyPartMap(bodyParts);
        displayImages(bodyPartMap, loadingInfoList);
    }

    /**
     * Create map of BodyParts. Key is hashcode of image parameters. That way every BodyPart can be matched with
     * request (ImageServeInfo)
     *
     * @param bodyParts List of BodyPart that represent a single image response
     * @return Map of BodyParts
     */
    SparseArray<BodyPart> createBodyPartMap(List<BodyPart> bodyParts)
    {
        SparseArray<BodyPart> bodyPartMap = new SparseArray<BodyPart>();
        for (BodyPart bodyPart : bodyParts)
        {
            Map<String, String> perImageParams = new TreeMap<String, String>();

            InternetHeaders headers = bodyPart.getHeaders();
            String[] contentType = headers.getHeader("Content-Type");

            //Handle case when parser returns empty body part
            if (contentType == null || contentType.length == 0)
            {
                L.e("Empty Content-Type header");
                continue;
            }

            String[] params = contentType[0].split(";");

            boolean error = false;
            for (String param : params)
            {
                if (param.equals("error"))
                {
                    error = true;
                }
                else if (param.contains(ImageFormat.JPEG.toString()))
                {
                    perImageParams.put("out", ImageFormat.JPEG.toString());
                }
                else if (param.contains(ImageFormat.PNG.toString()))
                {
                    perImageParams.put("out", ImageFormat.PNG.toString());
                }
                else if (param.contains(ImageFormat.WEBP.toString()))
                {
                    perImageParams.put("out", ImageFormat.WEBP.toString());
                }
                else if (param.contains("="))
                {
                    String[] paramNameValue = param.split("=");
                    if (paramNameValue.length == 2)
                        perImageParams.put(paramNameValue[0], paramNameValue[1]);
                }

            }

            if (error)
            {
                L.e("Multipart response for params " + contentType[0] + " contained error");
                continue;
            }

            bodyPartMap.put(perImageParams.hashCode(), bodyPart);
        }

        return bodyPartMap;
    }

    /**
     * For every request try to load image from memory/disk otherwise try to find matching BodyPart. In most cases image
     * will not be in memory/disk at this point. However if there have been multiple requests for the same image but different
     * view then try to load image from memory/disk.
     *
     * @param bodyPartMap Map of BodyParts that is used to match request with BodyPart
     * @param loadingInfoList List of requests
     *
     * @throws TaskCancelledException
     * @throws IOException
     */
    void displayImages(SparseArray<BodyPart> bodyPartMap, List<ImageServeInfo> loadingInfoList) throws TaskCancelledException, IOException
    {
        for (ImageServeInfo info : loadingInfoList)
        {
            Bitmap bitmap = getBitmap(info);
            if (bitmap != null)
            {
                displayBitmapTask(bitmap, info);
                continue;
            }

            BodyPart bodyPart = bodyPartMap.get(info.imageServeParams.hashCode());
            if (bodyPart != null) displayImage(bodyPart.getData(), info);
        }
    }

    void displayImage(byte[] imageBinary, ImageServeInfo loadingInfo) throws IOException
    {
        boolean cachedToDisc;
        try
        {
            long start = System.currentTimeMillis();
            boolean cacheResult = tryCacheImageOnDisk(loadingInfo, imageBinary);
            log("TIME Save bitmap to disk: " + (System.currentTimeMillis() - start));

            cachedToDisc = loadingInfo.options.isCacheOnDisk() && cacheResult;
        }
        catch (TaskCancelledException e)
        {
            fireCancelEvent(loadingInfoList);
            return;
        }

        File imageFile = configuration.diskCache.get(loadingInfo.uri);
        if (imageFile == null)
            return;

        String cacheFileUri = ImageDownloader.Scheme.FILE.wrap(imageFile.getAbsolutePath());


        if (isTaskNotActual(loadingInfo.imageAware, loadingInfo.memoryCacheKey))
        {
            log("View has been collected or reused, cancel display of bitmap");
            return;
        }

        //If disc cache enabled then load from cache otherwise directly from byte[]
        Bitmap bitmap;
        if (cachedToDisc)
            bitmap = decodeImage(loadingInfo, cacheFileUri);
        else
            bitmap = decodeImage(loadingInfo, imageBinary);

        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0)
        {
            fireFailEvent(FailReason.FailType.DECODING_ERROR, null, loadingInfo);
            return;
        }

        tryPreprocessBitmap(bitmap, loadingInfo);
        //Save to memory cache if allowed
        tryCacheBitmapInMemory(bitmap, loadingInfo);
        tryPostprocessBitmap(bitmap, loadingInfo);

        DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bitmap, loadingInfo, engine, loadedFrom);
        displayBitmapTask.setLoggingEnabled(writeLogs);
        runTask(displayBitmapTask, loadingInfo.options.isSyncLoading(), handler, engine);
    }

    private Bitmap decodeImage(ImageLoadingInfo loadingInfo, byte[] imageData) throws IOException
    {
        ViewScaleType viewScaleType = loadingInfo.imageAware.getScaleType();
        ImageDecodingInfo decodingInfo = new ImageDecodingInfo(loadingInfo.memoryCacheKey, null, loadingInfo.uri, loadingInfo.targetSize, viewScaleType,
                getDownloader(), loadingInfo.options);

        return decoder.decode(decodingInfo, imageData);
    }

    private Bitmap decodeImage(ImageLoadingInfo loadingInfo, String imageUri) throws IOException
    {
        ViewScaleType viewScaleType = loadingInfo.imageAware.getScaleType();
        ImageDecodingInfo decodingInfo = new ImageDecodingInfo(loadingInfo.memoryCacheKey, imageUri, loadingInfo.uri, loadingInfo.targetSize, viewScaleType,
                getDownloader(), loadingInfo.options);

        return decoder.decode(decodingInfo);
    }


    private boolean tryCacheImageOnDisk(ImageLoadingInfo loadingInfo, byte[] imageData) throws TaskCancelledException
    {
        log(LOG_CACHE_IMAGE_ON_DISC, loadingInfo.memoryCacheKey);

        boolean loaded = false;
        try
        {
            loaded = configuration.diskCache.save(loadingInfo.uri, new ByteArrayInputStream(imageData), null);
            if (loaded)
            {
                int width = configuration.maxImageWidthForDiskCache;
                int height = configuration.maxImageHeightForDiskCache;
                if (width > 0 || height > 0)
                {
                    log(LOG_RESIZE_CACHED_IMAGE_FILE, loadingInfo.memoryCacheKey);
                    loaded = resizeAndSaveImage(loadingInfo, width, height); // TODO : process boolean result
                }
            }
        }
        catch (IOException e)
        {
            L.e(e);
        }

        return loaded;
    }


    /**
     * Decodes image file into Bitmap, resize it and save it back
     */
    private boolean resizeAndSaveImage(ImageLoadingInfo loadingInfo, int maxWidth, int maxHeight) throws IOException
    {
        boolean saved = false;
        File targetFile = configuration.diskCache.get(loadingInfo.uri);
        if (targetFile != null && targetFile.exists())
        {
            ImageSize targetImageSize = new ImageSize(maxWidth, maxHeight);
            DisplayImageOptions specialOptions = new DisplayImageOptions.Builder().cloneFrom(loadingInfo.options)
                    .imageScaleType(ImageScaleType.IN_SAMPLE_INT).build();
            ImageDecodingInfo decodingInfo = new ImageDecodingInfo(loadingInfo.memoryCacheKey,
                    ImageDownloader.Scheme.FILE.wrap(targetFile.getAbsolutePath()), loadingInfo.uri, targetImageSize, ViewScaleType.FIT_INSIDE,
                    getDownloader(), specialOptions);
            Bitmap bmp = decoder.decode(decodingInfo);
            if (bmp != null && configuration.processorForDiskCache != null)
            {
                log(LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK, loadingInfo.memoryCacheKey);
                bmp = configuration.processorForDiskCache.process(bmp);
                if (bmp == null)
                {
                    L.e(ERROR_PROCESSOR_FOR_DISK_CACHE_NULL, loadingInfo.memoryCacheKey);
                }
            }
            if (bmp != null)
            {
                saved = configuration.diskCache.save(loadingInfo.uri, bmp);
                bmp.recycle();
            }
        }

        return saved;
    }

    private void fireFailEvent(final FailReason.FailType failType, final Throwable failCause, final ImageServeInfo loadingInfo)
    {
        if (loadingInfo.options.isSyncLoading() || isTaskInterrupted(loadingInfo.memoryCacheKey) || isTaskNotActual(loadingInfo.imageAware, loadingInfo.memoryCacheKey))
        {
            return;
        }
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                if (loadingInfo.options.shouldShowImageOnFail())
                {
                    loadingInfo.imageAware.setImageDrawable(loadingInfo.options.getImageOnFail(configuration.resources));
                }
                loadingInfo.listener.onLoadingFailed(loadingInfo.uri, loadingInfo.imageAware.getWrappedView(), new FailReason(failType, failCause));
            }
        };
        runTask(r, false, handler, engine);
    }

    private void fireCancelEvent(final ImageServeInfo loadingInfo)
    {
        if (loadingInfo.options.isSyncLoading() || isTaskInterrupted(loadingInfo.memoryCacheKey)) return;
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                loadingInfo.listener.onLoadingCancelled(loadingInfo.uri, loadingInfo.imageAware.getWrappedView());
            }
        };
        runTask(r, false, handler, engine);
    }


    private void fireCancelEvent(final List<ImageServeInfo> loadingInfoList)
    {
        for (ImageServeInfo imageLoadingInfo : loadingInfoList)
            fireCancelEvent(imageLoadingInfo);
    }

    private void waitIfPaused()
    {
        AtomicBoolean pause = engine.getPause();
        if (pause.get())
        {
            synchronized (engine.getPauseLock())
            {
                if (pause.get())
                {
                    log(LOG_WAITING_FOR_RESUME);
                    try
                    {
                        engine.getPauseLock().wait();
                    }
                    catch (InterruptedException e)
                    {
                        L.e(LOG_TASK_INTERRUPTED, "Request number: " + loadingInfoList.size());
                    }
                    log(LOG_RESUME_AFTER_PAUSE);
                }
            }
        }
    }


    private ImageDownloader getDownloader()
    {
        ImageDownloader d;
        if (engine.isNetworkDenied())
        {
            d = networkDeniedDownloader;
        }
        else if (engine.isSlowNetwork())
        {
            d = slowNetworkDownloader;
        }
        else
        {
            d = downloader;
        }
        return d;
    }

    /**
     * @return <b>true</b> - if task is not actual (target ImageAware is collected by GC or the image URI of this task
     * doesn't match to image URI which is actual for current ImageAware at this moment)); <b>false</b> - otherwise
     */
    private boolean isTaskNotActual(ImageAware imageAware, String memoryCacheKey)
    {
        return isViewCollected(imageAware, memoryCacheKey) || isViewReused(imageAware, memoryCacheKey);
    }


    /**
     * @return <b>true</b> - if current ImageAware is reused for displaying another image; <b>false</b> - otherwise
     */
    private boolean isViewReused(ImageAware imageAware, String memoryCacheKey)
    {
        String currentCacheKey = engine.getLoadingUriForView(imageAware);
        // Check whether memory cache key (image URI) for current ImageAware is actual.
        // If ImageAware is reused for another task then current task should be cancelled.
        boolean imageAwareWasReused = !memoryCacheKey.equals(currentCacheKey);
        if (imageAwareWasReused)
        {
            log(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey);
            return true;
        }
        return false;
    }

    /**
     * @return <b>true</b> - if target ImageAware is collected by GC; <b>false</b> - otherwise
     */
    private boolean isViewCollected(ImageAware imageAware, String memoryCacheKey)
    {
        if (imageAware.isCollected())
        {
            log(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey);
            return true;
        }
        return false;
    }


    /**
     * @return <b>true</b> - if current task was interrupted; <b>false</b> - otherwise
     */
    private boolean isTaskInterrupted(String memoryCacheKey)
    {
        if (Thread.interrupted())
        {
            log(LOG_TASK_INTERRUPTED, memoryCacheKey);
            return true;
        }
        return false;
    }

    private void log(String message, Object... args)
    {
        if (writeLogs) L.d(message, args);
    }

    static void runTask(Runnable r, boolean sync, Handler handler, ImageLoaderEngine engine)
    {
        if (sync)
        {
            r.run();
        }
        else if (handler == null)
        {
            engine.fireCallback(r);
        }
        else
        {
            handler.post(r);
        }
    }

    @Override
    public boolean onBytesCopied(final int current, final int total)
    {
        //TODO implement progress update per image
        return true;
    }
}
