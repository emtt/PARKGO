package cl.suministra.parkgo;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Recaudacion extends AppCompatActivity implements View.OnClickListener {

    Print_Thread printThread = null;

    private Button BTN_Recaudar;
    private EditText EDT_FechaRecaudacion;
    private TextView TV_Operador;
    private TextView TV_Ubicacion;
    private TextView TV_Transacciones;
    private TextView TV_Minutos;
    private TextView TV_Prepago;
    private TextView TV_Efectivo;
    private TextView TV_Recaudado;
    private TextView TV_Retirado;
    private TextView TV_Total;

    private TextView TXT_Usuario_MSJ;
    private EditText EDT_Usuario;
    private TextView TXT_Clave_MSJ;
    private EditText EDT_Clave;
    private TextView TXT_Monto_MSJ;
    private EditText EDT_Monto;

    private int g_total_recaudado = 0;
    private int g_total_operador  = 0;

    private DatePickerDialog fechaDatePickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_recaudacion);
        inicio();
        setDateTimeField();
        getRecaudacion();
    }

    private void inicio(){

        EDT_FechaRecaudacion = (EditText) findViewById(R.id.EDT_FechaRecaudacion);
        EDT_FechaRecaudacion.setEnabled(false);
        TV_Operador          = (TextView) findViewById(R.id.TV_Operador);
        TV_Ubicacion         = (TextView) findViewById(R.id.TV_Ubicacion);
        TV_Transacciones     = (TextView) findViewById(R.id.TV_Transacciones);
        TV_Minutos           = (TextView) findViewById(R.id.TV_Minutos);
        TV_Prepago           = (TextView) findViewById(R.id.TV_Prepago);
        TV_Efectivo          = (TextView) findViewById(R.id.TV_Efectivo);
        TV_Recaudado         = (TextView) findViewById(R.id.TV_Recaudado);
        TV_Retirado          = (TextView) findViewById(R.id.TV_Retirado);
        TV_Total             = (TextView) findViewById(R.id.TV_Total);

        EDT_FechaRecaudacion.setInputType(InputType.TYPE_NULL);
        EDT_FechaRecaudacion.requestFocus();
        EDT_FechaRecaudacion.setText(AppHelper.fechaFormat.format(new Date()));

        BTN_Recaudar = (Button)  findViewById(R.id.BTN_Recaudar);
        BTN_Recaudar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialogRetiroRecaudacion(Recaudacion.this, "Complete la información requerida finalizar el proceso.");
            }

        });
    }

    private void setDateTimeField() {
        EDT_FechaRecaudacion.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        fechaDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                EDT_FechaRecaudacion.setText(AppHelper.fechaFormat.format(newDate.getTime()));
                getRecaudacion();
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

    }

    private void getRecaudacion(){

        TV_Operador.setText(AppHelper.getUsuario_nombre());
        TV_Ubicacion.setText(AppHelper.getUbicacion_nombre());

        String fecha_recaudacion = EDT_FechaRecaudacion.getText().toString();

        Cursor c;
        try {
            fecha_recaudacion = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd-MM-yyyy").parse(fecha_recaudacion));

            String[] args = new String[]{String.valueOf(AppHelper.getUbicacion_id()), String.valueOf(AppHelper.getUsuario_rut()), fecha_recaudacion, "0", fecha_recaudacion, "2" };
            String qry = "SELECT  trp.id_cliente_ubicacion, "+
                                  "tcu.descripcion, "+
                                  "tcu.minutos_gratis, "+
                                  "tcu.valor_minuto, "+
                                  "trp.rut_usuario_out, "+
                                  "tu.nombre, "+
                                  "COUNT(*) AS transacciones_total, "+
                            "SUM(trp.minutos) AS recaudacion_minutos, "+
                            "SUM(trp.prepago) AS recaudacion_prepago, "+
                            "SUM(trp.efectivo) AS recaudacion_efectivo, "+
                            "SUM(trp.precio) AS recaudacion_total "+
                            "FROM tb_registro_patentes trp "+
                            "INNER JOIN tb_cliente_ubicaciones tcu ON tcu.id = trp.id_cliente_ubicacion "+
                            "INNER JOIN tb_usuario tu ON tu.rut = trp.rut_usuario_out "+
                            "WHERE trp.id_cliente_ubicacion=? AND tu.rut =? " +
                            "AND ( ( DATE(trp.fecha_hora_out) =? AND trp.id_estado_deuda =? )  " +
                            "OR ( DATE(trp.fecha_hora_estado_deuda) =? AND trp.id_estado_deuda =? ) ) "+
                            "GROUP BY trp.id_cliente_ubicacion, rut_usuario_out ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {

                String rs_transacciones = c.getString(6);
                int rs_minutos = c.getInt(7);
                int rs_prepago = c.getInt(8);
                int rs_efectivo= c.getInt(9);
                int rs_recaudado  = c.getInt(10);
                g_total_recaudado = c.getInt(10);

                TV_Transacciones.setText(rs_transacciones);
                TV_Minutos.setText(String.format("%,d", rs_minutos).replace(",","."));
                TV_Prepago.setText("$"+String.format("%,d", rs_prepago).replace(",","."));
                TV_Efectivo.setText("$"+String.format("%,d",rs_efectivo).replace(",","."));
                TV_Recaudado.setText("$"+String.format("%,d", rs_recaudado).replace(",","."));

            } else {

                TV_Transacciones.setText("0");
                TV_Minutos.setText("0");
                TV_Prepago.setText("$0");
                TV_Efectivo.setText("$0");
                TV_Recaudado.setText("$0");

            }
            c.close();

            //Calcula el resto que se queda el operador.
            String[] args_1 = new String[]{String.valueOf(AppHelper.getUbicacion_id()), String.valueOf(AppHelper.getUsuario_rut()), fecha_recaudacion };
            String qry_1    = "SELECT SUM(monto) FROM tb_recaudacion_retiro "+
                             "WHERE "+
                             "id_cliente_ubicacion =? AND rut_usuario_operador =? AND fecha_recaudacion =? ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry_1, args_1);
            if (c.moveToFirst()) {
                int rs_monto_retirado = c.getInt(0);
                g_total_operador      = g_total_recaudado -  rs_monto_retirado;
                TV_Retirado.setText("$"+String.format("%,d", rs_monto_retirado).replace(",","."));
                TV_Total.setText("$"+String.format("%,d", g_total_operador).replace(",","."));
            }else{
                TV_Retirado.setText("$0");
                TV_Total.setText("$0");
            }
            c.close();


        } catch (SQLException e) {
            Util.alertDialog(Recaudacion.this, "SQLException Recaudación", e.getMessage());
        } catch (ParseException e) {
            Util.alertDialog(Recaudacion.this, "SQLException ParseException", e.getMessage());
        }

    }

    private void confirmDialogRetiroRecaudacion(Context context, String mensaje) {

        LinearLayout lila= new LinearLayout(this);
        lila.setOrientation(LinearLayout.VERTICAL);

        TXT_Usuario_MSJ = new TextView(this);
        EDT_Usuario     = new EditText(this);
        TXT_Clave_MSJ   = new TextView(this);
        EDT_Clave       = new EditText(this);
        TXT_Monto_MSJ   = new TextView(this);
        EDT_Monto       = new EditText(this);



        EDT_Usuario.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        EDT_Usuario.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
        EDT_Usuario.setHint("Usuario");
        EDT_Usuario.setHintTextColor(Color.GRAY);

        EDT_Usuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TXT_Usuario_MSJ.setTextColor(Color.GRAY);
                    TXT_Usuario_MSJ.setText("  "+EDT_Usuario.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Usuario.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TXT_Usuario_MSJ.setTextColor(Color.GRAY);
                TXT_Usuario_MSJ.setText("  "+EDT_Usuario.getHint());

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TXT_Usuario_MSJ.setText("");
                }
            }
        });

        EDT_Clave.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        EDT_Clave.setTransformationMethod(PasswordTransformationMethod.getInstance());
        EDT_Clave.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        EDT_Clave.setHint("Clave");
        EDT_Clave.setHintTextColor(Color.GRAY);

        EDT_Clave.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TXT_Clave_MSJ.setTextColor(Color.GRAY);
                    TXT_Clave_MSJ.setText("  "+EDT_Clave.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Clave.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TXT_Clave_MSJ.setTextColor(Color.GRAY);
                TXT_Clave_MSJ.setText("  "+EDT_Clave.getHint());

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TXT_Clave_MSJ.setText("");
                }
            }
        });

        EDT_Monto.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        EDT_Monto.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        EDT_Monto.setHint("Monto");
        EDT_Monto.setHintTextColor(Color.GRAY);

        EDT_Monto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TXT_Monto_MSJ.setTextColor(Color.GRAY);
                    TXT_Monto_MSJ.setText("  "+EDT_Monto.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Monto.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TXT_Monto_MSJ.setTextColor(Color.GRAY);
                TXT_Monto_MSJ.setText("  "+EDT_Monto.getHint());

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TXT_Monto_MSJ.setText("");
                }
            }
        });

        EDT_Monto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && !EDT_Monto.getText().toString().isEmpty() ){
                    int monto = Integer.parseInt(EDT_Monto.getText().toString());
                    EDT_Monto.setText(String.format("%,d", monto).replace(",","."));
                }
            }
        });

        lila.addView(TXT_Usuario_MSJ);
        lila.addView(EDT_Usuario);
        lila.addView(TXT_Clave_MSJ);
        lila.addView(EDT_Clave);
        lila.addView(TXT_Monto_MSJ);
        lila.addView(EDT_Monto);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Registrar Recaudación");
        builder.setMessage(mensaje);
        builder.setView(lila);
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

                if(EDT_Usuario.getText().toString() == null || EDT_Usuario.getText().toString().isEmpty()){
                    TXT_Usuario_MSJ.setTextColor(Color.RED);
                    TXT_Usuario_MSJ.setText(" Ingrese "+EDT_Usuario.getHint());
                    return;
                }else{
                    TXT_Usuario_MSJ.setText("");
                }

                if(EDT_Clave.getText().toString() == null || EDT_Clave.getText().toString().isEmpty()){
                    TXT_Clave_MSJ.setTextColor(Color.RED);
                    TXT_Clave_MSJ.setText(" Ingrese "+EDT_Clave.getHint());
                    return;
                }else{
                    TXT_Clave_MSJ.setText("");
                }

                if(EDT_Monto.getText().toString() == null || EDT_Monto.getText().toString().isEmpty()){
                    TXT_Monto_MSJ.setTextColor(Color.RED);
                    TXT_Monto_MSJ.setText("  Ingrese "+EDT_Monto.getHint());
                    return;
                }else{
                    TXT_Monto_MSJ.setText("");
                }

                int monto = Integer.parseInt(EDT_Monto.getText().toString().replace(".", ""));

                if(validaMontoRetiroRecaudacion(monto)) {
                    //Verifica usuario valido para recaudar.
                    try {

                        String[] args = new String[]{EDT_Usuario.getText().toString(), EDT_Clave.getText().toString()};
                        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT tu.rut, tu.nombre , tr.nombre AS rol_nombre, tr.es_recaudador " +
                                "FROM tb_usuario tu\n" +
                                "INNER JOIN tb_rol tr ON tr.id = tu.id_rol\n" +
                                "WHERE tu.codigo =? AND tu.clave =? ", args);
                        if (c.moveToFirst()) {

                            String rs_rut_usuario_retiro = c.getString(0);
                            String rs_nombre_usuario_retiro = c.getString(1);
                            String rs_rol_nombre = c.getString(2);
                            int rs_es_recaudador = c.getInt(3);

                            if (rs_es_recaudador == 1) {

                                try {

                                    String fecha_recaudacion = EDT_FechaRecaudacion.getText().toString();
                                    fecha_recaudacion = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd-MM-yyyy").parse(fecha_recaudacion));

                                    int print_result = imprimeVoucherRetiroRecaudacion(fecha_recaudacion, rs_rut_usuario_retiro, rs_nombre_usuario_retiro, monto);
                                    if (print_result == 0 || print_result == -2) {
                                        Date fechahora = new Date();
                                        String id_recaudacion_retiro = AppHelper.fechaHoraFormatID.format(fechahora) + "_" + AppHelper.getSerialNum() + "_" + EDT_Usuario.getText().toString();

                                        AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_recaudacion_retiro " +
                                                "(id, id_cliente_ubicacion, rut_usuario_operador," +
                                                "maquina, rut_usuario_retiro, fecha_recaudacion, " +
                                                "monto, enviado)" +
                                                "VALUES " +
                                                "('" + id_recaudacion_retiro + "', " + AppHelper.getUbicacion_id() + " , '" + AppHelper.getUsuario_rut() + "', '" + AppHelper.getSerialNum() + "', '" + rs_rut_usuario_retiro + "' , '" + fecha_recaudacion + "', " + monto + ", 0);");

                                    }
                                    dialog.dismiss();
                                    getRecaudacion();

                                } catch (SQLException e) {
                                    Util.alertDialog(Recaudacion.this, "SQLException Recaudación", e.getMessage());
                                }
                            } else {
                                Util.alertDialog(Recaudacion.this, "Recaudación", "Su rol asignado " + rs_rol_nombre + " no permite realizar recaudaciones, verifique");
                            }

                        } else {
                            Util.alertDialog(Recaudacion.this, "Recaudación", "Usuario y clave ingresado no existe en el sistema");
                        }
                        c.close();

                    } catch (SQLException e0) {
                        Util.alertDialog(Recaudacion.this, "SQLException Recaudación", e0.getMessage());
                    } catch (ParseException e1) {
                        Util.alertDialog(Recaudacion.this, "SQLException Recaudación", e1.getMessage());
                    }
                }

            }

        });



    }

    private boolean validaMontoRetiroRecaudacion(int monto_retiro_recaudacion){

        try{
            int rs_recaudacion_retiro_actual = 0;
            int recaudacion_retiro_total     = 0;
            int monto_maximo_retiro          = 0;

            String fecha_recaudacion = EDT_FechaRecaudacion.getText().toString();
            fecha_recaudacion = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd-MM-yyyy").parse(fecha_recaudacion));

            String[] args = new String[] {String.valueOf(AppHelper.getUbicacion_id()), String.valueOf(AppHelper.getUsuario_rut()), fecha_recaudacion };
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT SUM(monto) AS recaudacion_retiro_total \n" +
                                                                "FROM tb_recaudacion_retiro\n" +
                                                            "WHERE id_cliente_ubicacion =? AND rut_usuario_operador =? AND fecha_recaudacion =? ", args);
            if (c.moveToFirst()){
                rs_recaudacion_retiro_actual = c.getInt(0);
            }
            c.close();

            monto_maximo_retiro      = g_total_recaudado - rs_recaudacion_retiro_actual;
            recaudacion_retiro_total = rs_recaudacion_retiro_actual + monto_retiro_recaudacion;
            if(g_total_recaudado < recaudacion_retiro_total){
                EDT_Monto.setText("");
                Util.alertDialog(Recaudacion.this, "Recaudación", "El monto a retirar no puede ser superior a $"+String.format("%,d", monto_maximo_retiro).replace(",","."));
                return false;
            }

        }catch(SQLException e0){
            Util.alertDialog(Recaudacion.this,"SQLException Recaudación", e0.getMessage());
            return false;
        }
        catch (ParseException e1) {
            Util.alertDialog(Recaudacion.this,"SQLException ParseException", e1.getMessage());
            return false;
        }
        return true;

    }

    private int imprimeVoucherRetiroRecaudacion(String fecha_recaudacion, String rut_usuario_retiro, String nombre_usuario_retiro, int monto){

        try {
            /** IMPRIME LA ETIQUETA **/
            if (printThread != null && !printThread.isThreadFinished()) {
                Log.d(AppHelper.LOG_PRINT, "Thread is still running...");
                return -1;
            }else {
                printThread = new Print_Thread(3, fecha_recaudacion, rut_usuario_retiro, nombre_usuario_retiro, monto, g_total_recaudado);
                printThread.start();
                    printThread.join();
                return printThread.getRESULT_CODE();
            }
        } catch (InterruptedException e) {
            Util.alertDialog(Recaudacion.this,"InterruptedException Recaudación", e.getMessage());
            return -1;
        }


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
    public void onClick(View v) {
        if(v == EDT_FechaRecaudacion) {
            fechaDatePickerDialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
