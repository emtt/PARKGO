package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class Login extends AppCompatActivity {

    private EditText EDT_UsuarioCodigo;
    private EditText EDT_UsuarioClave;
    private Button BTN_Login;
    private Button BTN_Sincronizar;
    private String UsuarioCodigo;
    private String UsuarioClave;

    int g_maestro_numero;
    String g_maestro_nombre;
    String g_maestro_alias;

    private ProgressDialog esperaDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppHelper.initParkgoDB(this);
        AppHelper.initSerialNum(this);
        init();
    }

    public void init(){

        EDT_UsuarioCodigo = (EditText) findViewById(R.id.EDT_UsuarioCodigo);
        EDT_UsuarioCodigo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() == 0) {
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
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioCodigo);
                    label.setText("");
                }
            }
        });


        EDT_UsuarioClave = (EditText) findViewById(R.id.EDT_UsuarioClave);
        EDT_UsuarioClave.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() == 0) {
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
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioClave);
                    label.setText("");
                }
            }
        });

        EDT_UsuarioCodigo.setText("admin");
        EDT_UsuarioClave.setText("admin");

        BTN_Login = (Button) findViewById(R.id.BTN_Login);
        BTN_Login.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick (View v){
                loginUsuario();
            }

        });


        BTN_Sincronizar = (Button) findViewById(R.id.BTN_Sincronizar);
        BTN_Sincronizar.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                g_maestro_numero = 0;
                g_maestro_nombre = "usuarios";
                g_maestro_alias  = "Usuarios";
                syncMaestros(AppHelper.getUrl_restful() + g_maestro_nombre, new MaestrosCallback() {
                    @Override
                    public void onResponse(int esError, int statusCode, String responseBody) {
                        SincronizarMaestros(g_maestro_numero, g_maestro_nombre, g_maestro_alias, responseBody);
                    }
                });

            }
        });

    }

    private void loginUsuario(){
        esperaDialog = ProgressDialog.show(this, "", "Autenticando...", true);
        esperaDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                procesoLoginUsuario();
                esperaDialog.dismiss();
            }
        }, 1000);
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
                                "tcu.minutos_gratis AS minutos_gratis, tcu.valor_minuto AS valor_minuto \n"+
                          "FROM tb_usuario tu\n" +
                          "LEFT JOIN tb_cliente_ubicaciones tcu ON tcu.id = tu.id_cliente_ubicacion\n" +
                          "LEFT JOIN tb_cliente tc ON tc.id = tcu.id_cliente\n" +
                          "WHERE tu.codigo =? AND tu.clave =? ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {
                String rs_usuario_rut          = c.getString(0);
                String rs_usuario_nombre       = c.getString(1);
                int rs_cliente_id              = c.getInt(2);
                String rs_cliente_razon_social = c.getString(3);
                int rs_ubicacion_id            = c.getInt(4);
                String rs_usuario_ubicacion    = c.getString(5);
                String rs_usuario_ubicacion_dir= c.getString(6);
                int    rs_minutos_gratis       = c.getInt(7);
                int    rs_valor_minuto         = c.getInt(8);

                AppHelper.setUsuario_rut(rs_usuario_rut);
                //De acuerdo a la ubicacion del usuario.
                AppHelper.setCliente_id(rs_cliente_id);
                AppHelper.setUbicacion_id(rs_ubicacion_id);
                AppHelper.setMinutos_gratis(rs_minutos_gratis);
                AppHelper.setValor_minuto(rs_valor_minuto);

                Intent intent = new Intent(this, Menu.class);
                intent.putExtra("usuario_nombre", rs_usuario_nombre);
                intent.putExtra("cliente_razon_social", rs_cliente_razon_social);
                intent.putExtra("usuario_ubicacion", rs_usuario_ubicacion);
                intent.putExtra("usuario_ubicacion_dir", rs_usuario_ubicacion_dir);
                startActivity(intent);

            } else {
                Util.alertDialog(Login.this, "Login ParkGO", "Usuario y clave ingresados no existe, sincronize y verifique" );
            }
            c.close();

        } catch (SQLException e) { Util.alertDialog(Login.this, "Login ParkGO", e.getMessage() ); }
    }

    public interface MaestrosCallback{
        void onResponse(int esError, int statusCode, String responseBody);
    }

    public void syncMaestros(String url, final MaestrosCallback maestrosCallback) {

        AsyncHttpClient cliente = new AsyncHttpClient () ;
        cliente.get(url, new AsyncHttpResponseHandler () {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                maestrosCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                maestrosCallback.onResponse(1, statusCode, new String(responseBody));
            }

        }) ;

    }


    public void SincronizarMaestros(final int numeroMaestro, final String nombreMaestro , final String aliasMaestro, final String jsonString) {

        Toast.makeText(getApplicationContext(), numeroMaestro+" /// "+nombreMaestro+" --- "+jsonString,Toast.LENGTH_LONG).show();

        if (esperaDialog != null && esperaDialog.isShowing()) {
            esperaDialog.setMessage(aliasMaestro);
        }else{
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
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_usuario;");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String rut    = jsonObject.optString("rut").toString();
                                String nombre = jsonObject.optString("nombre").toString();
                                String codigo = jsonObject.optString("codigo").toString();
                                String clave  = jsonObject.optString("clave").toString();
                                String id_cliente_ubicacion = jsonObject.optString("id_cliente_ubicacion").toString();
                                qry = "INSERT INTO tb_usuario (rut, nombre, codigo, clave, id_cliente_ubicacion) VALUES " +
                                        "('" + rut + "','" + nombre + "','" + codigo + "','" + clave + "','" + id_cliente_ubicacion + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 1:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String id  = jsonObject.optString("id").toString();
                                String rut = jsonObject.optString("rut").toString();
                                String razon_social = jsonObject.optString("razon_social").toString();
                                qry = "INSERT INTO tb_cliente (id, rut, razon_social ) VALUES " +
                                        "('" + id + "','" + rut + "','" + razon_social + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 2:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_cliente_ubicaciones;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String id          = jsonObject.optString("id").toString();
                                String id_cliente  = jsonObject.optString("id_cliente").toString();
                                String descripcion = jsonObject.optString("descripcion").toString();
                                String direccion   = jsonObject.optString("direccion").toString();
                                String latitud     = jsonObject.optString("latitud").toString();
                                String longitud    = jsonObject.optString("longitud").toString();
                                String minutos_gratis = jsonObject.optString("minutos_gratis").toString();
                                String valor_minuto   = jsonObject.optString("valor_minuto").toString();
                                qry = "INSERT INTO tb_cliente_ubicaciones (id, id_cliente, descripcion, direccion, latitud, longitud, minutos_gratis, valor_minuto ) VALUES " +
                                        "('" + id + "','" + id_cliente + "','" + descripcion + "','" + direccion + "','" + latitud + "','" + longitud + "','" + minutos_gratis + "','" + valor_minuto + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 3:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_conductor;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String rut    = jsonObject.optString("rut").toString();
                                String nombre = jsonObject.optString("nombre").toString();
                                String id_cliente = jsonObject.optString("id_cliente").toString();
                                String clave      = jsonObject.optString("clave").toString();
                                String telefono   = jsonObject.optString("telefono").toString();
                                String email = jsonObject.optString("email").toString();
                                String saldo = jsonObject.optString("saldo").toString();

                                qry = "INSERT INTO tb_conductor (rut, nombre, id_cliente, clave, telefono, email, saldo ) VALUES " +
                                        "('" + rut + "','" + nombre + "','" + id_cliente + "','" + clave + "','" + telefono + "','" + email + "','" + saldo + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;

                        case 4:
                            AppHelper.getParkgoSQLite().execSQL("DELETE FROM tb_conductor_patentes;");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String id  = jsonObject.optString("id").toString();
                                String rut_conductor = jsonObject.optString("rut_conductor").toString();
                                String patente       = jsonObject.optString("patente").toString();
                                qry = "INSERT INTO tb_conductor_patentes (id, rut_conductor, patente ) VALUES " +
                                        "('" + id + "','" + rut_conductor + "','" + patente + "');";
                                AppHelper.getParkgoSQLite().execSQL(qry);
                            }
                            break;
                    }

                } catch (SQLException e0) {
                    Util.alertDialog(Login.this, "Login", e0.getMessage());
                } catch (JSONException e1) {
                    Util.alertDialog(Login.this, "Login", e1.getMessage());
                }

                g_maestro_numero++;
                switch(g_maestro_numero){
                    case 1:
                        g_maestro_nombre = "clientes";
                        g_maestro_alias  = "Clientes";
                        break;
                    case 2:
                        g_maestro_nombre = "cliente_ubicaciones";
                        g_maestro_alias  = "Ubicaciones por cliente";
                        break;
                    case 3:
                        g_maestro_nombre = "conductores";
                        g_maestro_alias  = "Conductores";
                        break;

                    case 4:
                        g_maestro_nombre = "conductor_patentes";
                        g_maestro_alias  = "Patentes por conductor";
                        break;
                }

               if (g_maestro_numero <= 4) {
                    syncMaestros(AppHelper.getUrl_restful() + g_maestro_nombre, new MaestrosCallback() {
                        @Override
                        public void onResponse(int esError, int statusCode, String responseBody) {
                            SincronizarMaestros(g_maestro_numero, g_maestro_nombre, g_maestro_alias, responseBody);
                        }
                    });
               }else{
                   esperaDialog.dismiss();
               }

            }

        }, 1000);


    }

}
