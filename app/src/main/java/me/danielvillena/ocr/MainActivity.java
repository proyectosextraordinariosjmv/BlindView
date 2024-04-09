package me.danielvillena.ocr;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    //ChatGPT API Key
    public static String CHATGPT_API_KEY = "KEY";
    //Google Maps API Key
    public static String MAPS_API_KEY = "KEY";

    //Required Android permissions
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"};
    private final int REQUEST_CODE_PERMISSIONS = 1001;

    //Prompts for ChatGPT API
    public static final String promptFoto = "Eres un asistente virtual para personas invidentes. Describe los contenidos de esta imagen. Responde brevemente, por favor.";
    public static final String promptFotoBusqueda = "Eres un asistente virtual para personas invidentes. Ayuda a la persona a llegar a encontrar lo que quiere. Inicia tu respuesta diciendo SÍ o NO, pero luego elabora brevemente en la respuesta. Responde muy brevemente, por favor. Estás buscando: ";
    public static final String promptSemaforos = "Indica si el semáforo está en verde o rojo. Responde 'El semáforo está en verde', 'El semáforo está en rojo' o 'No hay ningún semáforo'";
    //public static final String promptIndicacionesOrigen = "Tu mensaje debe contener, exclusivamente, el orígen explícito del siguiente mensaje. No debes añadir más información: limítate a indicar el orígen tal cual lo dice en el mensaje. El mensaje es: ";
    //public static final String promptIndicacionesDestino = "Tu mensaje debe contener, exclusivamente, el destino explícito del siguiente mensaje. No debes añadir más información: limítate a indicar el destino tal cual lo dice en el mensaje. El mensaje es: ";
    //public static final String promptIndicaciones = "Eres un asistente virtual para personas invidentes. Extrae las indicaciones de este archivo, e indícalas claramente: ";
    public static final int maxTokens = 1000; //1300

    //Camera Preview
    public PreviewView previewView;

    //Text To Speech
    public TextToSpeech tts;
    public SpeechRecognizer speechRecognizer;
    public Intent speechRecognizerIntent;

    //Debug alerts
    public AlertDialog.Builder alertBuilder;

    public Context context;

    public Handler handler;
    public final int delay = 10000;

    //User Interface
    public Interfaz interfaz;
    public ImageButton settingsButton;
    public TextView stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        //Find widgets
        previewView = findViewById(R.id.preview);
        settingsButton = findViewById(R.id.settingsButton);
        stateView = findViewById(R.id.estado);

        //Request permissions
        if (checkPermissions(getApplicationContext())) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        //Setup text to speech
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String s) {

                    }

                    @Override
                    public void onDone(String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                interfaz.setProcessing(false);
                            }
                        });
                    }

                    @Override
                    public void onError(String s) {

                    }
                });
            }
        });
        tts.setLanguage(new Locale("es", "ES"));
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                interfaz.procesarResultado(data.get(0), previewView.getBitmap(), context);
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        alertBuilder = new AlertDialog.Builder(MainActivity.this);

        //Setup UI
        interfaz = new Interfaz(tts, alertBuilder, stateView, context);

        //Runnable para orientar
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                interfaz.orientar(previewView.getBitmap(), context);
                handler.postDelayed(this, delay);
            }
        }, delay);

        //Necesario para enviar imagenes en base64 en otro hilo
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //Settings button listener
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    //Permissions
    public static boolean checkPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    //Camera preview
    public void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        final ImageCapture imageCapture = builder.setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

        /*previewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interfaz.playAudio(context, R.raw.sound);
                if (!interfaz.isListening()) speechRecognizer.startListening(speechRecognizerIntent);
                else speechRecognizer.stopListening();
                interfaz.click();
            }
        });*/

        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                tts.stop();
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    interfaz.playAudio(context, R.raw.sound);
                    speechRecognizer.startListening(speechRecognizerIntent);
                    interfaz.setListening(true);
                    stateView.setText("Escuchando...");
                    stateView.setTextColor(Color.GREEN);
                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    interfaz.playAudio(context, R.raw.sound);
                    speechRecognizer.stopListening();
                    interfaz.setListening(false);
                    stateView.setText("Pulsa para hablar");
                    stateView.setTextColor(Color.RED);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (checkPermissions(getApplicationContext())) {
                startCamera();
            } else {
                Toast.makeText(this, "Permisos insuficientes...", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    @Override
    public void onInit(int i) {
        System.out.println("TTS inicializado");
    }
}