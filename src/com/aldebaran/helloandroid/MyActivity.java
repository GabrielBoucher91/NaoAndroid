package com.aldebaran.helloandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.aldebaran.qimessaging.CallError;
import com.aldebaran.qimessaging.Object;
import com.aldebaran.qimessaging.EmbeddedTools;
import com.aldebaran.qimessaging.Session;
import com.aldebaran.qimessaging.helpers.al.ALAnimatedSpeech;
import com.aldebaran.qimessaging.helpers.al.ALAutonomousLife;
import com.aldebaran.qimessaging.helpers.al.ALBattery;
import com.aldebaran.qimessaging.helpers.al.ALLeds;
import com.aldebaran.qimessaging.helpers.al.ALMemory;
import com.aldebaran.qimessaging.helpers.al.ALMotion;
import com.aldebaran.qimessaging.helpers.al.ALRobotPosture;
import com.aldebaran.qimessaging.helpers.al.ALTextToSpeech;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MyActivity extends Activity {
    private static final String TAG = "MyActivity";
    private ALMotion alMotion;
    private ALLeds alLeds;
    private ALTextToSpeech alSpeech;
    private ALAnimatedSpeech alAnSpeech;
    private ALAutonomousLife alAuto;
    private Session session;
    private EditText ip;
    private Context context;
    private ALRobotPosture alPosture;
    private ALBattery alBatt;
    private boolean running = false;
    private long timeout = 90; //secondes - 1:30min
    private long startTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.main);
        ip = (EditText) findViewById(R.id.robot_ip_edit);

        EmbeddedTools ebt = new EmbeddedTools();
        File cacheDir = getApplicationContext().getCacheDir();
        ebt.overrideTempDirectory(cacheDir);
        ebt.loadEmbeddedLibraries();

        setContentView(R.layout.main);
    }

    private void startServiceRoutine() {

        Thread routine = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                if (ip.getText() != null && !ip.getText().toString().equals("")) {
                    session = new Session();
                    running=false;
                    try {
                        String ipAddress = ip.getText().toString();
                        if (!ipAddress.contains(".")) {
                            InetAddress[] inets = InetAddress.getAllByName(ipAddress);
                            if (inets != null && inets.length > 0)
                                ipAddress = inets[0].getHostAddress();
                        }
                        Log.i(TAG, "Ip address : " + ipAddress);
                        session.connect("tcp://" + ipAddress + ":9559").sync(500, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "error", e);
                    }
                    alMotion = new ALMotion(session);
                    alSpeech = new ALTextToSpeech(session);
                    alAnSpeech = new ALAnimatedSpeech(session);
                    alBatt = new ALBattery(session);
                    alLeds = new ALLeds(session);
                    alSpeech.setAsynchronous(true);
                    alAnSpeech.setAsynchronous(true);
                    alPosture = new ALRobotPosture(session);
                    alAuto = new ALAutonomousLife(session);
                    startTime = System.nanoTime()/100000;

                    try{
                        alSpeech.setLanguage("French");
                        alAnSpeech.setBodyLanguageEnabled(true);
                        alBatt.enablePowerMonitoring(false);
                        //alPosture.goToPosture("Stand", 0.5f);
                        alAuto.setState("solitary");
                        running=true;}
                    catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "error", e);
                        running = false;
                    }
                }
            }
        });

        routine.start();
    }

    public void update() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "error", e);
                    }
                    handler.post(new Runnable(){
                        @Override
                        public void run() {
                            if(running) {
                                try {
                                    TextView myTextView = (TextView) findViewById(R.id.statustext);
                                    if (session != null) {
                                        if (session.isConnected()) {
                                            float charge = alBatt.getBatteryCharge();
                                            myTextView.setText("CONNECTE (" + charge + "%)");
                                            //Log.i(TAG, "timeout "+(System.nanoTime()/100000-startTime));
                                            if ((System.nanoTime()/100000 - startTime) > (timeout * 10000))
                                                alMotion.rest();
                                            if (charge < 5.0f)
                                                alPosture.goToPosture("LyingBack", 0.5f);
                                        } else {
                                            myTextView.setText("OFFLINE");
                                            startServiceRoutine();
                                        }
                                    } else{
                                        myTextView.setText("OFFLINE");
                                        startServiceRoutine();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "error", e);
                                }
                            }
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    public void connect(View view) throws InterruptedException, CallError {
        //view.setKeepScreenOn(true);
        startServiceRoutine();
        update();

    }

    public void onQuit(View view) throws InterruptedException, CallError {
        view.setKeepScreenOn(false);
        running=false;
        Thread.sleep(150);
        if (session != null) {
            session.close();
        }
        finish();
        System.exit(0);
    }

    public void onBonjour(View view) throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : INTRO");
            alPosture.goToPosture("StandInit", 0.5f);
            String messageToDisplay = "\\rspd=80\\ ^mode(contextual) Bonjour. \\pau=60\\Je m'appelle Nao. \\pau=120\\Je suis un robot! \\pau=250\\hen fai, je suis un robot humanohide. \\pau=150\\Il y en a aussi qui sont de gros bras industriels, \\pau=80\\des drones qui volent dans le ciel et \\pau=80\\maime dans vos tailaiphones \\pau=30\\des logiciels robots... \\pau=100\\Autant de daifinition des robots que de systaimes avec une certaine intteligence.^start(animations/Stand/Gestures/ShowSky_8) \\pau=100\\Mais je suis le meilleur!^wait(animations/Stand/Gestures/ShowSky_8) ^run(animations/Stand/Emotions/Neutral/Embarrassed_1)";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
        }
    }

    public void onYou(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : YOU!");
            alPosture.goToPosture("Stand", 0.5f);
            String messageToDisplay = "\\rspd=50\\ ^start(animations/Stand/Gestures/You_1) \\vct=30\\J'ai besoin de toi ! ^wait(animations/Stand/Gestures/You_1)";
            alLeds.fadeRGB("AllLeds", (int)0x00990000, 0.1f);
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onBye(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : END");
            alPosture.goToPosture("Stand", 0.5f);
            String messageToDisplay = "^mode(contextual) \\rspd=85\\Je vous remercie de votre attention. ^start(animations/Stand/Gestures/Salute_1) Au revoir ! ^wait(animations/Stand/Gestures/Salute_1)";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onIci(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : ICI!");
            alPosture.goToPosture("Stand", 0.5f);
            String messageToDisplay = "^mode(disabled) \\rspd=100\\Je suis ici, je suis ici !";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }

    }

    public void onOffusque(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : OFFUSQUE");
            alPosture.goToPosture("Stand",0.85f);
            String messageToDisplay = "^mode(contextual) \\rspd=85\\Non !\\pau=250\\Je peux me présenter tout seul !\\pau=250\\Je m'appelle Nao. Je suis un robot humanoide bla bla bla";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void on1Arm(View view) throws InterruptedException, CallError {
        if(running) {
            //Function to lift one arm up, might use a pose.
        }
    }

    public void onFakeout(View view) throws InterruptedException, CallError {
        if (running) {
            //Function to fake giving the card. Might use a different pose and laugh.
            //Could also use animated text to do it.
        }
    }

    public void on2Arms(View view) throws InterruptedException, CallError {
        if (running) {
            //Function to lift 2 arms up, might use a pose.
        }
    }

    public void onWake(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : REVEIL");
            alMotion.wakeUp();
            alPosture.goToPosture("Stand", 0.5f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onRest(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : REPOS");
            alMotion.rest();
            startTime = System.nanoTime()/100000;
        }
    }

    private float velocityX = 0f;
    private float velocityY = 0f;
    private float velocityR = 0f;

    public void onGoToFront(View view) throws InterruptedException, CallError {
        if(running) {
            if(velocityR<0)
                velocityR=0;
            velocityX += 0.1f;
            if(velocityX>1)
                velocityX=1;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("AVANCE " + velocityX);
            alMotion.moveToward(velocityX, velocityY, 0f);
            startTime = System.nanoTime()/100000;
        }
    }


    public void onLying(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : COUCHE");
            alPosture.goToPosture("LyingBack", 0.5f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onStop(View view) throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            velocityX = 0f;
            velocityY = 0f;
            velocityR = 0f;
            alMotion.moveToward(velocityX, velocityY, velocityR);
            alSpeech.stopAll();
            alPosture.goToPosture("StandInit", 0.5f);
            startTime = System.nanoTime()/100000;
        }
    }


    public void onGoToLeft(View view) throws InterruptedException, CallError {
        if(running) {
            if(velocityY<0)
                velocityY=0;
            velocityY += 0.1f;
            if(velocityY>1)
                velocityY=1;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("GAUCHE " + velocityY);
            alMotion.moveToward(velocityX, velocityY, 0f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onTurnLeft(View view) throws InterruptedException, CallError {
        if(running) {
            if(velocityR<0)
                velocityR=0;
            velocityR += 0.1f;
            if(velocityR>1)
                velocityR=1;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("TOURNE " + velocityR);
            alMotion.moveToward(0f, 0f, velocityR);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onTurnRight(View view) throws InterruptedException, CallError {
        if(running) {
            if(velocityR>0)
                velocityR=0;
            velocityR -= 0.1f;
            if(velocityR<-1)
                velocityR=-1;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("TOURNE " + velocityR);
            alMotion.moveToward(0f, 0f, velocityR);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onGoToRight(View view) throws InterruptedException, CallError {
        if(running) {
            if(velocityY>0)
                velocityY=0;
            velocityY -= 0.1f;
            if(velocityY<-1)
                velocityY=-1;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("DROITE " + velocityY);
            alMotion.moveToward(velocityX, velocityY, 0f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onGoToBack(View view) throws InterruptedException, CallError {
        if(running) {
            if(velocityX>0)
                velocityX=0;
            velocityX -= 0.1f;
            if(velocityX<-1)
                velocityX=-1;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("RECULE " + velocityX);
            alMotion.moveToward(velocityX, velocityY, 0f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onSit(View view) throws InterruptedException, CallError {
        if(running) {
            alPosture.goToPosture("Sit", 0.5f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onVideo(View view) throws InterruptedException, CallError {
        if (running) {
           startActivity(new Intent(this, VideoActivity.class));
        }
    }

    public void onConf(View view) throws InterruptedException, CallError {
        if (running) {
            running=false;
            session.close();
            //Log.i(TAG, "change activity focus");
            Intent intent = new Intent(MyActivity.this, ConfActivity.class);
            intent.putExtra("ip", ip.getText().toString());
            startActivity(intent);
        }
    }
}
