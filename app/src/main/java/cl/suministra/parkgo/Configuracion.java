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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

    private boolean EXISTE_SERVIDOR_URL;
    private boolean EXISTE_SERVIDOR_PAGINA_TEST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_configuracion);
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

        EXISTE_SERVIDOR_URL = false;
        EXISTE_SERVIDOR_PAGINA_TEST = false;

        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT seccion, clave, valor FROM tb_configuracion", null);
        if (c.moveToFirst()){
          do {
              if (c.getString(0).equals("SERVER") && c.getString(1).equals("URL")) {
                  EXISTE_SERVIDOR_URL = true;
                  EDT_Servidor.setText(c.getString(2));
              }else if (c.getString(0).equals("SERVER") && c.getString(1).equals("PAGINA_TEST")) {
                  EXISTE_SERVIDOR_PAGINA_TEST = true;
                  EDT_PaginaTest.setText(c.getString(2));
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
            if(!Util.verificaURL(Configuracion.this, url)){
                return;
            }

        } catch (MalformedURLException e) {
            Util.alertDialog(Configuracion.this, "MalformedURLException Configuración", e.getMessage());
            return;
        }

        try{

            if(EXISTE_SERVIDOR_URL){
                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_configuracion SET valor = '"+EDT_Servidor.getText().toString()+"' WHERE seccion = 'SERVER' AND clave = 'URL'");
            }else{
                AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_configuracion (seccion, clave, valor) VALUES ('SERVER', 'URL', '"+EDT_Servidor.getText()+"');");
            }

            if(EXISTE_SERVIDOR_PAGINA_TEST){
                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_configuracion SET valor = '"+EDT_PaginaTest.getText().toString()+"' WHERE seccion = 'SERVER' AND clave = 'PAGINA_TEST'");
            }else{
                AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_configuracion (seccion, clave, valor) VALUES ('SERVER', 'PAGINA_TEST', '"+EDT_PaginaTest.getText()+"');");
            }

        }catch(SQLException e){
            Util.alertDialog(Configuracion.this, "SQLException Configuración", e.getMessage());
            return;
        }

        Util.alertDialog(Configuracion.this, "Configuración", "Configuración grabada correctamente");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Write your logic here
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
