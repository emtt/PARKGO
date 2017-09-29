package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;

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
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
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
                Util.alertDialog(context, "HTTP Error verificaURL", "Error code: " + responseCode + "\n" + responseMsj);
                return false;
            } else {
                Log.d(AppHelper.LOG_TAG, "verificaURL responseCode " + responseCode + " " + responseMsj);
                return true;
            }
        } catch (IOException e) {
            Util.alertDialog(context, "IOException verificaURL", e.getMessage());
            return false;
        } finally {
            // Disconnect HTTP connection.
            if (connection != null) {
                connection.disconnect();
            }
        }

    }



}
