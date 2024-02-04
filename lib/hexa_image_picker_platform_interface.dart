import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'hexa_image_picker_method_channel.dart';

abstract class HexaImagePickerPlatform extends PlatformInterface {
  /// Constructs a HexaImagePickerPlatform.
  HexaImagePickerPlatform() : super(token: _token);

  static final Object _token = Object();

  static HexaImagePickerPlatform _instance = MethodChannelHexaImagePicker();

  /// The default instance of [HexaImagePickerPlatform] to use.
  ///
  /// Defaults to [MethodChannelHexaImagePicker].
  static HexaImagePickerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [HexaImagePickerPlatform] when
  /// they register themselves.
  static set instance(HexaImagePickerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
  Future<List<Map>?> pick() {
    throw UnimplementedError('pick() has not been implemented.');
  }
}
