package com.example.exercise2_android_sentiment_analysis;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private EditText inputText;
    private Button analyzeButton;
    private ImageView sentimentIcon;

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GOOGLE_API_KEY;  // Secure key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.inputText);
        analyzeButton = findViewById(R.id.analyzeButton);
        sentimentIcon = findViewById(R.id.sentimentIcon);

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString().trim();
                if (!text.isEmpty()) {
                    analyzeSentiment(text);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter text", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void analyzeSentiment(String text) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .callTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build();

                // Create request JSON
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray partsArray = new JSONArray();
                JSONObject textPart = new JSONObject();

                textPart.put("text", "Analyze the sentiment of this text: '" + text + "'. Provide only a sentiment score between -1 (negative) and 1 (positive).");
                partsArray.put(textPart);
                contentObj.put("parts", partsArray);
                contents.put(contentObj);
                requestJson.put("contents", contents);

                RequestBody body = RequestBody.create(requestJson.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    double score = extractSentimentScore(responseData);

                    runOnUiThread(() -> updateUI(score));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + response.message(), Toast.LENGTH_LONG).show());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Extracts sentiment score from Gemini API response
    private double extractSentimentScore(String responseText) {
        Pattern pattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
        Matcher matcher = pattern.matcher(responseText);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }
        return 0; // Default neutral sentiment
    }

    private void updateUI(double score) {
        if (score >= 0) {
            sentimentIcon.setImageResource(R.drawable.smile);
        } else {
            sentimentIcon.setImageResource(R.drawable.sad);
        }
    }
}
