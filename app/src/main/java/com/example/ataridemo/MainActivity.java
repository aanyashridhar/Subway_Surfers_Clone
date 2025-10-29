package com.example.ataridemo;

import android.content.Context;
import android.media.MediaPlayer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.SoundPool;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import android.graphics.Rect;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    GameSurface gameSurface;
    MediaPlayer mediaPlayer;
    SoundPool soundPool;
    float x;
    int change = 0;
    int score = 0;
    int boomSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameSurface = new GameSurface(this);
        setContentView(gameSurface);

        mediaPlayer = MediaPlayer.create(this, R.raw.dodgegamebackgroundmusic);
        mediaPlayer.setVolume(1.0f, 1.0f);
        mediaPlayer.start();

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build();
        boomSound = soundPool.load(this, R.raw.boomsound, 1);


        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        gameSurface.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        gameSurface.resume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        x = sensorEvent.values[0];

        if (x >= 1 || x <= -1) {
            change -= (x * 5);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class GameSurface extends SurfaceView implements Runnable {
        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap player;
        Bitmap cop;
        Bitmap train;
        Bitmap obstacle;
        Bitmap crashedPlayer;
        Bitmap can;
        Paint paintProperty;

        int screenWidth;
        int screenHeight;
        ArrayList<Integer> yPositionsObjects;
        ArrayList<Integer> xPositionsObjects;
        ArrayList<Bitmap> objects;
        ArrayList<Bitmap> ammo;
        ArrayList<Integer> yPositionsAmmo;
        ArrayList<Integer> xPositionsAmmo;

        long lastSpawnTime;
        long startTime;
        long crashStartTime;
        long touchStartTime;
        long fireStartTime;
        long cooldownStartTime;
        boolean gameOver;

        boolean fast;
        boolean crashed;
        boolean firing;
        boolean cooldown;

        public GameSurface(Context context) {
            super(context);

            holder = getHolder();

            player = BitmapFactory.decodeResource(getResources(), R.drawable.jakesubway);
            cop = BitmapFactory.decodeResource(getResources(), R.drawable.copsubway);
            train = BitmapFactory.decodeResource(getResources(), R.drawable.trainsubway);
            obstacle = BitmapFactory.decodeResource(getResources(), R.drawable.obstaclesubway);
            crashedPlayer = BitmapFactory.decodeResource(getResources(), R.drawable.crashedjakesubway);
            can = BitmapFactory.decodeResource(getResources(), R.drawable.spraypaintsubway);

            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);
            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;

            paintProperty = new Paint();
            paintProperty.setTextSize(100);

            change = screenWidth / 2 - player.getWidth() / 2;

            yPositionsObjects = new ArrayList<Integer>();
            xPositionsObjects = new ArrayList<Integer>();
            objects = new ArrayList<Bitmap>();
            ammo = new ArrayList<Bitmap>();
            yPositionsAmmo = new ArrayList<Integer>();
            xPositionsAmmo = new ArrayList<Integer>();

            lastSpawnTime = 0;
            touchStartTime = 0;
        }

        @Override
        public void run() {
            startTime = System.currentTimeMillis();

            while (running == true) {
                if (holder.getSurface().isValid() == false) {
                    continue;
                }
                Canvas canvas= holder.lockCanvas();
                canvas.drawRGB(255,255,255);

                long currentTime = System.currentTimeMillis();

                for (int i = objects.size() - 1; i >= 0; i--) {
                    if (fast) {
                        yPositionsObjects.set(i, yPositionsObjects.get(i) + 10);
                    }
                    else {
                        yPositionsObjects.set(i, yPositionsObjects.get(i) + 5);
                    }

                    if (yPositionsObjects.get(i) > screenHeight) {
                        yPositionsObjects.remove(i);
                        xPositionsObjects.remove(i);
                        objects.remove(i);
                        score++;
                        continue;
                    }

                    canvas.drawBitmap(objects.get(i), xPositionsObjects.get(i), yPositionsObjects.get(i), null);

                    Rect playerBox = new Rect(change, screenHeight - 300, change + player.getWidth(), screenHeight - 300 + player.getHeight());
                    Rect objBox = new Rect(xPositionsObjects.get(i), yPositionsObjects.get(i), xPositionsObjects.get(i) + objects.get(i).getWidth(), yPositionsObjects.get(i) + objects.get(i).getHeight());

                    if (Rect.intersects(playerBox, objBox)) {
                        if (!crashed) {
                            crashed = true;
                            crashStartTime = System.currentTimeMillis();
                            soundPool.play(boomSound, 1, 1, 0, 0, 1);
                        }
                    }
                }

                for (int i = yPositionsAmmo.size() - 1; i >= 0; i--) {
                    yPositionsAmmo.set(i, yPositionsAmmo.get(i) - 20);

                    if (yPositionsAmmo.get(i) < 0) {
                        xPositionsAmmo.remove(i);
                        yPositionsAmmo.remove(i);
                        ammo.remove(i);
                        continue;
                    }

                    canvas.drawBitmap(ammo.get(i), xPositionsAmmo.get(i), yPositionsAmmo.get(i), paintProperty);

                    for (int j = objects.size() - 1; j >= 0; j--) {
                        Rect ammoBox = new Rect(xPositionsAmmo.get(i) - can.getWidth(), yPositionsAmmo.get(i) - can.getHeight(), xPositionsAmmo.get(i) + can.getWidth(), yPositionsAmmo.get(i) + can.getHeight());
                        Rect objBox = new Rect(
                                xPositionsObjects.get(j) + 50,
                                yPositionsObjects.get(j) + 15,
                                xPositionsObjects.get(j) + objects.get(j).getWidth() - 50,
                                yPositionsObjects.get(j) + objects.get(j).getHeight() - 15
                        );

                        if (Rect.intersects(ammoBox, objBox)) {
                            xPositionsAmmo.remove(i);
                            yPositionsAmmo.remove(i);
                            ammo.remove(i);
                            xPositionsObjects.remove(j);
                            yPositionsObjects.remove(j);
                            objects.remove(j);
                            score++;
                            break;
                        }
                    }
                }

                if (change < 0) {
                    change = 0;
                }
                if (change > gameSurface.screenWidth - gameSurface.player.getWidth()) {
                    change = gameSurface.screenWidth - gameSurface.player.getWidth();
                }
                canvas.drawText("Score: " + score,50,200, paintProperty);

                if (firing && currentTime - fireStartTime > 3000) {
                    firing = false;
                    cooldown = true;
                    cooldownStartTime = currentTime;
                }

                if (cooldown && currentTime - cooldownStartTime > 3000) {
                    cooldown = false;
                }

                if (firing && currentTime % 200 < 16) {
                    spawnAmmo();
                }

                Bitmap playerVersion = null;
                if (crashed) {
                    playerVersion = crashedPlayer;
                }
                else {
                    playerVersion = player;
                }
                canvas.drawBitmap(playerVersion,change,screenHeight - 300,null);

                if (crashed) {
                    if (currentTime - crashStartTime > 1000) {
                        crashed = false;
                    }
                }

                if (currentTime - startTime >= 30000) {
                    gameOver = true;
                    running = false;
                    paintProperty.setTextSize(150);
                    canvas.drawText("Time's Up!", screenWidth / 2 - 300, screenHeight / 2, paintProperty);
                }

                holder.unlockCanvasAndPost(canvas);

                if (!gameOver && currentTime - lastSpawnTime > 1500) {
                    spawnObjects();
                    lastSpawnTime = currentTime;
                }
            }
        }

        public void resume(){
            running = true;
            gameOver = false;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchStartTime = System.currentTimeMillis();

                if (!cooldown) {
                    firing = true;
                    fireStartTime = touchStartTime;
                }
            }
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                long duration = System.currentTimeMillis() - touchStartTime;

                if (duration < 200) {
                    fast = !fast;
                }

                if (firing) {
                    firing = false;
                    cooldown = true;
                    cooldownStartTime = System.currentTimeMillis();
                }
            }
            return true;
        }

        public void spawnObjects() {
            Bitmap obj = null;
            int r = (int)(Math.random()*3);
            switch (r) {
                case 0:
                    obj =  cop;
                    break;
                case 1:
                    obj = train;;
                    break;
                case 2:
                    obj = obstacle;;
                    break;
            }

            int x = (int)(Math.random()*(screenWidth - obj.getWidth()));

            if (objects.size() > 0) {
                int lastX = xPositionsObjects.get(xPositionsObjects.size() - 1);
                int attempts = 0;
                do {
                    x = (int) (Math.random() * (screenWidth - obj.getWidth()));
                    attempts++;
                } while (x < lastX + obj.getWidth() + 100 && x > lastX - obj.getWidth() - 100 && attempts < 5);
            }

            int y = -100;

            objects.add(obj);
            xPositionsObjects.add(x);
            yPositionsObjects.add(y);
        }

        public void spawnAmmo() {
            int x = change + player.getWidth() / 2;
            int y = screenHeight - 300;

            ammo.add(can);
            xPositionsAmmo.add(x);
            yPositionsAmmo.add(y);
        }
    }
}