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


    public static int getDescuentoGrupoConductor(Context context, String patente, int cliente_id){

        Cursor c;
        int rs_descuento = 0;
        try {
            String[] args = new String[]{patente, String.valueOf(cliente_id)};

            String qry = "SELECT tcp.rut_conductor, tc.id_conductor_grupo, tcg.descuento FROM tb_conductor_patentes tcp\n" +
                            "INNER JOIN tb_conductor tc ON tc.rut = tcp.rut_conductor\n" +
                            "INNER JOIN tb_conductor_grupo tcg ON tcg.id = tc.id_conductor_grupo\n" +
                            "WHERE tcp.patente =? AND tcg.id_cliente =? ";

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


}
