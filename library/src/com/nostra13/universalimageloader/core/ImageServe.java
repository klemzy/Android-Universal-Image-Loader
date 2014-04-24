package com.nostra13.universalimageloader.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.nostra13.universalimageloader.core.assist.ImageRequest;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.ImageServeUtil;
import com.nostra13.universalimageloader.utils.ImageSizeUtils;
import com.nostra13.universalimageloader.utils.L;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * IntelliJ IDEA.
 * User: Klemen
 * Date: 23. 04. 14
 * Time: 14:25
 */
public class ImageServe extends ImageLoader
{

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

    public void displayMultipleImage(Set<ImageRequest> displayRequests, boolean synchronous)
    {
        checkConfiguration();

        L.i("Display " + displayRequests.size() + " images");

        List<ImageServeInfo> loadingInfoList = new ArrayList<ImageServeInfo>();
        for (ImageRequest request : displayRequests)
        {
            ImageAware imageAware = request.getImageAware();
            ImageLoadingListener listener = request.getLoadingListener();
            DisplayImageOptions options = request.getDisplayImageOptions();
            String uri = ImageServeUtil.getProcessedImageUri(request.getKey(), imageAware.getWidth(), imageAware.getHeight(), request.isTransparent(), request.getParams(), imageAware.getScaleType());

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

                loadingInfoList.add(imageLoadingInfo);
            }
        }

        if (loadingInfoList.size() < 20 && loadingInfoList.size() > 0)
        {
            L.i("Dispatched multi image task with " + loadingInfoList.size() + " images");
            dispatchMultiGetTask(loadingInfoList, synchronous);
        }
        else
        {
            //Only 18 per multi get request should be sent
            List<ImageServeInfo> list = new ArrayList<ImageServeInfo>();
            for (int i = 0; i < loadingInfoList.size(); i++)
            {
                if (i % 18 == 0)
                {
                    if (list.size() > 0)
                    {
                        L.i("Dispatched multi image task with " + list.size() + " images");
                        dispatchMultiGetTask(list, synchronous);
                    }

                    list = new ArrayList<ImageServeInfo>();
                }

                list.add(loadingInfoList.get(i));
            }

            if (list.size() > 0)
            {
                L.i("Dispatched multi image task with " + list.size() + " images");
                dispatchMultiGetTask(list, synchronous);
            }
        }

    }

    private void dispatchMultiGetTask(List<ImageServeInfo> loadingInfoList, boolean synchronous)
    {
        LoadAndDisplayMultiImageTask displayTask = new LoadAndDisplayMultiImageTask(loadingInfoList, engine, defineHandler(synchronous));
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
