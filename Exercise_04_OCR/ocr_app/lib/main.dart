import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:flutter_tesseract_ocr/flutter_tesseract_ocr.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: OCRScreen(),
    );
  }
}

abstract class ITextRecognizer {
  Future<String> processImage(String imgPath);
}

class TesseractTextRecognizer extends ITextRecognizer {
  @override
  Future<String> processImage(String imgPath) async {
    final res = await FlutterTesseractOcr.extractText(imgPath, args: {
      "psm": "4",
      "preserve_interword_spaces": "1",
    });
    return res;
  }
}

class OCRScreen extends StatefulWidget {
  @override
  _OCRScreenState createState() => _OCRScreenState();
}

class _OCRScreenState extends State<OCRScreen> {
  final ImagePicker _picker = ImagePicker();
  String _ocrResult = '';
  bool _isProcessing = false;  // Flag for showing loading indicator

  // Function to pick an image from the gallery or camera
  Future<void> _pickImage(BuildContext context, bool isLongPress) async {
    setState(() {
      _isProcessing = true;  // Show loading indicator
    });

    XFile? image;
    try {
      if (isLongPress) {
        image = await _picker.pickImage(source: ImageSource.gallery);
      } else {
        image = await _picker.pickImage(source: ImageSource.camera);
      }

      if (image != null) {
        // Perform OCR on the selected image
        String result = await FlutterTesseractOcr.extractText(image.path);
        setState(() {
          _ocrResult = result;
        });
      } else {
        setState(() {
          _ocrResult = 'No image selected';
        });
      }
    } catch (e) {
      setState(() {
        _ocrResult = 'Error: $e';
      });
    } finally {
      setState(() {
        _isProcessing = false;  // Hide loading indicator
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('OCR Scanner'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // OCR result container
            Container(
              width: 300,
              height: 200,
              color: Colors.grey[200],
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: SingleChildScrollView(
                  child: Text(
                    _ocrResult.isNotEmpty ? _ocrResult : 'OCR result will appear here.',
                    style: TextStyle(fontSize: 16, color: Colors.black),
                  ),
                ),
              ),
            ),
            SizedBox(height: 20),
            // Image selection container
            GestureDetector(
              onTap: () => _pickImage(context, false), // Tap: take image with camera
              onLongPress: () => _pickImage(context, true), // Long press: select image from gallery
              child: Container(
                width: 300,
                height: 150,
                color: Colors.blueAccent,
                child: Center(
                  child: Text(
                    'Tap to take a picture\nLong press to select from gallery',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.white, fontSize: 16),
                  ),
                ),
              ),
            ),
            SizedBox(height: 20),
            // Show loading indicator while processing the image
            if (_isProcessing)
              CircularProgressIndicator(),
          ],
        ),
      ),
    );
  }
}
