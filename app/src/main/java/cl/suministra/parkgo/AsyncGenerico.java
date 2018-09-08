package cl.suministra.parkgo;

import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Created by LENOVO on 06-09-2017.
 */

public class AsyncGenerico extends AsyncTask<Void, Integer,  Boolean> {

    private AsyncHttpClient cliente = null;

    public void cancelTask(AsyncGenerico asyncGenerico) {
        if (asyncGenerico == null) return;
        asyncGenerico.cancel(false);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(AppHelper.LOG_TAG, "AsyncGenerico onPreExecute");
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            int i = 1;
             do{
                publishProgress(i);
                i++;
                TimeUnit.SECONDS.sleep(AppHelper.getTimesleep());
                isCancelled();
            }while(!isCancelled());
        } catch (InterruptedException e) {
            Log.d(AppHelper.LOG_TAG, "AsyncGenerico InterruptedException "+e.getMessage());
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

        super.onProgressUpdate(values);
        int progreso = values[0].intValue();

        Log.d(AppHelper.LOG_TAG, "#################### onProgressUpdate Iniciado ###########################: "+progreso);
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getPatentesIngresoExterno #######################: "+progreso);
        getPatentesIngresoExterno();
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getPatentesRetiroExterno ########################: "+progreso);
        getPatentesRetiroExterno();
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getPatentesIngresoPendienteSYNC #################: "+progreso);
        getPatentesIngresoPendienteSYNC();
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getPatentesIngresoPendienteImagenSYNC ###########: "+progreso);
        getPatentesIngresoPendienteImagenSYNC();
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getPatentesRetiroPendienteSync ##################: "+progreso);
        getPatentesRetiroPendienteSync();
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico autoRetiroPatentes ##############################: "+progreso);
        autoRetiroPatentes();

        //Genéricos
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getUbicacionUsuarioPendienteSync ################: "+progreso);
        getUbicacionUsuarioPendienteSync(); //OK
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getAlertasPendienteSync #########################: "+progreso);
        getAlertasPendienteSync(); //OK
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getRetiroRecaudacionSync ########################: "+progreso);
        getRetiroRecaudacionSync();
        Log.d(AppHelper.LOG_TAG, "########## AsyncGenerico getRetiroRecaudacionExterno #####################: "+progreso);
        getRetiroRecaudacionExterno();
        Log.d(AppHelper.LOG_TAG, "################### onProgressUpdate Terminado ###########################: "+progreso);

    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result)
            Log.d(AppHelper.LOG_TAG, "AsyncGenerico onPostExecute Finalizada");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(AppHelper.LOG_TAG, "AsyncGenerico onCancelled Cancelada");;
    }

    private void getPatentesIngresoExterno(){

        String maquina = AppHelper.getSerialNum();
        int id_cliente_ubicacion = AppHelper.getUbicacion_id();
        if (id_cliente_ubicacion > 0 && !maquina.equals("")) {
            ClienteAsync(AppHelper.getUrl_restful() + "registro_patentes_maquinas_in/" + maquina + "/" + id_cliente_ubicacion, new ClienteCallback() {

                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {
                    if(esError == 0 && !responseBody.equals("")) {
                        insertaIngresoPatentesExterno(responseBody);
                    }else{
                        Log.d(AppHelper.LOG_TAG,"getPatentesIngresoExterno ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                    }
                }

            });
        }

    }

    private void insertaIngresoPatentesExterno(String jsonString){

        String qry;
        try {

            JSONObject jsonRootObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonRootObject.optJSONArray("registro_patentes_maquinas_in");
            if(jsonArray != null){

                JSONArray registrosArray = new JSONArray();
                JSONObject jsonParams    = null;
                StringEntity entity      = null;

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id_registro_patente = jsonObject.optString("id");
                    int id_cliente_ubicacion   = jsonObject.optInt("id_cliente_ubicacion");
                    String patente        = jsonObject.optString("patente");
                    int espacios          = jsonObject.optInt("espacios");
                    String fecha_hora_in  = jsonObject.optString("fecha_hora_in");
                    String rut_usuario_in = jsonObject.optString("rut_usuario_in");
                    String maquina_in     = jsonObject.optString("maquina_in");
                    String imagen_in      = jsonObject.optString("imagen_in");
                    String latitud        = jsonObject.optString("latitud");
                    String longitud       = jsonObject.optString("longitud");
                    String comentario     = jsonObject.optString("comentario");

                    qry =   "INSERT OR IGNORE INTO tb_registro_patentes "+
                            "(id, id_cliente_ubicacion, patente, " +
                            "espacios, fecha_hora_in, rut_usuario_in, " +
                            "maquina_in, imagen_in, enviado_in, " +
                            "fecha_hora_out, rut_usuario_out, maquina_out, " +
                            "enviado_out, minutos, precio, " +
                            "prepago, efectivo, latitud, " +
                            "longitud, comentario, finalizado, id_estado_deuda, fecha_hora_estado_deuda)"+
                            "VALUES " +
                            "('"+id_registro_patente+"','"+id_cliente_ubicacion+"','"+patente+"'," +
                            "'"+espacios+"','"+fecha_hora_in+"' ,'"+rut_usuario_in+"'," +
                            "'"+maquina_in+"' ,'"+imagen_in+"', '1'," +
                            "'', '', ''," +
                            "'0','0','0'," +
                            "'0','0','"+latitud+"'," +
                            "'"+longitud+"','"+comentario+"','0', '0', '');";

                    AppHelper.getParkgoSQLite().execSQL(qry);

                    jsonParams = new JSONObject();
                    jsonParams.put("id_registro_patente",id_registro_patente);
                    jsonParams.put("maquina", AppHelper.getSerialNum());
                    registrosArray.put(i, jsonParams);

                }

                if(registrosArray.length() > 0) {
                    entity = new StringEntity(registrosArray.toString());
                    sincronizaIngresoPatentesExterno(entity);
                }

            }else{

                jsonArray = jsonRootObject.optJSONArray("error");
                if(jsonArray != null){
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    Log.d(AppHelper.LOG_TAG, "insertaIngresoPatentesExterno ERROR RESPONSE "+jsonObject.optString("text"));
                }

            }

        } catch (SQLException e) {
            Log.d(AppHelper.LOG_TAG, "insertaIngresoPatentesExterno AppHelper "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "insertaIngresoPatentesExterno JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "insertaIngresoPatentesExterno UnsupportedEncodingException "+e.getMessage());
        }

    }

    public void sincronizaIngresoPatentesExterno(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "registro_patentes_maquinas_upt_in/upt_multiple_in" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno onSuccess "+new String(responseBody));
                try {
                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){

                        //Muestra el log de los registros externos recibidos correctamente.
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Log.d(AppHelper.LOG_TAG,"sincronizaIngresoPatentesExterno REGISTRO ID "+jsonObject.optString("id")+" RECIBIDO CORRECTAMENTE E INFORMADO EN SERVIDOR");
                        }

                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatentesExterno onFailure cancelRequests");
            }

        });

    }

    private void getPatentesRetiroExterno(){

        String maquina = AppHelper.getSerialNum();
        int id_cliente_ubicacion = AppHelper.getUbicacion_id();
        if (id_cliente_ubicacion > 0 && !maquina.equals("")) {
            ClienteAsync(AppHelper.getUrl_restful() + "registro_patentes_maquinas_out/" + maquina + "/" + id_cliente_ubicacion, new ClienteCallback() {

                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {
                    if(esError == 0 && !responseBody.equals("")) {
                        actualizaRetiroPatentesExterno(responseBody);
                    }else{
                        Log.d(AppHelper.LOG_TAG,"getPatentesRetiroExterno ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                    }
                }

            });
        }

    }

    private void actualizaRetiroPatentesExterno(String jsonString){

        String qry;
        try {

            JSONObject jsonRootObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonRootObject.optJSONArray("registro_patentes_maquinas_out");
            if(jsonArray != null){

                JSONArray registrosArray = new JSONArray();
                JSONObject jsonParams    = null;
                StringEntity entity      = null;

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id_registro_patente = jsonObject.optString("id");
                    int id_cliente_ubicacion   = jsonObject.optInt("id_cliente_ubicacion");
                    String fecha_hora_out      = jsonObject.optString("fecha_hora_out");
                    String rut_usuario_out     = jsonObject.optString("rut_usuario_out");
                    String maquina_out         = jsonObject.optString("maquina_out");
                    int minutos                = jsonObject.optInt("minutos");
                    int precio                 = jsonObject.optInt("precio");
                    int prepago                = jsonObject.optInt("prepago");
                    int efectivo               = jsonObject.optInt("efectivo");
                    int finalizado             = jsonObject.optInt("finalizado");
                    int id_estado_deuda        = jsonObject.optInt("id_estado_deuda");
                    String fecha_hora_estado_deuda = jsonObject.optString("fecha_hora_estado_deuda");

                    qry =   "UPDATE tb_registro_patentes " +
                            "SET " +
                            "fecha_hora_out = '"+fecha_hora_out+"', " +
                            "rut_usuario_out = '"+rut_usuario_out+"' , " +
                            "maquina_out = '"+maquina_out+"', " +
                            "minutos = "+minutos+", " +
                            "precio = "+precio+", " +
                            "prepago = "+prepago+", " +
                            "efectivo = "+efectivo+", " +
                            "finalizado = "+finalizado+", " +
                            "id_estado_deuda = "+id_estado_deuda+", " +
                            "fecha_hora_estado_deuda = '"+fecha_hora_estado_deuda+"' " +
                            "WHERE id = '"+id_registro_patente+"'";

                    AppHelper.getParkgoSQLite().execSQL(qry);

                    jsonParams = new JSONObject();
                    jsonParams.put("id_registro_patente",id_registro_patente);
                    jsonParams.put("maquina", AppHelper.getSerialNum());
                    registrosArray.put(i, jsonParams);

                }

                if(registrosArray.length() > 0) {
                    entity = new StringEntity(registrosArray.toString());
                    sincronizaRetiroPatentesExterno(entity);
                }

            }else{

                jsonArray = jsonRootObject.optJSONArray("error");
                if(jsonArray != null){
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    Log.d(AppHelper.LOG_TAG, "actualizaRetiroPatentesExterno ERROR RESPONSE "+jsonObject.optString("text"));
                }

            }

        } catch (SQLException e) {
            Log.d(AppHelper.LOG_TAG, "actualizaRetiroPatentesExterno AppHelper "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "actualizaRetiroPatentesExterno JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "actualizaRetiroPatentesExterno UnsupportedEncodingException "+e.getMessage());
        }

    }

    public void sincronizaRetiroPatentesExterno(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "registro_patentes_maquinas_upt_out/upt_multiple_out" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno onSuccess "+new String(responseBody));
                try {
                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){

                        //Muestra el log de los registros externos retirados correctamente.
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Log.d(AppHelper.LOG_TAG,"sincronizaRetiroPatentesExterno REGISTRO ID "+jsonObject.optString("id")+" RECIBIDO CORRECTAMENTE E INFORMADO EN SERVIDOR");
                        }

                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroPatentesExterno onFailure cancelRequests");
            }

        });

    }

    private void getPatentesIngresoPendienteSYNC(){

        try{

            String[] args = new String[]{"0", ""};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, id_cliente_ubicacion, patente, espacios," +
                                                            "fecha_hora_in, rut_usuario_in, maquina_in, imagen_in, " +
                                                            "latitud, longitud, comentario " +
                                                            "FROM tb_registro_patentes WHERE enviado_in =? AND imagen_in =? ", args);
            JSONArray registrosArray = new JSONArray();
            JSONObject jsonParams    = null;
            StringEntity entity      = null;
            int i = 0;

            if (c.moveToFirst()) {

                do {

                    jsonParams = new JSONObject();
                    jsonParams.put("id", c.getString(0));
                    jsonParams.put("id_cliente_ubicacion", c.getInt(1));
                    jsonParams.put("patente", c.getString(2));
                    jsonParams.put("espacios", c.getInt(3));
                    jsonParams.put("fecha_hora_in", c.getString(4));
                    jsonParams.put("rut_usuario_in", c.getString(5));
                    jsonParams.put("maquina_in", c.getString(6));
                    jsonParams.put("imagen_in", c.getString(7));
                    jsonParams.put("enviado_in", 1);
                    jsonParams.put("fecha_hora_out", "");
                    jsonParams.put("rut_usuario_out", "0");
                    jsonParams.put("maquina_out", "");
                    jsonParams.put("enviado_out", 0);
                    jsonParams.put("minutos", 0);
                    jsonParams.put("precio", 0);
                    jsonParams.put("prepago", 0);
                    jsonParams.put("efectivo", 0);
                    jsonParams.put("latitud", c.getString(8));
                    jsonParams.put("longitud", c.getString(9));
                    jsonParams.put("comentario", c.getString(10));
                    jsonParams.put("finalizado", 0);

                    registrosArray.put(i, jsonParams);
                    i++;

                } while (c.moveToNext());

                c.close();
                entity = new StringEntity(registrosArray.toString());
                sincronizaIngresoPatente(entity);
            }
            c.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "getPatentesIngresoPendienteSYNC SQLException "+e.getMessage());
        }catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "getPatentesIngresoPendienteSYNC JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "getPatentesIngresoPendienteSYNC UnsupportedEncodingException "+e.getMessage());
        }

    }

    private void getPatentesIngresoPendienteImagenSYNC(){

        try{

            String[] args = new String[]{"0", ""};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, id_cliente_ubicacion, patente, espacios," +
                                                            "fecha_hora_in, rut_usuario_in, maquina_in, imagen_in, " +
                                                            "latitud, longitud, comentario " +
                                                            "FROM tb_registro_patentes WHERE enviado_in =? AND imagen_in <>? ", args);
            JSONArray registrosArray   = new JSONArray();
            JSONObject jsonParams      = null;
            StringEntity entity        = null;

            if(c.moveToFirst())
            {

                jsonParams = new JSONObject();
                jsonParams.put("id", c.getString(0));
                jsonParams.put("id_cliente_ubicacion", c.getInt(1));
                jsonParams.put("patente",c.getString(2));
                jsonParams.put("espacios",c.getInt(3));
                jsonParams.put("fecha_hora_in",c.getString(4));
                jsonParams.put("rut_usuario_in",c.getString(5));
                jsonParams.put("maquina_in",c.getString(6));
                jsonParams.put("imagen_in",c.getString(7));
                jsonParams.put("enviado_in",1);
                jsonParams.put("fecha_hora_out","");
                jsonParams.put("rut_usuario_out","0");
                jsonParams.put("maquina_out","");
                jsonParams.put("enviado_out",0);
                jsonParams.put("minutos",0);
                jsonParams.put("precio",0);
                jsonParams.put("prepago",0);
                jsonParams.put("efectivo",0);
                jsonParams.put("latitud",c.getString(8));
                jsonParams.put("longitud",c.getString(9));
                jsonParams.put("comentario",c.getString(10));
                jsonParams.put("finalizado",0);

                registrosArray.put(0, jsonParams);
                entity = new StringEntity(registrosArray.toString());

                String archivo_imagen_nombre = c.getString(7);
                String archivo_imagen_ruta   = AppHelper.getImageDir(App.context).getAbsolutePath() + "/" + archivo_imagen_nombre;

                c.close();

                File imagen = new File(archivo_imagen_ruta);
                if (imagen.exists()) {
                    uploadImage(entity, archivo_imagen_ruta, archivo_imagen_nombre, imagen);
                } else {
                    sincronizaIngresoPatente(entity);
                }

            }
            c.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "getPatentesIngresoPendienteImagenSYNC SQLException "+e.getMessage());
        }catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "getPatentesIngresoPendienteImagenSYNC JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "getPatentesIngresoPendienteImagenSYNC UnsupportedEncodingException "+e.getMessage());
        }

    }

    private void uploadImage(final StringEntity entity, final String archivo_imagen_ruta, final String archivo_imagen_nombre, final File imagen)
    {

        StringRequest stringRequest = new StringRequest(Request.Method.POST,  AppHelper.getUrl_restful() + "imagenes_up", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonRootObject = new JSONObject(response);
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");

                    if(jsonArray != null){

                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        Log.d(AppHelper.LOG_TAG, "uploadImage IMAGEN UPLOAD "+jsonObject.optString("text"));

                        sincronizaIngresoPatente(entity);

                        if(imagen.delete()){
                            Log.d(AppHelper.LOG_TAG, "uploadImage IMAGEN ELIMINADA");
                        }else{
                            Log.d(AppHelper.LOG_TAG, "uploadImage IMAGEN NO ELIMINADA");
                        }

                    }else{

                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "uploadImage ERROR RESPONSE IMAGEN UPLOAD "+jsonObject.optString("text"));
                        }
                    }

                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "uploadImage JSONException IMAGEN UPLOAD "+e.getMessage());
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(AppHelper.LOG_TAG, "uploadImage onErrorResponse IMAGEN UPLOAD " + error.getMessage() +" "+ error.getLocalizedMessage());
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("nombre", archivo_imagen_nombre);
                params.put("imagen", imageToString(BitmapFactory.decodeFile(archivo_imagen_ruta)));
                return params;
            }
        };

        MySingleton.getInstance(App.context).addToRequestQueue(stringRequest);

    }

    public void sincronizaIngresoPatente(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "registro_patentes/add_multiple_in" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente onSuccess "+new String(responseBody));
                try {
                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){

                        try{
                            //Marca los registros como enviados.
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patentes SET enviado_in = '1' WHERE id = '"+jsonObject.optString("id")+"'");
                                Log.d(AppHelper.LOG_TAG,"sincronizaIngresoPatente REGISTRO ID "+jsonObject.optString("id")+" ENVIADO CORRECTAMENTE AL SERVIDOR");
                            }

                        }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente SQLException "+e.getMessage());}

                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sincronizaIngresoPatente onFailure cancelRequests");
            }

        });

    }

    private String imageToString(Bitmap bitmap){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, AppHelper.getImagen_calidad() , byteArrayOutputStream);
        byte[] imgBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    private void getPatentesRetiroPendienteSync(){

        try{
            String[] args = new String[] {"1","0","1"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, fecha_hora_out, rut_usuario_out, maquina_out, minutos, precio, prepago, efectivo " +
                                                            "FROM tb_registro_patentes WHERE enviado_in =? AND enviado_out =? AND finalizado =?", args);
            JSONArray registrosArray = new JSONArray();
            JSONObject jsonParams    = null;
            StringEntity entity      = null;
            int i = 0;

            if (c.moveToFirst()){

                do {

                    jsonParams = new JSONObject();
                    jsonParams.put("id",c.getString(0));
                    jsonParams.put("fecha_hora_out",c.getString(1));
                    jsonParams.put("rut_usuario_out",c.getString(2));
                    jsonParams.put("maquina_out",c.getString(3));
                    jsonParams.put("enviado_out",1);
                    jsonParams.put("minutos",c.getInt(4));
                    jsonParams.put("precio",c.getInt(5));
                    jsonParams.put("prepago",c.getInt(6));
                    jsonParams.put("efectivo",c.getInt(7));
                    jsonParams.put("finalizado",1);
                    jsonParams.put("maquina", AppHelper.getSerialNum());

                    registrosArray.put(i, jsonParams);
                    i++;

                }while(c.moveToNext());

                c.close();
                entity = new StringEntity(registrosArray.toString());
                sinncronizaRetiroPatente(entity);

            }
            c.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "getPatentesRetiroPendienteSync SQLException "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "getPatentesRetiroPendienteSync JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "getPatentesRetiroPendienteSync UnsupportedEncodingException "+e.getMessage());
        }

    }

    public void sinncronizaRetiroPatente(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "registro_patentes/upt_multiple_out" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente onSuccess "+new String(responseBody));
                try {

                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){
                        try{

                            //Marca los registros como enviados.
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patentes SET enviado_out = '1' WHERE id = '"+jsonObject.optString("id")+"'");
                                Log.d(AppHelper.LOG_TAG,"sinncronizaRetiroPatente REGISTRO ID "+jsonObject.optString("id")+" ENVIADO CORRECTAMENTE AL SERVIDOR");

                            }

                        }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente SQLException "+e.getMessage());}
                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroPatente onFailure cancelRequests");
            }

        });

    }

    private void autoRetiroPatentes(){
        try{

            String[] args0 = new String[] {String.valueOf(AppHelper.getUbicacion_id()), "0"};
            Cursor c0 = AppHelper.getParkgoSQLite().rawQuery("SELECT trp.id, trp.patente, trp.espacios, trp.fecha_hora_in, tcu.id_cliente " +
                                                                "FROM tb_registro_patentes trp " +
                                                                "INNER JOIN tb_cliente_ubicaciones tcu ON tcu.id = trp.id_cliente_ubicacion " +
                                                                "WHERE trp.id_cliente_ubicacion=? AND trp.finalizado =? ", args0);
            if (c0.moveToFirst()){
                do{
                    String rs_id            = c0.getString(0);
                    String rs_patente       = c0.getString(1);
                    int    rs_espacios      = c0.getInt(2);
                    String rs_fecha_hora_in = c0.getString(3);
                    int    rs_id_cliente    = c0.getInt(4);

                    String nombre_dia_in    = Util.nombreDiaSemana(rs_fecha_hora_in);

                    String[] args1 = new String[] {String.valueOf(AppHelper.getUbicacion_id()),nombre_dia_in};
                    Cursor c1 = AppHelper.getParkgoSQLite().rawQuery("SELECT suma_dia, dia_hasta, hora_hasta FROM tb_cliente_ubicaciones_horarios "+
                                                                        "WHERE id_cliente_ubicacion =? AND dia_desde =? ", args1);
                    if (c1.moveToFirst()){
                        int suma_dia      = c1.getInt(0);
                        String dia_hasta  = c1.getString(1);
                        String hora_hasta = c1.getString(2);

                        Date fechahora_in          = AppHelper.fechaHoraFormat.parse(rs_fecha_hora_in);
                        Date fechahora_actual      = new Date();
                        Date fechahora_auto_retiro = AppHelper.fechaHoraFormat.parse(rs_fecha_hora_in.substring(0,10)+" "+hora_hasta);
                        fechahora_auto_retiro      = new Date(fechahora_auto_retiro.getTime() + TimeUnit.DAYS.toMillis(suma_dia));
                        String fechahora_out       = AppHelper.fechaHoraFormat.format(fechahora_auto_retiro);

                        //si la fechahora_acual es mayor a la fecha maxima fijada para el retiro de patente por ubicación.
                        if(fechahora_actual.after(fechahora_auto_retiro)){

                            long diff   = fechahora_auto_retiro.getTime() - fechahora_in.getTime();//as given
                            int minutos = (int) TimeUnit.MILLISECONDS.toMinutes(diff);

                            int precio = 0;
                            int total_minutos = (minutos - AppHelper.getMinutos_gratis());

                            //Calcula el precio, ya sea por minuto, tramo ó primer tramo mas minutos.
                            if (total_minutos > 0) {
                                precio = Util.calcularPrecio(total_minutos, rs_espacios, 0, 0);
                            }

                            //Aplica descuento de grupo conductor en caso que existe.
                            int descuento_porciento = AppCRUD.getDescuentoGrupoConductor(App.context, rs_patente);
                            precio = Util.redondearPrecio(precio, descuento_porciento);

                            try{

                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patentes " +
                                        "SET " +
                                        "fecha_hora_out = '"+fechahora_out+"', " +
                                        "rut_usuario_out = '0', " +
                                        "maquina_out = '"+AppHelper.getSerialNum()+"', " +
                                        "enviado_out = '1', " +
                                        "minutos = "+minutos+", " +
                                        "precio = "+precio+", " +
                                        "prepago = "+0+", " +
                                        "efectivo = "+0+", " +
                                        "finalizado = '1', " +
                                        "id_estado_deuda = '1', " +
                                        "fecha_hora_estado_deuda = datetime('now','localtime') " +
                                        "WHERE id = '"+rs_id+"'");
                                Log.d(AppHelper.LOG_TAG,"autoRetiroPatentes Registro ID "+rs_id+" finalizado automáticamente");
                            }catch(SQLException e){   Log.d(AppHelper.LOG_TAG, "autoRetiroPatentes SQLException "+e.getMessage()); }

                        }
                    }
                    c1.close();

                } while(c0.moveToNext());
            }
            c0.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "autoRetiroPatentes SQLException "+e.getMessage()); }
        catch (ParseException e) {
            Log.d(AppHelper.LOG_TAG, "autoRetiroPatentes ParseException "+e.getMessage());
        }

    }

    //Genericos
    private void getUbicacionUsuarioPendienteSync(){

        try{
            String[] args = new String[] {"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, rut_usuario, id_cliente_ubicacion," +
                                                                   "latitud, longitud, fecha_hora " +
                                                            "FROM tb_usuario_ubicaciones WHERE enviado =? ", args);
            JSONArray registrosArray = new JSONArray();
            JSONObject jsonParams    = null;
            StringEntity entity      = null;
            int i = 0;

            if (c.moveToFirst()){

                do {

                    jsonParams = new JSONObject();
                    jsonParams.put("id",c.getString(0));
                    jsonParams.put("rut_usuario",c.getString(1));
                    jsonParams.put("id_cliente_ubicacion",c.getInt(2));
                    jsonParams.put("latitud",c.getString(3));
                    jsonParams.put("longitud",c.getString(4));
                    jsonParams.put("fecha_hora",c.getString(5));

                    registrosArray.put(i, jsonParams);
                    i++;

                }while(c.moveToNext());

                c.close();
                entity = new StringEntity(registrosArray.toString());
                sinncronizaUbicacionUsuario(entity);

            }
            c.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "getUbicacionUsuarioPendienteSync SQLException "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "getUbicacionUsuarioPendienteSync JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "getUbicacionUsuarioPendienteSync UnsupportedEncodingException "+e.getMessage());
        }

    }

    public void sinncronizaUbicacionUsuario(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "usuario_ubicaciones/add_multiple" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario onSuccess "+new String(responseBody));
                try {

                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){
                        try{

                            //Marca los registros como enviados.
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_usuario_ubicaciones SET enviado = '1' WHERE id = '"+jsonObject.optString("id")+"'");
                                Log.d(AppHelper.LOG_TAG,"sinncronizaUbicacionUsuario UBICACION USUARIO ID "+jsonObject.optString("id")+" ENVIADO CORRECTAMENTE AL SERVIDOR");
                            }

                        }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario SQLException "+e.getMessage());}
                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sinncronizaUbicacionUsuario onFailure cancelRequests");
            }

        });

    }

    private void getAlertasPendienteSync(){

        try{
            String[] args = new String[] {"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, id_tipo_alerta, id_cliente_ubicacion, " +
                                                                   "rut_usuario, maquina, comentario, fecha_hora " +
                                                            "FROM tb_alertas WHERE enviado =? ", args);

            JSONArray registrosArray = new JSONArray();
            JSONObject jsonParams    = null;
            StringEntity entity      = null;
            int i = 0;

            if (c.moveToFirst()){

                do {

                    jsonParams = new JSONObject();
                    jsonParams.put("id",c.getString(0));
                    jsonParams.put("id_tipo_alerta",c.getInt(1));
                    jsonParams.put("id_cliente_ubicacion",c.getInt(2));
                    jsonParams.put("rut_usuario",c.getString(3));
                    jsonParams.put("maquina",c.getString(4));
                    jsonParams.put("comentario",c.getString(5));
                    jsonParams.put("fecha_hora",c.getString(6));

                    registrosArray.put(i, jsonParams);
                    i++;

                }while(c.moveToNext());

                c.close();
                entity = new StringEntity(registrosArray.toString());
                sinncronizaAlertas(entity);

            }
            c.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "getAlertasPendienteSync SQLException "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "getAlertasPendienteSync JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "getAlertasPendienteSync UnsupportedEncodingException "+e.getMessage());
        }

    }

    public void sinncronizaAlertas(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "alertas/add_multiple" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas onSuccess "+new String(responseBody));
                try {

                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){
                        try{
                            //Marca los registros como enviados.
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_alertas SET enviado = '1' WHERE id = '"+jsonObject.optString("id")+"'");
                                Log.d(AppHelper.LOG_TAG,"sinncronizaAlertas ALERTA ID "+jsonObject.optString("id")+" ENVIADO CORRECTAMENTE AL SERVIDOR");
                            }

                        }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas SQLException "+e.getMessage());}
                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sinncronizaAlertas onFailure cancelRequests");
            }

        });

    }

    private void getRetiroRecaudacionSync(){

        try{

            String[] args = new String[] {"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, id_cliente_ubicacion, rut_usuario_operador," +
                                                            "maquina, rut_usuario_retiro, fecha_recaudacion, monto " +
                                                            "FROM tb_recaudacion_retiro WHERE enviado =? ", args);

            JSONArray registrosArray = new JSONArray();
            JSONObject jsonParams    = null;
            StringEntity entity      = null;
            int i = 0;

            if (c.moveToFirst()){

                do {

                    jsonParams = new JSONObject();
                    jsonParams.put("id",c.getString(0));
                    jsonParams.put("id_cliente_ubicacion",c.getInt(1));
                    jsonParams.put("rut_usuario_operador",c.getString(2));
                    jsonParams.put("maquina",c.getString(3));
                    jsonParams.put("rut_usuario_retiro",c.getString(4));
                    jsonParams.put("fecha_recaudacion",c.getString(5));
                    jsonParams.put("monto",c.getInt(6));

                    registrosArray.put(i, jsonParams);
                    i++;

                }while(c.moveToNext());

                c.close();
                entity = new StringEntity(registrosArray.toString());
                sinncronizaRetiroRecaudacion(entity);

            }
            c.close();

        }catch(SQLException e){
            Log.d(AppHelper.LOG_TAG, "getRetiroRecaudacionSync SQLException "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "getRetiroRecaudacionSync JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "getRetiroRecaudacionSync UnsupportedEncodingException "+e.getMessage());
        }

    }

    public void sinncronizaRetiroRecaudacion(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "recaudacion_retiro/add_multiple" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion onSuccess "+new String(responseBody));
                try {

                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){
                        try{

                            //Marca los registros como enviados.
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_recaudacion_retiro SET enviado = '1' WHERE id = '"+jsonObject.optString("id")+"'");
                                Log.d(AppHelper.LOG_TAG,"sinncronizaRetiroRecaudacion RECAUDACION RETIRO ID "+jsonObject.optString("id")+" ENVIADO CORRECTAMENTE AL SERVIDOR");
                            }

                        }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion SQLException "+e.getMessage());}
                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sinncronizaRetiroRecaudacion onFailure cancelRequests");
            }

        });

    }

    private void getRetiroRecaudacionExterno(){

        String rut_usuario = AppHelper.getUsuario_rut();
        String maquina     = AppHelper.getSerialNum();
        int id_cliente_ubicacion = AppHelper.getUbicacion_id();

        if (id_cliente_ubicacion > 0 && !maquina.equals("") && !rut_usuario.equals("")) {
            ClienteAsync(AppHelper.getUrl_restful() + "recaudacion_retiro_operador_in/"+ rut_usuario +"/" + maquina + "/" + id_cliente_ubicacion, new ClienteCallback() {

                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {
                    if(esError == 0 && !responseBody.equals("")) {
                        insertaRetiroRecaudacionExterno(responseBody);
                    }else{
                        Log.d(AppHelper.LOG_TAG,"getRetiroRecaudacionExterno ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                    }
                }

            });
        }

    }

    private void insertaRetiroRecaudacionExterno(String jsonString){

        String qry;
        try {

            JSONObject jsonRootObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonRootObject.optJSONArray("recaudacion_retiro_operador_in");
            if(jsonArray != null){

                JSONArray registrosArray = new JSONArray();
                JSONObject jsonParams    = null;
                StringEntity entity      = null;

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id_recaudacion_retiro = jsonObject.optString("id");
                    int id_cliente_ubicacion     = jsonObject.optInt("id_cliente_ubicacion");
                    String rut_usuario_operador  = jsonObject.optString("rut_usuario_operador");
                    String maquina               = jsonObject.optString("maquina");
                    String rut_usuario_retiro    = jsonObject.optString("rut_usuario_retiro");
                    String fecha_recaudacion     = jsonObject.optString("fecha_recaudacion");
                    int monto                    = jsonObject.optInt("monto");

                    qry =  "INSERT OR IGNORE INTO tb_recaudacion_retiro " +
                            "(id, id_cliente_ubicacion, rut_usuario_operador," +
                            "maquina, rut_usuario_retiro, fecha_recaudacion, " +
                            "monto, enviado)" +
                            "VALUES " +
                            "('" + id_recaudacion_retiro + "', " + id_cliente_ubicacion + " , '" + rut_usuario_operador +
                            "', '" + maquina + "', '" + rut_usuario_retiro + "' , '" + fecha_recaudacion +
                            "', " + monto + ", 1);";

                    AppHelper.getParkgoSQLite().execSQL(qry);

                    jsonParams = new JSONObject();
                    jsonParams.put("rut_usuario",AppHelper.getUsuario_rut());
                    jsonParams.put("maquina", AppHelper.getSerialNum());
                    jsonParams.put("id_recaudacion_retiro", id_recaudacion_retiro);
                    registrosArray.put(i, jsonParams);

                }

                if(registrosArray.length() > 0) {
                    entity = new StringEntity(registrosArray.toString());
                    sincronizaRetiroRecaudacionExterno(entity);
                }

            }else{
                jsonArray = jsonRootObject.optJSONArray("error");
                if(jsonArray != null){
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    Log.d(AppHelper.LOG_TAG, "insertaRetiroRecaudacionExterno ERROR RESPONSE "+jsonObject.optString("text"));
                }
            }

        } catch (SQLException e) {
            Log.d(AppHelper.LOG_TAG, "insertaRetiroRecaudacionExterno AppHelper "+e.getMessage());
        } catch (JSONException e) {
            Log.d(AppHelper.LOG_TAG, "insertaRetiroRecaudacionExterno JSONException "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.d(AppHelper.LOG_TAG, "insertaRetiroRecaudacionExterno JSONException "+e.getMessage());
        }

    }

    public void sincronizaRetiroRecaudacionExterno(StringEntity entity) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
        cliente.post(App.context, AppHelper.getUrl_restful() + "recaudacion_retiro_operador_upt_in/upt_multiple_in" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno onSuccess "+new String(responseBody));
                try {
                    JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                    JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                    if(jsonArray != null){

                        //Muestra el log de los registros externos recibidos correctamente.
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Log.d(AppHelper.LOG_TAG,"sincronizaRetiroRecaudacionExterno REGISTRO ID "+jsonObject.optString("id")+" RECIBIDO CORRECTAMENTE E INFORMADO EN SERVIDOR");
                        }

                    }else{
                        jsonArray = jsonRootObject.optJSONArray("error");
                        if(jsonArray != null){
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno ERROR RESPONSE "+jsonObject.optString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno JSONException "+e.getMessage());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG, "sincronizaRetiroRecaudacionExterno onFailure cancelRequests");
            }

        });

    }

    public void ClienteAsync(String url, final ClienteCallback clienteCallback) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        cliente.get(App.context, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG,"AsyncGenerico onSuccess " + new String(responseBody));
                clienteCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "AsyncGenerico onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "AsyncGenerico onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "AsyncGenerico onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG,"AsyncGenerico onFailure cancelRequests");
            }

        });
    }

    public void pruebas() {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        JSONArray list         = new JSONArray();
        JSONObject jsonParams  = null;
        StringEntity entity    = null;
        try {
            for(int i=0; i < 3; i++){
                jsonParams = new JSONObject();
                jsonParams.put("id",String.valueOf(i));
                jsonParams.put("descripcion","descripcion "+i);
                list.put(i, jsonParams);
            }
            entity = new StringEntity(list.toString());

            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            cliente.post(App.context, AppHelper.getUrl_restful() + "pruebas" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    //Log.d(AppHelper.LOG_TAG, "pruebas onSuccess responseBody "+new String(responseBody));
                    JSONObject jsonRootObject = null;
                    try {
                        jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray = jsonRootObject.optJSONArray("success");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id      = jsonObject.optInt("id");
                            String desc = jsonObject.optString("desc");
                            Log.d(AppHelper.LOG_TAG, "pruebas onSuccess responseBody "+id+" descrip "+desc);
                        }
                    } catch (JSONException e) {
                        Log.d(AppHelper.LOG_TAG, "pruebas onSuccess JSONException "+e.getMessage());
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d(AppHelper.LOG_TAG, "pruebas onFailure responseBody "+String.valueOf(responseBody));
                    Log.d(AppHelper.LOG_TAG, "pruebas onFailure error "+String.valueOf(error.getMessage()));
                    cliente.cancelRequests(App.context, true);
                    Log.d(AppHelper.LOG_TAG, "pruebas onFailure cancelRequests");
                }

            });

        } catch (UnsupportedEncodingException e0) {
            Log.d(AppHelper.LOG_TAG, "pruebas UnsupportedEncodingException "+e0.getMessage());
        } catch (JSONException e1) {
            Log.d(AppHelper.LOG_TAG, "pruebas JSONException "+e1.getMessage());
        }

    }

}
