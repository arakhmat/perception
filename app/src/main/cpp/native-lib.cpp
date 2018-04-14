#include <jni.h>
#include <string>
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

const char * actions[] {
        "NW", "W", "SW", "N", "Stand", "S", "NE", "E", "SE", "Undefined"
};

void loadCaffe2Net(AAssetManager* mgr, caffe2::NetDef* net, const char *filename) {
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

extern "C" void Java_nextrev_perception_activities_CameraActivity_initializeNeuralNetwork(
        JNIEnv* env,
        jobject /* this */,
        jobject assetManager) {
    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    loadCaffe2Net(mgr, &_initNet,   "init_net.pb");
    loadCaffe2Net(mgr, &_predictNet,"predict_net.pb");
    _predictor = new caffe2::Predictor(_initNet, _predictNet);
}

float avg_fps = 0.0;
float total_fps = 0.0;
int iters_fps = 10;
int call_count = 0;

extern "C" JNIEXPORT jobject JNICALL Java_nextrev_perception_activities_CameraActivity_predict(
        JNIEnv *env,
        jobject /* this */,
        jint h, jint w, jbyteArray YUV,
        jint rowStride, jint pixelStride) {

    call_count++;

    jclass javaClass = env->FindClass("nextrev/perception/Prediction");
    jmethodID constructor = env->GetMethodID(javaClass, "<init>", "()V");
    jobject prediction = env->NewObject(javaClass, constructor);

    if (!_predictor) {
        return prediction;
    }

    jbyte * YUV_data = env->GetByteArrayElements(YUV, 0);

#define min(a,b) ((a) > (b)) ? (b) : (a)
#define max(a,b) ((a) > (b)) ? (a) : (b)

/* Following code converts YUV to RGB only for Sony Xperia Aqua M4
 * It most likely is not going to work on any other Android device */

    int y_len = h * w;
    int u_len = y_len / 4;
    int v_len = y_len / 4;

    int u_start = 27598;
    int v_start = 41422;

    unsigned char Y[y_len];
    for (auto i = 0; i < h; i++) {
        for (auto j = 0; j < rowStride; j++) {
            if (j >= w) continue;
            Y[i * w + j] = YUV_data[i * rowStride + j];
        }
    }

    unsigned char U[u_len];
    for (auto i = 0; i < h/pixelStride; i++) {
        for (auto j = 0; j < rowStride; j += pixelStride) {
            int jj = j / 2;
            if (jj < 8)
                jj = w/pixelStride - (8 - jj);
            else
                jj -= 16;
            if (jj >= w/pixelStride or jj < 0) continue;
            U[i * w/pixelStride + jj] = YUV_data[u_start + i * rowStride + j];
        }
    }

    unsigned char V[v_len];
    for (auto i = 0; i < h/pixelStride; i++) {
        for (auto j = 1; j < rowStride; j += pixelStride) {
            int jj;
            if (j < 1)
                jj = w/pixelStride - 1;
            else
                jj = j - 16;
            jj = jj / 2;
            if (jj >= w/pixelStride or jj < 0) continue;
            V[i * w/pixelStride + jj] = YUV_data[v_start + i * rowStride + j];
        }
    }

    for (auto i = 0; i < IMG_H; i++) {
        int ii = (int) ((float) i * 1.375f);
        for (auto j = 0; j < IMG_W; j++) {
            int jj = (int) ((float) j * 1.125f);

            auto r_i = 0 * IMG_H * IMG_W + i * IMG_W  + (IMG_W - 1 - j);
            auto g_i = 1 * IMG_H * IMG_W + i * IMG_W  + (IMG_W - 1 - j);
            auto b_i = 2 * IMG_H * IMG_W + i * IMG_W  + (IMG_W - 1 - j);

            auto b_i_1 = b_i + (IMG_C * IMG_H * IMG_W);
            auto g_i_1 = g_i + (IMG_C * IMG_H * IMG_W);
            auto r_i_1 = r_i + (IMG_C * IMG_H * IMG_W);

            auto b_i_2 = b_i_1 + (IMG_C * IMG_H * IMG_W);
            auto g_i_2 = g_i_1 + (IMG_C * IMG_H * IMG_W);
            auto r_i_2 = r_i_1 + (IMG_C * IMG_H * IMG_W);

            input_data[r_i_2] = input_data[r_i_1];
            input_data[g_i_2] = input_data[g_i_1];
            input_data[b_i_2] = input_data[b_i_1];

            input_data[r_i_1] = input_data[r_i];
            input_data[g_i_1] = input_data[g_i];
            input_data[b_i_1] = input_data[b_i];

            int uv_idx = jj/2 * w/2 + ii/2;
            float y = (float) Y[jj * w + ii];
            float u = (float) U[uv_idx];
            float v = (float) V[uv_idx];

            input_data[r_i] = y + 1.370705f * (v - 128.0f);
            input_data[g_i] = y - 0.337633f * (u - 128.0f) - 0.698001f * (v - 128.0f);
            input_data[b_i] = y + 1.732446f * (u - 128.0f);

            input_data[r_i] = max(0.0f, min(255.0f, input_data[r_i]));
            input_data[g_i] = max(0.0f, min(255.0f, input_data[g_i]));
            input_data[b_i] = max(0.0f, min(255.0f, input_data[b_i]));

            input_data[r_i] -= 128.0f;
            input_data[g_i] -= 128.0f;
            input_data[b_i] -= 128.0f;

            input_data[r_i] /= 128.0f;
            input_data[g_i] /= 128.0f;
            input_data[b_i] /= 128.0f;
        }
    }

    caffe2::TensorCPU input;
    input.Resize(std::vector<int>({1, IMG_D, IMG_H, IMG_W}));
    memcpy(input.mutable_data<float>(), input_data, IMG_D * IMG_H * IMG_W * sizeof(float));

    caffe2::Predictor::TensorVector input_vec{&input};
    caffe2::Predictor::TensorVector output_vec;
    caffe2::Timer t;

    t.Start();
    _predictor->run(input_vec, &output_vec);
    float fps = 1000/t.MilliSeconds();
    total_fps += fps;
    avg_fps = total_fps / iters_fps;
    total_fps -= avg_fps;

    // Extract the output of the neural network
    float values[10];
    for (auto output : output_vec) {
        for (auto i = 0; i < output->size(); i++) {
            values[i] =  output->template data<float>()[i];
//            alog("%d %f", i, values[i])
        }
    }

    // Find top outputs
    constexpr int n_max = 10;
    float max_values[n_max];
    int max_indices[n_max];
    for (auto i = 0; i < n_max; i++) {
        max_values[i] = -std::numeric_limits<float>::infinity();
        max_indices[i] = 0;
//        alog("%d %f %d\n", i, max_values[i], max_indices[i])
//        alog("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&")
        // Go through output and find the current max
        for (auto j = 0; j < 10; j++) {
//            alog("%d %f", j, values[j])
            if (values[j] > max_values[i]) {
                max_values[i] = values[j];
                max_indices[i] = j;
            }
        }
//        alog("++++++++++++++++++++++++++++++++++++++++")
//        alog("%d %f %d\n", i, max_values[i], max_indices[i])
//        alog("*****************************************")
        // Set current max to negative infinity so that it won't be used on the next iteration
        values[max_indices[i]] =  -std::numeric_limits<float>::infinity();
//        for (auto j = 0; j < 10; j++) {
//            alog("%d %f", j, values[j])
//        }
//        alog("########################################")

    }

    std::ostringstream stringStream;
    stringStream << avg_fps << " FPS\n";
    stringStream << call_count << " calls\n";

    for (auto j = 0; j < n_max; ++j) {
        stringStream << j << ": " << actions[max_indices[j]] << " = " << max_values[j] << "\n";
//        alog("Max Values %d %f", j, max_values[j])
    }
//    alog("-----------------------------------")
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    static int debug_idx = 0;
//    char debug_file[100];
//    sprintf(debug_file, "/sdcard/perception/debug_%04d", debug_idx);
//    alog("%s", debug_file)
//    FILE* file = fopen(debug_file, "wb");
//    if (file != NULL)
//    {
//        fwrite(input_data,  sizeof(float), 9*128*128, file);
//        fwrite(max_values,  sizeof(float),         3, file);
//        fwrite(max_indices, sizeof(int),           3, file);
//        fclose(file);
//        alog("file stored successfully")
//    }
//
////    sprintf(debug_file, "/sdcard/perception/YUV_%04d.dat", debug_idx);
////    alog("%s", debug_file)
////    file = fopen(debug_file, "wb");
////    if (file != NULL)
////    {
////        alog("file stored successfully")
////        fwrite(YUV_data, sizeof(char), 27632 + 13807 + 13807, file);
////        fclose(file);
////    }
//    debug_idx++;
///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    jfieldID value = env->GetFieldID(javaClass, "value", "I");
    jfieldID info = env->GetFieldID(javaClass, "info", "Ljava/lang/String;");
    env->SetIntField(prediction, value, max_indices[0]);
    env->SetObjectField(prediction, info, env->NewStringUTF(stringStream.str().c_str()));
    return prediction;
}
