package com.nostra13.universalimageloader.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final String LOG_DELAY_BEFORE_LOADING = "Delay %d ms before loading...  [%s]";

    private static final String LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]";

    private static final String LOG_WAITING_FOR_IMAGE_LOADED = "Image already is loading. Waiting... [%s]";

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

    private final boolean writeLogs;

    private String contentType;


    private LoadedFrom loadedFrom = LoadedFrom.NETWORK;

    public LoadAndDisplayMultiImageTask(List<ImageServeInfo> loadingInfoList, ImageLoaderEngine engine)
    {
        this.loadingInfoList = loadingInfoList;
        this.engine = engine;

        this.configuration = engine.configuration;
        this.downloader = configuration.downloader;
        this.networkDeniedDownloader = configuration.networkDeniedDownloader;
        this.slowNetworkDownloader = configuration.slowNetworkDownloader;
        this.decoder = configuration.decoder;
        this.writeLogs = configuration.writeLogs;

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

            //Check if image with this uri is already being downloaded
            ReentrantLock loadFromUriLock = loadingInfo.loadFromUriLock;
            log(LOG_START_DISPLAY_IMAGE_TASK);
            if (loadFromUriLock.isLocked())
            {
                log(LOG_WAITING_FOR_IMAGE_LOADED);
                continue;
            }

            loadFromUriLock.lock();

            //Check if image exists on disc or in memory
            Bitmap bmp;
            try
            {

                if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey)) continue;
                if (isTaskInterrupted()) throw new TaskCancelledException();

                bmp = configuration.memoryCache.get(memoryCacheKey);
                if (bmp == null)
                {
                    bmp = tryLoadBitmapFromDisk(loadingInfo);
                    if (bmp == null) continue;

                    if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey)) continue;
                    if (isTaskInterrupted())
                        if (loadingInfo.options.shouldPreProcess())
                        {
                            log(LOG_PREPROCESS_IMAGE);
                            bmp = loadingInfo.options.getPreProcessor().process(bmp);
                            if (bmp == null)
                            {
                                L.e(ERROR_PRE_PROCESSOR_NULL, memoryCacheKey);
                            }
                        }

                    if (bmp != null && loadingInfo.options.isCacheInMemory())
                    {
                        log(LOG_CACHE_IMAGE_IN_MEMORY);
                        configuration.memoryCache.put(memoryCacheKey, bmp);
                    }
                }
                else
                {
                    loadedFrom = LoadedFrom.MEMORY_CACHE;
                    log(LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING);
                }

                if (bmp != null && loadingInfo.options.shouldPostProcess())
                {
                    log(LOG_POSTPROCESS_IMAGE);
                    bmp = loadingInfo.options.getPostProcessor().process(bmp);
                    if (bmp == null)
                    {
                        L.e(ERROR_POST_PROCESSOR_NULL, memoryCacheKey);
                    }
                }


                if (isTaskNotActual(loadingInfo.imageAware, memoryCacheKey)) continue;
                if (isTaskInterrupted()) throw new TaskCancelledException();

                //Bitmap has been found, display it and remove from list
                if (bmp != null)
                {
                    displayBitmapTask(bmp, loadingInfo);
                    iterator.remove();
                    loadFromUriLock.unlock();
                }

            }
            catch (TaskCancelledException e)
            {
                fireCancelEvent(loadingInfo);
                return;
            }
        }

        //Matching map is used for matching image with image request
        final SparseArray<ImageServeInfo> matchingMap = new SparseArray<ImageServeInfo>();
        populateMatchingMap(loadingInfoList, matchingMap);

        try
        {
            String imageRequests = convertToJsonString(loadingInfoList);
            String hashString = StringUtils.sha256(ACCEPT_HEADER + "#" + imageRequests);
            downloadImages(IMAGE_URL + "?hash=" + URLEncoder.encode(hashString, "UTF-8"), imageRequests, matchingMap);
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

    }


    void populateMatchingMap(List<ImageServeInfo> loadingInfoList, SparseArray<ImageServeInfo> matchingMap)
    {
        for (ImageServeInfo loadingInfo : loadingInfoList)
            matchingMap.put(loadingInfo.imageServeParams.hashCode(), loadingInfo);
    }

    private String convertToJsonString(List<ImageServeInfo> loadingInfoList) throws JSONException
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
        for (String key : map.keySet())
        {
            Map<String, String> params = map.get(key);
            JSONObject object = new JSONObject();
            for (String paramName : params.keySet())
                object.put(paramName, params.get(paramName));

            array.put(object);
        }

        listObject.put("list", array);

        return listObject.toString();
    }


    private void displayBitmapTask(Bitmap bitmap, ImageServeInfo loadingInfo)
    {
        DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bitmap, loadingInfo, engine, loadedFrom);
        displayBitmapTask.setLoggingEnabled(writeLogs);
        runTask(displayBitmapTask, loadingInfo.options.isSyncLoading(), loadingInfo.options.getHandler(), engine);
    }

    private Bitmap tryLoadBitmapFromDisk(ImageServeInfo loadingInfo) throws TaskCancelledException
    {
        File imageFile = configuration.diskCache.get(loadingInfo.uri);

        Bitmap bitmap = null;
        try
        {
            String cacheFileUri = ImageDownloader.Scheme.FILE.wrap(imageFile.getAbsolutePath());
            if (imageFile.exists())
            {
                log(LOG_LOAD_IMAGE_FROM_DISC_CACHE);
                loadedFrom = LoadedFrom.DISC_CACHE;

                if (!isTaskNotActual(loadingInfo.imageAware, loadingInfo.memoryCacheKey))
                    bitmap = decodeImage(loadingInfo, cacheFileUri);
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


    private void downloadImages(final String uri, String requestsJson, final SparseArray<ImageServeInfo> matchingMap) throws IOException
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

        MultipartParser parser = new MultipartParser(getDownloader().getStream(uri, downloadExtra), contentType);
        List<BodyPart> bodyParts = parser.parse();
        for (BodyPart bodyPart : bodyParts)
        {
            Map<String, String> perImageParams = new TreeMap<String, String>();

            InternetHeaders headers = bodyPart.getHeaders();
            String[] contentType = headers.getHeader("Content-Type");
            if (contentType == null)
                continue;

            String[] params = contentType[0].split(";");
            for (String param : params)
            {
                String[] paramNameValue = param.split("=");
                if (paramNameValue.length == 1)
                {
                    String imageFormat = paramNameValue[0].split("/")[1];
                    perImageParams.put("out", imageFormat);
                }
                else
                {
                    perImageParams.put(paramNameValue[0], paramNameValue[1]);
                }
            }

            displayImage(bodyPart.getData(), matchingMap.get(perImageParams.hashCode()));
        }
    }

    void displayImage(byte[] imageBinary, ImageServeInfo loadingInfo) throws IOException
    {
        File imageFile = configuration.diskCache.get(loadingInfo.uri);
        String cacheFileUri = ImageDownloader.Scheme.FILE.wrap(imageFile.getAbsolutePath());

        boolean cachedToDisc;
        try
        {
            cachedToDisc = loadingInfo.options.isCacheOnDisk() && tryCacheImageOnDisk(loadingInfo, imageBinary);
        }
        catch (TaskCancelledException e)
        {
            fireCancelEvent(loadingInfoList);
            return;
        }

        if (isTaskNotActual(loadingInfo.imageAware, loadingInfo.memoryCacheKey)) return;

        //If disc cache enabled then load from cache otherwise directly from byte[]
        Bitmap bitmap;
        if (cachedToDisc)
            bitmap = decodeImage(loadingInfo, cacheFileUri);
        else
            bitmap = decodeImage(loadingInfo, imageBinary);

        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0)
        {
            fireFailEvent(FailReason.FailType.DECODING_ERROR, null, loadingInfo);
        }

        DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bitmap, loadingInfo, engine, loadedFrom);
        displayBitmapTask.setLoggingEnabled(writeLogs);
        runTask(displayBitmapTask, loadingInfo.options.isSyncLoading(), new Handler(Looper.getMainLooper()), engine);
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
        log(LOG_CACHE_IMAGE_ON_DISC);

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
                    log(LOG_RESIZE_CACHED_IMAGE_FILE);
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
                log(LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK);
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
        if (loadingInfo.options.isSyncLoading() || isTaskInterrupted() || isTaskNotActual(loadingInfo.imageAware, loadingInfo.memoryCacheKey))
            return;
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
        runTask(r, false, loadingInfo.options.getHandler(), engine);
    }

    private void fireCancelEvent(final ImageServeInfo loadingInfo)
    {
        if (loadingInfo.options.isSyncLoading() || isTaskInterrupted()) return;
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                loadingInfo.listener.onLoadingCancelled(loadingInfo.uri, loadingInfo.imageAware.getWrappedView());
            }
        };
        runTask(r, false, loadingInfo.options.getHandler(), engine);
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
        return isViewCollected(imageAware) || isViewReused(imageAware, memoryCacheKey);
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
            log(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED);
            return true;
        }
        return false;
    }

    /**
     * @return <b>true</b> - if target ImageAware is collected by GC; <b>false</b> - otherwise
     */
    private boolean isViewCollected(ImageAware imageAware)
    {
        if (imageAware.isCollected())
        {
            log(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED);
            return true;
        }
        return false;
    }


    /**
     * @return <b>true</b> - if current task was interrupted; <b>false</b> - otherwise
     */
    private boolean isTaskInterrupted()
    {
        if (Thread.interrupted())
        {
            log(LOG_TASK_INTERRUPTED);
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
