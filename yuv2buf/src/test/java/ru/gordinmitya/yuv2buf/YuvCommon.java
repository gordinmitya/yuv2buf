package ru.gordinmitya.yuv2buf;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

class YuvCommon {
    // Values for each channel
    // P == padding
    static final byte P = -1, Y = 1, U = 2, V = 3;

    // Test cases for Yuv420Test and YuvNV21Test
    // {width, height, rowStride}
    static final Collection<Object[]> testCases = Arrays.asList(new Object[][]{
            // square
            {4, 4, 6}, // padding
            {4, 4, 4}, // no padding

            // wide
            {10, 4, 12},
            {10, 4, 10},

            // tall
            {4, 10, 8},
            {4, 10, 4},

            {864, 480, 896}
    });

    static int getBufferSize(int width, int height, int rowStride, int pixelStride) {
        return rowStride * (height - 1) + width * pixelStride - pixelStride / 2;
    }

    static Yuv.ImageWrapper make(Yuv.Type type, int width, int height, int rowStride) {
        assert rowStride >= width;
        Yuv.PlaneWrapper y = makeCompact(Y, width, height, rowStride);
        Yuv.PlaneWrapper u, v;
        if (Yuv.Type.YUV_420.equals(type)) {
            int stride = rowStride == width ? width / 2 : rowStride;
            u = makeCompact(U, width / 2, height / 2, stride);
            v = makeCompact(V, width / 2, height / 2, stride);
        } else {
            Yuv.PlaneWrapper[] planes = makeNV21_UV(width / 2, height / 2, rowStride);
            u = planes[0];
            v = planes[1];
        }
        return new Yuv.ImageWrapper(width, height, y, u, v);
    }

    static byte[] buffer2array(ByteBuffer buffer) {
        byte[] array = new byte[buffer.capacity()];
        ByteBuffer copy = buffer.duplicate();
        copy.rewind();
        copy.get(array);
        return array;
    }

    private static Yuv.PlaneWrapper makeCompact(byte element, int width, int height, int rowStride) {
        int size = getBufferSize(width, height, rowStride, 1);
        byte[] array = new byte[size];
        Arrays.fill(array, P);
        for (int i = 0; i < height; i++) {
            Arrays.fill(array, i * rowStride, i * rowStride + width, element);
        }
        // we always obtain direct buffers from camera
        ByteBuffer buffer = ByteBuffer.allocateDirect(array.length);
        buffer.put(array);
        buffer.rewind();
        return new Yuv.PlaneWrapper(
                width,
                height,
                buffer,
                rowStride,
                1
        );
    }

    private static Yuv.PlaneWrapper[] makeNV21_UV(int width, int height, int rowStride) {
        assert rowStride >= width * 2;
        // +1 because we're making it for 2 planes
        int size = getBufferSize(width, height, rowStride, 2) + 1;
        byte[] array = new byte[size];
        Arrays.fill(array, P);
        for (int i = 0; i < width * 2; i++)
            array[i] = i % 2 == 0 ? U : V;
        for (int i = 1; i < height; i++)
            System.arraycopy(array, 0, array, i * rowStride, width * 2);
        ByteBuffer u = ByteBuffer.allocateDirect(array.length - 1);
        u.put(array, 0, array.length - 1);
        u.rewind();
        ByteBuffer v = ByteBuffer.allocateDirect(array.length - 1);
        v.put(array, 1, array.length - 1);
        v.rewind();
        return new Yuv.PlaneWrapper[]{
                new Yuv.PlaneWrapper(width, height, u, rowStride, 2),
                new Yuv.PlaneWrapper(width, height, v, rowStride, 2)
        };
    }
}
