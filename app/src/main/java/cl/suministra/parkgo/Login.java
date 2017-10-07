package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;


public class Login extends AppCompatActivity {

    /* Activity */
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private EditText EDT_UsuarioCodigo;
    private EditText EDT_UsuarioClave;
    private Button BTN_Login;
    private Button BTN_Sincronizar;
    private String UsuarioCodigo;
    private String UsuarioClave;
    private ProgressDialog esperaDialog;
    /* Global */
    int    g_maestro_numero;
    String g_maestro_nombre;
    String g_maestro_alias;
    /* GPS */
    AppGPS appGPS;
    /* Tareas Async */
    AsyncSENDIngresoPatente asyncSENDIngresoPatente;
    AsyncGETIngresoPatente asyncGETIngresoPatente;
    AsyncSENDRetiroPatente asyncSENDRetiroPatente;
    AsyncGETRetiroPatente asyncGETRetiroPatente;

    AsyncGenerico asyncGenerico;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_login);
        this.setTitle("PARKGO");
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.abrir, R.string.cerrar);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AppHelper.initParkgoDB(this);
        AppHelper.initSerialNum(this);
        appGPS = new AppGPS();
        init();

        //crea los objetos para recepcion y envio de datos.
        asyncSENDIngresoPatente = new AsyncSENDIngresoPatente();
        asyncSENDRetiroPatente  = new AsyncSENDRetiroPatente();
        asyncGETIngresoPatente  = new AsyncGETIngresoPatente();
        asyncGETRetiroPatente   = new AsyncGETRetiroPatente();
        asyncGenerico           = new AsyncGenerico();

        procesoSincronizarMaestros();

    }

    private void init() {

        EDT_UsuarioCodigo = (EditText) findViewById(R.id.EDT_UsuarioCodigo);
        EDT_UsuarioCodigo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioCodigo);
                    label.setText(EDT_UsuarioCodigo.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_UsuarioCodigo.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_UsuarioCodigo);
                label.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioCodigo);
                    label.setText("");
                }
            }
        });


        EDT_UsuarioClave = (EditText) findViewById(R.id.EDT_UsuarioClave);
        EDT_UsuarioClave.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioClave);
                    label.setText(EDT_UsuarioClave.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_UsuarioClave.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_UsuarioClave);
                label.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioClave);
                    label.setText("");
                }
            }
        });

        BTN_Login = (Button) findViewById(R.id.BTN_Login);
        BTN_Login.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                if (!verficaServidorConfigurado()){
                }else {
                    loginUsuario();
                }
            }

        });


        BTN_Sincronizar = (Button) findViewById(R.id.BTN_Sincronizar);
        BTN_Sincronizar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            procesoSincronizarMaestros();
            }
        });

    }

    private void loginUsuario() {
        esperaDialog = ProgressDialog.show(this, "", "Autenticando...", true);
        esperaDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                procesoLoginUsuario();
                esperaDialog.dismiss();
            }
        }, 500);
    }

    private void procesoLoginUsuario() {

        UsuarioCodigo = EDT_UsuarioCodigo.getText().toString();
        if (UsuarioCodigo == null || UsuarioCodigo.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_UsuarioCodigo);
            view.setText("Ingrese " + EDT_UsuarioCodigo.getHint());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_UsuarioCodigo.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return;
        }

        UsuarioClave = EDT_UsuarioClave.getText().toString();
        if (UsuarioClave == null || UsuarioClave.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_UsuarioClave);
            view.setText("Ingrese " + EDT_UsuarioClave.getHint());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_UsuarioClave.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return;
        }

        Cursor c;
        try {
            String[] args = new String[]{UsuarioCodigo, UsuarioClave};

            String qry = "SELECT tu.rut AS rut_usuario, tu.nombre AS nombre_usuario, tc.id AS cliente_id, tc.razon_social AS razon_social,\n" +
                                "tcu.id AS ubicacion_id, tcu.descripcion AS ubicacion_desc, tcu.direccion AS ubicacion_dir, \n" +
                                "tcu.minutos_gratis AS minutos_gratis, tcu.valor_minuto AS valor_minuto \n" +
                                "FROM tb_usuario tu\n" +
                                "LEFT JOIN tb_cliente_ubicaciones tcu ON tcu.id = tu.id_cliente_ubicacion\n" +
                                "LEFT JOIN tb_cliente tc ON tc.id = tcu.id_cliente\n" +
                                "WHERE tu.codigo =? AND tu.clave =? ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {
                String rs_usuario_rut = c.getString(0);
                String rs_usuario_nombre = c.getString(1);
                int rs_cliente_id = c.getInt(2);
                String rs_cliente_razon_social = c.getString(3);
                int rs_ubicacion_id = c.getInt(4);
                String rs_usuario_ubicacion = c.getString(5);
                String rs_usuario_ubicacion_dir = c.getString(6);
                int rs_minutos_gratis = c.getInt(7);
                int rs_valor_minuto = c.getInt(8);

                AppHelper.setUsuario_rut(rs_usuario_rut);
                //De acuerdo a la ubicacion del usuario.
                AppHelper.setCliente_id(rs_cliente_id);
                AppHelper.setUbicacion_id(rs_ubicacion_id);
                AppHelper.setMinutos_gratis(rs_minutos_gratis);
                AppHelper.setValor_minuto(rs_valor_minuto);

                //graba latitud y longitud del ingreso.
                registraUsuarioUbicacion(rs_usuario_rut, rs_ubicacion_id);

                Intent intent = new Intent(this, Menu.class);
                intent.putExtra("usuario_nombre", rs_usuario_nombre);
                intent.putExtra("cliente_razon_social", rs_cliente_razon_social);
                intent.putExtra("usuario_ubicacion", rs_usuario_ubicacion);
                intent.putExtra("usuario_ubicacion_dir", rs_usuario_ubicacion_dir);
                startActivity(intent);

            } else {
                Util.alertDialog(Login.this, "Login", "Usuario y clave ingresados no existe, sincronize y verifique");
            }
            c.close();

        } catch (SQLException e) {
            Util.alertDialog(Login.this, "SQLException Login", e.getMessage());
        }
    }

    private void ClienteAsync(String url, final ClienteCallback clienteCallback) {

        AsyncHttpClient cliente = new AsyncHttpClient();
        cliente.setConnectTimeout(1000);
        cliente.setResponseTimeout(1000);
        cliente.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                clienteCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                esperaDialog.dismiss();
                Util.alertDialog(Login.this, "onFailure Login", error.getMessage());
            }

        });

    }

    private void procesoSincronizarMaestros(){

        if (!verficaServidorConfigurado()){
        }else {

            g_maestro_numero = 0;
            g_maestro_nombre = "configuracion";
            g_maestro_alias = "1/5 Configuración";

            if (esperaDialog != null && esperaDialog.isShowing()) {
                esperaDialog.setMessage(g_maestro_alias);
            } else {
                esperaDialog = ProgressDialog.show(Login.this, "Sincronizando...", g_maestro_alias);
            }

            ClienteAsync(AppHelper.getUrl_restful() + g_maestro_nombre, new ClienteCallback() {
                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {
                    if (esError == 0) {
                        SincronizarMaestros(g_maestro_numero, g_maestro_nombre, g_maestro_alias, responseBody);
                    } else {
                        esperaDialog.dismiss();
                        Util.alertDialog(Login.this, "ErrorSync Login", "Código: " + statusCode + "\n" + responseBody);
                    }
                }
            });

        }

    }

    private void SincronizarMaestros(final int numeroMaestro, final String nombreMaestro, final String aliasMaestro, final String jsonString) {

        if (esperaDialog != null && esperaDialog.isShowing()) {
            esperaDialog.setMessage(aliasMaestro);
        } else {
            esperaDialog = ProgressDialog.show(Login.this, "Sincronizando...", aliasMaestro);
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {

                String qry;
                try {

                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    JSONArray jsonArray = jsonRootObject.optJSONArray(nombreMaestro);

                    switch (numeroMaestro) {

                        case 0:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_configuracion;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int codigo     = jsonObject.optInt("codigo");
                                String seccion = jsonObject.optString("seccion");
                                String clave   = jsonObject.optString("clave");
                                String valor   = jsonObject.optString("valor");
                                qry = "INSERT INTO tb_configuracion (codigo, seccion, clave, valor) VALUES " +
                                        "('" + codigo + "','" + seccion + "','" + clave + "','" + valor + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 1:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_usuario;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String rut = jsonObject.optString("rut");
                                String nombre = jsonObject.optString("nombre");
                                String codigo = jsonObject.optString("codigo");
                                String clave = jsonObject.optString("clave");
                                String id_cliente_ubicacion = jsonObject.optString("id_cliente_ubicacion");
                                qry = "INSERT INTO tb_usuario (rut, nombre, codigo, clave, id_cliente_ubicacion) VALUES " +
                                        "('" + rut + "','" + nombre + "','" + codigo + "','" + clave + "','" + id_cliente_ubicacion + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 2:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String id = jsonObject.optString("id");
                                String rut = jsonObject.optString("rut");
                                String razon_social = jsonObject.optString("razon_social");
                                qry = "INSERT INTO tb_cliente (id, rut, razon_social ) VALUES " +
                                        "('" + id + "','" + rut + "','" + razon_social + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 3:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente_ubicaciones;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String id = jsonObject.optString("id");
                                String id_cliente = jsonObject.optString("id_cliente");
                                String descripcion = jsonObject.optString("descripcion");
                                String direccion = jsonObject.optString("direccion");
                                String latitud = jsonObject.optString("latitud");
                                String longitud = jsonObject.optString("longitud");
                                String minutos_gratis = jsonObject.optString("minutos_gratis");
                                String valor_minuto = jsonObject.optString("valor_minuto");
                                qry = "INSERT INTO tb_cliente_ubicaciones (id, id_cliente, descripcion, direccion, latitud, longitud, minutos_gratis, valor_minuto ) VALUES " +
                                        "('" + id + "','" + id_cliente + "','" + descripcion + "','" + direccion + "','" + latitud + "','" + longitud + "','" + minutos_gratis + "','" + valor_minuto + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 4:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente_ubicaciones_horarios;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id = jsonObject.optInt("id");
                                int id_cliente_ubicacion = jsonObject.optInt("id_cliente_ubicacion");
                                String dia_desde  = jsonObject.optString("dia_desde");
                                String hora_desde = jsonObject.optString("hora_desde");
                                int suma_dia      = jsonObject.optInt("suma_dia");
                                String dia_hasta  = jsonObject.optString("dia_hasta");
                                String hora_hasta = jsonObject.optString("hora_hasta");

                                qry = "INSERT INTO tb_cliente_ubicaciones_horarios (id, id_cliente_ubicacion, dia_desde, hora_desde, suma_dia, dia_hasta, hora_hasta) VALUES " +
                                        "(" + id + ", " + id_cliente_ubicacion + ",'" + dia_desde + "','" + hora_desde + "',"+suma_dia+",'" + dia_hasta + "','" + hora_hasta + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;
                    }

                } catch (SQLException e0) {
                    Util.alertDialog(Login.this, "SQLException Login", e0.getMessage());
                } catch (JSONException e1) {
                    Util.alertDialog(Login.this, "JSONException Login", e1.getMessage());
                }

                g_maestro_numero++;
                switch (g_maestro_numero) {
                    case 1:
                        g_maestro_nombre = "usuarios";
                        g_maestro_alias = "2/5 Usuarios";
                        break;
                    case 2:
                        g_maestro_nombre = "clientes";
                        g_maestro_alias = "3/5 Clientes";
                        break;
                    case 3:
                        g_maestro_nombre = "cliente_ubicaciones";
                        g_maestro_alias = "4/5 Ubicaciones por cliente";
                        break;
                    case 4:
                        g_maestro_nombre = "cliente_ubicaciones_horarios";
                        g_maestro_alias = "5/5 Horarios por ubicación cliente";
                        break;
                }

                if (g_maestro_numero <= 5) {
                    ClienteAsync(AppHelper.getUrl_restful() + g_maestro_nombre, new ClienteCallback() {
                        @Override
                        public void onResponse(int esError, int statusCode, String responseBody) {
                            if (esError == 0) {
                                SincronizarMaestros(g_maestro_numero, g_maestro_nombre, g_maestro_alias, responseBody);
                            } else {
                                esperaDialog.dismiss();
                                Util.alertDialog(Login.this, "ErrorSync Login", "Código: " + statusCode + "\n" + responseBody);
                            }
                        }
                    });
                } else {
                    esperaDialog.dismiss();
                }

            }

        }, 1000);


    }

    private void registraUsuarioUbicacion(final String rut_usuario, final int id_ubicacion_usuario){

        AppGPS.getLastLocation(new GPSCallback() {
            @Override
            public void onResponseSuccess(Location location) {
                if(location != null){


                    String fecha_hora   = AppHelper.fechaHoraFormat.format(new Date());
                    String id_usuario_ubicacion  = AppHelper.fechaHoraFormatID.format(new Date())+"_"+AppHelper.getSerialNum()+"_"+rut_usuario;
                    try{

                        AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_usuario_ubicaciones " +
                                                            "(id, rut_usuario, id_cliente_ubicacion, latitud, longitud, fecha_hora, enviado) " +
                                                            "VALUES " +
                                                            "('"+id_usuario_ubicacion+"','"+rut_usuario+"', "+id_ubicacion_usuario+" ," +
                                                            "'"+ Double.toString(location.getLatitude())+"'," + "'"+Double.toString(location.getLongitude())+"', '"+fecha_hora+"', '0');");

                    }catch(SQLException e){  Util.alertDialog(Login.this, "SQLException Login",e.getMessage());   }

                }
            }
            @Override
            public void onResponseFailure(Exception e) {
                Util.alertDialog(Login.this, "onResponseFailure Login",e.getMessage());
            }
        });

    }

    private boolean verficaServidorConfigurado(){

        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT seccion, clave, valor FROM tb_configuracion", null);
        if (c.moveToFirst()){
            do {
                if (c.getString(0).equals("SERVER") && c.getString(1).equals("URL")) {
                    AppHelper.setUrl_restful(c.getString(2));
                }else if (c.getString(0).equals("SERVER") && c.getString(1).equals("PAGINA_TEST")) {
                    AppHelper.setPagina_test(c.getString(2));
                }
            }while(c.moveToNext());
        }
        c.close();


       if(!AppHelper.getUrl_restful().isEmpty() && !AppHelper.getPagina_test().isEmpty()) {

           //AsyncTask.Status.PENDING (Tarea no se ha iniciado)
           //AsyncTask.Status.RUNNING (Tarea se encuentra realizando el trabajo en doInBackground())
           //AsyncTask.Status.FINISHED (Tarea se ha ejecutado y se encuentra en onPostExecute())

           //inicia la tarea de envio patenes ingresadas.
           if(asyncSENDIngresoPatente.getStatus() == AsyncTask.Status.PENDING){
               asyncSENDIngresoPatente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
           }

           //inicia la tarea de envio patenes retiradas.
           if(asyncSENDRetiroPatente.getStatus() == AsyncTask.Status.PENDING){
               asyncSENDRetiroPatente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
           }

           //inicia la tarea que recibe patentes ingresadas externas.
           if(asyncGETIngresoPatente.getStatus() == AsyncTask.Status.PENDING) {
               asyncGETIngresoPatente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
           }

           //inicia la tarea que recibe patentes retiradas externas.
           if(asyncGETRetiroPatente.getStatus() == AsyncTask.Status.PENDING) {
               asyncGETRetiroPatente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
           }

           //inicia la tarea de envío genérico.
           if(asyncGenerico.getStatus() == AsyncTask.Status.PENDING) {
               asyncGenerico.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
           }

           return true;

       }else{

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Información importante!");
            builder.setMessage("No se encuentra configurado el servidor de aplicaciones, verifique");
            builder.setPositiveButton("Configurar",  new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(Login.this, Configuracion.class);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    dialog.cancel();
                }
            });
            builder.show();

            return false;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    public void abrirConfiguracion(MenuItem item){

        Intent intent = new Intent(this, Configuracion.class);
        startActivity(intent);

    }

    @Override
    protected void onStart() {
        super.onStart();
        appGPS.conectaGPS();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appGPS.pausarGPS();
    }

    @Override
    protected void onStop() {
        super.onStop();
        appGPS.desconectaGPS();
    }

}
