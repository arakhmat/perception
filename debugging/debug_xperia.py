import glob
import array
import numpy as np
import matplotlib.pyplot as plt

phone = 'xperia'
h, w = 144, 176
yRowStride, yPixelStride = 192, 1
uRowStride, uPixelStride = 192, 2
vRowStride, vPixelStride = 192, 2
y_len, u_len, v_len = 27632, 13807, 13807
iter_h, iter_w = 128, 128

IMG_C = 3
IMG_H = 128
IMG_W = 128

if __name__ == '__main__':
    
#    for data_file in sorted(glob.glob(phone + '/debug*')):
#        frame = array.array('f')
#
#        with open(data_file, 'rb') as f:
#            frame.fromfile(f, 9 * 128 * 128)
#            
#        scale = 128.0
#        offset = 128.0
#        frames = np.clip(np.uint8(np.array(frame).reshape((1, 9, 128, 128)) * scale + offset), 0, 255)        
#        for frame_idx in range(frames.shape[0]):
#            frame = frames[frame_idx]
#            f, ax = plt.subplots(1, 3)
#            ax[0].imshow(frame[0:3].transpose((1,2,0)))
#            ax[1].imshow(frame[3:6].transpose((1,2,0)))
#            ax[2].imshow(frame[6:9].transpose((1,2,0)))
#            plt.show()
        
   
    for data_file in sorted(glob.glob(phone + '/YUV*')):
        yuv = array.array('b')
        with open(data_file, 'rb') as f:
            yuv.fromfile(f, y_len + u_len + v_len)
        yuv = np.array(yuv, dtype=np.uint8)
        yuv = np.float32(yuv)
        
