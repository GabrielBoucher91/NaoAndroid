package com.aldebaran.helloandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telecom.Call;
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

import org.w3c.dom.Text;

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



    //-----------------Présentation musée de la Civilisation---------------------//
    public void onIntroduction(View view) throws InterruptedException, CallError {
        if(running){
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : INTRO");
            alPosture.goToPosture("StandInit",0.5f);
            String messageToDisplay = "\\rspd=80\\ ^mode(contextual) Mesdames et messieurs, je m'appelle Nao. \\pau=120\\Il me fait plaisir de vous souhaither la bienvenue à ce spectacle sur la robotique et l'intelligence artificielle!";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
        }
    }

    public void onAcceuil(View view) throws InterruptedException, CallError {
        if(running){
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : ACCEUIL");
            String messageToDisplay = "\\rspd=85\\ ^mode(contextual) Veuillez acceuillir votre animateur, Joel Leblanc!";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
        }
    }

    public void onAssoirCollege(View view) throws InterruptedException, CallError {
        if(running){
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : INTRO");
            alPosture.goToPosture("StandInit",0.5f);
            String messageToDisplay = "\\rspd=70\\ ^mode(contextual) Je suis capable de m'assoir.";
            alSpeech.setVolume(0.75f);
            alAnSpeech.say(messageToDisplay);
        }
    }







    //--------------------------Présentation de la JIQ----------------------------//
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

    public void onPetit(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : PETIT");
            String messageToDisplay = "\\rspd=85\\Désolé,\\pau=450\\ j'ai juste de petites pattes.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onCertainement(View view) throws InterruptedException, CallError {
        if(running) {
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : PETIT");
            String messageToDisplay = "\\rspd=85\\Oui, certainement madame.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onIci(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : ICI!");
            String messageToDisplay = "\\rspd=85\\J'arrive! J'arrive! \\pau=500\\Il n'y a pas le feu.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }

    }

    private int presente_toggle=0;

    public void onPresente(View view) throws InterruptedException, CallError {
        if (running) {
            if (presente_toggle==0){
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : PRESENTE 0");
                alPosture.goToPosture("Stand",0.85f);
                String messageToDisplay = "^mode(contextual) \\rspd=85\\Pardon,\\pau=250\\mais pourrais-je me présenter?";
                alSpeech.setVolume(1f);
                alAnSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                presente_toggle++;
            }
            else{
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : PRESENTE 1");
                alPosture.goToPosture("Stand",0.85f);
                String messageToDisplay = "^mode(contextual) \\rspd=85\\Bonjour! Je m'appelle NAO. Je suis le meilleur robot au monde!\\pau=750\\ C'est mon programmeur qui dit ça!";
                alSpeech.setVolume(1f);
                alAnSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                presente_toggle=0;
            }

        }
    }

    public void onAssoir(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime() / 100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : ASSOIR");
            String messageToDisplay = "\\rspd=85\\Je peux m'assoir.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime() / 100000;
        }
    }

    private int taichi_Toggle=0;

    public void onTaichi(View view) throws  InterruptedException, CallError {
        if (running) {
            if (taichi_Toggle==0){
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : TAICHI 1");
                String messageToDisplay = "\\rspd=85\\Je pourrais faire du Taï-Chi, mais mes batteries sont trop faibles. Il faut que je me repose un peu";
                alSpeech.setVolume(1f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                taichi_Toggle++;
            }
            else if (taichi_Toggle==1){
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : TAICHI 2");
                String messageToDisplay = "\\rspd=85\\Maintenant que je suis reposé, est-ce que je peux faire ma démonstration de taï-chi?";
                alSpeech.setVolume(1f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                taichi_Toggle++;
            }
            else{
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : TAICHI 3");
                String messageToDisplay = "\\rspd=85\\Super!";
                alSpeech.setVolume(1f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                taichi_Toggle=0;
            }
        }
    }

    public void onDors(View view) throws  InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : DORS");
            String messageToDisplay = "\\rspd=75\\Fermez les lumières, je veux dormir.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onRealite(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : REVE");
            String messageToDisplay = "\\rspd=75\\ Moi j'adore ça la réalité!.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onReve(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : REVE");
            String messageToDisplay = "\\rspd=75\\ Je pense que j'ai dormi un peu \\pau=250\\ et j'ai fait un beau rêve.";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onFinale(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : REVE");
            String messageToDisplay = "\\rspd=85\\ Pardon madame, je fais de petits pas pour l'homme, mais de grands pas pour la robotique!";
            alSpeech.setVolume(1f);
            alSpeech.say(messageToDisplay);
            startTime = System.nanoTime()/100000;
        }
    }

    public void onBailler(View view) throws InterruptedException, CallError {
        if (running) {
            startTime = System.nanoTime()/100000;
            TextView myTextView = (TextView) findViewById(R.id.statustext);
            myTextView.setText("CONNECTE : BAILLER");
            alPosture.goToPosture("Stand",0.85f);
            String messageToDisplay = "^mode(disabled) \\rspd=85\\^run(animations/Stand/Gestures/ShowSky_8)";
            alAnSpeech.say(messageToDisplay);
        }
    }



    private int fake_Toggle=0;

    public void onFakeout(View view) throws InterruptedException, CallError {
        if (running) {
            if (fake_Toggle==0){
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : FAKEOUT 1");
                String messageToDisplay = "\\rspd=90\\ Haha!";
                alSpeech.setVolume(0.65f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                fake_Toggle++;
            }
            else{
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : FAKEOUT 2");
                String messageToDisplay = "\\rspd=75\\ Mon bras s'est emballé!";
                alSpeech.setVolume(0.65f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                fake_Toggle=0;
            }
        }
    }

    private int merci_Toggle=0;

    public void onMerci(View view) throws InterruptedException, CallError {
        if (running) {
            if (merci_Toggle==0){
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : MERCI 1");
                String messageToDisplay = "\\rspd=80\\ C'est un beau compliment!\\pau=250\\Merci!";
                alSpeech.setVolume(1f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                merci_Toggle++;
            }
            else{
                startTime = System.nanoTime()/100000;
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : MERCI 2");
                String messageToDisplay = "\\rspd=75\\ Merci bien madame.";
                alSpeech.setVolume(1f);
                alSpeech.say(messageToDisplay);
                startTime = System.nanoTime()/100000;
                merci_Toggle=0;
            }
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

    private int handToggle = 0;

    public void onHand(View view) throws InterruptedException, CallError {
        if (running) {
            if (handToggle==0) {
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE : MAIN FERME");
                alMotion.closeHand("RHand");
                handToggle = 1;
                startTime = System.nanoTime()/100000;
            }
            else {
                TextView myTextView = (TextView) findViewById(R.id.statustext);
                myTextView.setText("CONNECTE MAIN OUVERTE");
                alMotion.openHand("RHand");
                handToggle = 0;
                startTime = System.nanoTime()/100000;
            }
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
