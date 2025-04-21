import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';

void main() => runApp(const NewsApp());

class NewsApp extends StatelessWidget {
  const NewsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'News Sentiment Analyzer',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
        scaffoldBackgroundColor: Colors.grey[100],
        appBarTheme: AppBarTheme(
          backgroundColor: Colors.lightBlue.shade400,
          elevation: 2,
          titleTextStyle: const TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.w600,
          ),
          iconTheme: const IconThemeData(color: Colors.white),
        ),
      ),
      home: const NewsScreen(),
    );
  }
}

class NewsScreen extends StatefulWidget {
  const NewsScreen({super.key});
  @override
  State<NewsScreen> createState() => _NewsScreenState();
}

class _NewsScreenState extends State<NewsScreen> {
  final List<dynamic> _articles = [];
  final _scrollController = ScrollController();

  final String _newsApiKey = '13dffeaf86c74d89898a73a26da775bb';
  final String _newsBaseUrl = 'https://newsapi.org/v2';
  final String _sentimentBaseUrl = 'https://f597-34-127-48-106.ngrok-free.app';

  int _page = 1;
  bool _isLoading = false, _isAnalyzing = false, _hasMore = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchArticles();
    _scrollController.addListener(() {
      if (_scrollController.position.pixels >=
          _scrollController.position.maxScrollExtent * 0.95 &&
          !_isLoading &&
          !_isAnalyzing &&
          _hasMore) {
        _fetchArticles();
      }
    });
  }

  Future<List<dynamic>> _fetchNews({int page = 1}) async {
    final url = Uri.parse('$_newsBaseUrl/top-headlines?country=us&page=$page&apiKey=$_newsApiKey');
    final response = await http.get(url);
    if (response.statusCode == 200) {
      return json.decode(response.body)['articles'];
    } else {
      throw Exception('Failed to fetch news');
    }
  }

  Future<String> _analyzeSentiment(String text) async {
    final uri = Uri.parse('$_sentimentBaseUrl/predict');
    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: json.encode({'text': text}),
    );
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return data['sentiment'] ?? 'Unknown';
    } else {
      throw Exception('Sentiment analysis failed: ${response.statusCode}');
    }
  }

  Future<void> _fetchArticles() async {
    if (_isLoading || !_hasMore) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final articles = await _fetchNews(page: _page);
      if (articles.isEmpty) {
        _hasMore = false;
        _isLoading = false;
        return;
      }

      setState(() => _isAnalyzing = true);

      final analyzed = await Future.wait(articles.map((article) async {
        final a = Map<String, dynamic>.from(article);
        final content = a['title'] ?? a['description'] ?? '';
        try {
          a['sentiment_label'] = content.isNotEmpty
              ? await _analyzeSentiment(content)
              : 'N/A';
        } catch (_) {
          a['sentiment_label'] = 'Error';
        }
        return a;
      }));

      setState(() {
        _articles.addAll(analyzed);
        _page++;
        _isLoading = _isAnalyzing = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = _isAnalyzing = false;
        _error = e.toString();
      });
      if (mounted && _articles.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_error!)));
      }
    }
  }

  Icon _sentimentIcon(String? label) {
    switch (label?.toLowerCase()) {
      case 'very positive':
        return const Icon(Icons.sentiment_very_satisfied, color: Colors.green);
      case 'positive':
        return const Icon(Icons.sentiment_satisfied, color: Colors.lightGreen);
      case 'neutral':
        return const Icon(Icons.sentiment_neutral, color: Colors.grey);
      case 'negative':
        return const Icon(Icons.sentiment_dissatisfied, color: Colors.orange);
      case 'very negative':
        return const Icon(Icons.sentiment_very_dissatisfied, color: Colors.red);
      case 'error':
        return const Icon(Icons.error_outline, color: Colors.blueGrey);
      case 'n/a':
        return const Icon(Icons.do_not_disturb_alt, color: Colors.grey);
      default:
        return const Icon(Icons.hourglass_empty, color: Colors.blueGrey);
    }
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('News Sentiment Analyzer'), centerTitle: true),
    body: _buildBody(),
  );

  Widget _buildBody() {
    final isBusy = _isLoading || _isAnalyzing;
    if (_articles.isEmpty) {
      if (_error != null) return Center(child: Text(_error!, style: const TextStyle(color: Colors.red)));
      if (_isLoading) return const Center(child: CircularProgressIndicator());
      if (!_hasMore) return const Center(child: Text("No news articles found."));
    }

    return ListView.builder(
      controller: _scrollController,
      itemCount: _articles.length + (isBusy || _hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index == _articles.length) {
          return isBusy
              ? const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Center(child: CircularProgressIndicator()),
          )
              : const SizedBox.shrink();
        }

        final a = _articles[index];
        final image = a['urlToImage'] ?? '';
        final title = a['title'] ?? 'No Title';
        final desc = a['description'] ?? 'No Description';
        final label = a['sentiment_label'];

        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          elevation: 2,
          clipBehavior: Clip.antiAlias,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (image.isNotEmpty)
                Image.network(
                  image,
                  height: 200,
                  width: double.infinity,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => Container(
                    height: 200,
                    color: Colors.grey[300],
                    child: const Center(child: Icon(Icons.broken_image)),
                  ),
                  loadingBuilder: (context, child, progress) => progress == null
                      ? child
                      : Container(
                    height: 200,
                    color: Colors.grey[300],
                    child: Center(
                      child: CircularProgressIndicator(
                        value: progress.expectedTotalBytes != null
                            ? progress.cumulativeBytesLoaded /
                            progress.expectedTotalBytes!
                            : null,
                      ),
                    ),
                  ),
                ),
              Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style: const TextStyle(
                            fontSize: 18, fontWeight: FontWeight.bold),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 8),
                    Text(desc,
                        style: TextStyle(fontSize: 14, color: Colors.grey[700]),
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        _sentimentIcon(label),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            label ?? 'Analyzing...',
                            style: const TextStyle(
                                fontSize: 14, fontWeight: FontWeight.w500),
                          ),
                        )
                      ],
                    )
                  ],
                ),
              )
            ],
          ),
        );
      },
    );
  }
}
