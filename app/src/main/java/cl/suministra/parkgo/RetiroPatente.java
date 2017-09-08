package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.obm.mylibrary.PrintConnect;
import com.obm.mylibrary.PrintUnits;
import com.obm.mylibrary.ScanConnect;

public class RetiroPatente extends AppCompatActivity {

    private EditText EDT_Patente;
    private Button   BTN_Efectivo;
    private Button   BTN_Prepago;

    private TextView TV_RS_Patente;
    private TextView TV_RS_Espacios;
    private TextView TV_RS_Fecha_IN;
    private TextView TV_RS_Fecha_OUT;
    private TextView TV_RS_Minutos;
    private TextView TV_RS_Precio;

    private ProgressDialog esperaDialog;

    public  PrintConnect mPrintConnect;
    private ScanConnect mScanConnect;
    private String data = "";
    private int    count= 0;

    //Variables utilizadas para finalizar la salida de la patente.
    private String g_id_registro_patente;
    private String g_patente;
    private int g_espacios;
    private String g_fecha_hora_in;
    private String g_fecha_hora_out;
    private int g_minutos;
    private int g_precio;

    private String g_prepago_rut;
    private String g_prepago_clave;
    private int g_prepago_saldo;
    private int g_prepago_saldo_final;

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
        setContentView(R.layout.activity_retiro_patente);
        //mPrintConnect = new PrintConnect(this);
        //mScanConnect  = new ScanConnect(this, mHandler);
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

        BTN_Efectivo = (Button) findViewById(R.id.BTN_Efectivo);
        BTN_Efectivo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                if (!g_id_registro_patente.equals("")) {
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
                if (!g_id_registro_patente.equals("")) {
                    if (g_prepago_saldo == 0 ) {
                        Util.alertDialog(RetiroPatente.this,"Retiro Patente","No tiene saldo prepago, verifique");
                    }else if(g_precio <= g_prepago_saldo) {
                        confirmDialogPrepago(RetiroPatente.this, "Ingrese clave de 4 dígitos y confirme retiro prepago patente " + g_patente);
                    }else{
                        Util.alertDialog(RetiroPatente.this,"Retiro Patente","Su saldo prepago es insuficiente para completar la operación");
                        return;
                    }

                   }else{
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente", "No ha ingresado patente a retirar, verifique ");
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
        }, 1500);

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

