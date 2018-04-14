import os
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"   # see issue #152
os.environ["CUDA_VISIBLE_DEVICES"] = ""

import glob
import array
import numpy as np
import matplotlib.pyplot as plt
from scipy import interpolate

import sys
sys.path.append('../../ai/supervised/keras')
from keras.models import  load_model
from metrics import fmeasure, recall, precision

import h5py
from collections import deque

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

DEBUG_IMAGE = 1
DEBUG_YUV   = 0

direction = ['NW', 'W', 'SW', 'N', 'Stand', 'S', 'NE', 'E', 'SE', 'Undefined']

model_file = 'xperia/feb_21/ddqn_rgb_feb_21.h5'
model_file = '/home/ahmed/Documents/41X/ai/supervised/keras/models/rgb/robot_model.h5'
model = load_model(model_file, {'fmeasure': fmeasure, 'recall': recall, 'precision': precision})

if __name__ == '__main__':
    yuv_files   = sorted(glob.glob(phone + '/feb_21/YUV*'))
    image_files = sorted(glob.glob(phone + '/mar_7/debug*'))
    image_files = np.array(image_files)
    
    states   = deque(maxlen=len(image_files))
    
    
    indices = np.random.permutation(len(image_files))[:100]
    
    plt.ion()
    for i, image_file in enumerate(image_files[indices]):
        print(image_file)
        if DEBUG_IMAGE:
            image = array.array('f')
            with open(image_file, 'rb') as f:
                image.fromfile(f, 9 * 128 * 128)

            image = np.array(image).reshape((9, 128, 128))
            states.append(image)
            pred = np.argmax(model.predict(image.reshape((1, 9, 128, 128))))

            label = int(image_file[:-4].split('_')[-1])
#            print(label, pred)
            
            fig, ax = plt.subplots(1, 3)
            fig.suptitle(direction[label] + ' ' +  direction[pred])
                
            scale  = 128.0
            offset = 128.0
            image = np.uint8(image.reshape((9, 128, 128)) * scale + offset)
            image = image.transpose((1,2,0))

            for i_ax in range(ax.shape[0]):
                ax[i_ax].axis('off')

            ax[0].set_title('i[t-2]')
            ax[0].imshow(image[:,:,0:3])
            ax[1].set_title('i[t-1]')
            ax[1].imshow(image[:,:,3:6])
            ax[2].set_title('i[t]')
            ax[2].imshow(image[:,:,6:9])
            
            plt.show()
            plt.pause(0.0000000001)
    
    states = np.array(states, dtype=np.float32)
    data_file = '../../ai/supervised/keras/' + 'xperia.h5'
    with h5py.File(data_file , 'w') as f:
        f.create_dataset('states', data=states)
    print('Saved generated states to %s' % data_file)


      
