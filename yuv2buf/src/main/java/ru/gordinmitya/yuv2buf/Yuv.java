package ru.gordinmitya.yuv2buf;

import android.media.Image;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

abstract public class Yuv {
    /*
        Public types.
     */
    public enum Type {
        YUV_NV21,
        YUV_420
    }

    public static class Converted {
        public final Type type;
        public final ByteBuffer buffer;

        private Converted(Type type, ByteBuffer buffer) {
            this.type = type;
            this.buffer = buffer;
        }
    }

    /*
        Api.
     */
    public static Type detectType(Image image) {
        return detectType(wrap(image));
    }

    public static Converted toBuffer(Image image) {
        return toBuffer(image, null);
    }

    public static Converted toBuffer(Image image, ByteBuffer reuse) {
        return toBuffer(wrap(image), reuse);
    }

    private static ImageWrapper wrap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        PlaneWrapper y = wrap(width, height, planes[0]);
        PlaneWrapper u = wrap(width / 2, height / 2, planes[1]);
        PlaneWrapper v = wrap(width / 2, height / 2, planes[2]);
        return new ImageWrapper(width, height, y, u, v);
    }

    private static PlaneWrapper wrap(int width, int height, Image.Plane plane) {
        return new PlaneWrapper(width, height, plane.getBuffer(), plane.getRowStride(), plane.getPixelStride());
    }

    /*
        CameraX api. If you don't need it – just comment lines below.
     */
    public static Type detectType(ImageProxy image) {
        return detectType(wrap(image));
    }

    public static Converted toBuffer(ImageProxy image) {
        return toBuffer(image, null);
    }

    public static Converted toBuffer(ImageProxy image, ByteBuffer reuse) {
        return toBuffer(wrap(image), reuse);
    }

    private static ImageWrapper wrap(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        PlaneWrapper y = wrap(width, height, planes[0]);
        PlaneWrapper u = wrap(width / 2, height / 2, planes[1]);
        PlaneWrapper v = wrap(width / 2, height / 2, planes[2]);
        return new ImageWrapper(width, height, y, u, v);
    }

    private static PlaneWrapper wrap(int width, int height, ImageProxy.PlaneProxy plane) {
        return new PlaneWrapper(width, height, plane.getBuffer(), plane.getRowStride(), plane.getPixelStride());
    }
    // End of CameraX api.

    /*
        Implementation
     */

    /*
     other pixelStride are not possible
     @see #ImageWrapper.checkFormat()
     */
    static Type detectType(ImageWrapper image) {
        if (image.u.pixelStride == 1)
            return Type.YUV_420;
        else
            return Type.YUV_NV21;
    }

    static Converted toBuffer(ImageWrapper image, ByteBuffer reuse) {
        Type type = detectType(image);
        ByteBuffer output = prepareOutput(image, reuse);
        removePadding(image, type, output);
        return new Converted(type, output);
    }

    private static ByteBuffer prepareOutput(ImageWrapper image, ByteBuffer reuse) {
        int sizeOutput = image.width * image.height * 3 / 2;
        ByteBuffer output;
        if (reuse == null
                || reuse.capacity() < sizeOutput
                || reuse.isReadOnly()
                || !reuse.isDirect()) {
            output = ByteBuffer.allocateDirect(sizeOutput);
        } else
            output = reuse;
        output.rewind();
        return output;
    }

    // Input buffers are always direct as described in
    // https://developer.android.com/reference/android/media/Image.Plane#getBuffer()
    private static void removePadding(ImageWrapper image, Type type, ByteBuffer output) {
        int sizeLuma = image.y.width * image.y.height;
        int sizeChroma = image.u.width * image.u.height;

        if (image.y.rowStride > image.y.width) {
            removePaddingCompact(image.y, output, 0);
        } else {
            output.position(0);
            output.put(image.y.buffer);
        }

        if (type.equals(Type.YUV_420)) {
            if (image.u.rowStride > image.u.width) {
                removePaddingCompact(image.u, output, sizeLuma);
                removePaddingCompact(image.v, output, sizeLuma + sizeChroma);
            } else {
                output.position(sizeLuma);
                output.put(image.u.buffer);
                output.position(sizeLuma + sizeChroma);
                output.put(image.v.buffer);
            }
        } else {
            if (image.u.rowStride > image.u.width * 2) {
                removePaddingNotCompact(image, output, sizeLuma);
            } else {
                output.position(sizeLuma);
                output.put(image.u.buffer);
                byte lastOne = image.v.buffer.get(image.v.buffer.capacity() - 1);
                output.put(lastOne);
            }
        }
        output.rewind();
    }

    private static void removePaddingCompact(PlaneWrapper plane, ByteBuffer dst, int offset) {
        if (plane.pixelStride != 1)
            throw new IllegalArgumentException("removePaddingCompact must be used with pixelStride == 1");

        ByteBuffer src = plane.buffer;
        int rowStride = plane.rowStride;
        ByteBuffer row;
        dst.position(offset);
        for (int i = 0; i < plane.height; i++) {
            row = clipBuffer(src, i * rowStride, plane.width);
            dst.put(row);
        }
    }

    private static void removePaddingNotCompact(ImageWrapper image, ByteBuffer dst, int offset) {
        if (image.u.pixelStride != 2)
            throw new IllegalArgumentException("removePaddingNotCompact must be used with pixelStride == 2");

        int width = image.u.width;
        int height = image.u.height;
        int rowStride = image.u.rowStride;
        ByteBuffer row;
        dst.position(offset);
        for (int i = 0; i < height - 1; i++) {
            row = clipBuffer(image.u.buffer, i * rowStride, width * 2);
            dst.put(row);
        }
        row = clipBuffer(image.v.buffer, (height - 1) * rowStride - 1, width * 2);
        dst.put(row);
    }

    private static ByteBuffer clipBuffer(ByteBuffer buffer, int start, int size) {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.position(start);
        duplicate.limit(start + size);
        return duplicate.slice();
    }

    // TODO support format conversions
    /* private static void fromNV21(
            PlaneWrapper plane,
            byte[] src,
            int srcOffset,
            byte[] dst,
            int dstOffset
    ) {
        int pixelStride = plane.pixelStride;
        int rowStride = plane.rowStride;
        int dstPos = dstOffset;
        for (int row = 0; row < plane.height; row++) {
            for (int col = 0; col < plane.width; col++) {
                dst[dstPos++] = src[row * rowStride + col * pixelStride + srcOffset];
            }
        }
    } */

    static class ImageWrapper {
        final int width, height;
        final PlaneWrapper y, u, v;

        ImageWrapper(int width, int height, PlaneWrapper y, PlaneWrapper u, PlaneWrapper v) {
            this.width = width;
            this.height = height;
            this.y = y;
            this.u = u;
            this.v = v;
            checkFormat();
        }


        // Check this is a supported image format
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        private void checkFormat() {
            if (y.pixelStride != 1) {
                throw new IllegalArgumentException(String.format(
                        "Pixel stride for Y plane must be 1 but got %d instead",
                        y.pixelStride
                ));
            }
            if (u.pixelStride != v.pixelStride || u.rowStride != v.rowStride) {
                throw new IllegalArgumentException(String.format(
                        "U and V planes must have the same pixel and row strides " +
                                "but got pixel=%d row=%d for U " +
                                "and pixel=%d and row=%d for V",
                        u.pixelStride, u.rowStride,
                        v.pixelStride, v.rowStride
                ));
            }
            if (u.pixelStride != 1 && u.pixelStride != 2) {
                throw new IllegalArgumentException(
                        "Supported pixel strides for U and V planes are 1 and 2"
                );
            }
        }
    }

    static class PlaneWrapper {
        final int width, height;
        final ByteBuffer buffer;
        final int rowStride, pixelStride;

        PlaneWrapper(int width, int height, ByteBuffer buffer, int rowStride, int pixelStride) {
            this.width = width;
            this.height = height;
            this.buffer = buffer;
            this.rowStride = rowStride;
            this.pixelStride = pixelStride;
        }
    }
}
