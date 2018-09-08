package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

public class RetiroPatente extends AppCompatActivity {

    Print_Thread printThread = null;
    private AsyncHttpClient cliente = null;

    private ScanBroadcastReceiver ScanReceiver = null;
    private IntentFilter ScanIntentFilter      = null;

    private Button   BTN_Scan;
    private EditText EDT_Patente;
    private Button   BTN_Efectivo;
    private Button   BTN_Prepago;

    private TextView TV_RS_Patente;
    private TextView TV_RS_Espacios;
    private TextView TV_RS_Fecha_IN;
    private TextView TV_RS_Fecha_OUT;
    private TextView TV_RS_Minutos;
    private TextView TV_RS_Precio;
    private TextView TV_RS_Minutos_Gratis;
    private TextView TV_RS_Porcentaje_Descuento;

    private ProgressDialog esperaDialog;

    private String data = "";
    private int    count= 0;

    //Variables utilizadas para finalizar la salida de la patente.
    private String g_id_registro_patente = "";
    private String g_patente        = "";
    private int g_espacios          = 0;
    private String g_fecha_hora_in  = "";
    private String g_fecha_hora_out = "";
    private int g_minutos           = 0;
    private int g_minutos_gratis    = 0;
    private int g_precio            = 0;
    private int g_porcent_descuento = 0;


