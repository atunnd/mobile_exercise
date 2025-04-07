import 'package:flutter/material.dart';
import 'package:splashscreen/splashscreen.dart';
import 'package:tflite/tflite.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: SplashScreenPage(),
    );
  }
}

class SplashScreenPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return SplashScreen(
      seconds: 3,  // Hiển thị splash screen trong 3 giây
      navigateAfterSeconds: HomePage(),  // Chuyển hướng sau khi hết thời gian
      image: Image.asset('assets/logo.png'),
      backgroundColor: Colors.blue,
      styleTextUnderTheLoader: TextStyle(),
      loaderColor: Colors.white,
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final ImagePicker _picker = ImagePicker();
  XFile? _image;  // Biến lưu trữ ảnh đã chọn/chụp
  String? _classfying_res;

  bool _isInterpreterBusy = false;

  @override
  void initState() {
    super.initState();
    _loadModel();  // Load mô hình khi bắt đầu
  }

  // Hàm load mô hình TensorFlow Lite
  Future<void> _loadModel() async {
    try {
      String? res = await Tflite.loadModel(
        model: "assets/model_unquant.tflite", // coco_ssd_mobilenet_v3_small_2020_01_14
        labels: "assets/labels.txt",
        numThreads: 1,
        isAsset: true,
        useGpuDelegate: false,
      );
      print("Model loaded successfully: $res");
    } catch (e) {
      print("Error loading model: $e");
    }
  }

  // Hàm chọn ảnh từ thư viện
  Future<void> _selectImage() async {
    if (_isInterpreterBusy) {
      print("Interpreter is busy, please wait...");
      return;
    }

    final XFile? selectedImage = await _picker.pickImage(source: ImageSource.gallery);
    setState(() {
      _image = selectedImage;
    });
    if (_image != null) {
      await _classifyImage(File(_image!.path));
    }
  }

  // Hàm chụp ảnh
  Future<void> _captureImage() async {
    if (_isInterpreterBusy) {
      print("Interpreter is busy, please wait...");
      return;
    }

    final XFile? capturedImage = await _picker.pickImage(source: ImageSource.camera);
    setState(() {
      _image = capturedImage;
    });
    if (_image != null) {
      await _classifyImage(File(_image!.path));
    }
  }

  // Hàm phân loại ảnh
  Future<void> _classifyImage(File image) async {
    setState(() {
      _isInterpreterBusy = true;
    });

    try {
      var output = await Tflite.runModelOnImage(
        path: image.path,
        numResults: 2,
        threshold: 0.5,
      );

      print("Ket qua: $output");

      if (output != null && output.isNotEmpty) {
        setState(() {
          _classfying_res = '${output[0]['label']}';
        });
      }
    } catch (e) {
      print("Error classifying image: $e");
    } finally {
      setState(() {
        _isInterpreterBusy = false;
      });
    }
  }

  @override
  void dispose() {
    // Đóng mô hình khi widget bị huỷ bỏ
    Tflite.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Home Page')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),  // Đặt padding cho toàn bộ body
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Title and Subtitle
            Container(
              padding: EdgeInsets.all(10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Coding Cafe',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Cats & Dogs Classifier App',
                    style: TextStyle(fontSize: 16, color: Colors.grey),
                  ),
                ],
              ),
            ),

            // Image display
            Container(
              padding: EdgeInsets.symmetric(vertical: 10),
              child: _image == null
                  ? Center(child: Icon(Icons.image, size: 100, color: Colors.grey))  // Biểu tượng ảnh mặc định
                  : Image.file(
                File(_image!.path),
                height: 200,
                width: double.infinity,
                fit: BoxFit.cover,
              ),
            ),

            // Classification Result
            Container(
              padding: EdgeInsets.symmetric(vertical: 10),
              child: Text(
                'This is',  // Dòng hiển thị "This is"
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
            ),

            // Classification result text
            Container(
              padding: EdgeInsets.symmetric(vertical: 10),
              child: Text(
                _classfying_res ?? 'Not classified yet',  // Kết quả phân loại hoặc thông báo nếu chưa phân loại
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
            ),

            // Buttons to capture/select image
            Container(
              padding: EdgeInsets.symmetric(vertical: 10),
              child: Align(
                alignment: Alignment.center,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    ElevatedButton(
                      onPressed: _captureImage,
                      child: Text('Capture a photo'),
                    ),
                    ElevatedButton(
                      onPressed: _selectImage,
                      child: Text('Select a photo'),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
