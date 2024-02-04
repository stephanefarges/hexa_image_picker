# hexa_image_picker

I had issues accessing gps location metadata with images picked from the gallery with the standard flutter image picker.
So I made this plugin as a workaround for this exact use case, by avoiding the Android image picker which removes location metadata 

## Usage

```dart
import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:hexa_image_picker/hexa_image_picker.dart';
import 'package:image_picker/image_picker.dart';

Future<XFile?> _pickImage(ImageSource imageSource) async {
bool useStandardImagePicker = true;
if (Platform.isAndroid && imageSource == ImageSource.gallery) {
DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();
AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
if (androidInfo.version.sdkInt >= 33) {
useStandardImagePicker = false;
}
}
if (useStandardImagePicker) {
ImagePicker _imagePicker = new ImagePicker();
return await _imagePicker.pickImage(
source: imageSource, imageQuality: 50, preferredCameraDevice: CameraDevice.rear);
} else {
HexaImagePicker _hexaImagePicker = new HexaImagePicker();
return await _hexaImagePicker.pickImage();
}
}
```