                String[] args = new String[]{patente, "0"};
                Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT\n" +
                                                                "id, patente, fecha_hora_in,\n" +
                                                                "datetime('now','localtime') as fecha_hora_out,\n" +
                                                                "espacios,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) As Integer) as dias,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) * 24 As Integer) as horas,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) * 24 * 60 As Integer) as minutos,\n" +
                                                                "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) * 24 * 60 * 60 As Integer) as segundos\n" +
                                                                "FROM tb_registro_patente\n" +
                                                                "WHERE patente =? AND finalizado =?", args);
                if (c.moveToFirst()) {
                    String rs_id             = c.getString(0);
                    String rs_patente        = c.getString(1);
                    String rs_fecha_hora_in  = c.getString(2);
                    String rs_fecha_hora_out = c.getString(3);
                    int rs_espacios = c.getInt(4);
                    int rs_dias     = c.getInt(5);
                    int rs_horas    = c.getInt(6);
                    int rs_minutos  = c.getInt(7);
                    int rs_segundos = c.getInt(8);

                    int precio      = 0;
                    int total_minutos =  (rs_minutos - AppHelper.getMinutos_gratis());
                    if (total_minutos > 0){
                        precio = total_minutos * AppHelper.getValor_minuto();
                    }

                    TV_RS_Patente.setText("Patente:    " + rs_patente);
                    TV_RS_Espacios.setText("Espacios: " + rs_espacios);
                    TV_RS_Fecha_IN.setText("Fecha ingreso: " + rs_fecha_hora_in);
                    TV_RS_Fecha_OUT.setText("Fecha retiro:     " + rs_fecha_hora_out);
                    TV_RS_Minutos.setText("Tiempo:            " + String.format("%,d", rs_minutos).replace(",",".") + " min");
                    TV_RS_Precio.setText("Precio:              $" + String.format("%,d", precio).replace(",","."));

                    EDT_Patente.setText("");
                    //Setea las variables para finalizar el retiro.
                    g_id_registro_patente = rs_id;
                    g_patente        = rs_patente;
                    g_espacios       = rs_espacios;
                    g_fecha_hora_in  = rs_fecha_hora_in;
                    g_fecha_hora_out = rs_fecha_hora_out;
                    g_minutos        = rs_minutos;
                    g_precio         = precio;

                    consultaPatentePrepago(patente);

                } else {
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente", "Patente: " + patente + " no registra ingreso, verifique");
                    reiniciaRetiro();
                }
                c.close();

            } catch (SQLException e) {
                Util.alertDialog(RetiroPatente.this,"Retiro Patente", e.getMessage() );
            }
        }
    }

    private void consultaPatentePrepago(String patente){
        String[] args = new String[]{patente, String.valueOf(AppHelper.getCliente_id())};
        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT tc.rut AS rut, tc.clave AS clave, tc.saldo AS saldo FROM tb_conductor tc\n" +
                                                        "INNER JOIN tb_conductor_patentes tcp ON tc.rut = tcp.rut_conductor\n" +
                                                        "WHERE tcp.patente =? AND tc.id_cliente=? ", args);
        if (c.moveToFirst()) {
            String rs_prepago_rut   = c.getString(0);
            String rs_prepago_clave = c.getString(1);
            int rs_prepago_saldo    = c.getInt(2);

            g_prepago_rut   = rs_prepago_rut;
            g_prepago_clave = rs_prepago_clave;
            g_prepago_saldo = rs_prepago_saldo;

        }else{

            g_prepago_rut   = "";
            g_prepago_clave = "";
            g_prepago_saldo = 0;
        }
        c.close();
    }

    private void confirmDialogEfectivo(Context context, String mensaje) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder
                .setTitle("Retiro Patente")
                .setMessage(mensaje)
                .setPositiveButton("Si",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String Resultado = actualizaPatenteRetiro(g_id_registro_patente, g_fecha_hora_out, g_minutos, g_precio, 0);
                        if (Resultado.equals("1")){
                            //imprimeVoucherRetiro(g_patente, g_espacios, g_fecha_hora_in, g_fecha_hora_out, g_minutos, g_precio);
                            Util.alertDialog(RetiroPatente.this,"Retiro Patente","Patente: "+g_patente+" retirada correctamente");
                        }else{
                            Util.alertDialog(RetiroPatente.this,"Retiro Patente",Resultado);
                        }
                        reiniciaRetiro();
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

    private void confirmDialogPrepago(Context context, String mensaje) {
        final EditText EDT_Clave_Prepago = new EditText(context);
        EDT_Clave_Prepago.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        EDT_Clave_Prepago.setTransformationMethod(PasswordTransformationMethod.getInstance());
        EDT_Clave_Prepago.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
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

        final android.app.AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                if (!g_prepago_clave.equals(EDT_Clave_Prepago.getText().toString())){
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente","Clave ingresada no corresponde, verifique");
                    return;
                }

                g_prepago_saldo_final = g_prepago_saldo - g_precio;

                String Resultado = actualizaPatenteRetiro(g_id_registro_patente, g_fecha_hora_out, g_minutos, g_precio, 1);
                if (Resultado.equals("1")) {
                    Resultado = actualizaPatenteSaldoPrepago();
                    if (Resultado.equals("1")) {
                        //imprimeVoucherRetiro(g_patente, g_espacios, g_fecha_hora_in, g_fecha_hora_out, g_minutos, g_precio);
                        Util.alertDialog(RetiroPatente.this, "Retiro Patente", "Patente: " + g_patente + " retirada correctamente");
                        dialog.dismiss();
                    }else{
                        Util.alertDialog(RetiroPatente.this,"Retiro Patente", Resultado);
                        return;
                    }
                }else{
                    Util.alertDialog(RetiroPatente.this,"Retiro Patente", Resultado);
                    return;
                }

                reiniciaRetiro();
            }

        });

    }

    private String actualizaPatenteRetiro(String id_registro_patente, String fecha_hora_out, int minutos, int precio, int prepago ){
        try{
            AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patente " +
                                                "SET " +
                                                       "fecha_hora_out = '"+fecha_hora_out+"', " +
                                                       "rut_usuario_out = '"+AppHelper.getUsuario_rut()+"' , " +
                                                       "maquina_out = '"+AppHelper.getSerialNum()+"', " +
                                                       "minutos = "+minutos+", " +
                                                       "precio = "+precio+", " +
                                                       "prepago = "+prepago+", " +
                                                       "finalizado = '1' " +
                                                "WHERE id = '"+id_registro_patente+"'");
        }catch(SQLException e){  return e.getMessage(); }

        return "1";
    }

    private String actualizaPatenteSaldoPrepago(){

        try{
            AppHelper.getParkgoSQLite().execSQL("UPDATE tb_conductor SET saldo = '"+g_prepago_saldo_final+"' WHERE rut = '"+g_prepago_rut+"'");
        }catch(SQLException e){  return e.getMessage(); }

        return "1";
    }

    private void imprimeVoucherRetiro(String patente, int espacios, String fecha_hora_in,
                                      String fecha_hora_out, int minutos, int precio){


        PrintUnits.setSpeed(mPrintConnect.os, 0);
        PrintUnits.setConcentration(mPrintConnect.os, 2);
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);

        /** IMPRIME EL TEXTO **/
        String Texto    =   "--------------------------------"+"\n"+
                            "         TICKET SALIDA          "+"\n"+
                            "--------------------------------"+"\n"+
                            "Sistema de Transito Ordenado S.A"+"\n"+
                            "        RUT 96 852 690 1        "+"\n"+
                            "      Giro Estacionamiento      "+"\n"+
                            "         WWW.STOCHILE.CL        "+"\n"+
                            "--------------------------------"+"\n"+
                            "          San Antonio           "+"\n"+
                            "     Av Centenario 285 Of 2     "+"\n"+
                            " Consultas Reclamos 35-2212017  "+"\n"+
                            "      contacto@stochile.cl      "+"\n"+
                            "--------------------------------"+"\n"+
                            "Patente:  "+patente+"\n"+
                            "Espacios: "+espacios+"\n"+
                            "Ingreso:  "+fecha_hora_in+"\n"+
                            "Retiro:   "+fecha_hora_out+"\n"+
                            "Tiempo:   "+String.format("%,d", minutos).replace(",",".")+" min\n"+
                            "Precio:   $"+String.format("%,d", precio).replace(",",".");

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
    }

    private void reiniciaRetiro(){

        EDT_Patente.setText("");
        TV_RS_Patente.setText("");
        TV_RS_Espacios.setText("");
        TV_RS_Fecha_IN.setText("");
        TV_RS_Fecha_OUT.setText("");
        TV_RS_Minutos.setText("");
        TV_RS_Precio.setText("");
        g_id_registro_patente = "";
        g_patente             = "";
        g_espacios            = 0;
        g_fecha_hora_out      = "";
        g_fecha_hora_in       = "";
        g_minutos             = 0;
        g_precio              = 0;
        g_prepago_rut         = "";
        g_prepago_clave       = "";
        g_prepago_saldo       = 0;
        g_prepago_saldo_final = 0;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (event.getKeyCode()) {
            case 223:
                mScanConnect.scan();
                break;
            case 224:
                mScanConnect.scan();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScanConnect != null) {
            mScanConnect.stop();
        }
        if (mPrintConnect != null) {
            mPrintConnect.stop();
        }
    }


}
