package cl.suministra.parkgo;

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

public class AsyncSENDUbicacionUsuario extends AsyncTask<Void, Integer,  Boolean> {

    private AsyncHttpClient cliente = null;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario onPreExecute");
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            int i = 1;
            do{
                if(Util.internetStatus(App.context)){
                    publishProgress(i);
                }else{
                    if (cliente != null) {
                        i = 0;
                        cliente.cancelRequests(App.context, true);
                        Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario cancelRequests");
                    }
                }

                i++;
                TimeUnit.SECONDS.sleep(1);
                isCancelled();
            }while(!isCancelled());
        } catch (InterruptedException e) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario InterruptedException "+e.getMessage());
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progreso = values[0].intValue();
        getUbicacionUsuarioPendienteSYNC();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario onProgressUpdate "+progreso);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result)
            Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario onPostExecute Finalizada");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario onCancelled Cancelada");;
    }

    private void getUbicacionUsuarioPendienteSYNC(){

        try{
            String[] args = new String[] {"0"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, rut_usuario, id_cliente_ubicacion," +
                                                                   "latitud, longitud, fecha_hora " +
                                                            "FROM tb_usuario_ubicaciones WHERE enviado =? ", args);
            if (c.moveToFirst()){
                String rs_id          = c.getString(0);
                String rs_rut_usuario = c.getString(1);
                int rs_id_cliente_ubicacion = c.getInt(2);
                String rs_latitud    = c.getString(3);
                String rs_longitud   = c.getString(4);
                String rs_fecha_hora = c.getString(5);

                sinncronizaUbicacionUsuario(rs_id, rs_rut_usuario, rs_id_cliente_ubicacion,
                                            rs_latitud, rs_longitud, rs_fecha_hora);
            }
            c.close();

        }catch(SQLException e){ Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario SQLException "+e.getMessage()); }

    }

    public void sinncronizaUbicacionUsuario(final String id, final String rut_usuario, final int id_cliente_ubicacion,
                                            final String latitud, final String longitud, final String fecha_hora) {

        cliente = new AsyncHttpClient();
        JSONObject jsonParams  = null;
        StringEntity entity    = null;
        try {
            jsonParams = new JSONObject();
            jsonParams.put("id",id);
            jsonParams.put("rut_usuario",rut_usuario);
            jsonParams.put("id_cliente_ubicacion",id_cliente_ubicacion);
            jsonParams.put("latitud",latitud);
            jsonParams.put("longitud",longitud);
            jsonParams.put("fecha_hora",fecha_hora);
            entity = new StringEntity(jsonParams.toString());

            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            cliente.post(App.context, AppHelper.getUrl_restful() + "usuario_ubicaciones/add" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario onSuccess "+new String(responseBody));
                    try {

                        JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                        if(jsonArray != null){
                            try{
                                //Marca el registro como enviado.
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_usuario_ubicaciones SET enviado = '1' WHERE id = '"+id+"'");
                                Log.d(AppHelper.LOG_TAG,"AsyncSENDUbicacionUsuario REGISTRO ID "+id+" ENVIADO CORRECTAMENTE AL SERVIDOR");

                            }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario SQLException "+e.getMessage());}
                        }else{
                            jsonArray = jsonRootObject.optJSONArray("error");
                            if(jsonArray != null){
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario ERROR RESPONSE "+jsonObject.optString("text"));
                            }
                        }
                    } catch (JSONException e) {
                        Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario JSONException "+e.getMessage());
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario onFailure "+error.getMessage());
                }

            });

        } catch (UnsupportedEncodingException e0) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario UnsupportedEncodingException "+e0.getMessage());
        } catch (JSONException e1) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDUbicacionUsuario UnsupportedEncodingException "+e1.getMessage());
        }

    }

}
