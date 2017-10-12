package cl.suministra.parkgo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by LENOVO on 02-08-2017.
 */

public class AppHelper {

    private static String db_nombre  = "db_parkgo";
    private static int db_version    = 4;
    private static BDParkgo parkgoDB;
    private static SQLiteDatabase SQLiteParkgo;

    private static String serial_no  = "";
    private static String usuario_rut= "";
    private static String usuario_nombre = "";

    public static DateFormat fechaHoraFormat   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static DateFormat fechaHoraFormatID = new SimpleDateFormat("yyyyMMddHHmmss");
    public static DateFormat fechaFormat       = new SimpleDateFormat("dd-MM-yyyy",Locale.US);

    public static int cliente_id     = 0;
    public static int ubicacion_id   = 0;

    public static String ubicacion_nombre = "";
    public static int minutos_gratis = 0;
    public static int valor_minuto   = 0;

    public static String url_restful = "";
    public static String pagina_test = "";
    public static String LOG_TAG     = "parkgo_log";

    public static void initParkgoDB(Context context){
        parkgoDB = new BDParkgo(context, db_nombre, null, db_version);
        SQLiteParkgo  = parkgoDB.getWritableDatabase();
    }

    public static SQLiteDatabase getParkgoSQLite(){
        return SQLiteParkgo;
    }

    public static String getUsuario_rut() {
        return usuario_rut;
    }

    public static void setUsuario_rut(String usuario_rut) {
        AppHelper.usuario_rut = usuario_rut;
    }

    public static String getUsuario_nombre() {
        return usuario_nombre;
    }

    public static void setUsuario_nombre(String usuario_nombre) {
        AppHelper.usuario_nombre = usuario_nombre;
    }

    public static int getCliente_id() {
        return cliente_id;
    }

    public static void setCliente_id(int cliente_id) {
        AppHelper.cliente_id = cliente_id;
    }

    public static int getUbicacion_id() {
        return ubicacion_id;
    }

    public static void setUbicacion_id(int ubicacion_id) {
        AppHelper.ubicacion_id = ubicacion_id;
    }

    public static String getUbicacion_nombre() {
        return ubicacion_nombre;
    }

    public static void setUbicacion_nombre(String ubicacion_nombre) {
        AppHelper.ubicacion_nombre = ubicacion_nombre;
    }

    public static int getMinutos_gratis() {
        return minutos_gratis;
    }

    public static void setMinutos_gratis(int minutos_gratis) {
        AppHelper.minutos_gratis = minutos_gratis;
    }

    public static int getValor_minuto() {
        return valor_minuto;
    }

    public static void setValor_minuto(int valor_minuto) {
        AppHelper.valor_minuto = valor_minuto;
    }

    public static void initSerialNum(Context context){
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial_no = (String) get.invoke(c, "ro.serialno");
        }catch (Exception e) {Toast.makeText(context,"Ocurrió un error al obtener número de serie "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    public static String getSerialNum(){
        return serial_no;
    }

    public static String getUrl_restful() {
        return url_restful;
    }

    public static void setUrl_restful(String url_restful) {
        AppHelper.url_restful =  url_restful;
    }

    public static String getPagina_test() {
        return pagina_test;
    }

    public static void setPagina_test(String pagina_test) {
        AppHelper.pagina_test = pagina_test;
    }

    //retorna directorio de almacenamiento para las imagenes de la camara.
    public static File getImageDir(Context context){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        return storageDir;

    }

}