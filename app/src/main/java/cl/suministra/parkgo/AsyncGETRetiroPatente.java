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

public class AsyncGETRetiroPatente extends AsyncTask<Void, Integer,  Boolean> {

    private AsyncHttpClient cliente = null;

    public void cancelTask(AsyncGETRetiroPatente asyncGETRetiroPatente) {
        if (asyncGETRetiroPatente == null) return;
        asyncGETRetiroPatente.cancel(false);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onPreExecute");
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
            Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente InterruptedException "+e.getMessage());
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progreso = values[0].intValue();
        getPatentesRetiroExterno();
        Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onProgressUpdate "+progreso);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result)
            Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onPostExecute Finalizada");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onCancelled Cancelada");;
    }

    private void getPatentesRetiroExterno(){

        String maquina = AppHelper.getSerialNum();
        int id_cliente_ubicacion = AppHelper.getUbicacion_id();
        if (id_cliente_ubicacion > 0 && !maquina.equals("")) {
            ClienteAsync(AppHelper.getUrl_restful() + "registro_patentes_maquinas_out/" + maquina + "/" + id_cliente_ubicacion, new ClienteCallback() {

                @Override
                public void onResponse(int esError, int statusCode, String responseBody) {
                    if(esError == 0 && !responseBody.equals("")) {
                        actualizaRetiroPatentesExternas(responseBody);
                    }else{
                        Log.d(AppHelper.LOG_TAG,"AsyncGETRetiroPatente ERROR SYNC Código: " + statusCode + "\n" + responseBody);
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
                Log.d(AppHelper.LOG_TAG,"AsyncGETRetiroPatente onSuccess " + new String(responseBody));
                clienteCallback.onResponse(0, statusCode, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onFailure statusCode "+String.valueOf(statusCode));
                Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onFailure responseBody "+String.valueOf(responseBody));
                Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente onFailure error "+String.valueOf(Log.getStackTraceString(error)));

                cliente.cancelRequests(App.context, true);
                Log.d(AppHelper.LOG_TAG,"AsyncGETRetiroPatente onFailure cancelRequests");
            }

        });
    }

    private void actualizaRetiroPatentesExternas(String jsonString){

        String qry;
        try {

            JSONObject jsonRootObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonRootObject.optJSONArray("registro_patentes_maquinas_out");
            if(jsonArray != null){

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
                    Log.d(AppHelper.LOG_TAG, qry);

                    //Marca el registro como recibido en el servidor.
                    ClienteAsync(AppHelper.getUrl_restful() + "registro_patentes_maquinas_upt_out/" + AppHelper.getSerialNum() + "/" + id_registro_patente, new ClienteCallback() {

                        @Override
                        public void onResponse(int esError, int statusCode, String responseBody) {
                            if(esError == 0 && !responseBody.equals("")) {
                                //Si no hay error entonces recibira
                            }else{
                                Log.d(AppHelper.LOG_TAG,"AsyncGETRetiroPatente ERROR SYNC Código: " + statusCode + "\n" + responseBody);
                            }
                        }

                    });



                }

            }else{
                jsonArray = jsonRootObject.optJSONArray("error");
                if(jsonArray != null){
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente ERROR RESPONSE "+jsonObject.optString("text"));
                }
            }

        } catch (SQLException e0) {
            Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente AppHelper "+e0.getMessage());
        } catch (JSONException e1) {
            Log.d(AppHelper.LOG_TAG, "AsyncGETRetiroPatente JSONException "+e1.getMessage());
        }

    }

}
