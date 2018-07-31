package cl.suministra.parkgo;

import android.database.SQLException;
import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

/**
 * Created by LENOVO on 07-09-2017.
 */

public class AsyncGETIngresoPatente extends AsyncTask<Void, Integer,  Boolean> {

    private AsyncHttpClient cliente = null;

    public void cancelTask(AsyncGETIngresoPatente asyncGETIngresoPatente) {
        if (asyncGETIngresoPatente == null) return;
        asyncGETIngresoPatente.cancel(false);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onPreExecute");
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
            Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente InterruptedException "+e.getMessage());
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progreso = values[0].intValue();
        getPatentesIngresoExterno();
        Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onProgressUpdate "+progreso);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result)
            Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onPostExecute Finalizada");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onCancelled Cancelada");;
    }

    private void getPatentesIngresoExterno(){

        String maquina = AppHelper.getSerialNum();
        int id_cliente_ubicacion = AppHelper.getUbicacion_id();
        if (id_cliente_ubicacion > 0 && !maquina.equals("")) {
            ClienteAsync(AppHelper.getUrl_restful() + "registro_patentes_maquinas_in/" + maquina + "/" + id_cliente_ubicacion, new ClienteCallback() {

                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {
                 if(esError == 0 && !responseBody.equals("")) {
                     insertaIngresoPatentesExternas(responseBody);
                 }else{
                     Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                 }
                }

            });
        }

    }

    public void ClienteAsync(String url, final ClienteCallback clienteCallback) {

        cliente = new AsyncHttpClient();
        cliente.setTimeout(AppHelper.getTimeout());
        cliente.setConnectTimeout(AppHelper.getTimeout());
        cliente.setResponseTimeout(AppHelper.getTimeout());

        cliente.get(App.context, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente onSuccess " + new String(responseBody));
                clienteCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente onFailure cancelRequests");
            }

        });
    }

    private void insertaIngresoPatentesExternas(String jsonString){

        String qry;
        try {

            JSONObject jsonRootObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonRootObject.optJSONArray("registro_patentes_maquinas_in");
            if(jsonArray != null){

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
                    Log.d(AppHelper.LOG_TAG, qry);

                    //Marca el registro como recibido en el servidor.
                    ClienteAsync(AppHelper.getUrl_restful() + "registro_patentes_maquinas_upt_in/" + AppHelper.getSerialNum() + "/" + id_registro_patente, new ClienteCallback() {

                        @Override
                        public void onResponse(int esError, int statusCode, String responseBody) {
                            if(esError == 0 && !responseBody.equals("")) {
                                //Si no hay error entonces recibira
                            }else{
                                Log.d(AppHelper.LOG_TAG,"AsyncGETIngresoPatente ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                            }
                        }

                    });

                }

            }else{
                jsonArray = jsonRootObject.optJSONArray("error");
                if(jsonArray != null){
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente ERROR RESPONSE "+jsonObject.optString("text"));
                }
            }

        } catch (SQLException e0) {
            Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente AppHelper "+e0.getMessage());
        } catch (JSONException e1) {
            Log.d(AppHelper.LOG_TAG, "AsyncGETIngresoPatente JSONException "+e1.getMessage());
        }


    }

}
