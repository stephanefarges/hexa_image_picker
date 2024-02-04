import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'hexa_image_picker_platform_interface.dart';

/// An implementation of [HexaImagePickerPlatform] that uses method channels.
class MethodChannelHexaImagePicker extends HexaImagePickerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('hexa_image_picker');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
  Future<List<Map>?> pick() async {
    return await methodChannel.invokeListMethod('pick');
  }

}
