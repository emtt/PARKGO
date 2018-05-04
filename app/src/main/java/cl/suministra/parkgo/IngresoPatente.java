package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.obm.mylibrary.PrintConnect;
import com.obm.mylibrary.PrintUnits;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;


public class IngresoPatente extends AppCompatActivity {

    private AsyncHttpClient cliente = null;

    public static PrintConnect mPrintConnect;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private EditText EDT_Patente;
    private Spinner  SPIN_Espacios;
    private FloatingActionButton BTN_IngresoPatente;

    private FloatingActionButton BTN_Comentario;
    private String g_comentario = "";

    private FloatingActionButton BTN_Camara;
    private ImageView IMG_IngresoPatente;
    private String g_imagen_path   = "";
    private String g_imagen_nombre = "";

    private AppGPS appGPS;
    private String g_latitud;
    private String g_longitud;

    //CONTROLES PAGO DEUDA DIALOG.
    private TextView TXT_LB_Transaccion;
    private TextView TXT_Transaccion;
    private TextView TXT_LB_FechaHora;
    private TextView TXT_FechaHora;
    private TextView TXT_LB_Ubicacion;
    private TextView TXT_Ubicacion;
    private TextView TXT_LB_Espacios;
    private TextView TXT_Espacios;
    private TextView TXT_LB_Operador;
    private TextView TXT_Operador;
    private TextView TXT_LB_Precio;
    private TextView TXT_Precio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingreso_patente);
        this.setTitle("Ingresar Vehículo");
        appGPS = new AppGPS();
        mPrintConnect = new PrintConnect(this);
        inicio();
    }

    private void inicio(){

        EDT_Patente = (EditText) findViewById(R.id.EDT_Patente);
        EDT_Patente.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_Patente);
                    label.setText(EDT_Patente.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Patente.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_Patente);
                label.setText("");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_Patente);
                    label.setText("");
                }
            }
        });


        SPIN_Espacios = (Spinner) findViewById(R.id.SPIN_Espacios);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.espacios_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SPIN_Espacios.setAdapter(adapter);

        BTN_IngresoPatente = (FloatingActionButton) findViewById(R.id.BTN_IngresoPatente);
        BTN_IngresoPatente.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                String patente = EDT_Patente.getText().toString();
                if (validaPatente() == 0){
                    return;
                }
                //Si tiene conexion a internet, entonces verifica si la patente registra deuda.
                if(Util.internetStatus(IngresoPatente.this)){
                    verificaPatenteDeuda(patente, 1);
                }else{
                    iniciarIngresoPatente(patente);
                }

            }

        });

        IMG_IngresoPatente = (ImageView) findViewById(R.id.IMG_IngresoPatente);

        BTN_Camara = (FloatingActionButton) findViewById(R.id.BTN_Camara);
        BTN_Camara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirCamara();
            }
        });

        BTN_Comentario = (FloatingActionButton) findViewById(R.id.BTN_Comentario);
        BTN_Comentario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comentarioDialog(IngresoPatente.this);
            }

        });

    }

    private void iniciarIngresoPatente(String patente){
        String espacios  = SPIN_Espacios.getSelectedItem().toString();

        Date fechahora_in = new Date();
        String fecha_hora_in = AppHelper.fechaHoraFormat.format(fechahora_in);
        String id_registro_patente  = AppHelper.fechaHoraFormatID.format(fechahora_in)+"_"+AppHelper.getSerialNum()+"_"+patente;

        //confirmDialog(IngresoPatente.this,"Confirme para ingresar la patente "+patente, id_registro_patente ,patente, espacios, fecha_hora_in);
        finalizarIngresoPatente(id_registro_patente ,patente, espacios, fecha_hora_in);
    }

    public void finalizarIngresoPatente(final String id_registro_patente, final String patente, final String espacios, final String fecha_hora_in){

        String Resultado = consultaPatenteIngreso(patente);
        if (Resultado.equals("1")){ //patente existe
            reiniciaIngreso();
        }else if (Resultado.equals("0")){ //patente no existe (inserta)
            //Intenta obtener la ubicación GPS
            AppGPS.getLastLocation(new GPSCallback() {
                @Override
                public void onResponseSuccess(Location location) {
                    if(location != null){
                        g_latitud  = Double.toString(location.getLatitude());
                        g_longitud = Double.toString(location.getLongitude());
                    }else{
                        g_latitud = "";
                        g_longitud= "";
                    }
                    insertaPatenteIngreso(id_registro_patente, patente, espacios, fecha_hora_in, g_imagen_nombre, g_latitud, g_longitud, g_comentario);
                }
                @Override
                public void onResponseFailure(Exception e) {
                    g_latitud = "";
                    g_longitud= "";
                    insertaPatenteIngreso(id_registro_patente, patente, espacios, fecha_hora_in, g_imagen_nombre, g_latitud, g_longitud, g_comentario);
                    Log.d(AppHelper.LOG_TAG, "Ingreso Patente onResponseFailure "+e.getMessage());
                }
            });

        }else{ //error SQL consulta patente
            reiniciaIngreso();
            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente",Resultado);
        }




    }

    /*
    public void confirmDialog(Context context, String mensaje, final String id_registro_patente, final String patente, final String espacios, final String fecha_hora_in) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setMessage(mensaje)
                .setPositiveButton("Si",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String Resultado = consultaPatenteIngreso(patente);
                        if (Resultado.equals("1")){ //patente existe
                            reiniciaIngreso();
                        }else if (Resultado.equals("0")){ //patente no existe (inserta)
                            //Intenta obtener la ubicación GPS
                            AppGPS.getLastLocation(new GPSCallback() {
                                @Override
                                public void onResponseSuccess(Location location) {
                                    if(location != null){
                                        g_latitud  = Double.toString(location.getLatitude());
                                        g_longitud = Double.toString(location.getLongitude());
                                      }else{
                                        g_latitud = "";
                                        g_longitud= "";
                                    }
                                    insertaPatenteIngreso(id_registro_patente, patente, espacios, fecha_hora_in, g_imagen_nombre, g_latitud, g_longitud, g_comentario);
                                }
                                @Override
                                public void onResponseFailure(Exception e) {
                                    g_latitud = "";
                                    g_longitud= "";
                                    insertaPatenteIngreso(id_registro_patente, patente, espacios, fecha_hora_in, g_imagen_nombre, g_latitud, g_longitud, g_comentario);
                                    Log.d(AppHelper.LOG_TAG, "Ingreso Patente onResponseFailure "+e.getMessage());
                                }
                            });

                        }else{ //error SQL consulta patente
                            reiniciaIngreso();
                            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente",Resultado);
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }
    */

    private String consultaPatenteIngreso(String patente){

        try{
            String resultado;
            String[] args = new String[] {patente, String.valueOf(AppHelper.getUbicacion_id()),"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, patente, fecha_hora_in " +
                                                            "FROM tb_registro_patentes " +
                                                            "WHERE patente =? AND id_cliente_ubicacion =? AND finalizado =?", args);
            if (c.moveToFirst()){
                String  rs_patente = c.getString(1);
                String  rs_fecha_hora_in = c.getString(2);
                Util.alertDialog(IngresoPatente.this,"Ingreso Patente","Patente: "+rs_patente+" ya se encuentra ingresada"+"\n"+
                                                                       "Fecha hora ingreso: "+rs_fecha_hora_in);
                resultado = "1";
            }else{
                resultado = "0";
            }
            c.close();
            return resultado;

        }catch(SQLException e){ return e.getMessage(); }

    }

    private void insertaPatenteIngreso(String id_registro_patente, String patente, String espacios,
                                       String fecha_hora_in, String imagen_nombre, String latitud, String longitud, String comentario){

        try{

            AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_registro_patentes "+
                                                "(id, id_cliente_ubicacion, patente," +
                                                "espacios, fecha_hora_in, rut_usuario_in, " +
                                                "maquina_in, imagen_in, enviado_in, " +
                                                "fecha_hora_out, rut_usuario_out, maquina_out, " +
                                                "enviado_out, minutos, precio, " +
                                                "prepago, efectivo, latitud, " +
                                                "longitud, comentario ,finalizado, id_estado_deuda, fecha_hora_estado_deuda)"+
                                                "VALUES " +
                                                "('"+id_registro_patente+"','"+AppHelper.getUbicacion_id()+"','"+patente+"'," +
                                                "'"+espacios+"','"+fecha_hora_in+"' ,'"+AppHelper.getUsuario_rut()+"'," +
                                                "'"+AppHelper.getSerialNum()+"' ,'"+imagen_nombre+"', '0', " +
                                                "'', '', ''," +
                                                "'0','0','0'," +
                                                "'0','0','"+latitud+"'," +
                                                "'"+longitud+"','"+comentario+"','0', '0', datetime('now','localtime'));");

            imprimeVoucherIngreso(patente, espacios, fecha_hora_in);
            reiniciaIngreso();
            //Util.alertDialog(IngresoPatente.this,"Ingreso Patente","Patente: "+patente+" registrada correctamente");

        }catch(SQLException e){
            reiniciaIngreso();
            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente", e.getMessage());
            return;
        }

    }

    private void imprimeVoucherIngreso(String patente, String espacios, String fecha_hora_in){

        PrintUnits.setSpeed(mPrintConnect.os, 0);
        PrintUnits.setConcentration(mPrintConnect.os, 2);
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);

        String lb_ubicacion         = Util.formateaLineaEtiqueta("Zona:      "+AppHelper.getUbicacion_nombre());
        String lb_operador          = Util.formateaLineaEtiqueta("Operador:  "+AppHelper.getUsuario_codigo()+" "+AppHelper.getUsuario_nombre());
        String lb_patente           = Util.formateaLineaEtiqueta("Patente:   "+patente);
        String lb_fecha_hora_in     = Util.formateaLineaEtiqueta("Ingreso:   "+fecha_hora_in);
        String lb_espacios          = Util.formateaLineaEtiqueta("Espacios:  "+espacios);

        /** IMPRIME EL TEXTO **/
        String Texto    =  AppHelper.getVoucher_ingreso()+"\n"+
                           AppHelper.getDescripcion_tarifa()+"\n\n"+
                           lb_ubicacion+"\n"+
                           lb_operador+ "\n"+
                           lb_patente+"\n"+
                           lb_fecha_hora_in+"\n"+
                           lb_espacios+"\n";

        for (int i = 0; i < Texto.length(); i++) {
            sb.append(Texto.charAt(i));
        }
        sb.append("\n");
        mPrintConnect.send(sb.toString());

        /** IMPRIME EL CODIGO DE BARRAS **/
        sb.setLength(0);
        String Codigo_Barras = patente;
        for (int i = 0; i < Codigo_Barras.length(); i++) {
            sb.append(Codigo_Barras.charAt(i));
        }
        sb.append("\n");
        mPrintConnect.sendCode128(sb.toString(), 2, 80);

        /** IMPRIME ESPACIO PARA CORTAR ETIQUETA **/
        sb.setLength(0);
        for (int i = 0; i < 4; i++) {
            sb.append("\n");
        }
        mPrintConnect.send(sb.toString());

        /** SUMA UNA ETIQUETA IMPRESA **/
        int num_etiqueta_actual = 0;
        try{
            String[] args = new String[] {};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT num_etiqueta_actual FROM tb_etiquetas",args);
            if (c.moveToFirst()) {
                num_etiqueta_actual = c.getInt(0);
            }else{
                AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_etiquetas (num_etiqueta_actual) VALUES (0);");
            }

            c.close();

            int etiquetas_restantes = AppHelper.getVoucher_rollo_max() - num_etiqueta_actual;
            if (num_etiqueta_actual >= AppHelper.getVoucher_rollo_alert() && num_etiqueta_actual < AppHelper.getVoucher_rollo_max()){
                Toast.makeText(IngresoPatente.this, "El rollo de etiquetas ya casi se acaba, quedan cerca de "+etiquetas_restantes+" etiquetas disponibles para imprimir.", Toast.LENGTH_LONG).show();
            }else if (num_etiqueta_actual >= AppHelper.getVoucher_rollo_alert() && num_etiqueta_actual >= AppHelper.getVoucher_rollo_max()){
                Toast.makeText(IngresoPatente.this, "El rollo de etiquetas se acabó, inserte otro y reinicie el contador en el Menú de pantalla de inicio opción Reiniciar Etiquetas.", Toast.LENGTH_LONG).show();
            }

            AppCRUD.actualizaNumeroEtiqueta(IngresoPatente.this, num_etiqueta_actual, num_etiqueta_actual, true);

        }catch(SQLException e){
            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente", e.getMessage());
        }
    }

    private void reiniciaIngreso(){
        EDT_Patente.setText("");
        SPIN_Espacios.setSelection(0);
        IMG_IngresoPatente.setScaleType(ImageView.ScaleType.CENTER);
        IMG_IngresoPatente.setImageResource(R.drawable.ic_photo);
        g_imagen_nombre = "";
        g_comentario = "";
        g_latitud    = "";
        g_longitud   = "";
        BTN_Comentario.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue));
        BTN_Camara.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue));
    }

    private int validaPatente(){

        String patente = EDT_Patente.getText().toString();
        if (patente == null || patente.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_Patente);
            view.setText(EDT_Patente.getHint() + " no puede ser vacío");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_Patente.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return 0;
        }
        return 100;
    }

    private int verificaPatenteDeuda(final String patente, final int resultado){

        if (resultado == 1){
           ClienteAsync(AppHelper.getUrl_restful() + "verifica_patente_deuda/" + patente, new ClienteCallback() {

               @Override
               public void onResponse(int esError, int statusCode, String responseBody) {
                   if (esError == 0 && !responseBody.equals("")) {

                       JSONObject jsonRootObject = null;
                       try {
                           jsonRootObject = new JSONObject(responseBody);
                           JSONArray jsonArray = jsonRootObject.optJSONArray("verifica_patente_deuda");

                           if(jsonArray != null){
                               //Si el vehiculo no registra deuda, continua el ingreso de la patente.
                               if(jsonArray.length() == 0){
                                  iniciarIngresoPatente(patente);
                               }else{
                                   //Si el vehiculo registra deuda, entonces solicita confirmar el pago de esta para continuar.
                                   confirmDialogPagaPatenteDeuda(IngresoPatente.this, patente, "El vehículo ingresado registra deuda. Confirme el pago y luego ingrese el vehículo.", jsonArray);
                               }
                           }else{
                               jsonArray = jsonRootObject.optJSONArray("error");
                               if(jsonArray != null){
                                   JSONObject jsonObject = jsonArray.getJSONObject(0);
                                   Util.alertDialog(IngresoPatente.this,"AsyncError Ingreso Patente",jsonObject.optString("text"));
                               }
                           }

                       } catch (JSONException e) {
                           Util.alertDialog(IngresoPatente.this, "JSONException Ingreso Patente", e.getMessage());
                       }

                   } else {
                       Util.alertDialog(IngresoPatente.this, "Async Ingreso Patente", "ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                   }
               }

           });
        }

        return resultado;
    }

    private void confirmDialogPagaPatenteDeuda(Context context,final String patente, String mensaje, JSONArray jsonArray) {

        String id_registro_patente = "";
        String fechahora_in        = "";
        String nombre_ubicacion    = "";
        int espacios               = 0;
        String nombre_usuario_in   = "";
        int precio                 = 0;

        try {

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                id_registro_patente = jsonObject.optString("id");
                fechahora_in        = jsonObject.optString("fecha_hora_in");
                nombre_ubicacion    = jsonObject.optString("nombre_ubicacion");
                espacios            = jsonObject.optInt("espacios");
                nombre_usuario_in   = jsonObject.optString("nombre_usuario_in");
                precio              = jsonObject.optInt("precio");
            }

            final String cadena    = id_registro_patente+"/"+AppHelper.getUsuario_rut() +"/"+AppHelper.getSerialNum()+"/"+0+"/"+precio;

            LinearLayout lilav = new LinearLayout(this);
            lilav.setOrientation(LinearLayout.VERTICAL);

            LinearLayout lilah = new LinearLayout(this);
            lilah.setOrientation(LinearLayout.HORIZONTAL);
            lilah.setPadding(35,0,10,0);

            TXT_LB_Transaccion = new TextView(this);
            TXT_LB_Transaccion.setTextColor(Color.BLACK);
            TXT_LB_Transaccion.setTextSize(15);
            TXT_LB_Transaccion.setText("ID: ");
            TXT_Transaccion    = new TextView(this);
            TXT_Transaccion.setTextColor(Color.GRAY);
            TXT_Transaccion.setTextSize(15);
            TXT_Transaccion.setText(id_registro_patente);
            lilah.addView(TXT_LB_Transaccion);
            lilah.addView(TXT_Transaccion);
            lilav.addView(lilah);

            lilah = new LinearLayout(this);
            lilah.setOrientation(LinearLayout.HORIZONTAL);
            lilah.setPadding(35,0,10,0);
            TXT_LB_FechaHora = new TextView(this);
            TXT_LB_FechaHora.setTextColor(Color.BLACK);
            TXT_LB_FechaHora.setTextSize(15);
            TXT_LB_FechaHora.setText("Fecha: ");
            TXT_FechaHora    = new TextView(this);
            TXT_FechaHora.setTextColor(Color.GRAY);
            TXT_FechaHora.setTextSize(15);
            TXT_FechaHora.setText(fechahora_in);
            lilah.addView(TXT_LB_FechaHora);
            lilah.addView(TXT_FechaHora);
            lilav.addView(lilah);

            lilah = new LinearLayout(this);
            lilah.setOrientation(LinearLayout.HORIZONTAL);
            lilah.setPadding(35,0,10,0);
            TXT_LB_Ubicacion = new TextView(this);
            TXT_LB_Ubicacion.setTextColor(Color.BLACK);
            TXT_LB_Ubicacion.setTextSize(15);
            TXT_LB_Ubicacion.setText("Ubicación: ");
            TXT_Ubicacion    = new TextView(this);
            TXT_Ubicacion.setTextColor(Color.GRAY);
            TXT_Ubicacion.setTextSize(15);
            TXT_Ubicacion.setText(nombre_ubicacion);
            lilah.addView(TXT_LB_Ubicacion);
            lilah.addView(TXT_Ubicacion);
            lilav.addView(lilah);

            lilah = new LinearLayout(this);
            lilah.setOrientation(LinearLayout.HORIZONTAL);
            lilah.setPadding(35,0,10,0);
            TXT_LB_Espacios = new TextView(this);
            TXT_LB_Espacios.setTextColor(Color.BLACK);
            TXT_LB_Espacios.setTextSize(15);
            TXT_LB_Espacios.setText("Espacios: ");
            TXT_Espacios    = new TextView(this);
            TXT_Espacios.setTextColor(Color.GRAY);
            TXT_Espacios.setTextSize(15);
            TXT_Espacios.setText(String.valueOf(espacios));
            lilah.addView(TXT_LB_Espacios);
            lilah.addView(TXT_Espacios);
            lilav.addView(lilah);

            lilah = new LinearLayout(this);
            lilah.setOrientation(LinearLayout.HORIZONTAL);
            lilah.setPadding(35,0,10,0);
            TXT_LB_Operador = new TextView(this);
            TXT_LB_Operador.setTextColor(Color.BLACK);
            TXT_LB_Operador.setTextSize(15);
            TXT_LB_Operador.setText("Operador: ");
            TXT_Operador    = new TextView(this);
            TXT_Operador.setTextColor(Color.GRAY);
            TXT_Operador.setTextSize(15);
            TXT_Operador.setText(nombre_usuario_in);
            lilah.addView(TXT_LB_Operador);
            lilah.addView(TXT_Operador);
            lilav.addView(lilah);

            lilah = new LinearLayout(this);
            lilah.setOrientation(LinearLayout.HORIZONTAL);
            lilah.setPadding(35,0,10,0);
            TXT_LB_Precio = new TextView(this);
            TXT_LB_Precio.setTextColor(Color.BLACK);
            TXT_LB_Precio.setTextSize(15);
            TXT_LB_Precio.setText("Precio: ");
            TXT_Precio    = new TextView(this);
            TXT_Precio.setTextColor(Color.GRAY);
            TXT_Precio.setTextSize(15);
            TXT_Precio.setText("$"+String.format("%,d", precio).replace(",","."));
            lilah.addView(TXT_LB_Precio);
            lilah.addView(TXT_Precio);
            lilav.addView(lilah);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Regularizar Deuda Pendiente");
            builder.setMessage(mensaje);
            builder.setView(lilav);
            builder.setPositiveButton("Confirmar",  new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    EDT_Patente.setText("");
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
                    ClienteAsync(AppHelper.getUrl_restful() + "verifica_patente_deuda_upt/"+ cadena, new ClienteCallback() {

                        @Override
                        public void onResponse(int esError, int statusCode, String responseBody) {
                            if(esError == 0 && !responseBody.equals("")) {
                                //Si no hay error entonces recibira
                                try {
                                    JSONObject jsonRootObject = new JSONObject(responseBody);
                                    JSONArray jsonArray       = jsonRootObject.optJSONArray("registro_patente_deuda");
                                    if(jsonArray != null){
                                        try{

                                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                                            AppHelper.getParkgoSQLite().execSQL("INSERT OR REPLACE INTO tb_registro_patentes "+
                                                                                "(id, id_cliente_ubicacion, patente," +
                                                                                "espacios, fecha_hora_in, rut_usuario_in, " +
                                                                                "maquina_in, imagen_in, enviado_in, " +
                                                                                "fecha_hora_out, rut_usuario_out, maquina_out, " +
                                                                                "enviado_out, minutos, precio, " +
                                                                                "prepago, efectivo, latitud, " +
                                                                                "longitud, comentario ,finalizado," +
                                                                                "id_estado_deuda, fecha_hora_estado_deuda)"+
                                                                                "VALUES " +
                                                                                "('"+jsonObject.optString("id")+"','"+jsonObject.optInt("id_cliente_ubicacion")+"','"+jsonObject.optString("patente")+"'," +
                                                                                "'"+jsonObject.optInt("espacios")+"','"+jsonObject.optString("fecha_hora_in")+"' ,'"+jsonObject.optString("rut_usuario_in")+"'," +
                                                                                "'"+jsonObject.optString("maquina_in")+"' ,'"+jsonObject.optString("imagen_in")+"', '"+jsonObject.optInt("enviado_in")+"', " +
                                                                                "'"+jsonObject.optString("fecha_hora_out")+"', '"+jsonObject.optString("rut_usuario_out")+"', '"+jsonObject.optString("maquina_out")+"'," +
                                                                                "'"+jsonObject.optInt("enviado_out")+"','"+jsonObject.optInt("minutos")+"','"+jsonObject.optInt("precio")+"'," +
                                                                                "'"+jsonObject.optInt("prepago")+"','"+jsonObject.optInt("efectivo")+"','"+jsonObject.optString("latitud")+"'," +
                                                                                "'"+jsonObject.optString("longitud")+"','"+jsonObject.optString("comentario")+"','"+jsonObject.optInt("finalizado")+"'," +
                                                                                "'"+jsonObject.optInt("id_estado_deuda")+"', '"+jsonObject.optString("fecha_hora_estado_deuda")+"');");

                                            dialog.dismiss();
                                            int descuento_porciento = AppCRUD.getDescuentoGrupoConductor(IngresoPatente.this, jsonObject.optString("patente"));
                                            imprimeVoucherPagaPatenteDeuda(jsonObject.optString("patente"), jsonObject.optInt("espacios"), jsonObject.optString("fecha_hora_in"),
                                                                           jsonObject.optString("fecha_hora_out"), jsonObject.optInt("minutos"), AppHelper.getMinutos_gratis(),
                                                                           jsonObject.optInt("precio"), descuento_porciento);
                                            Util.alertDialog(IngresoPatente.this,"Ingreso Patente","Pago registrado correctamente. Ahora puede ingresar el vehículo.");

                                        }catch(SQLException e){
                                            dialog.dismiss();
                                            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente", e.getMessage());
                                        }
                                    }else{
                                        jsonArray = jsonRootObject.optJSONArray("error");
                                        if(jsonArray != null){
                                            dialog.dismiss();
                                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                                            Util.alertDialog(IngresoPatente.this,"AsyncError Ingreso Patente", jsonObject.optString("text"));
                                        }
                                    }
                                } catch (JSONException e) {
                                    dialog.dismiss();
                                    Util.alertDialog(IngresoPatente.this,"JSONException Ingreso Patente", e.getMessage());
                                }

                            }else{
                                dialog.dismiss();
                                Util.alertDialog(IngresoPatente.this,"Async Ingreso Patente", "ERROR SYNC Código: "+ statusCode + "\n" + responseBody );
                            }
                        }

                    });

                }

            });

        }catch (JSONException e1) {
            Util.alertDialog(IngresoPatente.this,"JSONException Ingreso Patente",e1.getMessage());
        }

    }

    private void imprimeVoucherPagaPatenteDeuda(String patente, int espacios, String fecha_hora_in,
                                                      String fecha_hora_out, int minutos, int minutos_gratis, int precio,
                                                      int porcent_descuento){

        PrintUnits.setSpeed(mPrintConnect.os, 0);
        PrintUnits.setConcentration(mPrintConnect.os, 2);
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);

        String lb_ubicacion         = Util.formateaLineaEtiqueta("Zona:      "+AppHelper.getUbicacion_nombre());
        String lb_operador          = Util.formateaLineaEtiqueta("Operador:  "+AppHelper.getUsuario_codigo()+" "+AppHelper.getUsuario_nombre());
        String lb_patente           = Util.formateaLineaEtiqueta("Patente:   "+patente);
        String lb_espacios          = Util.formateaLineaEtiqueta("Espacios:  "+espacios);
        String lb_fecha_hora_in     = Util.formateaLineaEtiqueta("Ingreso:   "+fecha_hora_in);
        String lb_fecha_hora_out    = Util.formateaLineaEtiqueta("Retiro:    "+fecha_hora_out);
        String lb_tiempo            = Util.formateaLineaEtiqueta("Tiempo:    "+String.format("%,d", minutos).replace(",",".")+" min");
        String lb_gratis            = Util.formateaLineaEtiqueta("Gratis:    "+String.format("%,d", minutos_gratis).replace(",",".")+" min");
        String lb_total             = Util.formateaLineaEtiqueta("TOTAL:     $"+String.format("%,d", precio).replace(",","."));
        String lb_descuento         = Util.formateaLineaEtiqueta("Descuento: "+String.format("%,d", porcent_descuento).replace(",",".")+"%");

        /** IMPRIME EL TEXTO **/
        String Texto    =   AppHelper.getVoucher_salida()+"\n"+
                AppHelper.getDescripcion_tarifa()+"\n\n"+
                lb_ubicacion+"\n"+
                lb_operador+"\n"+
                lb_patente+"\n"+
                lb_espacios+"\n"+
                lb_fecha_hora_in+"\n"+
                lb_fecha_hora_out+"\n"+
                lb_tiempo+"\n"+
                lb_gratis+"\n"+
                lb_total+"\n"+
                lb_descuento;

        for (int i = 0; i < Texto.length(); i++) {
            sb.append(Texto.charAt(i));
        }
        sb.append("\n");
        mPrintConnect.send(sb.toString());

        /** IMPRIME ESPACIO PARA CORTAR ETIQUETA **/
        sb.setLength(0);
        for (int i = 0; i < 4; i++) {
            sb.append("\n");
        }
        mPrintConnect.send(sb.toString());
        EDT_Patente.setText("");


        /** SUMA UNA ETIQUETA IMPRESA **/
        int num_etiqueta_actual = 0;
        try{
            String[] args = new String[] {};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT num_etiqueta_actual FROM tb_etiquetas",args);
            if (c.moveToFirst()) {
                num_etiqueta_actual = c.getInt(0);
            }else{
                AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_etiquetas (num_etiqueta_actual) VALUES (0);");
            }
            c.close();

            int etiquetas_restantes = AppHelper.getVoucher_rollo_max() - num_etiqueta_actual;
            if (num_etiqueta_actual >= AppHelper.getVoucher_rollo_alert() && num_etiqueta_actual < AppHelper.getVoucher_rollo_max()){
                Toast.makeText(IngresoPatente.this, "El rollo de etiquetas ya casi se acaba, quedan cerca de "+etiquetas_restantes+" etiquetas disponibles para imprimir.", Toast.LENGTH_LONG).show();
            }else if (num_etiqueta_actual >= AppHelper.getVoucher_rollo_alert() && num_etiqueta_actual >= AppHelper.getVoucher_rollo_max()){
                Toast.makeText(IngresoPatente.this, "El rollo de etiquetas se acabó, inserte otro y reinicie el contador en el Menú de pantalla de inicio opción Reiniciar Etiquetas.", Toast.LENGTH_LONG).show();
            }

            AppCRUD.actualizaNumeroEtiqueta(IngresoPatente.this, num_etiqueta_actual, num_etiqueta_actual, true);

        }catch(SQLException e){
            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente", e.getMessage());
        }
    }

    public void ClienteAsync(String url, final ClienteCallback clienteCallback) {

        cliente = new AsyncHttpClient();
        cliente.get(App.context, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente onSuccess " + new String(responseBody));
                clienteCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente onFailure " + error.getMessage());
                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente onFailure cancelRequests");
            }

        });
    }

    private void abrirCamara() {

        String patente = EDT_Patente.getText().toString();
        if (validaPatente() == 0){
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = creaArchivoImagen(patente);

            } catch (IOException ex) {
                Util.alertDialog(IngresoPatente.this,"IOException Ingreso Patente","Error al crear archivo imagen "+ex.getMessage());
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,  Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        }
    }

    private File creaArchivoImagen(String patente) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = patente+"_" + timeStamp + "_";
        File storageDir = AppHelper.getImageDir(this);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        //elimina la imagen anterior para reemplazarla por la actual.
        if(!g_imagen_path.isEmpty()) {
            File file = new File(g_imagen_path);
            if(file.delete()){

                IMG_IngresoPatente.setScaleType(ImageView.ScaleType.CENTER);
                IMG_IngresoPatente.setImageResource(R.drawable.ic_photo);
            }
        }

        // Save a file: path for use with ACTION_VIEW intents
        g_imagen_path   = image.getAbsolutePath();
        g_imagen_nombre = image.getName();

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //recupera muestra la imagen de la camara.
            Bitmap imagenBitmap = BitmapFactory.decodeFile(g_imagen_path);
            IMG_IngresoPatente.setScaleType(ImageView.ScaleType.FIT_XY);
            IMG_IngresoPatente.setImageBitmap(imagenBitmap);
            BTN_Camara.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.orange));

            //Obtiene el tamaño del archivo en KB
            double sizeInBytes = new File(g_imagen_path).length();
            //Convierte los KB a MB
            double sizeInMb = sizeInBytes / (double)(1024 * 1024);
            if (sizeInMb > AppHelper.getImagen_max_mb()){
                //elimina la imagen.
                if(!g_imagen_path.isEmpty()) {
                    File file = new File(g_imagen_path);
                    if(file.delete()){
                        IMG_IngresoPatente.setScaleType(ImageView.ScaleType.CENTER);
                        IMG_IngresoPatente.setImageResource(R.drawable.ic_photo);
                    }
                }
                Util.alertDialog(IngresoPatente.this, "Ingreso Patente","El tamaño de la imagen es "+sizeInMb+" MB y excede el máximo permitido de "+AppHelper.getImagen_max_mb()+" MB, configure la cámara del dispositivo según la especificación entregada.");
            }

        }else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            File file = new File(g_imagen_path);
            file.delete();
            g_imagen_nombre = "";
            g_imagen_path   = "";
            BTN_Camara.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue));
        }
    }

    private void comentarioDialog(Context context) {

        final EditText EDT_Comentario = new EditText(context);
        EDT_Comentario.setSingleLine(false);
        EDT_Comentario.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        EDT_Comentario.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EDT_Comentario.setLines(1);
        EDT_Comentario.setMaxLines(10);
        EDT_Comentario.setVerticalScrollBarEnabled(true);
        EDT_Comentario.setMovementMethod(ScrollingMovementMethod.getInstance());
        EDT_Comentario.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        EDT_Comentario.setText(g_comentario);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Agregar comentario");
        builder.setView(EDT_Comentario);
        builder.setPositiveButton("Agregar",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                BTN_Comentario.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.orange));
                g_comentario = EDT_Comentario.getText().toString();

            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,int id) {
                BTN_Comentario.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue));
                g_comentario = "";
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (appGPS.verificaConexionGoogleApi(IngresoPatente.this)) {
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
        if (appGPS.verificaConexionGoogleApi(IngresoPatente.this)) {
            appGPS.pausarGPS();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appGPS.verificaConexionGoogleApi(IngresoPatente.this)) {
            appGPS.desconectaGPS();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPrintConnect != null) {
            mPrintConnect.stop();
        }
    }

}
