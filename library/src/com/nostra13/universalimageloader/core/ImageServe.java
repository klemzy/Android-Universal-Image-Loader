package com.nostra13.universalimageloader.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.nostra13.universalimageloader.core.assist.*;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.ImageServeUtil;
import com.nostra13.universalimageloader.utils.ImageSizeUtils;
import com.nostra13.universalimageloader.utils.L;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 23. 04. 14
 * Time: 14:25
 */
public class ImageServe extends ImageLoader
{

    public static final String TAG = ImageServe.class.getSimpleName();

    protected volatile static ImageServe instance;

    /**
     * Returns singleton class instance
     */
    public static ImageServe getInstance()
    {
        if (instance == null)
        {
            synchronized (ImageLoader.class)
            {
                if (instance == null)
                {
                    instance = new ImageServe();
                }
            }
        }
        return instance;
    }

    protected ImageServe()
    {
    }

    public void serveImages(List<ImageRequest> requests, boolean synchronous)
    {
        List<ImageServeInfo> infoList = new ArrayList<ImageServeInfo>();
        for (ImageRequest imageRequest : requests)
        {
            processImageRequest(infoList, imageRequest);
        }

        processImageServeInfo(infoList, synchronous);
    }

    public void loadImage(LoadImageRequest imageRequest)
    {
        displayImage(imageRequest.getUri(), imageRequest.getImageAware(), imageRequest.getDisplayImageOptions(), imageRequest.getLoadingListener());
    }

    public void displayImage(DisplayImageRequest imageRequest)
    {
        displayImage(imageRequest.getUri(), imageRequest.getImageAware(), imageRequest.getDisplayImageOptions(), imageRequest.getLoadingListener());
    }

    public void cancelRequest(ImageRequest imageRequest)
    {
        cancelDisplayTask(imageRequest.getImageAware());
    }

    public void cancelRequest(List<ImageRequest> requests)
    {
        for (ImageRequest request : requests)
            cancelDisplayTask(request.getImageAware());
    }

    private void processImageRequest(List<ImageServeInfo> serveInfoList, ImageRequest request)
    {
        ImageAware imageAware = request.getImageAware();
        ImageLoadingListener listener = request.getLoadingListener();
        DisplayImageOptions options = request.getDisplayImageOptions();
        String uri = ImageServeUtil.getProcessedImageUri(request.getUri(), imageAware.getWidth(), imageAware.getHeight(), request.isTransparent(), request.getParams(), imageAware.getScaleType());

        if (listener == null)
        {
            listener = emptyListener;
        }
        if (options == null)
        {
            options = configuration.defaultDisplayImageOptions;
        }

        if (TextUtils.isEmpty(uri))
        {
            engine.cancelDisplayTaskFor(imageAware);
            listener.onLoadingStarted(uri, imageAware.getWrappedView());
            if (options.shouldShowImageForEmptyUri())
            {
                imageAware.setImageDrawable(options.getImageForEmptyUri(configuration.resources));
            }
            else
            {
                imageAware.setImageDrawable(null);
            }
            listener.onLoadingComplete(uri, imageAware.getWrappedView(), null, null);
            return;
        }

        ImageSize targetSize = ImageSizeUtils.defineTargetSizeForView(imageAware, configuration.getMaxImageSize());
        String memoryCacheKey = MemoryCacheUtils.generateKey(uri, targetSize);
        engine.prepareDisplayTaskFor(imageAware, memoryCacheKey);

        listener.onLoadingStarted(uri, imageAware.getWrappedView());

        ImageServeInfo imageLoadingInfo = new ImageServeInfo(uri, imageAware, targetSize, memoryCacheKey,
                options, listener, null, engine.getLockForUri(uri), ImageServeUtil.parseImageServeParams(uri));

        Bitmap bmp = configuration.memoryCache.get(memoryCacheKey);
        if (bmp != null && !bmp.isRecycled())
        {
            if (configuration.writeLogs) L.d(LOG_LOAD_IMAGE_FROM_MEMORY_CACHE, memoryCacheKey);

            if (options.shouldPostProcess())
            {

                ProcessAndDisplayImageTask displayTask = new ProcessAndDisplayImageTask(engine, bmp, imageLoadingInfo,
                        defineHandler(options));
                if (options.isSyncLoading())
                {
                    displayTask.run();
                }
                else
                {
                    engine.submit(displayTask);
                }
            }
            else
            {
                options.getDisplayer().display(bmp, imageAware, LoadedFrom.MEMORY_CACHE);
                listener.onLoadingComplete(uri, imageAware.getWrappedView(), bmp, LoadedFrom.MEMORY_CACHE);
            }
        }
        else
        {
            if (options.shouldShowImageOnLoading())
            {
                imageAware.setImageDrawable(options.getImageOnLoading(configuration.resources));
            }
            else if (options.isResetViewBeforeLoading())
            {
                imageAware.setImageDrawable(null);
            }

            serveInfoList.add(imageLoadingInfo);
        }
    }

    private void processImageServeInfo(List<ImageServeInfo> infoList, boolean synchronous)
    {
        if (infoList.size() < 20 && infoList.size() > 0)
        {
            L.i("Dispatched multi image task with " + infoList.size() + " images");
            dispatchMultipartRequest(infoList, synchronous);
        }
        else
        {
            //Only 18 per multi get request should be sent
            List<ImageServeInfo> list = new ArrayList<ImageServeInfo>();
            for (int i = 0; i < infoList.size(); i++)
            {
                if (i % 18 == 0)
                {
                    if (list.size() > 0)
                    {
                        L.i("Dispatched multi image task with " + list.size() + " images");
                        dispatchMultipartRequest(list, synchronous);
                    }

                    list = new ArrayList<ImageServeInfo>();
                }

                list.add(infoList.get(i));
            }

            if (list.size() > 0)
            {
                L.i("Dispatched multi image task with " + list.size() + " images");
                dispatchMultipartRequest(list, synchronous);

            }
        }
    }

    private void dispatchMultipartRequest(List<ImageServeInfo> infoList, boolean synchronous)
    {
        LoadAndDisplayMultiImageTask displayTask = new LoadAndDisplayMultiImageTask(
                infoList,
                engine,
                engine.configuration,
                configuration.downloader,
                configuration.networkDeniedDownloader,
                configuration.slowNetworkDownloader,
                configuration.decoder,
                configuration.writeLogs,
                defineHandler(synchronous));

        if (synchronous)
        {
            displayTask.run();
        }
        else
        {
            engine.submit(displayTask);
        }
    }

    protected static Handler defineHandler(boolean synchronous)
    {
        Handler handler = null;
        if (!synchronous && Looper.myLooper() == Looper.getMainLooper())
        {
            handler = new Handler(Looper.getMainLooper());
        }

        return handler;
    }
}