################################### Vectorized Implementation ###################################################
            
        y_len = 27648
        y = yuv[:y_len].reshape((h, yRowStride))
        y = y[:, :-16]
        y = y.T
        
        hh, ww = h//uPixelStride, uRowStride
        u = yuv[27598:41422].reshape((hh, ww))
        u = np.concatenate((u[:, 16:], u[:, :16]), axis=1)
        u = u[:, 16::uPixelStride]
        u = u.T
        
        hh, ww = h//vPixelStride, vRowStride
        v = yuv[41422:].reshape((hh, ww))
        v = np.concatenate((v[:, 1:], v[:, :1]), axis=1)
        v = v[:, 16::vPixelStride]
        v = v.T
        
        rgb = np.zeros((128, 128, 3), dtype=np.float32)
        for i in range(128):
            ii = int(i * 1.375)
            for j in range(128):
                jj = int(j * 1.125)
                if ii < 2:
                    rgb[i, j, 0] = y[ii, jj] + 1.370705 * (v[ii//2 + 1, jj//2] - 128.0)
                    rgb[i, j, 1] = y[ii, jj] - 0.337633 * (u[ii//2 + 1, jj//2] - 128.0) - 0.698001 * (v[ii//2 + 1, jj//2] - 128.0)
                    rgb[i, j, 2] = y[ii, jj] + 1.732446 * (u[ii//2 + 1, jj//2] - 128.0)
                else:
                    rgb[i, j, 0] = y[ii, jj] + 1.370705 * (v[ii//2, jj//2] - 128.0)
                    rgb[i, j, 1] = y[ii, jj] - 0.337633 * (u[ii//2, jj//2] - 128.0) - 0.698001 * (v[ii//2, jj//2] - 128.0)
                    rgb[i, j, 2] = y[ii, jj] + 1.732446 * (u[ii//2, jj//2] - 128.0)
        rgb = rgb[:,::-1,:]
        rgb_f = rgb
        rgb = np.clip(rgb, 0, 255)
        rgb = np.uint8(rgb)
        
#        f, ax = plt.subplots(2, 2)
#        ax[0, 0].imshow(y)
#        ax[1, 0].imshow(u)
#        ax[1, 1].imshow(v)
#        ax[0, 1].imshow(rgb)
#        plt.show()
    
################################## C++-like Implementation ###################################################
        
        Y = np.zeros((h * w), dtype=np.float32)
        for i in range(h):
            for j in range(yRowStride):
                if j >= w: continue
                Y[i * w + j] = yuv[i * yRowStride + j]
                
        U = np.zeros((h//uPixelStride * w//uPixelStride), dtype=np.float32)
        for i in range(h//uPixelStride):
            for j in range(0, uRowStride, uPixelStride):
                jj = j//2
                if jj < 8:
                    jj = w//uPixelStride - (8 - jj)
                else:
                    jj -= 16
                if (jj >= w//uPixelStride or jj < 0): continue
                U[i * w//uPixelStride + jj] = yuv[27598 + i * uRowStride + j]
                
        V = np.zeros((h//vPixelStride * w//vPixelStride), dtype=np.float32)
        for i in range(h//vPixelStride):
            for j in range(1, vRowStride, vPixelStride):
                if j < 1:
                    jj = w//vPixelStride - 1
                else:
                    jj = j - 16
                jj = jj//2
                if (jj >= w//vPixelStride or jj < 0): continue
                V[i * w//vPixelStride + jj] = yuv[41422 + i * vRowStride + j]
                
        RGB = np.zeros(128 * 128 * 3, dtype=np.float32)
        for i in range(128):
            ii = int(i * 1.375)
            for j in range(128):
                jj = int(j * 1.125)
                if ii < 2:
                    RGB[i * 128 * 3 + (127 - j) * 3 + 0] = Y[jj * w + ii] + 1.370705 * (V[jj//2 * w//2 + ii//2 + 1] - 128.0)
                    RGB[i * 128 * 3 + (127 - j) * 3 + 1] = Y[jj * w + ii] - 0.337633 * (U[jj//2 * w//2 + ii//2 + 1] - 128.0) - 0.698001 * (V[jj//2 * w//2 + ii//2 + 1] - 128.0)
                    RGB[i * 128 * 3 + (127 - j) * 3 + 2] = Y[jj * w + ii] + 1.732446 * (U[jj//2 * w//2 + ii//2 + 1] - 128.0)
                else:
                    RGB[i * 128 * 3 + (127 - j) * 3 + 0] = Y[jj * w + ii] + 1.370705 * (V[jj//2 * w//2 + ii//2] - 128.0)
                    RGB[i * 128 * 3 + (127 - j) * 3 + 1] = Y[jj * w + ii] - 0.337633 * (U[jj//2 * w//2 + ii//2] - 128.0) - 0.698001 * (V[jj//2 * w//2 + ii//2] - 128.0)
                    RGB[i * 128 * 3 + (127 - j) * 3 + 2] = Y[jj * w + ii] + 1.732446 * (U[jj//2 * w//2 + ii//2] - 128.0)
        RGB = np.clip(RGB, 0, 255)
        RGB = np.uint8(RGB)
        
#        f, ax = plt.subplots(2, 2)
#        ax[0, 0].imshow(Y.reshape((h, w)).T)
#        ax[1, 0].imshow(U.reshape((h//vPixelStride, w//vPixelStride)).T)
#        ax[1, 1].imshow(V.reshape((h//vPixelStride, w//vPixelStride)).T)
#        ax[0, 1].imshow(RGB.reshape((128, 128, 3)))
#        plt.show()
        
        plt.imshow(RGB.reshape((128, 128, 3)))
        plt.show()
        
        assert(np.isclose(Y.reshape((h, w)).T, y).all())
        assert(np.isclose(U.reshape((h//vPixelStride, w//vPixelStride)).T, u).all())
        assert(np.isclose(V.reshape((h//vPixelStride, w//vPixelStride)).T, v).all())
        assert(np.isclose(RGB.reshape((128, 128, 3)), rgb).all())
        
