package me.danielvillena.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicConvolve3x3;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;

public class Utils {

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public static String chatGPT(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        String model = "gpt-3.5-turbo";

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + MainActivity.CHATGPT_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");

            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return extractMessageFromJSONResponse(response.toString());

        } catch (IOException e) {
            return "La clave de API proporcionada es incorrecta.";
        }
    }

    public static String mapsApi(String origen, String destino) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origen + "&destination=" + destino + "&key=" + MainActivity.MAPS_API_KEY + "&language=es-ES";
        InputStream inputStream;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder;
        try {
            inputStream = new URL(url).openStream();
            try {

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                stringBuilder = new StringBuilder();

                int aux;
                while ((aux = bufferedReader.read()) != -1) {
                    stringBuilder.append((char) aux);
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringBuilder.toString();
    }

    public static String chatGPTWithImage(String prompt, String base64) {
        String url = "https://api.openai.com/v1/chat/completions";
        String model = "gpt-4-vision-preview";

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + MainActivity.CHATGPT_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");

            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": [{\"type\": \"text\", \"text\": \"" + prompt + "\"}, {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64, {" + base64 + "}\"}}]}], \"max_tokens\": " + MainActivity.maxTokens + "}";
            //String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": [{\"type\": \"text\", \"text\": \"" + prompt + "\"}, {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64, {" + base64 + "}\"}}]}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            //return response.toString();
            return extractMessageFromJSONResponse(response.toString());

        } catch (IOException e) {
            return "La clave de API proporcionada es incorrecta.";
        }
    }

    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content") + 11;

        int end = response.indexOf("\"", start);

        String message = response.substring(start, end);
        //message.replaceAll("\\n", " ").replaceAll("\\u00e1", "á").replaceAll("\\u00e9", "é").replaceAll("\\u00ed", "í").replaceAll("\\u00f3", "ó").replaceAll("\\u00fa", "ú").replaceAll("\\u00c1", "Á").replaceAll("\\u00c9", "É").replaceAll("\\u00cd", "Í").replaceAll("\\u00d3", "Ó").replaceAll("\\u00da", "Ú").replaceAll("\\u00f1", "ñ").replaceAll("\\u00d1", "Ñ");
        message = decode(message);
        return message;
    }

    public static final String decode(final String in) {
        String working = in;
        int index;
        index = working.indexOf("\\u");
        while(index > -1) {
            int length = working.length();
            if(index > (length-6))break;
            int numStart = index + 2;
            int numFinish = numStart + 4;
            String substring = working.substring(numStart, numFinish);
            int number = Integer.parseInt(substring,16);
            String stringStart = working.substring(0, index);
            String stringEnd   = working.substring(numFinish);
            working = stringStart + ((char)number) + stringEnd;
            index = working.indexOf("\\u");
        }
        return working;
    }

    //Image sharpening is needed in order to get ChatGPT recognize text in the images
    public static Bitmap filtro(Bitmap original, float[] radius, Context context) {
        Bitmap bitmap = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);

        RenderScript rs = RenderScript.create(context);

        Allocation allocIn = Allocation.createFromBitmap(rs, original);
        Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

        ScriptIntrinsicConvolve3x3 convolution = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        convolution.setInput(allocIn);
        convolution.setCoefficients(radius);
        convolution.forEach(allocOut);

        allocOut.copyTo(bitmap);
        rs.destroy();

        return bitmap;
    }

}
