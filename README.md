
# Perception
Android application for controlling air hockey robot in real-time using Convolutional Neural Network (Submodule of [Deep Learning Air Hockey Robot](https://github.com/arakhmat/41X))

### How it works
The application infers action that the robot should take by looking at the last 3 frames captured by the camera.
Then, the inferred action is sent to Arduino via Bluetooth LE.

Predictions are made by using a convolutional neural network. The network is pretrained 
with labeled frames generated using [Air Hockey Game Simulator](https://github.com/arakhmat/air-hockey), and then trained via DDQN using [gym-air-hockey](https://github.com/arakhmat/gym-air-hockey) as the environment. Finally, the model is converted from keras to caffe2 using [keras-to-caffe2 converter](https://github.com/arakhmat/keras-to-caffe2).
### Screenshots
#### Launched application
<img src="https://github.com/arakhmat/perception/blob/master/images/start.png" width="240" height="425">

#### Menu used to connect to a BLE device
<img src="https://github.com/arakhmat/perception/blob/master/images/connect.png" width="240" height="425">

#### Ready to start
<img src="https://github.com/arakhmat/perception/blob/master/images/ready.png" width="240" height="425">

#### During the game (Top prediction is to go southeast (bottom right) ☺)
<img src="https://github.com/arakhmat/perception/blob/master/images/action.png" width="240" height="425">

### Prerequisites
[Android Studio](https://developer.android.com/studio/index.html)
### Download
```
git clone https://github.com/arakhmat/perception 
```
### Build
Import the project to Android Studio and it will build automatically.
### Limitations
* The application was tested only on Sony Xperia m4 Aqua. It most likely will not convert raw YUV data to RGB image correctly on any other device due to an unusual format of YUV data on Xperia m4 Aqua.
## Acknowledgments
* [AI Camera](https://github.com/bwasti/AICamera) - Demonstration of using Caffe2 inside an Android application
* [Android Bluetooth Low Energy (BLE) Example](http://www.truiton.com/2015/04/android-bluetooth-low-energy-ble-example/)
* [How to Communicate with a Custom BLE using an Android App](https://www.allaboutcircuits.com/projects/how-to-communicate-with-a-custom-ble-using-an-android-app/)
* [HM-10 Bluetooth 4 BLE Modules](http://www.martyncurrey.com/hm-10-bluetooth-4ble-modules/)


