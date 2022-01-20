# Android Camera Api2 / CameraX YUV to ByteBuffer

![](https://github.com/gordinmitya/yuv2buf/workflows/Android%20CI/badge.svg)

## OUTDATED!
7 years passed since CameraApi 2 was introduced in Android 5.
So Google decided that it's time to make convenient way to get RGB images from it. 

ImageAnalysis from CaemraX now supports setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) [developer.android.com](https://developer.android.com/training/camerax/analyze#create-analyzer)

Hovewer if you do not use CameraX for some reason - welcome!

**Motivation:**

When you're attempting to get an `Image` from `ImageReader` or `ImageAnalysis.Analyzer` you actually get 3 separate `ByteBuffers` which you can't pass to further processing. You have to merge them but that also is not easy because they are full of row and pixel strides.

**Solution**:

This library carefully merges 3 buffers into one with respect to all strides. As a result, you receive [YUV type](https://user-images.githubusercontent.com/9286092/89119601-4f6f8100-d4b8-11ea-9a51-2765f7e513c2.jpg) (`NV21` or `I420`) and `ByteBuffer` to pass into OpenCV or a neural network framework.

The solution was tested with CameraX, CameraApi 2, and MediaCodec (for video files).

The whole library is a single file, you can just copy [Yuv.java](yuv2buf/src/main/java/ru/gordinmitya/yuv2buf/Yuv.java) into your project.

**Usage**

```kotlin
private var reuseBuffer: ByteBuffer? = null

fun convert(image: ImageProxy): Pair<Bitmap, Long> {

    // val converted = Yuv.toBuffer(image)
    // OR pass existing DirectBuffer for reuse
    val converted = Yuv.toBuffer(image, reuseBuffer)
    reuseBuffer = converted.buffer

    val format = when (converted.type) {
        ImageFormat.YUV_420_888 -> Imgproc.COLOR_YUV2RGB_I420
        ImageFormat.NV21 -> Imgproc.COLOR_YUV2RGB_NV21
        else -> throw IllegalArgumentException()
    }

    // process converted.buffer ByteBuffer with one of converters
}
```

**About android image formats**

In android documentation [ImageFormat.YUV_420_888](https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888) is stated as 'generic YCbCr format', so the actual format depends on u and v order and are these planes interleaved or not (pixelStride=1 or 2).

Most of the time you will get [NV21 format](https://developer.android.com/reference/android/graphics/ImageFormat#NV21) (just [as in Camera1 api](https://developer.android.com/reference/android/hardware/Camera.Parameters#setPreviewFormat(int))). 

And you even may start wondering is there a phone with I420 format (u and v pixelStride==1) – Yes! There is one. Blackview BV6000S Android 7.0. [Screenshot](https://user-images.githubusercontent.com/9286092/127649832-90aab826-64d5-439e-9911-18d0f857d25d.png).
It looks like it's the only one. Other models of Blackview I was able to find in local sellers used NV21. 

**Converters** 

1. [RenderScriptConverter.kt](app/src/main/java/ru/gordinmitya/yuv2buf_demo/RenderScriptConverter.kt)  - built-in, no additional libraries required. Uses Bitmap for rotation.
2. [OpenCVRoteterter.kt](app/src/main/java/ru/gordinmitya/yuv2buf_demo/OpenCVRoteterter.kt) - preform rotation on yuv image and then converts color. **The fastest in total time**.
3. [OpenCVConverter.kt](app/src/main/java/ru/gordinmitya/yuv2buf_demo/OpenCVConverter.kt) - the fastest color conversion without rotation. Rotation of a rgb image is slightly harder. 
5. [MNNConverter.kt](app/src/main/java/ru/gordinmitya/yuv2buf_demo/MNNConverter.kt) - if your goal is futher processing with neural network. Conversion and rotation performed in single operation.

PS: If your goal is to get Mat you may consider this method from [OpenCV](https://github.com/opencv/opencv/blob/master/modules/java/generator/android-21/java/org/opencv/android/JavaCamera2View.java#L344).

**Benchmark**

<img width="320" src="https://user-images.githubusercontent.com/9286092/89957124-537d6a80-dc3f-11ea-99d5-0e22301db688.jpg" />

Snapdragon 855 (Xiaomi Mi 9T Pro). Image resolution 480x640.
|        | OpenCV (YUV rotate)  | OpenCV (RGB rotate) | RenderScript | MNN   | 
| :-     | :-:                  |       :-:           | :-:          | :-:   |
| color  | ~1ms                 | ~1.6ms              | ~2.2ms       | ~21ms |
| rotate | ~3.5ms               | ~2.8ms              | ~16.3ms      | NA (included in color)    |

**Alternatives**

1. (For OpenCV users) Copy private method from OpenCV camera implementation: [JavaCamera2View, Mat rgba()](https://github.com/opencv/opencv/blob/master/modules/java/generator/android-21/java/org/opencv/android/JavaCamera2View.java#L344).
2. Capture image from camera directly to [RenderScript Allocation.getSurface()](https://developer.android.com/reference/android/renderscript/Allocation#getSurface());
3. RenderScript implementation in [android/camera-samples](https://github.com/android/camera-samples/blob/3730442b49189f76a1083a98f3acf3f5f09222a3/CameraUtils/lib/src/main/java/com/example/android/camera/utils/YuvToRgbConverter.kt) repo.
4. Switch back to CameraApi 1 [(some trade-offs)](https://github.com/tensorflow/tensorflow/issues/22620);
5. Manipulate pixels manually as it has done in TFLite demo [ImageUtils](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/env/ImageUtils.java#L161).
    However, even with [C++ implementation](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/jni/yuv2rgb.cc#L61)
    it's ridiculously slow. ~50ms for image about 1280x720 on Snapdragon 855;

## TODO

- [x] make the library;
- [x] write unit tests;
- [x] add RenderScript example;
- [x] add OpenCV example;
- [x] add MNN example;
- [ ] publish to GooglePlay;
- [ ] publish to mavenCentral();
- [ ] add TFLite example.
