package cl.suministra.parkgo;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import java.util.Date;

/**
 * Created by LENOVO on 04-10-2017.
 */

public class AppCRUD {


    public static boolean registrarAlerta(Context context, int id_tipo_alerta, String comentario){
        try{

            Date fechahora_actual   = new Date();
            String fecha_hora_actual = AppHelper.fechaHoraFormatID.format(fechahora_actual);
            String id_alerta  = fecha_hora_actual+"_"+AppHelper.getSerialNum()+"_"+AppHelper.getUsuario_rut();

            AppHelper.getParkgoSQLite().execSQL("INSERT INTO tb_alertas "+
                    "(id, id_tipo_alerta, id_cliente_ubicacion, rut_usuario," +
                    "maquina, comentario, fecha_hora, enviado)"+
                    "VALUES " +
                    "('"+id_alerta+"',"+id_tipo_alerta+",'"+AppHelper.getUbicacion_id()+"'," + "'"+AppHelper.getUsuario_rut()+"'," +
                    "'"+AppHelper.getSerialNum()+"' ,'"+comentario+"', datetime('now','localtime'), '0');");
            return true;
        }catch(SQLException e){
            Util.alertDialog(context,"SQLException AppCRUD", e.getMessage());
            return false;
        }
    }


    public static int getDescuentoGrupoConductor(Context context, String patente){

        Cursor c;
        int rs_descuento = 0;
        try {
            String[] args = new String[]{patente, String.valueOf(AppHelper.getUbicacion_id())};

            String qry = "SELECT tcgud.id_cliente_ubicacion, tcgud.id_cliente_ubicacion, tcgud.descuento\n" +
                            "FROM tb_conductor_grupo_ubicacion_descuento tcgud\n" +
                            "INNER JOIN tb_conductor tc ON tc.id_conductor_grupo = tcgud.id_conductor_grupo\n" +
                            "INNER JOIN tb_conductor_patentes tcp ON tcp.rut_conductor = tc.rut\n" +
                            "WHERE tcp.patente =? AND tcgud.id_cliente_ubicacion =?";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {
                rs_descuento = c.getInt(2);
            } else {
                rs_descuento = 0;
            }
            c.close();

        } catch (SQLException e) {
                Util.alertDialog(context, "SQLException AppCRUD", e.getMessage());
        }

        return rs_descuento;

    }

    public static int actualizaNumeroEtiqueta(Context context, int num_etiqueta_actual, int num_etiqueta_nueva, boolean suma_uno){
        try{
            if (suma_uno){
                num_etiqueta_nueva++;
                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_etiquetas " +
                        "SET " +
                        "num_etiqueta_actual = '"+num_etiqueta_nueva+"' " +
                        "WHERE num_etiqueta_actual = "+num_etiqueta_actual+" ");
            }else{
                AppHelper.getParkgoSQLite().execSQL("UPDATE tb_etiquetas " +
                        "SET " +
                        "num_etiqueta_actual = '"+num_etiqueta_nueva+"' " +
                        "WHERE num_etiqueta_actual = "+num_etiqueta_actual+" ");
            }
        } catch (SQLException e) {
            Util.alertDialog(context, "SQLException AppCRUD", e.getMessage());
        }

        return 100;

    }

}
