package ru.gordinmitya.yuv2buf;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ReuseTest {

    @Test
    public void test_reuse() {
        final int width = 10, height = 10;
        Yuv.ImageWrapper image = YuvCommon.make(Yuv.Type.YUV_420, width, height, width);
        int size = width * height * 3 / 2;
        ByteBuffer correctReuse;
        ByteBuffer result;

        // the same size
        correctReuse = ByteBuffer.allocateDirect(size);
        result = Yuv.toBuffer(image, correctReuse).buffer;
        assertSame(correctReuse, result);

        // bigger
        correctReuse = ByteBuffer.allocateDirect(size + 1);
        result = Yuv.toBuffer(image, correctReuse).buffer;
        assertSame(correctReuse, result);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test_makesNew() {
        final int width = 10, height = 10;
        Yuv.ImageWrapper image = YuvCommon.make(Yuv.Type.YUV_420, width, height, width);
        int size = width * height * 3 / 2;
        ByteBuffer incorrectReuse;
        ByteBuffer result;

        // null
        incorrectReuse = null;
        result = Yuv.toBuffer(image, incorrectReuse).buffer;
        assertNotSame(incorrectReuse, result);

        // smaller
        incorrectReuse = ByteBuffer.allocateDirect(size - 1);
        result = Yuv.toBuffer(image, incorrectReuse).buffer;
        assertNotSame(incorrectReuse, result);

        // heap
        incorrectReuse = ByteBuffer.allocate(size);
        result = Yuv.toBuffer(image, incorrectReuse).buffer;
        assertNotSame(incorrectReuse, result);

        // readonly
        incorrectReuse = ByteBuffer.allocateDirect(size)
                .asReadOnlyBuffer();
        result = Yuv.toBuffer(image, incorrectReuse).buffer;
        assertNotSame(incorrectReuse, result);
    }
}
