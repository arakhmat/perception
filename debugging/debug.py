import glob
import array
import numpy as np
import matplotlib.pyplot as plt

PHONE = 1
if PHONE == 1:
    phone = 'lenovo'
    h, w = 144, 176
    yRowStride, yPixelStride = 176, 1
    uRowStride, uPixelStride = 96,  1
    vRowStride, vPixelStride = 96,  1
    y_len, u_len, v_len = 25344, 6904, 6904
    iter_h, iter_w = 128, 128
elif PHONE == 2:
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
   
#    y_len, v_len, u_len = 27632, 13807, 13807
    yuv_len = y_len + u_len + v_len
    
    for data_file in sorted(glob.glob(phone + '/Y*')):
        yuv = array.array('b')
        with open(data_file, 'rb') as f:
            yuv.fromfile(f, yuv_len)
            
        yuv = np.array(yuv, dtype=np.uint8)
        plt.imshow(yuv[:y_len].reshape((h, yRowStride)).transpose())
        plt.show()
        
        hh, ww = h, uRowStride
        plt.imshow(yuv[y_len-16:y_len+13824-16].reshape((hh, ww)).transpose())
        plt.show()
        
#        plt.imshow(yuv[-8+y_len+u_len:y_len+u_len+v_len].reshape((72, 96)).transpose())
#        plt.show()
            
#        scale = 128.0
#        offset = 128.0
#        frames = np.clip(np.uint8(np.array(frame).reshape((1, 9, 128, 128)) * scale + offset), 0, 255)
#        frames[0, 1, 64:72, :] = 1
##        frames[0:9:3, :, :] = frames[0:9:3, :, :]
##        frames[1:9:3, :, :] = frames[1:9:3, :, :]
##        frames[2:9:3, :, :] = frames[2:9:3, :, :]
#        
#        print(frames.max(), frames.min(), frames.mean())
#        
#        direction = ['NW', 'W', 'SW', 'N', 'Stand', 'S', 'NE', 'E', 'SE', 'Undefined']
#        
#        for frame_idx in range(frames.shape[0]):
#            frame = frames[frame_idx]
#            f, ax = plt.subplots(1, 3)
#            ax[0].imshow(frame[0:3].transpose((1,2,0)))
#            ax[1].imshow(frame[3:6].transpose((1,2,0)))
#            ax[2].imshow(frame[6:9].transpose((1,2,0)))
#    #        f.suptitle(direction[labels[frame_idx]] + ' ' + direction[adversarial_labels[frame_idx]])
#            plt.show()
    