# Android Camera Api2 / CameraX YUV to ByteBuffer

![](https://github.com/gordinmitya/yuv2buf/workflows/Android%20CI/badge.svg)

**Motivation:**

When you're attempting to get an `Image` from `ImageReader` or `ImageAnalysis.Analyzer` you actually get 3 separate `ByteBuffers` which you can't pass to further processing. You have to merge them but that also is not easy because they are full of row and pixel strides.

**Solution**:

This library carefully merges 3 buffers into one with respect to all strides. As a result, you receive YUV type (`NV21` or `YUV_420`) and `ByteBuffer` to pass into OpenCV or a neural network framework.

The whole library is a single file, you can just copy [Yuv.java](yuv2buf/src/main/java/ru/gordinmitya/yuv2buf/Yuv.java) into your project.

## TODO

- [x] make the library;
- [x] write unit tests;
- [x] add RenderScript example;
- [ ] publish to jcenter;
- [ ] add OpenCV example;
- [ ] add MNN example;
- [ ] add TFLite example;
- [ ] support format conversions.