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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

    Print_Thread printThread = null;

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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_ingreso_patente);
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
                iniciarIngresoPatente(patente);


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
        int espacios  = Integer.parseInt(SPIN_Espacios.getSelectedItem().toString());

        Date fechahora_in = new Date();
        String fecha_hora_in = AppHelper.fechaHoraFormat.format(fechahora_in);
        String id_registro_patente  = AppHelper.fechaHoraFormatID.format(fechahora_in)+"_"+AppHelper.getSerialNum()+"_"+patente;

         finalizarIngresoPatente(id_registro_patente ,patente, espacios, fecha_hora_in);
    }

    public void finalizarIngresoPatente(final String id_registro_patente, final String patente, final int espacios, final String fecha_hora_in){

        String Resultado = consultaPatenteIngreso(patente);

        if (Resultado.equals("1")){ //patente existe
            reiniciaIngreso();
        }else if (Resultado.equals("0")){ //patente no existe (inserta)
            //Intenta obtener la ubicación GPS
            String latitud = "";
            String longitud= "";
            GpsTracker gt = new GpsTracker(getApplicationContext());
            Location location = gt.getLocation();
            if( location != null){
                latitud  = Double.toString(location.getLatitude());
                longitud = Double.toString(location.getLongitude());
            }
            insertaPatenteIngreso(id_registro_patente, patente, espacios, fecha_hora_in, g_imagen_nombre, latitud, longitud, g_comentario);

        }else{ //error SQL consulta patente
            reiniciaIngreso();
            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente",Resultado);
        }

    }

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

    private void insertaPatenteIngreso(String id_registro_patente, String patente, int espacios,
                                       String fecha_hora_in, String imagen_nombre, String latitud, String longitud, String comentario){

        try{
                int print_result = imprimeVoucherIngreso(patente, espacios, fecha_hora_in);
                if (print_result == 0 || print_result == -2) {
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
                    reiniciaIngreso();
                    //Util.alertDialog(IngresoPatente.this,"Ingreso Patente","Patente: "+patente+" registrada correctamente");
                }
        }catch(SQLException e){
            reiniciaIngreso();
            Util.alertDialog(IngresoPatente.this,"SQLException Ingreso Patente", e.getMessage());
            return;
        }

    }

    private int imprimeVoucherIngreso(String patente, int espacios, String fecha_hora_in){

        try {
            /** IMPRIME LA ETIQUETA **/
            if (printThread != null && !printThread.isThreadFinished()) {
                Log.d(AppHelper.LOG_PRINT, "Thread is still running...");
                return -1;
            }else {
                printThread = new Print_Thread(0, patente, espacios, fecha_hora_in);
                printThread.start();
                    //Espera que el thread finalice la ejecución.
                    printThread.join();
                return printThread.getRESULT_CODE();
            }

         } catch (InterruptedException e) {
            Util.alertDialog(IngresoPatente.this,"InterruptedException Ingreso Patente", e.getMessage());
            return -1;
        }
    }

    private void reiniciaIngreso(){
        EDT_Patente.setText("");
        SPIN_Espacios.setSelection(0);
        IMG_IngresoPatente.setScaleType(ImageView.ScaleType.CENTER);
        IMG_IngresoPatente.setImageResource(R.drawable.ic_photo);
        g_imagen_nombre = "";
        g_comentario = "";
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
