package cl.suministra.parkgo;

import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by LENOVO on 02-08-2017.
 */

public class AppHelper {

    private static String db_nombre  = "db_parkgo";
    private static int db_version    = 3;
    private static BDParkgo parkgoDB;
    private static SQLiteDatabase SQLiteParkgo;

    public static int minutos_gratis = 5;
    public static int valor_minuto   = 10;

    public static void initParkgoDB(Context context){
        parkgoDB = new BDParkgo(context, db_nombre, null, db_version);
        SQLiteParkgo  = parkgoDB.getWritableDatabase();
    }

    public static SQLiteDatabase getParkgoSQLite(){
        return SQLiteParkgo;
    }


}