    private String g_prepago_clave  = "";
    private int g_prepago_saldo     = 0;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 11:
                    String str=msg.obj.toString();
                    if (data.equals(str)){
                        count++;
                    }else{
                        count=1;
                    }
                    data=str;
                    EDT_Patente.setText(str);
                    retiroPatente();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_retiro_patente);
        inicio();
    }

    public void inicio(){

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

        BTN_Scan = (Button) findViewById(R.id.BTN_Scan);
        BTN_Scan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){

                EDT_Patente.setText("");

                Intent intent = new Intent ("ACTION_BAR_TRIGSCAN");
                intent.putExtra("timeout", 1);
                //levanta el scanner
                getApplicationContext().sendBroadcast(intent);
                //crea los objetos
                ScanReceiver     = new ScanBroadcastReceiver();
                ScanIntentFilter = new IntentFilter("ACTION_BAR_SCAN");
                //envia los objetos para obtener el resultado del scan
                getApplicationContext().registerReceiver(ScanReceiver, ScanIntentFilter);


            }
        });


        EDT_Patente.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction()!=KeyEvent.ACTION_UP)
                    return false;

                switch (event.getKeyCode()) {
                    case 66: //enter
                        retiroPatente();
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        break;
                    default:
                        break;
                }

                return false;
            }
        });

        TV_RS_Patente   = (TextView) findViewById(R.id.TV_RS_Patente);
        TV_RS_Espacios  = (TextView) findViewById(R.id.TV_RS_Espacios);
        TV_RS_Fecha_IN  = (TextView) findViewById(R.id.TV_RS_Fecha_IN);
        TV_RS_Fecha_OUT = (TextView) findViewById(R.id.TV_RS_Fecha_OUT);
        TV_RS_Minutos   = (TextView) findViewById(R.id.TV_RS_Minutos);
        TV_RS_Precio    = (TextView) findViewById(R.id.TV_RS_Precio);
        TV_RS_Minutos_Gratis = (TextView) findViewById(R.id.TV_RS_Minutos_Gratis);
        TV_RS_Porcentaje_Descuento = (TextView) findViewById(R.id.TV_RS_Porcentaje_Descuento);

        BTN_Efectivo = (Button) findViewById(R.id.BTN_Efectivo);
        BTN_Efectivo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){

                if(!String.valueOf(EDT_Patente.getText()).equals("")) {
                    retiroPatente();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }else if (!g_id_registro_patente.equals("") && String.valueOf(EDT_Patente.getText()).equals("")) {
                    confirmDialogEfectivo(RetiroPatente.this, "Confirme para retirar patente " + g_patente);
                }else{
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente", "No ha ingresado patente a retirar, verifique ");
                }

            }
        });

        BTN_Prepago = (Button) findViewById(R.id.BTN_Prepago);
        BTN_Prepago.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                if(Util.internetStatus(RetiroPatente.this)) {
                    if (!String.valueOf(EDT_Patente.getText()).equals("")) {
                        retiroPatente();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    } else if (!g_id_registro_patente.equals("") && String.valueOf(EDT_Patente.getText()).equals("")) {
                        esperaDialog = ProgressDialog.show(RetiroPatente.this, "", "Verificando saldo prepago...", true);
                        esperaDialog.show();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                consultaPatentePrepago(g_patente, AppHelper.getCliente_id());
                            }
                        }, 500);

                    } else {
                        Util.alertDialog(RetiroPatente.this, "Retiro Patente", "No ha ingresado patente a retirar, verifique ");
                    }
                }else{
                    Util.alertDialog(RetiroPatente.this, "Retiro Patente", "Verifique conexión a Internet, no puede retirar prepago");
                }


            }
        });

    }

    private void retiroPatente(){

        esperaDialog = ProgressDialog.show(this, "", "Consultando por favor espere...", true);
        esperaDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                procesoRetiroPatente();
                esperaDialog.dismiss();
            }
        }, 200);

    }

    private void procesoRetiroPatente(){

        String patente = EDT_Patente.getText().toString();
        EDT_Patente.setText("");
        if (patente == null || patente.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_Patente);
            view.setText(EDT_Patente.getHint() + " no puede ser vacío");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_Patente.setBackground(getDrawable(R.drawable.text_border_error));
            }
        }else {

            try {

                String[] args = new String[]{patente, String.valueOf(AppHelper.getUbicacion_id()), "0"};
                Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT\n" +
                                                                "trp.id, trp.patente, trp.fecha_hora_in,\n" +
                                                                "datetime('now','localtime') as fecha_hora_out,\n" +
                                                                "trp.espacios,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(trp.fecha_hora_in)) As Integer) as dias,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(trp.fecha_hora_in)) * 24 As Integer) as horas,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(trp.fecha_hora_in)) * 24 * 60 As Integer) as minutos,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(trp.fecha_hora_in)) * 24 * 60 * 60 As Integer) as segundos, \n" +
                                                                "trp.prepago, trp.efectivo, tcu.id_cliente \n"+
                                                                "FROM tb_registro_patentes trp\n" +
                                                                "INNER JOIN tb_cliente_ubicaciones tcu ON tcu.id = trp.id_cliente_ubicacion\n" +
                                                                "WHERE trp.patente =? AND trp.id_cliente_ubicacion=? AND trp.finalizado =?", args);
                if (c.moveToFirst()) {
                    String rs_id = c.getString(0);
                    String rs_patente = c.getString(1);
                    String rs_fecha_hora_in = c.getString(2);
                    String rs_fecha_hora_out = c.getString(3);
                    int rs_espacios = c.getInt(4);
                    int rs_dias = c.getInt(5);
                    int rs_horas = c.getInt(6);
                    int rs_minutos = c.getInt(7);
                    int rs_segundos = c.getInt(8);
                    int rs_prepago = c.getInt(9);
                    int rs_efectivo = c.getInt(10);
                    int rs_id_cliente = c.getInt(11);

                    int precio = 0;
                    int total_minutos = (rs_minutos - AppHelper.getMinutos_gratis());

                    //Calcula el precio, ya sea por minuto, tramo ó primer tramo mas minutos.
                    if (total_minutos > 0) {
                        precio = Util.calcularPrecio(total_minutos, rs_espacios, rs_prepago, rs_efectivo);
                    }

                    //Aplica descuento de grupo conductor en caso que existe.
                    int descuento_porciento = AppCRUD.getDescuentoGrupoConductor(RetiroPatente.this, rs_patente);
                    precio = Util.redondearPrecio(precio, descuento_porciento);

                    TV_RS_Patente.setText("Patente:    " + rs_patente);
                    TV_RS_Espacios.setText("Espacios: " + rs_espacios);
                    TV_RS_Fecha_IN.setText("Fecha ingreso: " + rs_fecha_hora_in);
                    TV_RS_Fecha_OUT.setText("Fecha retiro:     " + rs_fecha_hora_out);
                    TV_RS_Minutos.setText("Tiempo:            " + String.format("%,d", rs_minutos).replace(",",".") + " min");
                    TV_RS_Precio.setText("Precio:              $" + String.format("%,d", precio).replace(",","."));
                    TV_RS_Minutos_Gratis.setText("Gratis:               "+ AppHelper.getMinutos_gratis()+ " min");
                    TV_RS_Porcentaje_Descuento.setText("Descuento:       "+descuento_porciento+"%");


                    EDT_Patente.setText("");
                    //Setea las variables para finalizar el retiro.
                    g_id_registro_patente = rs_id;
                    g_patente           = rs_patente;
                    g_espacios          = rs_espacios;
                    g_fecha_hora_in     = rs_fecha_hora_in;
                    g_fecha_hora_out    = rs_fecha_hora_out;
                    g_minutos           = rs_minutos;
                    g_minutos_gratis    = AppHelper.getMinutos_gratis();
                    g_precio            = precio;
                    g_porcent_descuento = descuento_porciento;

                } else {
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente", "Patente: " + patente + " no registra ingreso, verifique");
                    reiniciaRetiro();
                }
                c.close();

            } catch (SQLException e) {
                Util.alertDialog(RetiroPatente.this,"SQLException Retiro Patente", e.getMessage() );
            }
        }
    }

    private void consultaPatentePrepago(String patente, int cliente_id){

        ClienteAsync(AppHelper.getUrl_restful() + "conductor_saldo_prepago/" + cliente_id + "/" + patente, new ClienteCallback() {

            @Override
            public void onResponse(int esError, int statusCode, String responseBody) {

                if (esError == 0 && !responseBody.equals("")) {
                    try {
                        g_prepago_clave = "";
                        g_prepago_saldo = 0;

                        JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray = jsonRootObject.optJSONArray("conductor_saldo_prepago");
                        if (jsonArray != null) {

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                g_prepago_clave = jsonObject.optString("clave");
                                g_prepago_saldo = jsonObject.optInt("saldo");
                            }

                            if (g_prepago_saldo == 0) {
                                Util.alertDialog(RetiroPatente.this, "Retiro Patente", "No tiene saldo prepago, verifique");
                            } else if (g_precio <= g_prepago_saldo) {
                                confirmDialogPrepago(RetiroPatente.this, g_precio, 0, "Ingrese clave de 4 dígitos y confirme retiro prepago patente " + g_patente);
                            } else if (g_precio > g_prepago_saldo) {
                                int efectivo = g_precio - g_prepago_saldo;
                                confirmDialogPrepagoSaldo(RetiroPatente.this, g_prepago_saldo, efectivo, "El saldo prepago del conductor insuficiente y deberá cancelar $" + String.format("%,d", efectivo).replace(",", ".") + " en efectivo, confirme para continuar");
                            }

                        } else {
                            jsonArray = jsonRootObject.optJSONArray("error");
                            if (jsonArray != null) {
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                Util.alertDialog(RetiroPatente.this, "ErrorSync Retiro Patente Prepago", jsonObject.optString("text"));
                            }
                        }
                    } catch (JSONException e) {
                        Util.alertDialog(RetiroPatente.this, "JSONException Retiro Patente Prepago", e.getMessage());
                    }

                } else {
                    Util.alertDialog(RetiroPatente.this, "ErrorSync Retiro Patente Prepago", "Código: " + statusCode + "\n" + responseBody);
                }
            }

        });

    }

    private void confirmDialogEfectivo(Context context, String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle("Retiro Patente")
                .setMessage(mensaje)
                .setPositiveButton("Si",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        int print_result =  imprimeVoucherRetiro(g_patente, g_espacios, g_fecha_hora_in, g_fecha_hora_out, g_minutos, g_minutos_gratis, g_precio, g_porcent_descuento);
                        if (print_result == 0 || print_result == -2) {
                            String Resultado = actualizaRetiroPatente(g_id_registro_patente, g_fecha_hora_out, g_minutos, g_precio, 0, g_precio);
                            if (Resultado.equals("1")) {
                                //Util.alertDialog(RetiroPatente.this,"Retiro Patente","Patente: "+g_patente+" retirada correctamente");
                            } else {
                                Util.alertDialog(RetiroPatente.this, "SQLException Retiro Patente", Resultado);
                            }
                            reiniciaRetiro();
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

    private void confirmDialogPrepago(Context context, final int prepago, final int efectivo, String mensaje) {

        final EditText EDT_Clave_Prepago = new EditText(context);
        EDT_Clave_Prepago.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        EDT_Clave_Prepago.setTransformationMethod(PasswordTransformationMethod.getInstance());
        EDT_Clave_Prepago.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Retiro Patente");
        builder.setMessage(mensaje);
        builder.setView(EDT_Clave_Prepago);
        builder.setPositiveButton("Confirmar",  new DialogInterface.OnClickListener() {
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
                if (!g_prepago_clave.equals(EDT_Clave_Prepago.getText().toString())){
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente","Clave ingresada no corresponde, verifique");
                }else {
                    dialog.dismiss();
                    int print_result = imprimeVoucherRetiro(g_patente, g_espacios, g_fecha_hora_in, g_fecha_hora_out, g_minutos, g_minutos_gratis, g_precio, g_porcent_descuento);
                    if (print_result == 0 || print_result == -2) {
                        String Resultado = actualizaRetiroPatente(g_id_registro_patente, g_fecha_hora_out, g_minutos, g_precio, prepago, efectivo);
                        if (Resultado.equals("1")) {
                            //Util.alertDialog(RetiroPatente.this, "Retiro Patente", "Patente " + g_patente + " retirada correctamente");
                            reiniciaRetiro();
                        } else {
                            Util.alertDialog(RetiroPatente.this, "SQLException Retiro Patente", Resultado);
                        }
                    }

                }
            }

        });

    }

    private void confirmDialogPrepagoSaldo(Context context, final int prepago, final int efectivo, String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle("Retiro Patente")
                .setMessage(mensaje)
                .setPositiveButton("Si",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        confirmDialogPrepago(RetiroPatente.this, prepago, efectivo, "Ingrese clave de 4 dígitos y confirme retiro prepago patente " + g_patente);
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

    private String actualizaRetiroPatente(String id_registro_patente, String fecha_hora_out, int minutos, int precio, int prepago, int efectivo ){
        try{
            AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patentes " +
                    "SET " +
                    "fecha_hora_out = '"+fecha_hora_out+"', " +
                    "rut_usuario_out = '"+AppHelper.getUsuario_rut()+"' , " +
                    "maquina_out = '"+AppHelper.getSerialNum()+"', " +
                    "minutos = "+minutos+", " +
                    "precio = "+precio+", " +
                    "prepago = "+prepago+", " +
                    "efectivo = "+efectivo+", " +
                    "finalizado = '1', " +
                    "id_estado_deuda = '0', " +
                    "fecha_hora_estado_deuda = datetime('now','localtime') "+
                    "WHERE id = '"+id_registro_patente+"'");
        }catch(SQLException e){  return e.getMessage(); }

        return "1";
    }

    private int imprimeVoucherRetiro(String patente, int espacios, String fecha_hora_in,
                                      String fecha_hora_out, int minutos, int minutos_gratis, int precio,
                                      int porcent_descuento){
        try {
            /** IMPRIME LA ETIQUETA **/
            if (printThread != null && !printThread.isThreadFinished()) {
                Log.d(AppHelper.LOG_PRINT, "Thread is still running...");
                return -1;
            }else {
                printThread = new Print_Thread(1, patente, espacios, fecha_hora_in, fecha_hora_out,
                                               minutos, minutos_gratis, precio, porcent_descuento);
                printThread.start();
                    printThread.join();
                EDT_Patente.setText("");
                return printThread.getRESULT_CODE();
            }
        } catch (InterruptedException e) {
            Util.alertDialog(RetiroPatente.this,"InterruptedException Retiro Patente", e.getMessage());
            return -1;
        }
    }

    private void reiniciaRetiro(){

        EDT_Patente.setText("");
        TV_RS_Patente.setText("");
        TV_RS_Espacios.setText("");
        TV_RS_Fecha_IN.setText("");
        TV_RS_Fecha_OUT.setText("");
        TV_RS_Minutos.setText("");
        TV_RS_Minutos_Gratis.setText("");
        TV_RS_Precio.setText("");
        TV_RS_Porcentaje_Descuento.setText("");

        g_id_registro_patente   = "";
        g_patente               = "";
        g_espacios              = 0;
        g_fecha_hora_out        = "";
        g_fecha_hora_in         = "";
        g_minutos               = 0;
        g_minutos_gratis        = 0;
        g_precio                = 0;
        g_porcent_descuento     = 0;
        g_prepago_clave         = "";
        g_prepago_saldo         = 0;

    }

    public void ClienteAsync(String url, final ClienteCallback clienteCallback) {
        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        cliente.get(RetiroPatente.this, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                esperaDialog.dismiss();
                clienteCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "onFailure RetiroPatente statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "onFailure RetiroPatente responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "onFailure RetiroPatente error "+String.valueOf(Log.getStackTraceString(error)));

                esperaDialog.dismiss();
                Util.alertDialog(RetiroPatente.this, "onFailure RetiroPatente Prepago", error.getMessage());
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (event.getKeyCode()) {
            case 223:
                break;
            case 224:
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private class ScanBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            /*****EXTRAS POSIBLES*****
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Set<String> keys = bundle.keySet();
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    Log.d(AppHelper.LOG_TAG, "[" + key + "=" + bundle.get(key) + "]");
                }
            }

                EXTRA_SCAN_STATE
                EXTRA_SCAN_ENCODE_MODE
                EXTRA_SCAN_LENGTH
                EXTRA_SCAN_DATA
            **************************/
             final String scanResult = intent.getStringExtra("EXTRA_SCAN_DATA");

            if (scanResult.length() > 0) {
                EDT_Patente.setText(scanResult);
                retiroPatente();
            }
            if (ScanReceiver != null) {
                context.unregisterReceiver(ScanReceiver);
                ScanReceiver = null;
            }
        }
    }


}
