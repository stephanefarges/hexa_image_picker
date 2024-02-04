library hexa_image_picker;

import 'package:cross_file/cross_file.dart';
import 'hexa_image_picker_platform_interface.dart';



class HexaImagePicker {

  Future<XFile?> pickImage() async {
    final List<Map>? result = await HexaImagePickerPlatform.instance.pick();
    if (result == null) {
      return null;
    }
    XFile? file = null;
    for (final Map fileMap in result) {
      if (fileMap['path'] != null) {
        file = XFile(fileMap['path']);
      }
    }
    return file;
  }

  Future<String?> getPlatformVersion() {
    return HexaImagePickerPlatform.instance.getPlatformVersion();
  }
}


