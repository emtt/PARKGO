package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.obm.mylibrary.PrintConnect;
import com.obm.mylibrary.PrintUnits;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class IngresoPatente extends AppCompatActivity {


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingreso_patente);
        this.setTitle("Ingresar Vehículo");
        appGPS = new AppGPS();
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
                String fecha_hora_in   = AppHelper.fechaHoraFormat.format(fechahora_in);
                String id_registro_patente  = AppHelper.fechaHoraFormatID.format(fechahora_in)+"_"+AppHelper.getSerialNum()+"_"+patente;

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

        BTN_Comentario = (FloatingActionButton) findViewById(R.id.BTN_Comentario);
        BTN_Comentario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comentarioDialog(IngresoPatente.this);
            }

        });

    }

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

    private String consultaPatenteIngreso(String patente){

        try{
            String resultado;
            String[] args = new String[] {patente, String.valueOf(AppHelper.getUbicacion_id()),"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, patente, fecha_hora_in " +
                                                            "FROM tb_registro_patente " +
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

            AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_registro_patente "+
                                                "(id, id_cliente_ubicacion, patente," +
                                                "espacios, fecha_hora_in, rut_usuario_in, " +
                                                "maquina_in, imagen_in, enviado_in, " +
                                                "fecha_hora_out, rut_usuario_out, maquina_out, " +
                                                "enviado_out, minutos, precio, " +
                                                "prepago, efectivo, latitud, " +
                                                "longitud, comentario ,finalizado)"+
                                                "VALUES " +
                                                "('"+id_registro_patente+"','"+AppHelper.getUbicacion_id()+"','"+patente+"'," +
                                                "'"+espacios+"','"+fecha_hora_in+"' ,'"+AppHelper.getUsuario_rut()+"'," +
                                                "'"+AppHelper.getSerialNum()+"' ,'"+imagen_nombre+"', '0', " +
                                                "'', '', ''," +
                                                "'0','0','0'," +
                                                "'0','0','"+latitud+"'," +
                                                "'"+longitud+"','"+comentario+"','0');");

            //imprimeVoucherIngreso(patente, espacios, fecha_hora_in);
            reiniciaIngreso();
            Util.alertDialog(IngresoPatente.this,"Ingreso Patente","Patente: "+patente+" registrada correctamente");

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
        if (mPrintConnect != null) {
            mPrintConnect.stop();
        }
        appGPS.desconectaGPS();
    }

    /*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPrintConnect != null) {
            mPrintConnect.stop();
        }
    }
    */
}
