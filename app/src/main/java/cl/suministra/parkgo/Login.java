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
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.ChallengeState;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;


public class Login extends AppCompatActivity {

    /* Activity */
    private Menu menu;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private EditText EDT_UsuarioCodigo;
    private EditText EDT_UsuarioClave;
    private Spinner SPIN_UsuarioUbicacion;
    private Button BTN_Login;
    private Button BTN_Sincronizar;
    private String UsuarioCodigo;
    private String UsuarioClave;
    private int    UsuarioUbicacionID;
    private ProgressDialog esperaDialog;
    /* Global */
    int    g_maestro_numero;
    String g_maestro_nombre;
    String g_maestro_alias;
    int  g_num_etiqueta_actual;
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

    ArrayList<String> spinUbicacionNombre = new ArrayList<String>();
    ArrayList<Integer> spinUbicacionID    = new  ArrayList<Integer>();

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

        SPIN_UsuarioUbicacion = (Spinner) findViewById(R.id.SPIN_UsuarioUbicacion);

        spinUbicacionID.add(0);
        spinUbicacionNombre.add("SELECCIONE UBICACIÓN");

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(Login.this, android.R.layout.simple_spinner_dropdown_item, spinUbicacionNombre);
        SPIN_UsuarioUbicacion.setAdapter(adapter);

        SPIN_UsuarioUbicacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tview = (TextView) findViewById(R.id.MSJ_UsuarioUbicacion);
                if (spinUbicacionID.get(position) > 0){
                    tview.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EDT_UsuarioCodigo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)

