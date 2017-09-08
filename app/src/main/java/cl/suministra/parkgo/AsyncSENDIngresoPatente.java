package cl.suministra.parkgo;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Created by LENOVO on 06-09-2017.
 */

public class AsyncSENDIngresoPatente extends AsyncTask<Void, Integer,  Boolean> {

    Context context;

    public AsyncSENDIngresoPatente(Context context){
        this.context = context;
    }

    public void cancelTask(AsyncSENDIngresoPatente asyncSENDIngresoPatente) {
        if (asyncSENDIngresoPatente == null) return;
        asyncSENDIngresoPatente.cancel(false);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente onPreExecute");
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            int i = 1;
            do{
                if(Util.internetStatus(context)){
                    publishProgress(i);
                }

                i++;
                TimeUnit.SECONDS.sleep(1);
                isCancelled();
            }while(!isCancelled());
        } catch (InterruptedException e) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente InterruptedException "+e.getMessage());
        }
        return true;
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progreso = values[0].intValue();
        getPatentesIngresoPendienteSYNC();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente onProgressUpdate "+progreso);
    }


    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result)
            Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente onPostExecute Finalizada");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente onCancelled Cancelada");;
    }

    private void getPatentesIngresoPendienteSYNC(){

        try{
            String[] args = new String[] {"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, id_cliente_ubicacion, patente, espacios," +
                    "                                               fecha_hora_in, rut_usuario_in, maquina_in, imagen_in " +
                                                                "FROM tb_registro_patente WHERE enviado_in =?", args);
            if (c.moveToFirst()){
                String rs_id = c.getString(0);
                int rs_id_cliente_ubicacion = c.getInt(1);
                String rs_patente = c.getString(2);
                int rs_espacios   = c.getInt(3);
                String rs_fecha_hora_in  = c.getString(4);
                String rs_rut_usuario_in = c.getString(5);
                String rs_maquina_in     = c.getString(6);
                String rs_archivo_imagen_nombre = c.getString(7);

                sinncronizaIngresoPatente(rs_id, rs_id_cliente_ubicacion, rs_patente, rs_espacios,  rs_fecha_hora_in, rs_rut_usuario_in, rs_maquina_in, rs_archivo_imagen_nombre);
            }
            c.close();

        }catch(SQLException e){ Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente SQLException "+e.getMessage()); }

    }


    public void sinncronizaIngresoPatente(final String id_registro_patente, final int id_cliente_ubicacion, final String patente, final int espacios,
                                          final String fecha_hora_in, final String rut_usuario_in, final String maquina_in, final String archivo_imagen_nombre) {

        AsyncHttpClient client = new AsyncHttpClient () ;
        JSONObject jsonParams  = null;
        StringEntity entity    = null;
        try {
            jsonParams = new JSONObject();
            jsonParams.put("id",id_registro_patente);
            jsonParams.put("id_cliente_ubicacion",id_cliente_ubicacion);
            jsonParams.put("patente",patente);
            jsonParams.put("espacios",espacios);
            jsonParams.put("fecha_hora_in",fecha_hora_in);
            jsonParams.put("rut_usuario_in",rut_usuario_in);
            jsonParams.put("maquina_in",maquina_in);
            jsonParams.put("imagen_in",archivo_imagen_nombre);
            jsonParams.put("enviado_in",1);
            jsonParams.put("fecha_hora_out","");
            jsonParams.put("rut_usuario_out","0");
            jsonParams.put("maquina_out","");
            jsonParams.put("enviado_out",0);
            jsonParams.put("minutos",0);
            jsonParams.put("finalizado",0);
            entity = new StringEntity(jsonParams.toString());

            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            client.post(context , AppHelper.getUrl_restful() + "registro_patentes/add_in" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    try {
                        JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                        if(jsonArray != null){
                            try{
                                //Marca el registro como enviado.
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patente SET enviado_in = '1' WHERE id = '"+id_registro_patente+"'");
                                Log.d(AppHelper.LOG_TAG,"AsyncSENDIngresoPatente REGISTRO ID "+id_registro_patente+" ENVIADO CORRECTAMENTE AL SERVIDOR");

                            }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente SQLException "+e.getMessage());}
                        }else{
                            jsonArray = jsonRootObject.optJSONArray("error");
                            if(jsonArray != null){
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente ERROR_1 SYNC "+jsonObject.optString("text"));
                            }
                        }
                    } catch (JSONException e) {
                        Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente JSONException "+e.getMessage());
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente ERROR_2 SYNC "+new String(responseBody));
                }

            });

        } catch (UnsupportedEncodingException e0) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente UnsupportedEncodingException "+e0.getMessage());
        } catch (JSONException e1) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDIngresoPatente JSONException "+e1.getMessage());
        }

    }

}
