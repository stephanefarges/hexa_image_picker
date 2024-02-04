import 'package:flutter_test/flutter_test.dart';
import 'package:hexa_image_picker/hexa_image_picker.dart';
import 'package:hexa_image_picker/hexa_image_picker_platform_interface.dart';
import 'package:hexa_image_picker/hexa_image_picker_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockHexaImagePickerPlatform
    with MockPlatformInterfaceMixin
    implements HexaImagePickerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<List<Map>?> pick() {
    // TODO: implement pick
    throw UnimplementedError();
  }
}

void main() {
  final HexaImagePickerPlatform initialPlatform = HexaImagePickerPlatform.instance;

  test('$MethodChannelHexaImagePicker is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelHexaImagePicker>());
  });

  test('getPlatformVersion', () async {
    HexaImagePicker hexaImagePickerPlugin = HexaImagePicker();
    MockHexaImagePickerPlatform fakePlatform = MockHexaImagePickerPlatform();
    HexaImagePickerPlatform.instance = fakePlatform;

    expect(await hexaImagePickerPlugin.getPlatformVersion(), '42');
  });
}
