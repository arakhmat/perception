#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_nextrev_perception_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

#include <algorithm>
#define PROTOBUF_USE_DLLS 1
#define CAFFE2_USE_LITE_PROTO 1
#include <caffe2/core/predictor.h>
#include <caffe2/core/operator.h>
#include <caffe2/core/timer.h>

#include "caffe2/core/init.h"

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#define IMG_H 128
#define IMG_W 128
#define IMG_C 3
#define IMG_D 9 // Depth
#define MAX_DATA_SIZE IMG_H * IMG_W * IMG_D
#define alog(...) __android_log_print(ANDROID_LOG_DEBUG, "PERCEPTION", __VA_ARGS__);

static caffe2::NetDef _initNet, _predictNet;
static caffe2::Predictor *_predictor;
static char raw_data[MAX_DATA_SIZE];
static float input_data[MAX_DATA_SIZE];
static caffe2::Workspace ws;

const char * directions_map[] {
        "NW", "N", "NE",
        "W", "", "E",
        "SW", "S", "SE", "Invalid"
};


// A function to load the NetDefs from protobufs.
void loadToNetDef(AAssetManager* mgr, caffe2::NetDef* net, const char *filename) {
    AAsset* asset = AAssetManager_open(mgr, filename, AASSET_MODE_BUFFER);
    assert(asset != nullptr);
    const void *data = AAsset_getBuffer(asset);
    assert(data != nullptr);
    off_t len = AAsset_getLength(asset);
    assert(len != 0);
    if (!net->ParseFromArray(data, len)) {
        alog("Couldn't parse net from data.\n");
    }
    AAsset_close(asset);
}

extern "C"
void
Java_nextrev_perception_CameraActivity_initCaffe2(
        JNIEnv* env,
        jobject /* this */,
        jobject assetManager) {
    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    alog("Attempting to load protobuf netdefs...");
    loadToNetDef(mgr, &_initNet,   "init_net.pb");
    loadToNetDef(mgr, &_predictNet,"predict_net.pb");
    alog("Instantiating predictor...");
    _predictor = new caffe2::Predictor(_initNet, _predictNet);
    alog("%d", ws.Blobs().size());
    for (auto blob : ws.Blobs())
        alog("%s", blob.c_str());
    alog("Network Initialized");
}

float avg_fps = 0.0;
float total_fps = 0.0;
int iters_fps = 10;

