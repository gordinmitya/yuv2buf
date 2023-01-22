package ru.gordinmitya.yuv2buf;

import android.graphics.ImageFormat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static ru.gordinmitya.yuv2buf.YuvCommon.P;
import static ru.gordinmitya.yuv2buf.YuvCommon.U;
import static ru.gordinmitya.yuv2buf.YuvCommon.V;
import static ru.gordinmitya.yuv2buf.YuvCommon.Y;
import static ru.gordinmitya.yuv2buf.YuvCommon.buffer2array;
import static ru.gordinmitya.yuv2buf.YuvCommon.getBufferSize;
import static ru.gordinmitya.yuv2buf.YuvCommon.make;

@RunWith(Parameterized.class)
public class YuvCommonTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return YuvCommon.testCases;
    }

    private final int width, height, rowStride;

    public YuvCommonTest(int width, int height, int rowStride) {
        this.width = width;
        this.height = height;
        this.rowStride = rowStride;
    }

    @Test
    public void test_getBufferSize() {
        int y = getBufferSize(864, 480, 896, 1);
        int chroma = getBufferSize(864 / 2, 480 / 2, 896, 2);
        Assert.assertEquals(430048, y);
        Assert.assertEquals(215007, chroma);
    }

    @Test
    public void test_ImageGeneration_YUV420() {
        Yuv.ImageWrapper image = make(ImageFormat.YUV_420_888, width, height, rowStride);

        assertPlaneParams(image.y, width, height, rowStride, 1);
        assertCompactPlane(image.y, Y);

        int stride = rowStride == width ? width / 2 : rowStride;
        assertPlaneParams(image.u, width / 2, height / 2, stride, 1);
        assertCompactPlane(image.u, U);

        assertPlaneParams(image.v, width / 2, height / 2, stride, 1);
        assertCompactPlane(image.v, V);
    }

    @Test
    public void test_ImageGeneration_NV21() {
        Yuv.ImageWrapper image = make(ImageFormat.NV21, width, height, rowStride);

        assertPlaneParams(image.y, width, height, rowStride, 1);
        assertCompactPlane(image.y, Y);

        assertPlaneParams(image.v, width / 2, height / 2, rowStride, 2);
        assertComplexPlane(image.v, V, 0);

        assertPlaneParams(image.u, width / 2, height / 2, rowStride, 2);
        assertComplexPlane(image.u, U, 1);
    }

    private static void assertPlaneParams(Yuv.PlaneWrapper plane, int width, int height, int rowStride, int pixelStride) {
        Assert.assertEquals(width, plane.width);
        Assert.assertEquals(height, plane.height);
        Assert.assertEquals(rowStride, plane.rowStride);
        Assert.assertEquals(pixelStride, plane.pixelStride);
    }

    private static void assertCompactPlane(Yuv.PlaneWrapper plane, byte element) {
        byte[] array = buffer2array(plane.buffer);
        for (int i = 0; i < plane.height; i++) {
            for (int j = 0; j < plane.rowStride; j++) {
                if (i == plane.height - 1 && j == plane.width)
                    break;
                int expected = j < plane.width ? element : P;
                int actual = array[i * plane.rowStride + j];
                Assert.assertEquals(expected, actual);
            }
        }
    }

    private static void assertComplexPlane(Yuv.PlaneWrapper plane, byte element, int offset) {
        byte[] array = buffer2array(plane.buffer);
        for (int i = 0; i < plane.height; i++) {
            for (int j = 0; j < plane.rowStride; j++) {
                if (i == plane.height - 1 && j == plane.width)
                    break;
                int actual = array[i * plane.rowStride + j];
                if (j >= plane.width * 2 - offset && j < plane.rowStride - offset)
                    assertEquals(P, actual);
                else if (j % 2 == 0)
                    Assert.assertEquals(element, actual);
                else {
                    int other = element == U ? V : U;
                    Assert.assertEquals(other, actual);
                }
            }
        }
    }
}
