//
//  mnnportraitnative.cpp
//  MNN
//
//  Created by MNN on 2019/01/29.
//  Copyright © 2018, Alibaba Group Holding Limited
//

#include <android/bitmap.h>
#include <jni.h>
#include <string.h>
#include <MNN/ImageProcess.hpp>
#include <MNN/Interpreter.hpp>
#include <MNN/Tensor.hpp>
#include <memory>

extern "C" JNIEXPORT jintArray JNICALL
Java_com_taobao_android_mnn_MNNHelperNative_nativeConvertMaskToPixelsMultiChannels(JNIEnv *env,
                                                                                   jclass jclazz,
                                                                                   jfloatArray jmaskarray) {
    int length = env->GetArrayLength(jmaskarray) / 3;
    float *image = (float *) env->GetFloatArrayElements(jmaskarray, 0);
    int *dst = new int[length];

    for (int l = 0; l < length; l++) {
        unsigned r = image[l];
        unsigned g = image[l + length];
        unsigned b = image[l + length * 2];

        dst[l] = 255 << 24 | r << 16 | g << 8 | b;
    }

    jintArray arr = env->NewIntArray(length);
    env->SetIntArrayRegion(arr, 0, length, dst);

    env->ReleaseFloatArrayElements(jmaskarray, image, 0);
    delete[] (dst);

    return arr;
}
