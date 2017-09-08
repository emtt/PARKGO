package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

/**
 * Created by LENOVO on 30-08-2017.
 */

public class Util {


    public static void alertDialog(Context context, String titulo, String mensaje){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(titulo);
        alertDialog.setMessage(mensaje);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {

            }
        });
        alertDialog.show();

    }

    public static boolean internetStatus(Context context){

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;

    }


}