extern "C"
JNIEXPORT jstring JNICALL
Java_nextrev_perception_CameraActivity_classificationFromCaffe2(
        JNIEnv *env,
        jobject /* this */,
        jint h, jint w, jbyteArray Y, jbyteArray U, jbyteArray V,
        jint rowStride, jint pixelStride) {
    if (!_predictor) {
        return env->NewStringUTF("Loading...");
    }

    jsize Y_len = env->GetArrayLength(Y);
    jbyte * Y_data = env->GetByteArrayElements(Y, 0);
    assert(Y_len <= MAX_DATA_SIZE);
    jsize U_len = env->GetArrayLength(U);
    jbyte * U_data = env->GetByteArrayElements(U, 0);
    assert(U_len <= MAX_DATA_SIZE);
    jsize V_len = env->GetArrayLength(V);
    jbyte * V_data = env->GetByteArrayElements(V, 0);
    assert(V_len <= MAX_DATA_SIZE);

#define min(a,b) ((a) > (b)) ? (b) : (a)
#define max(a,b) ((a) > (b)) ? (a) : (b)

    auto h_offset = max(0, (h - IMG_H) / 2);
    auto w_offset = max(0, (w - IMG_W) / 2);

    auto iter_h = IMG_H;
    auto iter_w = IMG_W;
    if (h < IMG_H) {
        iter_h = h;
    }
    if (w < IMG_W) {
        iter_w = w;
    }

    for (auto i = 0; i < iter_h; ++i) {
        jbyte* Y_row = &Y_data[(h_offset + i) * w];
        jbyte* U_row = &U_data[(h_offset + i) / 4 * rowStride];
        jbyte* V_row = &V_data[(h_offset + i) / 4 * rowStride];
        for (auto j = 0; j < iter_w; ++j) {
            // Tested on Pixel and S7.
            char y = Y_row[w_offset + j];
            char u = U_row[pixelStride * ((w_offset+j)/pixelStride)];
            char v = V_row[pixelStride * ((w_offset+j)/pixelStride)];

            float b_mean = 104.00698793f;
            float g_mean = 116.66876762f;
            float r_mean = 122.67891434f;

            auto b_i = 0 * IMG_H * IMG_W + j * IMG_W + i;
            auto g_i = 1 * IMG_H * IMG_W + j * IMG_W + i;
            auto r_i = 2 * IMG_H * IMG_W + j * IMG_W + i;

            auto b_i_1 = b_i + (IMG_H * IMG_W * IMG_C);
            auto g_i_1 = g_i + (IMG_H * IMG_W * IMG_C);
            auto r_i_1 = r_i + (IMG_H * IMG_W * IMG_C);
//
            auto b_i_2 = b_i + 2 * (IMG_H * IMG_W * IMG_C);
            auto g_i_2 = g_i + 2 * (IMG_H * IMG_W * IMG_C);
            auto r_i_2 = r_i + 2 * (IMG_H * IMG_W * IMG_C);

            if (i == iter_h - 1 && j == iter_w - 1) {
                alog("%d %d %d\n", r_i, g_i, b_i);
                alog("%d %d %d\n", r_i_1, g_i_1, b_i_1);
                alog("%d %d %d\n", r_i_2, g_i_2, b_i_2);
            }


/*
  R = Y + 1.402 (V-128)
  G = Y - 0.34414 (U-128) - 0.71414 (V-128)
  B = Y + 1.772 (U-V)
 */

//            input_data[r_i_2] = input_data[r_i_1];
//            input_data[g_i_2] = input_data[g_i_1];
//            input_data[b_i_2] = input_data[b_i_1];
//
//            input_data[r_i_1] = input_data[r_i];
//            input_data[g_i_1] = input_data[g_i];
//            input_data[b_i_1] = input_data[b_i];

            input_data[r_i] = -r_mean + (float) ((float) min(255., max(0., (float) (y + 1.402 * (v - 128)))));
            input_data[g_i] = -g_mean + (float) ((float) min(255., max(0., (float) (y - 0.34414 * (u - 128) - 0.71414 * (v - 128)))));
            input_data[b_i] = -b_mean + (float) ((float) min(255., max(0., (float) (y + 1.772 * (u - v)))));

//            input_data[r_i] = (input_data[r_i] + 128) / 255;
//            input_data[g_i] = (input_data[g_i] + 128) / 255;
//            input_data[b_i] = (input_data[b_i] + 128) / 255;

            if (i == iter_h - 1 && j == iter_w - 1) {
                alog("%f %f %f\n", input_data[r_i], input_data[g_i], input_data[b_i]);
                alog("%f %f %f\n", input_data[r_i_1], input_data[g_i_1], input_data[b_i_1]);
                alog("%f %f %f\n", input_data[r_i_2], input_data[g_i_2], input_data[b_i_2]);
            }

        }
    }
//    alog("Exited for-loop")
    caffe2::TensorCPU input;
    input.Resize(std::vector<int>({1, IMG_D, IMG_H, IMG_W}));

    memcpy(input.mutable_data<float>(), input_data, IMG_H * IMG_W * IMG_D * sizeof(float));
    caffe2::Predictor::TensorVector input_vec{&input};
    caffe2::Predictor::TensorVector output_vec;
    caffe2::Timer t;
    t.Start();
    alog("%d %d %d %d\n", IMG_C, IMG_H, IMG_W, IMG_D)
    _predictor->run(input_vec, &output_vec);
    float fps = 1000/t.MilliSeconds();
    total_fps += fps;
    avg_fps = total_fps / iters_fps;
    total_fps -= avg_fps;
//    alog("Predictor finished")

//    for (auto output : output_vec) {
//        for (auto i = 0; i < output->size(); ++i) {
//            alog("%f", output->template data<float>()[i]);
//        }
//    }

    constexpr int k = 5;
    float max[k] = {0};
    int max_index[k] = {0};
    // Find the top-k results manually.
    if (output_vec.capacity() > 0) {
        for (auto output : output_vec) {
            for (auto i = 0; i < output->size(); ++i) {
                for (auto j = 0; j < k; ++j) {
                    if (output->template data<float>()[i] > max[j]) {
                        for (auto _j = k - 1; _j > j; --_j) {
                            max[_j - 1] = max[_j];
                            max_index[_j - 1] = max_index[_j];
                        }
                        max[j] = output->template data<float>()[i];
                        max_index[j] = i;
                        goto skip;
                    }
                }
                skip:;
            }
        }
    }
    std::ostringstream stringStream;
    stringStream << avg_fps << " FPS\n";

    for (auto j = 0; j < k; ++j) {
        stringStream << j << ": " << directions_map[max_index[j]] << " - " << max[j] * 100 << "%\n";
    }
    return env->NewStringUTF(stringStream.str().c_str());
}
