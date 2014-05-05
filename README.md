Robust image loading library based on [Universal Image Loader](https://github.com/nostra13/Android-Universal-Image-Loader) with support for multipart downloading of images.

This project aims to provide a reusable instrument for asynchronous image loading, caching and displaying. It is originally based on [Fedor Vlasov's project](https://github.com/thest1/LazyList) and has been vastly refactored and improved since then.

## Features
 * Multithread image loading
 * Possibility of wide tuning ImageLoader's configuration (thread executors, downloader, decoder, memory and disc cache, display image options, and others)
 * Possibility of image caching in memory and/or on device's file system (or SD card)
 * Possibility to "listen" loading process
 * Possibility to customize every display image call with separated options
 * Widget support

Android 2.0+ support



### User Support
 1. Look into **[Useful Info](https://github.com/nostra13/Android-Universal-Image-Loader#useful-info)**
 2. Search problem solution on **[StackOverFlow](http://stackoverflow.com/questions/tagged/universal-image-loader)**
 3. Ask your own question on **[StackOverFlow](http://stackoverflow.com/questions/tagged/universal-image-loader)**.<br />
    Be sure to mention following information in your question:
   - UIL version (e.g. 1.9.1)
   - Android version tested on (e.g. 2.1)
   - your configuration (`ImageLoaderConfiguration`)
   - display options (`DisplayImageOptions`)
   - `getView()` method code of your adapter (if you use it)
   - XML layout of your ImageView you load image into

**Bugs** and **feature requests** put **[here](https://github.com/nostra13/Android-Universal-Image-Loader/issues/new)**.<br />
If you have some **issues on migration** to newer library version - be sure to ask for help **[here](https://github.com/nostra13/Android-Universal-Image-Loader/issues/169)**

## Quick Setup


#### 1. Maven dependency:
``` xml
<dependency>
	<groupId>com.iddiction.imageserve</groupId>
	<artifactId>image-serve</artifactId>
	<version>1.0</version>
</dependency>
```

#### 2. Android Manifest
``` xml
<manifest>
	<uses-permission android:name="android.permission.INTERNET" />
	<!-- Include next permission if you want to allow UIL to cache images on SD card -->
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	...
	<application android:name="MyApplication">
		...
	</application>
</manifest>
```

#### 3. Application class
``` java
public class MyApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		// Create global configuration and initialize ImageLoader with this configuration
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
			...
			.build();
		ImageServe.getInstance().init(config);
	}
}
```

## Configuration and Display Options

 * ImageServe **Configuration (`ImageLoaderConfiguration`) is global** for application.
 * **Display Options (`DisplayImageOptions`) are local** for every display task (`ImageServe.displayImage(...)`).

### Configuration
All options in Configuration builder are optional. Use only those you really want to customize.<br />*See default values for config options in Java docs for every option.*
``` java
// DON'T COPY THIS CODE TO YOUR PROJECT! This is just example of ALL options using.
File cacheDir = StorageUtils.getCacheDirectory(context);
ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
		.memoryCacheExtraOptions(480, 800) // default = device screen dimensions
		.discCacheExtraOptions(480, 800, CompressFormat.JPEG, 75, null)
		.taskExecutor(...)
		.taskExecutorForCachedImages(...)
		.threadPoolSize(3) // default
		.threadPriority(Thread.NORM_PRIORITY - 1) // default
		.tasksProcessingOrder(QueueProcessingType.FIFO) // default
		.denyCacheImageMultipleSizesInMemory()
		.memoryCache(new LruMemoryCache(2 * 1024 * 1024))
		.memoryCacheSize(2 * 1024 * 1024)
		.memoryCacheSizePercentage(13) // default
		.discCache(new UnlimitedDiscCache(cacheDir)) // default
		.discCacheSize(50 * 1024 * 1024)
		.discCacheFileCount(100)
		.discCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
		.imageDownloader(new BaseImageDownloader(context)) // default
		.imageDecoder(new BaseImageDecoder()) // default
		.defaultDisplayImageOptions(DisplayImageOptions.createSimple()) // default
		.writeDebugLogs()
		.build();
```

### Display Options
Display Options can be applied to every display task (`ImageServe.displayImage(...)` call).

**Note:** If Display Options wasn't passed to `ImageServe.displayImage(...)`method then default Display Options from configuration (`ImageLoaderConfiguration.defaultDisplayImageOptions(...)`) will be used.
``` java
// DON'T COPY THIS CODE TO YOUR PROJECT! This is just example of ALL options using.
DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.ic_stub) // resource or drawable
		.showImageForEmptyUri(R.drawable.ic_empty) // resource or drawable
		.showImageOnFail(R.drawable.ic_error) // resource or drawable
		.resetViewBeforeLoading(false)  // default
		.delayBeforeLoading(1000)
		.cacheInMemory(false) // default
		.cacheOnDisc(false) // default
		.preProcessor(...)
		.postProcessor(...)
		.extraForDownloader(...)
		.considerExifParams(false) // default
		.imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2) // default
		.bitmapConfig(Bitmap.Config.ARGB_8888) // default
		.decodingOptions(...)
		.displayer(new SimpleBitmapDisplayer()) // default
		.handler(new Handler()) // default
		.build();
```

## ImageServe

`ImageServe` is extended class of `ImageLoader` that provides serving of images through requests. The suggested usage is through `ImageRequest (LoadImageRequest and DisplayImageRequest)`, however images can also be loaded without using `ImageRequest`. 

Use `LoadImageRequest` for loading image

Use `DisplayImageRequest` for loading and displaying image

Set `DisplayImageOptions` and `LoadingImageListener` through `ImageRequest`. `ImageRequst` also accepts `blur`, `quality`, `recapp` parameters which enable additional settings for returned image through ImageServe API (https://github.com/Iddiction/backend/wiki/ImageServe-API). `ImageFormat (PNG, JPEG, WEBP)` can also be provided set on `ImageRequest`. That way specific format will be requested, otherwise `ImageFormat` is calculated based on Android API version and flag whether image can be transparent or not `ImageRequest.setTransparent`.

Difference between `LoadImageRequest` and `DisplayImageRequest` is that first one accepts the height, width and scale type `ViewScaleType (CROP and FIT_INSIDE), the second one accepts `ImageView` and gets this parameters from view.

`DisplayImageRequest` available params
```java
ImageRequest request = new DisplayImageRequest(uri, imageview); / new LoadImageRequest(url, width, height, ViewScaleType)
request.setBlur(10); // default -1
request.setImageQuality(70); // default -1
request.setTransparent(boolean) // default false
request.setLoadingListener(ImageLoadingListener) // default empty listener
request.setDisplayImageOptions(DisplayImageOptions) // default DisplayImageOptions from default configuration
request.setImageFormat(ImageFormat) //default null
```

**NOTE:** Default values are ignored when constructing image uri for ImageServe API (https://github.com/Iddiction/backend/wiki/ImageServe-API)


## Usage

### Acceptable URIs examples
``` java
String imageUri = "http://site.com/image.png"; // from Web
String imageUri = "file:///mnt/sdcard/image.png"; // from SD card
String imageUri = "content://media/external/audio/albumart/13"; // from content provider
String imageUri = "assets://image.png"; // from assets
String imageUri = "drawable://" + R.drawable.image; // from drawables (only images, non-9patch)
```
**NOTE:** Use `drawable://` only if you really need it! Always **consider the native way** to load drawables - `ImageView.setImageResource(...)` instead of using of `ImageServe`.

### Usage with `ImageRequest`

#### `DisplayImageRequest`
```java
DisplayImageRequest request = new DisplayImageRequest(uri, imageview);
ImageServe.getInstance().displayImage(request);
```

#### `LoadImageRequest`
``` java
LoadImageRequest request = new LoadImageRequest(uri, 100, 100, ViewScaleType.CROP);
ImageServe.getInstance().loadImage(request)
```

### Multipart loading
``` java
List<ImageRequest> list = new ArrayList<ImageRequest>();
list.add(DisplayImageRequest);
list.add(LoadImageRequest);

ImageServe.getInstance().serveImages(list, loadSynchroniously);
```

**NOTE:** `ImageServe.serveImages` will download all images in requests in one response and then handle every request as single with all the parameters that were set. If `DisplayImageRequest` the it will display it, if `LoadImageRequest` then it will only load it. `ImageLoadingListener` will be called for every request.

### Usage without `ImageRequest`

### Simple
``` java
// Load image, decode it to Bitmap and display Bitmap in ImageView (or any other view 
//	which implements ImageAware interface)
imageServe.displayImage(imageUri, imageView);
```
``` java
// Load image, decode it to Bitmap and return Bitmap to callback
imageServe.loadImage(imageUri, new SimpleImageLoadingListener() {
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		// Do whatever you want with Bitmap
	}
});
```
``` java
// Load image, decode it to Bitmap and return Bitmap synchronously
Bitmap bmp = imageServe.loadImageSync(imageUri);
```

### Complete
``` java
// Load image, decode it to Bitmap and display Bitmap in ImageView (or any other view 
//	which implements ImageAware interface)
imageServe.displayImage(imageUri, imageView, displayOptions, new ImageLoadingListener() {
	@Override
	public void onLoadingStarted(String imageUri, View view) {
		...
	}
	@Override
	public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
		...
	}
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		...
	}
	@Override
	public void onLoadingCancelled(String imageUri, View view) {
		...
	}
}, new ImageLoadingProgressListener() {
	@Override
	public void onProgressUpdate(String imageUri, View view, int current, int total) {
		...
	}
});
```
``` java
// Load image, decode it to Bitmap and return Bitmap to callback
ImageSize targetSize = new ImageSize(120, 80); // result Bitmap will be fit to this size
imageServe.loadImage(imageUri, targetSize, displayOptions, new SimpleImageLoadingListener() {
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		// Do whatever you want with Bitmap
	}
});
```
``` java
// Load image, decode it to Bitmap and return Bitmap synchronously
ImageSize targetSize = new ImageSize(120, 80); // result Bitmap will be fit to this size
Bitmap bmp = imageServe.loadImageSync(imageUri, targetSize, displayOptions);
```

### ImageLoader Helpers
Other useful methods and classes to consider.
<pre>
ImageServe |
			| - getMemoryCache()
			| - clearMemoryCache()
			| - getDiscCache()
			| - clearDiscCache()
			| - denyNetworkDownloads(boolean)
			| - handleSlowNetwork(boolean)
			| - pause()
			| - resume()
			| - stop()
			| - destroy()
			| - getLoadingUriForView(ImageView)
			| - getLoadingUriForView(ImageAware)
			| - cancelDisplayTask(ImageView)
			| - cancelDisplayTask(ImageAware)

MemoryCacheUtil |
				| - findCachedBitmapsForImageUri(...)
				| - findCacheKeysForImageUri(...)
				| - removeFromCache(...)

DiscCacheUtil |
			  | - findInCache(...)
			  | - removeFromCache(...)

StorageUtils |
			 | - getCacheDirectory(Context)
			 | - getIndividualCacheDirectory(Context)
			 | - getOwnCacheDirectory(Context, String)

PauseOnScrollListener

ImageAware |
		   | - getWidth()
		   | - getHeight()
		   | - getScaleType()
		   | - getWrappedView()
		   | - isCollected()
		   | - getId()
		   | - setImageDrawable(Drawable)
		   | - setImageBitmap(Bitmap)
</pre>

## Useful Info
1. **Caching is NOT enabled by default.** If you want loaded images will be cached in memory and/or on disc then you should enable caching in DisplayImageOptions this way:
``` java
// Create default options which will be used for every 
//  displayImage(...) call if no options will be passed to this method
DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
			...
            .cacheInMemory(true)
            .cacheOnDisc(true)
            ...
            .build();
ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
            ...
            .defaultDisplayImageOptions(defaultOptions)
            ...
            .build();
ImageServe.getInstance().init(config); // Do it on Application start
```
``` java
// Then later, when you want to display image
ImageServe.getInstance().displayImage(imageUrl, imageView); // Default options will be used
```
or this way:
``` java
DisplayImageOptions options = new DisplayImageOptions.Builder()
			...
            .cacheInMemory(true)
            .cacheOnDisc(true)
            ...
            .build();
ImageServe.getInstance().displayImage(imageUrl, imageView, options); // Incoming options will be used
```

2. If you enabled disc caching then UIL try to cache images on external storage (/sdcard/Android/data/[package_name]/cache). If external storage is not available then images are cached on device's filesystem.
To provide caching on external storage (SD card) add following permission to AndroidManifest.xml:
``` java
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

3. How UIL define Bitmap size needed for exact ImageView? It searches defined parameters:
 * Get actual measured width and height of ImageView
 * Get `android:layout_width` and `android:layout_height` parameters
 * Get `android:maxWidth` and/or `android:maxHeight` parameters
 * Get maximum width and/or height parameters from configuration (`memoryCacheExtraOptions(int, int)` option)
 * Get width and/or height of device screen

 So **try to set** `android:layout_width`|`android:layout_height` or `android:maxWidth`|`android:maxHeight` parameters for ImageView if you know approximate maximum size of it. It will help correctly compute Bitmap size needed for this view and **save memory**.

4. If you often got **OutOfMemoryError** in your app using Universal Image Loader then try next (all of them or several):
 - Reduce thread pool size in configuration (`.threadPoolSize(...)`). 1 - 5 is recommended.
 - Use `.bitmapConfig(Bitmap.Config.RGB_565)` in display options. Bitmaps in RGB_565 consume 2 times less memory than in ARGB_8888.
 - Use `.memoryCache(new WeakMemoryCache())` in configuration or disable caching in memory at all in display options (don't call `.cacheInMemory()`).
 - Use `.imageScaleType(ImageScaleType.IN_SAMPLE_INT)` in display options. Or try `.imageScaleType(ImageScaleType.EXACTLY)`.

5. For memory cache configuration (`ImageLoaderConfiguration.memoryCache(...)`) you can use already prepared implementations.
 * Cache using **only strong** references:
     * `LruMemoryCache` (Least recently used bitmap is deleted when cache size limit is exceeded) - **Used by default**
 * Caches using **weak and strong** references:
     * `UsingFreqLimitedMemoryCache` (Least frequently used bitmap is deleted when cache size limit is exceeded)
     * `LRULimitedMemoryCache` (Least recently used bitmap is deleted when cache size limit is exceeded)
     * `FIFOLimitedMemoryCache` (FIFO rule is used for deletion when cache size limit is exceeded)
     * `LargestLimitedMemoryCache` (The largest bitmap is deleted when cache size limit is exceeded)
     * `LimitedAgeMemoryCache` (Decorator. Cached object is deleted when its age exceeds defined value)
 * Cache using **only weak** references:
     * `WeakMemoryCache` (Unlimited cache)

6. For disc cache configuration (`ImageLoaderConfiguration.discCache(...)`) you can use already prepared implementations:
 * `UnlimitedDiscCache` (The fastest cache, doesn't limit cache size) - **Used by default**
 * `TotalSizeLimitedDiscCache` (Cache limited by total cache size. If cache size exceeds specified limit then file with the most oldest last usage date will be deleted)
 * `FileCountLimitedDiscCache` (Cache limited by file count. If file count in cache directory exceeds specified limit then file with the most oldest last usage date will be deleted. Use it if your cached files are of about the same size.)
 * `LimitedAgeDiscCache` (Size-unlimited cache with limited files' lifetime. If age of cached file exceeds defined limit then it will be deleted from cache.)
 
 **NOTE:** UnlimitedDiscCache is 30%-faster than other limited disc cache implementations.

7. To display bitmap (`DisplayImageOptions.displayer(...)`) you can use already prepared implementations: 
 * `RoundedBitmapDisplayer` (Displays bitmap with rounded corners)
 * `FadeInBitmapDisplayer` (Displays image with "fade in" animation)

8. To avoid list (grid, ...) scrolling lags you can use `PauseOnScrollListener`:
``` java
boolean pauseOnScroll = false; // or true
boolean pauseOnFling = true; // or false
PauseOnScrollListener listener = new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling);
listView.setOnScrollListener(listener);
```

9. If you see in logs some strange supplement at the end of image URL (e.g. `http://anysite.com/images/image.png_230x460`) then it doesn't mean this URL is used in requests. This is just "URL + target size", also this is key for Bitmap in memory cache. This postfix (`_230x460`) is **NOT used in requests**.


## License

Also I'll be grateful if you mention UIL in application UI with string **"Using Universal Image Loader (c) 2011-2014, Sergey Tarasevich"** (e.g. in some "About" section).

    Copyright 2011-2014 Sergey Tarasevich

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
