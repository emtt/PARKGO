package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;
import cz.msebera.android.httpclient.Header;

public class IngresoPatente extends AppCompatActivity {


    public static PrintConnect mPrintConnect;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private EditText EDT_Patente;
    private Spinner  SPIN_Espacios;
    private FloatingActionButton BTN_IngresoPatente;

    private FloatingActionButton BTN_Camara;
    private ImageView IMG_IngresoPatente;
    private String ArchivoImagenPath;
    private String ArchivoImagenNombre;

    DateFormat fechaHoraFormat   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    DateFormat fechaHoraFormatID = new SimpleDateFormat("yyyyMMddHHmmss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingreso_patente);
        //mPrintConnect = new PrintConnect(this);
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
                String espacios  = SPIN_Espacios.getSelectedItem().toString();

                Date fechahora_in = new Date();
                String fecha_hora_in   = fechaHoraFormat.format(fechahora_in);
                String id_registro_patente  = fechaHoraFormatID.format(fechahora_in)+"_"+AppHelper.getSerialNum()+"_"+patente;

                confirmDialog(IngresoPatente.this,"Confirme para ingresar la patente "+patente, id_registro_patente ,patente, espacios, fecha_hora_in);

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

    }

    public void confirmDialog(Context context, String mensaje, final String id_registro_patente, final String patente, final String espacios, final String fecha_hora_in) {
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
                            Resultado = insertaPatenteIngreso(id_registro_patente, patente, espacios, fecha_hora_in);
                            if(Resultado.equals("1")){
                                //imprimeVoucherIngreso(patente, espacios, fecha_hora_in);
                                //verifica conexión a internet para sincronizar.
                                reiniciaIngreso();
                                Util.alertDialog(IngresoPatente.this,"Ingreso Patente","Patente: "+patente+" registrada correctamente");
                            }else{ //error SQL inserta patente
                                reiniciaIngreso();
                                Util.alertDialog(IngresoPatente.this,"ERROR Ingreso Patente",Resultado);
                            }
                        }else{ //error SQL consulta patente
                            reiniciaIngreso();
                            Util.alertDialog(IngresoPatente.this,"ERROR Ingreso Patente",Resultado);
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

    private String insertaPatenteIngreso(String id_registro_patente, String patente, String espacios, String fecha_hora_in){

        try{
            AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_registro_patente "+
                                                "(id, id_cliente_ubicacion, patente, espacios, fecha_hora_in, rut_usuario_in, maquina_in, imagen_in, enviado_in, fecha_hora_out, rut_usuario_out, maquina_out, enviado_out, minutos, finalizado)"+
                                                "VALUES " +
                                                "('"+id_registro_patente+"','"+AppHelper.getUbicacion_id()+"','"+patente+"','"+espacios+"','"+fecha_hora_in+"' ,'"+AppHelper.getUsuario_rut()+"','"+AppHelper.getSerialNum()+"' ,'"+ArchivoImagenNombre+"', '0', '', '', '','0','0','0');");
            // datetime('now','localtime')
        }catch(SQLException e){  return e.getMessage(); }

        return "1";
    }

    private void imprimeVoucherIngreso(String patente, String espacios, String fecha_hora_in){


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
                          "Ingreso: "+fecha_hora_in+"\n"+
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

    public void sinncronizaIngresoPatente(final String id_registro_patente, final String patente, final String espacios, final String fecha_hora_in) {

        AsyncHttpClient client = new AsyncHttpClient () ;
        JSONObject jsonParams  = null;
        StringEntity entity    = null;
        try {
            jsonParams = new JSONObject();
            jsonParams.put("id",id_registro_patente);
            jsonParams.put("id_cliente_ubicacion",AppHelper.getUbicacion_id());
            jsonParams.put("patente",patente);
            jsonParams.put("espacios",espacios);
            jsonParams.put("fecha_hora_in",fecha_hora_in);
            jsonParams.put("rut_usuario_in",AppHelper.getUsuario_rut());
            jsonParams.put("maquina_in",AppHelper.getSerialNum());
            jsonParams.put("imagen_in",ArchivoImagenNombre);
            jsonParams.put("enviado_in","1");
            jsonParams.put("fecha_hora_out",fecha_hora_in);
            jsonParams.put("rut_usuario_out","0");
            jsonParams.put("maquina_out","0");
            jsonParams.put("enviado_out","0");
            jsonParams.put("minutos","0");
            jsonParams.put("finalizado","0");
            entity = new StringEntity(jsonParams.toString());


            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            client.post ( this.getApplicationContext () , AppHelper.getUrl_restful() + "registro_patentes/add" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    try {
                        JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                        if(jsonArray != null){
                                try{
                                    //Marca el registro como enviado.
                                    AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patente SET enviado_in = '1' WHERE id = '"+id_registro_patente+"'");
                                }catch(SQLException e){  Util.alertDialog(IngresoPatente.this,"EXCEP SYNC Ingreso Patente",e.getMessage()); }
                        }else{
                            jsonArray = jsonRootObject.optJSONArray("error");
                            if(jsonArray != null){
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                Util.alertDialog(IngresoPatente.this,"ERROR SYNC Ingreso Patente",jsonObject.optString("text"));
                            }
                        }
                    } catch (JSONException e) {
                        Util.alertDialog(IngresoPatente.this,"EXCEP SYNC Ingreso Patente",e.getMessage());
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Util.alertDialog(IngresoPatente.this,"ERROR SYNC Ingreso Patente",String.valueOf(responseBody));
                }

            });

        } catch (UnsupportedEncodingException e0) {
            Util.alertDialog(IngresoPatente.this,"EXCEP SYNC Ingreso Patente", e0.getMessage());
        } catch (JSONException e1) {
            Util.alertDialog(IngresoPatente.this,"EXCEP SYNC Ingreso Patente", e1.getMessage());
        }

    }

    private void reiniciaIngreso(){
        EDT_Patente.setText("");
        IMG_IngresoPatente.setScaleType(ImageView.ScaleType.CENTER);
        IMG_IngresoPatente.setImageResource(R.drawable.ic_photo);
        ArchivoImagenNombre = "";
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
                Util.alertDialog(IngresoPatente.this,"EXCEP IMG Ingreso Patente","Error al crear archivo imagen "+ex.getMessage());
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
        // Save a file: path for use with ACTION_VIEW intents
        ArchivoImagenPath   = image.getAbsolutePath();
        ArchivoImagenNombre = image.getName();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //recupera muestra la imagen de la camara.
            Bitmap imagenBitmap = BitmapFactory.decodeFile(ArchivoImagenPath);
            IMG_IngresoPatente.setScaleType(ImageView.ScaleType.FIT_XY);
            IMG_IngresoPatente.setImageBitmap(imagenBitmap);
        }else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            File file = new File(ArchivoImagenPath);
            file.delete();
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
