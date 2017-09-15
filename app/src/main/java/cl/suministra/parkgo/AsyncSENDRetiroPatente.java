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

public class AsyncSENDRetiroPatente extends AsyncTask<Void, Integer,  Boolean> {

    private AsyncHttpClient cliente = null;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente onPreExecute");
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
                        Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente cancelRequests");
                    }
                }

                i++;
                TimeUnit.SECONDS.sleep(1);
                isCancelled();
            }while(!isCancelled());
        } catch (InterruptedException e) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente InterruptedException "+e.getMessage());
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progreso = values[0].intValue();
        getPatentesRetiroPendienteSYNC();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente onProgressUpdate "+progreso);
    }


    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result)
            Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente onPostExecute Finalizada");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente onCancelled Cancelada");;
    }

    private void getPatentesRetiroPendienteSYNC(){

        try{
            String[] args = new String[] {"0","1"};
            Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT id, fecha_hora_out, rut_usuario_out, maquina_out, minutos, precio, prepago, efectivo " +
                                                            "FROM tb_registro_patente WHERE enviado_out =? AND finalizado =?", args);
            if (c.moveToFirst()){
                String rs_id = c.getString(0);
                String rs_fecha_hora_out = c.getString(1);
                String rs_rut_usuario_out= c.getString(2);
                String rs_maquina_out    = c.getString(3);
                int    rs_minutos        = c.getInt(4);
                int    rs_precio         = c.getInt(5);
                int    rs_prepago        = c.getInt(6);
                int    rs_efectivo       = c.getInt(7);
                sinncronizaRetiroPatente(rs_id, rs_fecha_hora_out, rs_rut_usuario_out, rs_maquina_out, rs_minutos, rs_precio, rs_prepago, rs_efectivo);
            }
            c.close();

        }catch(SQLException e){ Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente SQLException "+e.getMessage()); }

    }

    public void sinncronizaRetiroPatente(final String id_registro_patente, final String fecha_hora_out,
                                         final String rut_usuario_out, final String maquina_out, final int minutos,
                                         final int precio, final int prepago, final int efectivo) {

        cliente = new AsyncHttpClient();
        JSONObject jsonParams  = null;
        StringEntity entity    = null;
        try {
            jsonParams = new JSONObject();
            jsonParams.put("id",id_registro_patente);
            jsonParams.put("fecha_hora_out",fecha_hora_out);
            jsonParams.put("rut_usuario_out",rut_usuario_out);
            jsonParams.put("maquina_out",maquina_out);
            jsonParams.put("enviado_out",1);
            jsonParams.put("minutos",minutos);
            jsonParams.put("precio",precio);
            jsonParams.put("prepago",prepago);
            jsonParams.put("efectivo",efectivo);
            jsonParams.put("finalizado",1);
            entity = new StringEntity(jsonParams.toString());

            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            cliente.post(App.context, AppHelper.getUrl_restful() + "registro_patentes/upt_out" , entity , ContentType.APPLICATION_JSON.getMimeType() , new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente onSuccess "+new String(responseBody));
                    try {

                        JSONObject jsonRootObject = new JSONObject(new String(responseBody));
                        JSONArray jsonArray       = jsonRootObject.optJSONArray("success");
                        if(jsonArray != null){
                            try{
                                //Marca el registro como enviado.
                                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_registro_patente SET enviado_out = '1' WHERE id = '"+id_registro_patente+"'");
                                Log.d(AppHelper.LOG_TAG,"AsyncSENDRetiroPatente REGISTRO ID "+id_registro_patente+" ENVIADO CORRECTAMENTE AL SERVIDOR");

                            }catch(SQLException e){  Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente SQLException "+e.getMessage());}
                        }else{
                            jsonArray = jsonRootObject.optJSONArray("error");
                            if(jsonArray != null){
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente ERROR RESPONSE "+jsonObject.optString("text"));
                            }
                        }
                    } catch (JSONException e) {
                        Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente JSONException "+e.getMessage());
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente onFailure "+error.getMessage());
                }

            });

        } catch (UnsupportedEncodingException e0) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente UnsupportedEncodingException "+e0.getMessage());
        } catch (JSONException e1) {
            Log.d(AppHelper.LOG_TAG, "AsyncSENDRetiroPatente UnsupportedEncodingException "+e1.getMessage());
        }

    }

}
