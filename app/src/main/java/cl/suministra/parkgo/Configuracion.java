package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Configuracion extends AppCompatActivity {

    private EditText EDT_Servidor;
    private EditText EDT_PaginaTest;
    private Button BTN_Guardar;
    private ProgressDialog esperaDialog;

    private boolean EXISTE_SERVIDOR_IP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_configuracion);
        this.setTitle("Configuración");
        inicio();
    }

    private void inicio(){

        EDT_Servidor = (EditText) findViewById(R.id.EDT_Servidor);
        EDT_Servidor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_Servidor);
                    label.setText(EDT_Servidor.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Servidor.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_Servidor);
                label.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_Servidor);
                    label.setText("");
                }
            }
        });


        EDT_PaginaTest = (EditText) findViewById(R.id.EDT_PaginaTest);
        EDT_PaginaTest.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_PaginaTest);
                    label.setText(EDT_PaginaTest.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_PaginaTest.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_PaginaTest);
                label.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_PaginaTest);
                    label.setText("");
                }
            }
        });

        getConfiguracion();

        BTN_Guardar = (Button) findViewById(R.id.BTN_Guardar);
        BTN_Guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                esperaDialog = ProgressDialog.show(Configuracion.this, "", "Validando...", true);
                esperaDialog.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                      setConfiguracion();
                        esperaDialog.dismiss();
                    }
                }, 500);
            }
        });

    }

    private void getConfiguracion(){

        EXISTE_SERVIDOR_IP = false;

        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT seccion, clave, valor FROM tb_configuracion", null);
        if (c.moveToFirst()){
          do {
              if (c.getString(0).equals("SERVER") && c.getString(1).equals("IP")) {
                  EXISTE_SERVIDOR_IP = true;
                  EDT_Servidor.setText(c.getString(2));
              }
          }while(c.moveToNext());

        }
        c.close();

    }

    private void setConfiguracion(){

        //***********CONFIGURACION SERVIDOR****************//
        String Servidor = EDT_Servidor.getText().toString();
        if (Servidor == null || Servidor.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_Servidor);
            view.setText("Ingrese " + EDT_Servidor.getHint());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_Servidor.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return;
        }

        String Pagina_Test = EDT_PaginaTest.getText().toString();
        if (Pagina_Test == null || Pagina_Test.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_PaginaTest);
            view.setText("Ingrese " + EDT_PaginaTest.getHint());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_PaginaTest.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return;
        }

        try {
            URL url = new URL(Servidor+Pagina_Test);
            if(!checkUrl(url)){
                return;
            }
        } catch (MalformedURLException e) {
            Util.alertDialog(Configuracion.this, "MalformedURLException Configuración", e.getMessage());
            return;
        } catch (IOException e) {
            Util.alertDialog(Configuracion.this, "IOException Configuración", e.getMessage());
            return;
        }

        try{
            if(EXISTE_SERVIDOR_IP){
                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_configuracion SET valor = '"+EDT_Servidor.getText().toString()+"' WHERE seccion = 'SERVER' AND clave = 'IP'");
            }else{
                AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_configuracion (seccion, clave, valor) VALUES ('SERVER', 'IP', '"+EDT_Servidor.getText()+"');");
            }

        }catch(SQLException e){
            Util.alertDialog(Configuracion.this, "SQLException Configuración", e.getMessage());
            return;
        }

        Util.alertDialog(Configuracion.this, "Configuración", "Configuración grabada correctamente");
    }


    private boolean checkUrl(URL url) throws IOException {
        InputStream stream = null;
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
            int responseCode   = connection.getResponseCode();
            String responseMsj = connection.getResponseMessage();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Util.alertDialog(Configuracion.this, "HTTP Error Configuración", "Error code: "+responseCode+ "\n" + responseMsj);
                return false;
            } else {
                Log.d(AppHelper.LOG_TAG, "responseCode "+responseCode + " "+ responseMsj);
                return true;
            }
        } catch (IOException e) {
                Util.alertDialog(Configuracion.this, "IOException Configuración", e.getMessage());
                return false;
        } finally {
            // Close Stream and disconnect HTTP connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }


    }

}
