# Perception
Android application for controlling air hockey robot in real-time.

### How it works
The application infers action that the robot should take by looking at the last 3 frames obtained from the camera.
Then, it sends the inferred action to Arduino via Bluetooth LE.  

Predictions are made by using a convolutional neural network. The network is pretrained 
with labeled frames generated using [Air Hockey Game Simulator](https://github.com/arakhmat/air-hockey), and then trained via reinforcement learning techniques using [gym-air-hockey](https://github.com/arakhmat/gym-air-hockey) as environment. The model is convrted from keras to caffe2  using [keras-to-caffe2](https://github.com/arakhmat/keras-to-caffe2)
### Prerequisites
[Android Studio](https://developer.android.com/studio/index.html)
### Download
```
git clone https://github.com/arakhmat/perception 
```
### Build
Import the project to Android Studio and it will build automatically.
## Acknowledgments
* [AI Camera](https://github.com/bwasti/AICamera) - Demonstration of using Caffe2 inside an Android application
* [Android Bluetooth Low Energy (BLE) Example](http://www.truiton.com/2015/04/android-bluetooth-low-energy-ble-example/)
* [How to Communicate with a Custom BLE using an Android App](https://www.allaboutcircuits.com/projects/how-to-communicate-with-a-custom-ble-using-an-android-app/)
* [HM-10 Bluetooth 4 BLE Modules](http://www.martyncurrey.com/hm-10-bluetooth-4ble-modules/)
