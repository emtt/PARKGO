package cl.suministra.parkgo;

import android.content.Context;
import android.database.SQLException;

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
            Util.alertDialog(context,"SQLException Menu Alerta", e.getMessage());
            return false;
        }
    }



}
