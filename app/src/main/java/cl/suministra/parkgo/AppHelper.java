package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Created by LENOVO on 02-08-2017.
 */

public class AppHelper {

    private static String db_nombre  = "db_parkgo";
    private static int db_version    = 2;
    private static BDParkgo parkgoDB;
    private static SQLiteDatabase SQLiteParkgo;

    private static String serial_no  = "";
    private static String usuario_rut= "";

    public static int minutos_gratis = 5;
    public static int valor_minuto   = 10;

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

    //retorna directorio de almacenamiento para las imagenes de la camara.
    public static File getImageDir(Context context){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        return storageDir;

    }

}