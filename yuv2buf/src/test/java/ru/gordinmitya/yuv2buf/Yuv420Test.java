package ru.gordinmitya.yuv2buf;

import android.graphics.ImageFormat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(Parameterized.class)
public class Yuv420Test {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return YuvCommon.testCases;
    }

    private final int width, height, rowStride;

    public Yuv420Test(int width, int height, int rowStride) {
        this.width = width;
        this.height = height;
        this.rowStride = rowStride;
    }

    @Test
    public void check() {
        Yuv.ImageWrapper image = YuvCommon.make(ImageFormat.YUV_420_888, width, height, rowStride);
        Yuv.Converted converted = Yuv.toBuffer(image, null);
        ByteBuffer buffer = converted.buffer;

        assertEquals(ImageFormat.YUV_420_888, converted.type);
        assertEquals(0, buffer.position());

        int sizeY = width * height;
        int sizeChroma = sizeY / 4;
        int sizeTotal = sizeY + sizeChroma * 2;
        assertEquals(sizeTotal, buffer.capacity());
        assertTrue(YuvCommon.checkYuv420(converted.buffer, width, height));
    }
}
