#include <bits/stdc++.h>

#define IMG_H 128
#define IMG_W 128
#define IMG_C 3
#define IMG_D 9

#define min(a,b) ((a) > (b)) ? (b) : (a)
#define max(a,b) ((a) > (b)) ? (a) : (b)

static float input_data[IMG_H * IMG_W * IMG_D];
static unsigned char YUV_data[27632 + 13807 + 13807];

int main() {

    int idx = 0;
    char debug_file[100];
    sprintf(debug_file, "xperia/YUV_%04d.dat", idx);

    FILE* file = fopen(debug_file, "rb");
    if (file != NULL)
    {
        std::cout << "File read succesfully" << std::endl;
        fread(YUV_data, sizeof(unsigned char), sizeof(YUV_data), file);
        fclose(file);
    }

    int h = 144, w = 176;
    int rowStride = 192;
    int pixelStride = 2;

    int u_start = 27598;
    int v_start = 41422;

    int y_len = 25344;
    int u_len = 6336;
    int v_len = 6336;

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
            if (ii < 2) uv_idx++;
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

    file = fopen("debug_cpp.dat", "wb");
    if (file != NULL)
    {
        fwrite(input_data, sizeof(float), sizeof(input_data), file);
        fclose(file);
        std::cout << "debug file stored succesfully" << std::endl;
    } 

    file = fopen("YUV_cpp.dat", "wb");
    if (file != NULL)
    {
        fwrite(YUV_data, sizeof(char), sizeof(YUV_data), file);
        fclose(file);
        std::cout << "YUV file stored succesfully" << std::endl;
    }    
}