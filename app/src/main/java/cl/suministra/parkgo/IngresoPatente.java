package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.obm.mylibrary.PrintConnect;
import com.obm.mylibrary.PrintUnits;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IngresoPatente extends AppCompatActivity {


    public static PrintConnect mPrintConnect;
    private EditText EDT_Patente;
    private Spinner  SPIN_Espacios;
    private Button   BTN_IngresoPatente;

    //Variables utilizadas para finalizar la salida de la patente.
    private String g_fecha_hora_in;
    DateFormat fechaHoraFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingreso_patente);
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

        BTN_IngresoPatente = (Button) findViewById(R.id.BTN_IngresoPatente);
        BTN_IngresoPatente.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){

                 String patente = EDT_Patente.getText().toString();
                if (patente == null || patente.isEmpty()) {
                    TextView view = (TextView) findViewById(R.id.MSJ_Patente);
                    view.setText(EDT_Patente.getHint() + " no puede ser vacío");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Patente.setBackground(getDrawable(R.drawable.text_border_error));
                    }
                    return;
                }

                String espacios  = SPIN_Espacios.getSelectedItem().toString();
                confirmDialog(IngresoPatente.this,"Confirme para ingresar la patente "+patente, patente, espacios);

            }

        });

    }

    public void confirmDialog(Context context, String mensaje, final String patente, final String espacios) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder
                .setMessage(mensaje)
                .setPositiveButton("Si",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String Resultado = consultaPatenteIngreso(patente);
                        if (Resultado.equals("1")){ //patente existe
                            reiniciaIngreso();
                        }else if (Resultado.equals("0")){ //patente no existe (inserta)
                            Resultado = insertaPatenteIngreso(patente, espacios);
                            if(Resultado.equals("1")){
                                imprimeVoucherIngreso(patente, espacios);
                                reiniciaIngreso();
                                Toast.makeText(getApplicationContext(),"Patente: "+patente+" registrada correctamente",Toast.LENGTH_LONG).show();
                            }else{ //error SQL inserta patente
                                reiniciaIngreso();
                                Toast.makeText(getApplicationContext(),Resultado,Toast.LENGTH_LONG).show();
                            }
                        }else{ //error SQL consulta patente
                            reiniciaIngreso();
                            Toast.makeText(getApplicationContext(),Resultado,Toast.LENGTH_LONG).show();
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

    private String consultaPatenteIngreso(String patente){

        try{
            String resultado;
            String[] args = new String[] {patente,"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, patente, fecha_hora_in FROM tb_registro_patente WHERE patente =? AND finalizado =?", args);
            if (c.moveToFirst()){
                String  rs_patente = c.getString(1);
                String  rs_fecha_hora_in = c.getString(2);
                Toast.makeText(getApplicationContext(),"Patente: "+rs_patente+" ya se encuentra ingresada"+"\n"+
                                                       "Fecha hora ingreso: "+rs_fecha_hora_in
                                                        ,Toast.LENGTH_LONG).show();
                resultado = "1";
            }else{
                resultado = "0";
            }
            c.close();
            return resultado;

        }catch(SQLException e){ return e.getMessage(); }

    }

    private String insertaPatenteIngreso(String patente, String espacios){

        Date fechahora_in = new Date();
        g_fecha_hora_in   = fechaHoraFormat.format(fechahora_in);
        try{
            AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_registro_patente "+
                                                "(patente, espacios, fecha_hora_in, fecha_hora_out, minutos, finalizado, enviado)"+
                                                "VALUES " +
                                                "('"+patente+"','"+espacios+"','"+g_fecha_hora_in+"','','0','0','0');");
            // datetime('now','localtime')
        }catch(SQLException e){  return e.getMessage(); }

        return "1";
    }

    private void imprimeVoucherIngreso(String patente, String espacios){


        PrintUnits.setSpeed(mPrintConnect.os, 0);
        PrintUnits.setConcentration(mPrintConnect.os, 2);
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);

        /** IMPRIME EL TEXTO **/
        String Texto    = "--------------------------------"+"\n"+
                          "          TICKET INGRESO        "+"\n"+
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
                          "Patente: "+patente+"\n"+
                          "Ingreso: "+g_fecha_hora_in+"\n"+
                          "Espacios: "+espacios+"\n";
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
    }

    private void reiniciaIngreso(){
        EDT_Patente.setText("");
        g_fecha_hora_in = "";

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPrintConnect.stop();
    }

}
