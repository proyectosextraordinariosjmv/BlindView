package me.danielvillena.ocr;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

enum Mode {
    ORIENTACION, BUSQUEDA, INDICACIONES;
}

public class Interfaz {

    private Mode mode;
    private boolean listening;
    private TextToSpeech tts;
    private AlertDialog.Builder alertBuilder;

    public String origen, destino;

    double lat, lon;

    public String buscando = "Error";

    private LocationManager locationManager;
    private LocationListener locationListener;

    private TextView stateView;

    public Interfaz(TextToSpeech tts, AlertDialog.Builder alertBuilder, TextView stateView, Context context) {
        mode = null;
        listening = false;

        this.tts = tts;
        this.alertBuilder = alertBuilder;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        this.stateView = stateView;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                lat = location.getLatitude();
                lon = location.getLongitude();

                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                try {
                    Address address = geocoder.getFromLocation(lat, lon, 1).get(0);
                    origen = address.getAddressLine(0).toString();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    //Handle user input
    public void procesarResultado(String entrada, Bitmap bitmap, Context context) {
        HashMap<String, String> ttsParams = new HashMap<>();
        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "TTS_ID");

        if (entrada.isEmpty()) return;

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        entrada = entrada.toLowerCase();

        if (entrada.equals("men")) entrada = "menú";

        Toast.makeText(context, entrada, Toast.LENGTH_SHORT).show();

        setProcessing(true);

        String base64 = Utils.bitmapToBase64(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true));
        if (entrada.startsWith("menú")) {
            tts.speak("Las opciones disponibles son: orientación, pregunta, semáforo, lectura, indicaciones y desactivar", TextToSpeech.QUEUE_FLUSH, ttsParams);
            return;
        } else if (entrada.startsWith("pregunta")) {
            if (entrada.split(" ").length <= 1) {
                tts.speak("Repite el comando, por favor", TextToSpeech.QUEUE_FLUSH, ttsParams);
                return;
            }

            tts.speak(Utils.chatGPT("Tiempo actual: " + new SimpleDateFormat("yyyyMMdd_HHmmss EEEE").format(Calendar.getInstance().getTime()) +  " | " + entrada.split(" ", 2)[1]), TextToSpeech.QUEUE_FLUSH, ttsParams);
            return;
        } else if (entrada.startsWith("buscar")) {
            if (entrada.split(" ").length <= 1) {
                tts.speak("Repite el comando, por favor", TextToSpeech.QUEUE_FLUSH, ttsParams);
                return;
            }
           /* if (mode == Mode.BUSQUEDA) {
                mode = null;
                tts.speak("Modo búsqueda desactivado", TextToSpeech.QUEUE_ADD, null);
                return;
            }
            mode = Mode.BUSQUEDA;*/
            buscando = entrada.split(" ", 2)[1];
            switchMode(Mode.BUSQUEDA);
            return;
        } else if (entrada.startsWith("modo")) {
            if (getModo() == null) {
                tts.speak("No estás en ningún modo", TextToSpeech.QUEUE_FLUSH, ttsParams);
                return;
            }
            tts.speak("Estás en el modo " + getModo().toString(), TextToSpeech.QUEUE_FLUSH, ttsParams);
            return;
        } else if (entrada.startsWith("lectura")) {
            float[] filtro = {-0.60f, -0.60f, -0.60f, -0.60f, 5.81f, -0.60f, -0.60f, -0.60f, -0.60f};
            base64 = Utils.bitmapToBase64(Utils.filtro(bitmap, filtro, context));
            tts.speak(Utils.chatGPTWithImage("Identifica el texto en la siguiente imagen", base64), TextToSpeech.QUEUE_FLUSH, ttsParams);
            return;
        } else if (entrada.startsWith("semáforo")) {
            tts.speak(Utils.chatGPTWithImage(MainActivity.promptSemaforos, base64), TextToSpeech.QUEUE_FLUSH, ttsParams);
            return;
        } else if (entrada.startsWith("orientación")) {
            /*if (mode == Mode.ORIENTACION) {
                mode = null;
                tts.speak("Modo orientación desactivado", TextToSpeech.QUEUE_ADD, null);
                return;
            }
            mode = Mode.ORIENTACION;
            tts.speak("Modo orientación activado", TextToSpeech.QUEUE_ADD, null);*/
            switchMode(Mode.ORIENTACION);
            return;
        } else if (entrada.startsWith("desactivar")) {
            switchMode(null);
            return;
        } else if (entrada.startsWith("indicaciones")) {
            switchMode(Mode.INDICACIONES);
            if (mode == Mode.INDICACIONES) tts.speak("¿A dónde quiere ir?", TextToSpeech.QUEUE_ADD, ttsParams);
            return;
        } else if (mode == Mode.INDICACIONES) {
            if (origen == null) {
                tts.speak("Por favor, compruebe que la ubicación del dispositivo esté activada.", TextToSpeech.QUEUE_ADD, ttsParams);
                switchMode(null);
                return;
            }
            if (!entrada.startsWith("actualizar")) {
                destino = entrada;
                tts.speak("Iniciando trayecto desde " + origen + " hasta " + destino, TextToSpeech.QUEUE_ADD, ttsParams);
            }
            //Toast.makeText(context, Utils.mapsApi(origen, destino), Toast.LENGTH_SHORT).show();
            /*String instrucciones;
            InputStream inputStream = new ByteArrayInputStream(Utils.mapsApi(origen, destino).getBytes());
            JsonReader reader = new JsonReader(inputStream.);
            try {
                JSONObject json = new JSONObject(Utils.mapsApi(origen, destino));
                instrucciones = json.getString("html_instructions");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }*/
            String instrucciones = "";
            String respuesta = Utils.mapsApi(origen, destino);
            String[] tokens = respuesta.split("\"");
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("html_instructions")) {
                    if (i + 2 > tokens.length) return;
                    instrucciones += Utils.decode(tokens[i + 2] + " ").replaceAll("<b>", "").replaceAll("cwbr", "").replaceAll("</b", "").replaceAll("\\b", "").replaceAll("/", "") + ". ";
                }
            }
            tts.speak(instrucciones, TextToSpeech.QUEUE_ADD, ttsParams);
            return;
        }

        tts.speak(Utils.chatGPTWithImage(entrada, base64), TextToSpeech.QUEUE_FLUSH, ttsParams);
    }

    //This code gets executed every {MainActivity::delay} seconds
    public void orientar(Bitmap bitmap, Context context) {
        if (bitmap == null || mode == null) return;

        String result = "";
        String base64 = Utils.bitmapToBase64(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true));
        if (mode == Mode.ORIENTACION) {
            result = Utils.chatGPTWithImage(MainActivity.promptFoto, base64);
            tts.speak(result, TextToSpeech.QUEUE_ADD, null);
            return;
        } else if (mode == Mode.BUSQUEDA) {
            result = Utils.chatGPTWithImage(MainActivity.promptFotoBusqueda + buscando, base64);
            tts.speak(result, TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }

        if (result.toLowerCase().startsWith("s")) {
            switchMode(null);
        } else if (!result.toLowerCase().startsWith("n")) System.out.println("ERROR");
    }

    public void switchMode(Mode newMode) {
        HashMap<String, String> ttsParams = new HashMap<>();
        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "TTS_ID");

        if (mode == newMode) newMode = null;

        if (mode != null) tts.speak("Modo " + mode + " desactivado.", TextToSpeech.QUEUE_ADD, ttsParams);
        mode = newMode;
        if (newMode != null) {
            tts.speak(mode == Mode.BUSQUEDA ? "Buscando " + buscando : "Modo " + newMode + " activado.", TextToSpeech.QUEUE_ADD, ttsParams);
        }
    }

    public void playAudio(Context context, int resid) {
        MediaPlayer mp = MediaPlayer.create(context, resid);
        mp.start();
    }

    public void setProcessing(boolean processing) {
        stateView.setText(processing ? "Procesando..." : "Pulsa para hablar");
        stateView.setTextColor(processing ? Color.GRAY : Color.RED);
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public boolean isListening() {
        return listening;
    }

    public Mode getModo() {
        return mode;
    }
}