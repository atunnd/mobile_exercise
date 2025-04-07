package com.example.exercise2_android_sentiment_analysis;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private EditText inputText;
    private Button analyzeButton;
    private ImageView sentimentIcon;

    private static final String API_URL = "http://10.0.2.2:8000";

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

                // Tạo URL với query string chứa text
                String url = API_URL + "/predict/?text=" + text;  // Truyền tham số qua query string

                // Tạo RequestBody rỗng vì chỉ truyền qua query string
                RequestBody body = RequestBody.create("", MediaType.get("application/json"));

                // Tạo request POST
                Request request = new Request.Builder()
                        .url(url)  // Dùng URL có query string
                        .post(body)  // Sử dụng phương thức POST
                        .addHeader("Content-Type", "application/json") // Đảm bảo Content-Type là application/json
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);

                    // Đảm bảo khóa "sentiment" khớp với tên khóa trả về từ FastAPI
                    String sentiment = jsonResponse.getString("sentiment");

                    runOnUiThread(() -> updateUI(sentiment));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + response.message(), Toast.LENGTH_LONG).show());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }



    private void updateUI(String sentiment) {
        switch (sentiment) {
            case "POS":
                sentimentIcon.setImageResource(R.drawable.smile); // Positive sentiment
                break;
            case "NEG":
                sentimentIcon.setImageResource(R.drawable.sad); // Negative sentiment
                break;
            case "NEU":
                sentimentIcon.setImageResource(R.drawable.neutral); // Neutral sentiment
                break;
            default:
                sentimentIcon.setImageResource(R.drawable.neutral); // Default neutral
                break;
        }
    }
}
