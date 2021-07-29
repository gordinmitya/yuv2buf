package ru.gordinmitya.yuv2buf;

import android.graphics.ImageFormat;

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
        byte[] array = buffer2array(converted.buffer);

        for (int i = 0; i < sizeY; i++)
            assertEquals(Y, array[i]);

        for (int i = sizeY; i < sizeY + sizeChroma; i++)
            assertEquals(U, array[i]);

        for (int i = sizeY + sizeChroma; i < sizeTotal; i++)
            assertEquals(V, array[i]);
    }
}
