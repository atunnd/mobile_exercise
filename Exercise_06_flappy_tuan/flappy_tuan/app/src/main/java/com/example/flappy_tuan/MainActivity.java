package com.example.flappy_tuan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    ImageView playBtn;
    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Màn hình chính

        playBtn = findViewById(R.id.playBtn);
        gameView = new GameView(this); // khởi tạo GameView

        playBtn.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Game Start!", Toast.LENGTH_SHORT).show();
            setContentView(gameView); // chuyển sang GameView
        });
    }

    public void restartGame() {
        setContentView(R.layout.activity_main); // trở về màn hình chính
        playBtn = findViewById(R.id.playBtn);
        gameView = new GameView(this);
        playBtn.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Game Start!", Toast.LENGTH_SHORT).show();
            setContentView(gameView);
        });
    }

    // ================= GameView + AI =================
    public class GameView extends View {
        Bird bird;
        Pipe pipe;
        BirdBrain birdBrain;
        Paint paint = new Paint();
        int score = 0;

        private Bitmap birdBitmap; // Khai báo biến ảnh cho chim
        private Bitmap backgroundBitmap;

        public GameView(Context context) {
            super(context);
            bird = new Bird(200, 500);
            pipe = new Pipe(800, 300);
            birdBrain = new BirdBrain();

            birdBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cat);
            int newWidth = 100;
            int newHeight = 100;
            birdBitmap = Bitmap.createScaledBitmap(birdBitmap, newWidth, newHeight, false);
            backgroundBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.background);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();


            int x = (canvasWidth - backgroundBitmap.getWidth()) / 2;
            int y = (canvasHeight - backgroundBitmap.getHeight()) / 2;


            canvas.drawBitmap(backgroundBitmap, null, new Rect(0, 0, canvasWidth, canvasHeight), paint);


            bird.update();
            pipe.update();
            birdBrain.update(bird, pipe);


            canvas.drawBitmap(birdBitmap, bird.x - birdBitmap.getWidth() / 2, bird.y - birdBitmap.getHeight() / 2, paint);

            // Draw pipe
            if (pipe.x + 100 < bird.x) {
                score++;
            }

            paint.setColor(Color.GREEN);
            canvas.drawRect(pipe.x, 0, pipe.x + 100, pipe.gapY, paint);
            canvas.drawRect(pipe.x, pipe.gapY + pipe.gapSize, pipe.x + 100, getHeight(), paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(100);
            canvas.drawText("Score: " + score, 50, 100, paint);
            // Check game over
            if (pipe.x < bird.x + birdBitmap.getWidth() / 2 && pipe.x + 100 > bird.x - birdBitmap.getWidth() / 2) {
                // Check vertical collision (bird should be inside the gap)
                if (bird.y - birdBitmap.getHeight() / 2 < pipe.gapY || bird.y + birdBitmap.getHeight() / 2 > pipe.gapY + pipe.gapSize) {
                    // Game over, restart the game
                    ((MainActivity) getContext()).restartGame();
                    return;
                }
            }

            if (bird.y > getHeight() || bird.y < 0) {
                ((MainActivity) getContext()).restartGame();
                return;
            }

            invalidate(); // vẽ lại frame tiếp theo
        }
    }

    // ================= Bird =================
    public class Bird {
        float x, y, velocity = 0;

        public Bird(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            velocity += 1.5f;
            y += velocity;
        }

        public void jump() {
            velocity = -15;
        }
    }

    // ================= Pipe =================
    public class Pipe {
        float x;
        float gapY;
        float gapSize = 1000;
        Random random = new Random();

        public Pipe(float x, float gapY) {
            this.x = x;
            this.gapY = gapY;
        }

        public void update() {
            x -= 5;
            if (x < -100) {
                x = 1000;
                gapY = 200 + random.nextInt(400);
            }
        }
    }

    // ================= BirdBrain (AI) =================
    public class BirdBrain {
        NeuralNetwork network;

        public BirdBrain() {
            network = new NeuralNetwork(3, 1);
        }

        public void update(Bird bird, Pipe pipe) {
            float[] inputs = new float[3];
            inputs[0] = bird.y / 1000f;
            inputs[1] = pipe.x / 1000f;
            inputs[2] = pipe.gapY / 1000f;

            float output = network.feedForward(inputs)[0];

            if (output > 0.5f) {
                bird.jump();
            }
        }
    }

    // ================= Mạng Neural đơn giản =================
    public class NeuralNetwork {
        int inputSize, outputSize;
        float[][] weights;

        public NeuralNetwork(int inputSize, int outputSize) {
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            weights = new float[outputSize][inputSize];
            randomizeWeights();
        }

        public void randomizeWeights() {
            Random rand = new Random();
            for (int i = 0; i < outputSize; i++) {
                for (int j = 0; j < inputSize; j++) {
                    weights[i][j] = rand.nextFloat() * 2 - 1;
                }
            }
        }

        public float[] feedForward(float[] inputs) {
            float[] outputs = new float[outputSize];
            for (int i = 0; i < outputSize; i++) {
                float sum = 0;
                for (int j = 0; j < inputSize; j++) {
                    sum += inputs[j] * weights[i][j];
                }
                outputs[i] = sigmoid(sum);
            }
            return outputs;
        }

        private float sigmoid(float x) {
            return (float) (1 / (1 + Math.exp(-x)));
        }
    }
}