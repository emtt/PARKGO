package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by LENOVO on 30-08-2017.
 */

public class Util {


    public static void alertDialog(Context context, String titulo, String mensaje) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(titulo);
        alertDialog.setMessage(mensaje);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialog.show();

    }

    public static boolean internetStatus(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;

    }

    public static boolean verificaURL(Context context, URL url){

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(AppHelper.getTimeout());
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(AppHelper.getTimeout());
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            int responseCode = connection.getResponseCode();
            String responseMsj = connection.getResponseMessage();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Util.alertDialog(context, "HTTP Error Util verificaURL", "Error code: " + responseCode + "\n" + responseMsj);
                return false;
            } else {
                Log.d(AppHelper.LOG_TAG, "Util verificaURL responseCode " + responseCode + " " + responseMsj);
                return true;
            }
        } catch (IOException e) {
            Util.alertDialog(context, "IOException Util verificaURL", e.getMessage());
            return false;
        } finally {
            // Disconnect HTTP connection.
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    public static String nombreDiaSemana(String fecha){

        String nombre_dia = "";
        try {
            nombre_dia = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(new SimpleDateFormat("yyyy-M-d").parse(fecha));
            switch (nombre_dia.toUpperCase()){
                case "MONDAY":
                    nombre_dia = "LUNES";
                    break;
                case "TUESDAY":
                    nombre_dia = "MARTES";
                    break;
                case "WEDNESDAY":
                    nombre_dia = "MIERCOLES";
                    break;
                case "THURSDAY":
                    nombre_dia = "JUEVES";
                    break;
                case "FRIDAY":
                    nombre_dia = "VIERNES";
                    break;
                case "SATURDAY":
                    nombre_dia = "SABADO";
                    break;
                case "SUNDAY":
                    nombre_dia = "DOMINGO";
                    break;
            }

        } catch (ParseException e) {
            Log.d(AppHelper.LOG_TAG, "ParseException Util nombreDiaSemana "+ e.getMessage());
        }

        Log.d(AppHelper.LOG_TAG, "Util nombreDiaSemana "+ nombre_dia);
        return nombre_dia;
    }

    public static int calcularPrecio(int total_minutos, int espacios, int valor_prepago, int valor_efectivo){
        int precio = 0;
        if (total_minutos > 0) {
            switch (AppHelper.getTipo_cobro()) {
                case 1: //POR MINUTO
                    precio = (total_minutos * AppHelper.getValor_minuto()) - (valor_prepago + valor_efectivo);
                    break;
                case 2: //POR TRAMO
                    double tramos_vencidos = ( (double) total_minutos /  (double) AppHelper.getMinutos_tramo());
                    precio = (int) (Math.ceil(tramos_vencidos) * AppHelper.getValor_tramo()) - (valor_prepago + valor_efectivo);
                    break;
                case 3: //PRIMER TRAMO + VALOR * MINUTO
                    int minutos_restantes = (total_minutos - AppHelper.getMinutos_tramo());
                    if (minutos_restantes > 0 ) {
                        precio = (minutos_restantes * AppHelper.getValor_minuto()) - (valor_prepago + valor_efectivo);
                        precio = precio + AppHelper.getValor_tramo();
                    }else{
                        precio = AppHelper.getValor_tramo();
                    }
                    break;
            }
        }
        return precio * espacios;
    }


    public static int redondearPrecio(int precio_total, int descuento_porciento){
        //calcula el descuento.
        int precio_descuento = Math.round((precio_total * descuento_porciento) / 100);
        //resta el descuento al precio total.
        int precio_final = precio_total - precio_descuento;
        //redondea el precio.
        int precio_redondeado = precio_final - precio_final % 10;
        return precio_redondeado;
    }

    public static String formateaLineaEtiqueta(String linea){
        if (linea.length() > AppHelper.getVoucher_largo_linea()){
            return linea.substring(0, AppHelper.getVoucher_largo_linea());
        }else{
            return linea;
        }
    }

}
