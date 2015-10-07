package com.aldebaran.helloandroid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aldebaran.qimessaging.Application;
import com.aldebaran.qimessaging.CallError;
import com.aldebaran.qimessaging.Session;
import com.aldebaran.qimessaging.helpers.al.ALAnimatedSpeech;
import com.aldebaran.qimessaging.helpers.al.ALAutonomousLife;
import com.aldebaran.qimessaging.helpers.al.ALBattery;
import com.aldebaran.qimessaging.helpers.al.ALLeds;
import com.aldebaran.qimessaging.helpers.al.ALMemory;
import com.aldebaran.qimessaging.helpers.al.ALMotion;
import com.aldebaran.qimessaging.helpers.al.ALRobotPosture;
import com.aldebaran.qimessaging.helpers.al.ALTextToSpeech;
import java.util.concurrent.TimeUnit;

public class ConfActivity extends Activity {
    private static final String TAG = "ConfActivity";
    private Application application;
    private ALMotion alMotion;
    private ALLeds alLeds;
    private ALTextToSpeech alSpeech;
    private ALAnimatedSpeech alAnSpeech;
    private ALAutonomousLife alAuto;
    private Session session;
    private String ip;
    private Context context;
    private ALRobotPosture alPosture;
    private ALBattery alBatt;
    private boolean running = false;
    private long timeout = 3600; //secondes
    private long startTime;
    private int currAction =0, counter =0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.conf);
        Bundle bundle = getIntent().getExtras();
        ip = bundle.getString("ip");

        startServiceRoutine();
        update();
        Actions();
    }

    private void startServiceRoutine() {

        Thread routine = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                session = new Session();
                running=false;
                    try{
                        session.connect("tcp://" + ip + ":9559").sync(500, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "error", e);
                        onDestroy();
                    }
                    application = new Application();
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
                        onDestroy();
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
                                            Log.i(TAG, "timeout "+(System.nanoTime()/100000-startTime));
                                            if ((System.nanoTime()/100000 - startTime) > (timeout * 10000))
                                                alMotion.rest();
                                            if (charge < 5.0f)
                                                alPosture.goToPosture("LyingBack", 0.5f);
                                        } else{
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

    public void Actions() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(150);
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
                                    TextView myTextView = (TextView) findViewById(R.id.confidtext);
                                    myTextView.setText("CONF " + counter);
                                    switch(currAction){
                                        case 0:
                                            break;
                                        case 1:
                                            Intro();
                                            currAction=0;
                                            break;
                                        case 2:
                                            Me();
                                            currAction=0;
                                            break;
                                        case 3:
                                            Sit();
                                            currAction=0;
                                            break;
                                        case 4:
                                            Art();
                                            currAction=0;
                                            break;
                                        case 5:
                                            End();
                                            currAction=0;
                                            break;
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

    public void onNext(View view) throws InterruptedException, CallError {
        counter+=1;
        if(counter>5)
            counter=0;
        currAction+=counter;
    }

    public void onIntro(View view) throws InterruptedException, CallError {
        Intro();
    }

    public void Intro() throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : INTRO");
            alPosture.goToPosture("StandInit", 0.5f);
            //alMotion.walkInit();
            //String messageToDisplay = "Oui oui, j'y vais";
            //alSpeech.setVolume(0.1f);
            //Thread.sleep(10);
            //alSpeech.say(messageToDisplay);
            //alMotion.moveTo(2f, 0f, 0.5f);
            alMotion.moveTo(0f, 0f, 1.4f);
            String messageToDisplay = "\\rspd=80\\ ^mode(contextual) Bonjour. \\pau=120\\Je m'appelle Nao. \\pau=200\\Je suis un robot! \\pau=350\\hen fai, je suis un robot humanohide. \\pau=200\\Il y en a aussi qui sont de gros bras industriels, \\pau=120\\des drones qui volent dans le ciel et \\pau=80\\maime dans vos tailaiphones \\pau=30\\des logiciels robots... \\pau=100\\Autant de daifinition des robots que de systaimes avec une certaine intteligence.^start(animations/Stand/Gestures/ShowSky_8) \\pau=100\\Mais je suis le meilleur!^wait(animations/Stand/Gestures/ShowSky_8) ^run(animations/Stand/Emotions/Neutral/Embarrassed_1) Voici votre confairencier : ^start(animations/Stand/Gestures/Salute_3) David SaintOnge. ^wait(animations/Stand/Gestures/Salute_3)";
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void Sit() throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            String messageToDisplay = "\\rspd=65\\^start(animations/Stand/Emotions/Negative/Bored_1) Bon... d'accord... ^wait(animations/Stand/Emotions/Negative/Bored_1)";
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            alPosture.goToPosture("Sit", 0.5f);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onArt(View view) throws InterruptedException, CallError {
        Art();
    }

    public void Art() throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : ART?");
            alPosture.goToPosture("Sit", 0.5f);
            String messageToDisplay = "^mode(contextual) Les artistes ? ^run(animations/Sit/Emotions/Positive/Mocker_1) \\pau=100\\Et pourquoi ?";
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onMe(View view) throws InterruptedException, CallError {
        Me();
    }

    public void Me() throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : MOI!");
            alPosture.goToPosture("Stand", 0.5f);
            String messageToDisplay = "\\rspd=90\\ ^start(animations/Stand/Gestures/ShowSky_8) Comme moi! \\pau=100\\Je suis le robot commercial le plus avansai sur le marchai!^wait(animations/Stand/Gestures/ShowSky_8)";
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onYou(View view) throws InterruptedException, CallError {
        You();
    }

    public void You() throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : YOU!");
            alPosture.goToPosture("Stand", 0.5f);
            String messageToDisplay = "\\rspd=50\\ ^start(animations/Stand/Gestures/You_1) \\vct=30\\J'ai besoin de... toi ! ^wait(animations/Stand/Gestures/You_1)";
            alLeds.fadeRGB("AllLeds", (int)0x00990000, 0.1f);
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onEnd(View view) throws InterruptedException, CallError {
        End();
    }

    public void End() throws InterruptedException, CallError {
        if(running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : END");
            String messageToDisplay = "^run(animations/Sit/Emotions/Neutral/AskForAttention_1)";
            alAnSpeech.say(messageToDisplay);
            alAnSpeech.say(messageToDisplay);
            alPosture.goToPosture("Stand", 0.5f);
            messageToDisplay = "^mode(contextual) \\rspd=85\\Il faut remercier le Theatrre de La Border et l'organisation des Matins Craiatifs pour avoir rendu cette confairence possible, et surtout le laboratoire de robotique pour me permettre d'aitrre ici! ^start(animations/Stand/Gestures/Salute_1) Au revoir ! ^wait(animations/Stand/Gestures/Salute_1)";
            alSpeech.setVolume(1f);
            alAnSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    @Override
    protected void onDestroy() {
        running=false;
        session.close();
        application.stop();
        super.onDestroy();
    }
}
