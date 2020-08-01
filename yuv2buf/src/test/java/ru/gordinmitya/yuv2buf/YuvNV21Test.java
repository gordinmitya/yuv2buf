package ru.gordinmitya.yuv2buf;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static ru.gordinmitya.yuv2buf.YuvCommon.U;
import static ru.gordinmitya.yuv2buf.YuvCommon.V;
import static ru.gordinmitya.yuv2buf.YuvCommon.Y;
import static ru.gordinmitya.yuv2buf.YuvCommon.buffer2array;


@RunWith(Parameterized.class)
public class YuvNV21Test {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return YuvCommon.testCases;
    }

    private final int width, height, rowStride;

    public YuvNV21Test(int width, int height, int rowStride) {
        this.width = width;
        this.height = height;
        this.rowStride = rowStride;
    }

    @Test
    public void check() {
        Yuv.ImageWrapper image = YuvCommon.make(Yuv.Type.YUV_NV21, width, height, rowStride);
        Yuv.Converted converted = Yuv.toBuffer(image, null);
        ByteBuffer buffer = converted.buffer;

        assertEquals(Yuv.Type.YUV_NV21, converted.type);
        assertEquals(0, buffer.position());

        int sizeY = width * height;
        int sizeChroma = sizeY / 4;
        int sizeTotal = sizeY + sizeChroma * 2;
        assertEquals(sizeTotal, buffer.capacity());
        byte[] array = buffer2array(converted.buffer);

        for (int i = 0; i < sizeY; i++)
            assertEquals(Y, array[i]);

        for (int i = sizeY; i < sizeTotal; i++) {
            byte expected = i % 2 == 0 ? V : U;
            assertEquals(expected, array[i]);
        }
    }
}