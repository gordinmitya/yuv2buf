# Android Camera Api2 / CameraX YUV to ByteBuffer

![](https://github.com/gordinmitya/yuv2buf/workflows/Android%20CI/badge.svg)

**Motivation:**

When you're attempting to get an `Image` from `ImageReader` or `ImageAnalysis.Analyzer` you actually get 3 separate `ByteBuffers` which you can't pass to further processing. You have to merge them but that also is not easy because they are full of row and pixel strides.

**Solution**:

This library carefully merges 3 buffers into one with respect to all strides. As a result, you receive YUV type (`NV21` or `YUV_420`) and `ByteBuffer` to pass into OpenCV or a neural network framework.

The whole library is a single file, you can just copy [Yuv.java](yuv2buf/src/main/java/ru/gordinmitya/yuv2buf/Yuv.java) into your project.

**Alternatives**

1. Switch back to CameraApi 1 [(some trade offs)](https://github.com/tensorflow/tensorflow/issues/22620);
2. Capture image from camera directly to [RenderScript Allocation.getSurface()](https://developer.android.com/reference/android/renderscript/Allocation#getSurface());
3. Manipulate pixels manually as it has done in TFLite demo [ImageUtils](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/env/ImageUtils.java#L161).
    However, even with [C++ implementation](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/jni/yuv2rgb.cc#L61)
    it's ridiculously slow. ~50ms for image about 1280x720 on Snapdragon 855;


## TODO

- [x] make the library;
- [x] write unit tests;
- [x] add RenderScript example;
- [x] add OpenCV example;
- [ ] publish to jcenter;
- [ ] add MNN example;
- [ ] add TFLite example;
- [ ] support format conversions.