                    try {

                        spinUbicacionID.clear();
                        spinUbicacionNombre.clear();

                        spinUbicacionID.add(0);
                        spinUbicacionNombre.add("SELECCIONE UBICACIÓN");

                        int ubicacionDefaultIndex = 0;

                        String[] args = new String[]{EDT_UsuarioCodigo.getText().toString()};

                        //OBTIENE EL LISTADO DE UBICACIONES PARA EL USUARIO QUE SE ENCUENTREN EN LA MISMA COMUNA DE SU UBICACION POR DEFECTO.
                        String qry = "SELECT tcu2.id, tcu2.descripcion, tcu2.direccion, tu.id_cliente_ubicacion AS id_ubicacion_default, tcu1.id_comuna \n" +
                                "FROM tb_usuario tu \n" +
                                "LEFT JOIN tb_cliente_ubicaciones tcu1 ON tcu1.id = tu.id_cliente_ubicacion \n" +
                                "LEFT JOIN tb_cliente_ubicaciones tcu2 ON tcu2.id_comuna = tcu1.id_comuna \n" +
                                "WHERE tu.codigo = ? \n" +
                                "ORDER BY tcu2.descripcion";

                        Cursor c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
                        if (c.moveToFirst()) {

                            do{
                                spinUbicacionID.add(c.getInt(0));
                                spinUbicacionNombre.add(c.getString(1));
                                Log.d(AppHelper.LOG_TAG, c.getInt(0)+" "+c.getString(1)+" "+spinUbicacionID.size());


                                if(ubicacionDefaultIndex == 0){
                                    if (c.getInt(0) == c.getInt(3)){
                                        ubicacionDefaultIndex = spinUbicacionID.size() - 1;
                                    }
                                }

                            }while(c.moveToNext());
                        }
                        c.close();

                        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(Login.this, android.R.layout.simple_spinner_dropdown_item, spinUbicacionNombre);
                        SPIN_UsuarioUbicacion.setAdapter(adapter);
                        SPIN_UsuarioUbicacion.setSelection(ubicacionDefaultIndex);

                    } catch (SQLException e) {
                        Util.alertDialog(Login.this, "SQLException Login", e.getMessage());
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

                if (!configuracionInicial()){
                }else {
                    comparaHoraServidor();
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


        UsuarioUbicacionID = Integer.parseInt(spinUbicacionID.get(SPIN_UsuarioUbicacion.getSelectedItemPosition()).toString());
        if(UsuarioUbicacionID == 0){
            TextView view = (TextView) findViewById(R.id.MSJ_UsuarioUbicacion);
            view.setText("Selecione una ubicación válida");
            return;
        }

        Cursor c;
        try {
            String[] args = new String[]{UsuarioCodigo, UsuarioClave};

            String qry = "SELECT tu.rut AS rut_usuario, tu.nombre AS nombre_usuario, tu.codigo AS codigo_usuario, tc.id AS cliente_id, tc.razon_social AS razon_social,\n" +
                                "tcu.id AS ubicacion_id, tcu.descripcion AS ubicacion_desc, tcu.direccion AS ubicacion_dir, \n" +
                                "tcu.tipo_cobro AS tipo_cobro, tcu.valor_minuto AS valor_minuto, tcu.valor_tramo AS valor_tramo, \n" +
                                "tcu.minutos_tramo AS minutos_tramo, tcu.minutos_gratis AS minutos_gratis, tcu.descripcion_tarifa AS descripcion_tarifa \n" +
                                "FROM tb_usuario tu\n" +
                                "LEFT JOIN tb_cliente_ubicaciones tcu ON tcu.id = "+UsuarioUbicacionID+"\n" +
                                "LEFT JOIN tb_cliente tc ON tc.id = tcu.id_cliente\n" +
                                "WHERE tu.codigo =? AND tu.clave =? ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {
                String rs_usuario_rut = c.getString(0);
                String rs_usuario_nombre = c.getString(1);
                String rs_usuario_codigo = c.getString(2);
                int rs_cliente_id   = c.getInt(3);
                String rs_cliente_razon_social = c.getString(4);
                int rs_ubicacion_id = c.getInt(5);
                String rs_usuario_ubicacion     = c.getString(6);
                String rs_usuario_ubicacion_dir = c.getString(7);
                int rs_tipo_cobro     = c.getInt(8);
                int rs_valor_minuto   = c.getInt(9);
                int rs_valor_tramo    = c.getInt(10);
                int rs_minutos_tramo  = c.getInt(11);
                int rs_minutos_gratis = c.getInt(12);
                String rs_descripcion_tarifa = c.getString(13);

                AppHelper.setUsuario_rut(rs_usuario_rut);
                AppHelper.setUsuario_nombre(rs_usuario_nombre);
                AppHelper.setUsuario_codigo(rs_usuario_codigo);
                //De acuerdo a la ubicacion del usuario.
                AppHelper.setCliente_id(rs_cliente_id);
                AppHelper.setUbicacion_id(rs_ubicacion_id);
                AppHelper.setUbicacion_nombre(rs_usuario_ubicacion);

                AppHelper.setTipo_cobro(rs_tipo_cobro);
                AppHelper.setValor_minuto(rs_valor_minuto);
                AppHelper.setValor_tramo(rs_valor_tramo);
                AppHelper.setMinutos_tramo(rs_minutos_tramo);
                AppHelper.setMinutos_gratis(rs_minutos_gratis);
                AppHelper.setDescripcion_tarifa(rs_descripcion_tarifa);

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
        cliente.setConnectTimeout(AppHelper.timeout);
        cliente.setResponseTimeout(AppHelper.timeout);
        cliente.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                clienteCallback.onResponse(0, statusCode, new String(responseBody));

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "onFailure Login statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "onFailure Login responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "onFailure Login error "+String.valueOf(Log.getStackTraceString(error)));

                esperaDialog.dismiss();
                Util.alertDialog(Login.this, "onFailure Login ", String.valueOf(Log.getStackTraceString(error)));
            }

        });

    }

    private void procesoSincronizarMaestros(){

        if (!configuracionInicial()){
        }else {

            g_maestro_numero = 0;
            g_maestro_nombre = "configuracion";
            g_maestro_alias = "1/10 Configuración";

            if (esperaDialog != null && esperaDialog.isShowing()) {
                esperaDialog.setMessage(g_maestro_alias);
            } else {
                esperaDialog = ProgressDialog.show(Login.this, "Sincronizando...", g_maestro_alias);
            }

            ClienteAsync(AppHelper.getUrl_restful() + g_maestro_nombre, new ClienteCallback() {
                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {

                    try {

                        JSONObject jsonRootObject = new JSONObject(responseBody);
                        JSONArray jsonArray = jsonRootObject.optJSONArray(g_maestro_nombre);
                        if(jsonArray != null){

                            if (esError == 0) {
                                SincronizarMaestros(g_maestro_numero, g_maestro_nombre, g_maestro_alias, responseBody);
                            } else {
                                esperaDialog.dismiss();
                                Util.alertDialog(Login.this, "ErrorSync Login", "Código: " + statusCode + "\n" + responseBody);
                            }

                        }else{
                            esperaDialog.dismiss();
                            jsonArray = jsonRootObject.optJSONArray("error");
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Util.alertDialog(Login.this,"ErrorSync Login", jsonObject.optString("text") );
                        }

                    } catch (JSONException e) {
                        Util.alertDialog(Login.this,"ErrorSync Login",e.getMessage() );
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
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_rol;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id     = jsonObject.optInt("id");
                                String nombre = jsonObject.optString("nombre");
                                int ing_web   = jsonObject.optInt("ing_web");
                                int ing_web_pagina_inicio  = jsonObject.optInt("ing_web_pagina_inicio");
                                int ing_mobile= jsonObject.optInt("ing_mobile");
                                int es_recaudador = jsonObject.optInt("es_recaudador");
                                qry = "INSERT INTO tb_rol (id, nombre, ing_web, ing_web_pagina_inicio, ing_mobile, es_recaudador) VALUES " +
                                        "(" + id + ",'" + nombre + "', " + ing_web + "," + ing_web_pagina_inicio + ","+ing_mobile+" ,"+es_recaudador+");";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 2:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_usuario;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String rut    = jsonObject.optString("rut");
                                String nombre = jsonObject.optString("nombre");
                                String codigo = jsonObject.optString("codigo");
                                String clave  = jsonObject.optString("clave");
                                int id_cliente_ubicacion = jsonObject.optInt("id_cliente_ubicacion");
                                int id_rol    = jsonObject.optInt("id_rol");
                                qry = "INSERT INTO tb_usuario (rut, nombre, codigo, clave, id_cliente_ubicacion, id_rol) VALUES " +
                                        "('" + rut + "','" + nombre + "','" + codigo + "','" + clave + "'," + id_cliente_ubicacion + ", "+ id_rol +");";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 3:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id = jsonObject.optInt("id");
                                String rut = jsonObject.optString("rut");
                                String razon_social = jsonObject.optString("razon_social");
                                String logo = jsonObject.optString("logo");
                                String email = jsonObject.optString("logo");

                                qry = "INSERT INTO tb_cliente (id, rut, razon_social, logo, email ) VALUES " +
                                        "(" + id + ",'" + rut + "','" + razon_social + "','"+logo+"','"+email+"');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 4:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente_ubicaciones;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id             = jsonObject.optInt("id");
                                int id_cliente     = jsonObject.optInt("id_cliente");
                                String descripcion = jsonObject.optString("descripcion");
                                int id_comuna      = jsonObject.optInt("id_comuna");
                                String direccion   = jsonObject.optString("direccion");
                                String latitud     = jsonObject.optString("latitud");
                                String longitud    = jsonObject.optString("longitud");
                                int tipo_cobro     = jsonObject.optInt("tipo_cobro");
                                int valor_minuto   = jsonObject.optInt("valor_minuto");
                                int valor_tramo    = jsonObject.optInt("valor_tramo");
                                int minutos_tramo  = jsonObject.optInt("minutos_tramo");
                                int minutos_gratis = jsonObject.optInt("minutos_gratis");
                                String descripcion_tarifa = jsonObject.optString("descripcion_tarifa");

                                qry = "INSERT INTO tb_cliente_ubicaciones (id, id_cliente, descripcion, id_comuna,  direccion, latitud, longitud, tipo_cobro, valor_minuto, valor_tramo, minutos_tramo , minutos_gratis, descripcion_tarifa ) VALUES " +
                                        "(" + id + "," + id_cliente + ",'" + descripcion + "', "+id_comuna+" ,'" + direccion + "','" + latitud + "','" + longitud + "'," + tipo_cobro + "," + valor_minuto + "," + valor_tramo + "," + minutos_tramo + "," + minutos_gratis + ", '" + descripcion_tarifa + "' );";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 5:
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

                        case 6:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_conductor;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String rut     = jsonObject.optString("rut");
                                String nombre  = jsonObject.optString("nombre");
                                int id_conductor_grupo = jsonObject.optInt("id_conductor_grupo");
                                int clave      = jsonObject.optInt("clave");
                                String telefono= jsonObject.optString("telefono");
                                String email   = jsonObject.optString("telefono");
                                int saldo      = jsonObject.optInt("saldo");

                                qry = "INSERT INTO tb_conductor (rut, nombre, id_conductor_grupo, clave, telefono, email, saldo) VALUES " +
                                        "('" + rut + "','" + nombre + "', " + id_conductor_grupo + ", " + clave + ", '"+ telefono +"', '"+ email +"', "+ saldo +");";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 7:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_conductor_grupo;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id = jsonObject.optInt("id");
                                int id_cliente = jsonObject.optInt("id_cliente");
                                String descripcion = jsonObject.optString("descripcion");
                                int envia_mail_ingreso  = jsonObject.optInt("envia_mail_ingreso");
                                int envia_mail_retiro  = jsonObject.optInt("envia_mail_retiro");

                                qry = "INSERT INTO tb_conductor_grupo (id, id_cliente, descripcion, envia_mail_ingreso, envia_mail_retiro ) VALUES " +
                                        "(" + id + "," + id_cliente + ",'" + descripcion + "', " + envia_mail_ingreso + ", "+envia_mail_retiro+");";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 8:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_conductor_grupo_ubicacion_descuento;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id_conductor_grupo   = jsonObject.optInt("id_conductor_grupo");
                                int id_cliente_ubicacion = jsonObject.optInt("id_cliente_ubicacion");
                                int descuento  = jsonObject.optInt("descuento");

                                qry = "INSERT INTO tb_conductor_grupo_ubicacion_descuento (id_conductor_grupo, id_cliente_ubicacion, descuento ) VALUES " +
                                        "(" + id_conductor_grupo + "," + id_cliente_ubicacion + ", " + descuento + " );";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;


                        case 9:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_conductor_patentes;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                int id     = jsonObject.optInt("id");
                                String rut_conductor  = jsonObject.optString("rut_conductor");
                                String patente  = jsonObject.optString("patente");

                                qry = "INSERT INTO tb_conductor_patentes (id, rut_conductor, patente ) VALUES " +
                                        "(" + id + ",'" + rut_conductor + "', '" + patente + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                    }

                } catch (SQLException e0) {
                    Util.alertDialog(Login.this, "SQLException Login maestro"+numeroMaestro, e0.getMessage());
                } catch (JSONException e1) {
                    Util.alertDialog(Login.this, "JSONException Login", e1.getMessage());
                }

                g_maestro_numero++;
                switch (g_maestro_numero) {
                    case 1:
                        g_maestro_nombre = "roles";
                        g_maestro_alias = "2/10 Roles";
                        break;
                    case 2:
                        g_maestro_nombre = "usuarios";
                        g_maestro_alias = "3/10 Usuarios";
                        break;
                    case 3:
                        g_maestro_nombre = "clientes";
                        g_maestro_alias = "4/10 Clientes";
                        break;
                    case 4:
                        g_maestro_nombre = "cliente_ubicaciones";
                        g_maestro_alias = "5/10 Ubicaciones por cliente";
                        break;
                    case 5:
                        g_maestro_nombre = "cliente_ubicaciones_horarios";
                        g_maestro_alias = "6/10 Horarios por ubicación cliente";
                        break;
                    case 6:
                        g_maestro_nombre = "conductores";
                        g_maestro_alias = "7/10 Conductores";
                        break;
                    case 7:
                        g_maestro_nombre = "conductores_grupo";
                        g_maestro_alias = "8/10 Grupo Conductores";
                        break;
                    case 8:
                        g_maestro_nombre = "conductores_grupo_ubicacion_descuento";
                        g_maestro_alias = "9/10 Descuento para grupo conductores por ubicación";
                        break;
                    case 9:
                        g_maestro_nombre = "conductor_patentes";
                        g_maestro_alias = "10/10 Conductor Patentes";
                        break;
                }

                if (g_maestro_numero <= 9) {
                    ClienteAsync(AppHelper.getUrl_restful() + g_maestro_nombre, new ClienteCallback() {
                        @Override
                        public void onResponse(int esError, int statusCode, String responseBody) {

                            try {

                                JSONObject jsonRootObject = new JSONObject(responseBody);
                                JSONArray jsonArray = jsonRootObject.optJSONArray(g_maestro_nombre);
                                if(jsonArray != null){

                                    if (esError == 0) {
                                        SincronizarMaestros(g_maestro_numero, g_maestro_nombre, g_maestro_alias, responseBody);
                                    } else {
                                        esperaDialog.dismiss();
                                        Util.alertDialog(Login.this, "ErrorSync Login", "Código: " + statusCode + "\n" + responseBody);
                                    }

                                }else{
                                    esperaDialog.dismiss();
                                    jsonArray = jsonRootObject.optJSONArray("error");
                                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                                    Util.alertDialog(Login.this,"ErrorSync Login", jsonObject.optString("text") );
                                }

                            } catch (JSONException e) {
                                Util.alertDialog(Login.this,"ErrorSync Login",e.getMessage() );
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

    private boolean configuracionInicial(){

        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT seccion, clave, valor FROM tb_configuracion", null);
        if (c.moveToFirst()){
            do {
                if (c.getString(0).equals("SERVER") && c.getString(1).equals("URL")) {
                    AppHelper.setUrl_restful(c.getString(2));
                }else if (c.getString(0).equals("SERVER") && c.getString(1).equals("PAGINA_TEST")) {
                    AppHelper.setPagina_test(c.getString(2));
                }else if (c.getString(0).equals("SERVER_MAQUINA") && c.getString(1).equals("MINUTOS_DIFF")) {
                    AppHelper.setMinutos_diff(c.getInt(2));
                }else if (c.getString(0).equals("VOUCHER") && c.getString(1).equals("INGRESO")) {
                AppHelper.setVoucher_ingreso(c.getString(2));
                }else if (c.getString(0).equals("VOUCHER") && c.getString(1).equals("SALIDA")) {
                    AppHelper.setVoucher_salida(c.getString(2));
                }else if (c.getString(0).equals("VOUCHER") && c.getString(1).equals("ESTACIONADOS")) {
                    AppHelper.setVoucher_estacionados(c.getString(2));
                }else if (c.getString(0).equals("VOUCHER") && c.getString(1).equals("RECAUDACION")) {
                    AppHelper.setVoucher_retiro_recaudacion(c.getString(2));
                }else if (c.getString(0).equals("IMAGEN") && c.getString(1).equals("CALIDAD")) {
                    AppHelper.setImagen_calidad(Integer.parseInt(c.getString(2)));
                }else if (c.getString(0).equals("IMAGEN") && c.getString(1).equals("MAX_MB")) {
                    AppHelper.setImagen_max_mb(Double.parseDouble(c.getString(2)));
                }else if (c.getString(0).equals("VOUCHER") && c.getString(1).equals("CANTIDAD_ROLLO_MAX")) {
                    AppHelper.setVoucher_rollo_max(Integer.parseInt(c.getString(2)));
                }else if (c.getString(0).equals("VOUCHER") && c.getString(1).equals("CANTIDAD_ROLLO_ALERT")) {
                    AppHelper.setVoucher_rollo_alert(Integer.parseInt(c.getString(2)));
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

                    try{
                        Intent intent = new Intent(Login.this, Configuracion.class);
                        startActivity(intent);
                    }catch (IllegalStateException e){
                        Util.alertDialog(Login.this, "Login", e.getMessage());
                    }

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

    public void salirApp(MenuItem item){
       System.exit(0);
    }

    public void cambiarEtiqueta(MenuItem item){

        g_num_etiqueta_actual = 0;
        try{
            String[] args = new String[] {};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT num_etiqueta_actual FROM tb_etiquetas",args);
            if (c.moveToFirst()) {
                g_num_etiqueta_actual = c.getInt(0);
            }else{
                AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_etiquetas (num_etiqueta_actual) VALUES (0);");
            }

            c.close();

        }catch(SQLException e){
            Util.alertDialog(Login.this,"SQLException Login", e.getMessage());
        }

        final EditText EDT_EtiquetaActual = new EditText(Login.this);
        EDT_EtiquetaActual.setInputType(InputType.TYPE_CLASS_NUMBER);
        EDT_EtiquetaActual.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        EDT_EtiquetaActual.setText(String.valueOf(g_num_etiqueta_actual));

        AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
        builder.setTitle("Cambio de Rollo Etiqueta");
        builder.setMessage("Ingrese el número de la etiqueta inicial");
        builder.setView(EDT_EtiquetaActual);
        builder.setPositiveButton("Reiniciar",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                    int num_etiqueta_nueva = Integer.parseInt(EDT_EtiquetaActual.getText().toString());
                    if (AppCRUD.actualizaNumeroEtiqueta(Login.this, g_num_etiqueta_actual, num_etiqueta_nueva, false) == 100){
                        Util.alertDialog(Login.this, "Login","Número etiqueta actualizado correctamente");
                    }
                    dialog.dismiss();
            }

        });
    }

    public void comparaHoraServidor(){

        String fecha_hora_maquina   = AppHelper.fechaHoraFormat.format(new Date());
        final AsyncHttpClient cliente = new AsyncHttpClient();
        cliente.setConnectTimeout(AppHelper.timeout);
        cliente.setResponseTimeout(AppHelper.timeout);

        JSONObject jsonParams  = null;
        StringEntity entity    = null;


        try {

            jsonParams = new JSONObject();
            jsonParams.put("fecha_hora_maquina",fecha_hora_maquina);
            entity = new StringEntity(jsonParams.toString());

            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            cliente.post(App.context, AppHelper.getUrl_restful() + "fecha_hora_servidor" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    Log.d(AppHelper.LOG_TAG, "AsyncComparaHoraServidor onSuccess "+new String(responseBody));

                    try {
                        JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                        if(jsonArray != null){

                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            int minutos_diff      = jsonObject.optInt("minutos_diff");
                            String fecha_hora_servidor =  jsonObject.optString("fecha_hora_servidor");

                            //Si la diferencia en minutos es mayor a la configurada, entonces deberá ajustar la hora.
                            if(minutos_diff > AppHelper.getMinutos_diff()){
                                AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
                                builder.setTitle("Información importante!");
                                builder.setMessage("Fecha y hora configurada en la máquina no coincide con el servidor "+fecha_hora_servidor+", existe una diferencia de "+minutos_diff+" minutos, verifique");
                                builder.setPositiveButton("Configurar",
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        startActivityForResult(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS), 0);
                                    }
                                });
                                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });
                                builder.show();

                            }else{
                                loginUsuario();
                            }

                        }else{
                            jsonArray = jsonRootObject.optJSONArray("error");
                            if(jsonArray != null){
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                Util.alertDialog(Login.this,"Error comparaHoraServidor Login", jsonObject.optString("text"));
                              }
                        }
                    } catch (JSONException e) {
                        Util.alertDialog(Login.this, "JSONException comparaHoraServidor Login", e.getMessage());
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    Log.d(AppHelper.LOG_TAG, "onFailure comparaHoraServidor Login statusCode "+String.valueOf(statusCode));
                    Log.d(AppHelper.LOG_TAG, "onFailure comparaHoraServidor Login responseBody "+String.valueOf(responseBody));
                    Log.d(AppHelper.LOG_TAG, "onFailure comparaHoraServidor Login error "+String.valueOf(Log.getStackTraceString(error)));

                    Util.alertDialog(Login.this, "onFailure comparaHoraServidor Login ", String.valueOf(Log.getStackTraceString(error)));
                   cliente.cancelRequests(App.context, true);
                }

            });

        } catch (UnsupportedEncodingException e0) {
            Util.alertDialog(Login.this, "UnsupportedEncodingException comparaHoraServidor Login", e0.getMessage());
        } catch (JSONException e1) {
            Util.alertDialog(Login.this, "JSONException comparaHoraServidor Login", e1.getMessage());
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (appGPS.verificaConexionGoogleApi(Login.this)) {
            appGPS.conectaGPS();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (appGPS.verificaConexionGoogleApi(Login.this)) {
            appGPS.pausarGPS();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appGPS.verificaConexionGoogleApi(Login.this)) {
            appGPS.desconectaGPS();
        }
    }



}
