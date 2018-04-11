package cl.suministra.parkgo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by LENOVO on 02-08-2017.
 */

public class BDParkgo extends SQLiteOpenHelper {

    String sqlScript = "DROP TABLE IF EXISTS [tb_registro_patentes];\n"+
                       "CREATE TABLE [tb_registro_patentes] (\n" +
                           "[id] VARCHAR(36) NOT NULL PRIMARY KEY,\n" +
                           "[id_cliente_ubicacion] INTEGER  NULL,\n"+
                           "[patente] VARCHAR(10)  NULL,\n" +
                           "[espacios] INTEGER NULL,\n" +
                           "[fecha_hora_in] TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,\n" +
                           "[rut_usuario_in] VARCHAR(12)  NULL,\n" +
                           "[maquina_in] VARCHAR(32)  NULL,\n" +
                           "[imagen_in] VARCHAR(255)  NULL,\n" +
                           "[enviado_in] INTEGER NULL,\n" +
                           "[fecha_hora_out] TIMESTAMP  NULL,\n" +
                           "[rut_usuario_out] VARCHAR(12)  NULL,\n" +
                           "[maquina_out] VARCHAR(32)  NULL,\n" +
                           "[enviado_out] INTEGER NULL,\n" +
                           "[minutos] INTEGER NULL,\n" +
                           "[precio] INTEGER NULL,\n" +
                           "[prepago] INTEGER NULL,\n" +
                           "[efectivo] INTEGER NULL,\n" +
                           "[latitud] VARCHAR(32)  NULL,\n" +
                           "[longitud] VARCHAR(32)  NULL,\n" +
                           "[comentario] VARCHAR(255)  NULL,\n" +
                           "[finalizado] INTEGER NULL\n" +
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_usuario];\n"+
                       "CREATE TABLE [tb_usuario] (\n" +
                           "[rut] VARCHAR(12)  NULL PRIMARY KEY,\n" +
                           "[nombre] VARCHAR(128)  NULL,\n" +
                           "[codigo] VARCHAR(10)  NULL,\n" +
                           "[clave] VARCHAR(10)  NULL,\n" +
                           "[id_cliente_ubicacion] INTEGER  NULL,\n"+
                           "[id_rol] INTEGER  NULL\n"+
                           ");\n" +
                       "DROP TABLE IF EXISTS [tb_usuario_ubicaciones]; \n"+
                        "CREATE TABLE [tb_usuario_ubicaciones] (\n" +
                        "[id] VARCHAR(36)  NULL PRIMARY KEY,\n" +
                        "[rut_usuario] VARCHAR(12)  NULL,\n" +
                        "[id_cliente_ubicacion] INTEGER NULL, \n"+
                        "[latitud] VARCHAR(32)  NULL, \n" +
                        "[longitud] VARCHAR(32)  NULL, \n" +
                        "[fecha_hora] TIMESTAMP  NULL,\n" +
                        "[enviado] INTEGER NULL "+
                        "); \n"+
                       "DROP TABLE IF EXISTS [tb_cliente];\n"+
                       "CREATE TABLE [tb_cliente] (\n"+
                           "[id] INTEGER  NOT NULL PRIMARY KEY,\n"+
                           "[rut] VARCHAR(12)  NULL,\n"+
                           "[razon_social] VARCHAR(255)  NULL,\n"+
                           "[logo] VARCHAR(36)  NULL, \n"+
                           "[email] VARCHAR(128)  NULL \n"+
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_cliente_ubicaciones];\n"+
                       "CREATE TABLE [tb_cliente_ubicaciones] (\n"+
                           "[id] INTEGER  NOT NULL PRIMARY KEY,\n"+
                           "[id_cliente] INTEGER  NULL,\n"+
                           "[descripcion] VARCHAR(255)  NULL,\n"+
                           "[id_comuna] INTEGER  NULL,\n"+
                           "[direccion] VARCHAR(255)  NULL,\n"+
                           "[latitud] FLOAT  NULL,\n"+
                           "[longitud] FLOAT  NULL,\n"+
                           "[tipo_cobro] INTEGER  NULL,\n"+
                           "[valor_minuto] INTEGER  NULL,\n"+
                           "[valor_tramo] INTEGER  NULL,\n"+
                           "[minutos_tramo] INTEGER  NULL,\n"+
                           "[minutos_gratis] INTEGER  NULL,\n"+
                           "[descripcion_tarifa] VARCHAR(128)  NULL\n"+
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_cliente_ubicaciones_horarios];\n"+
                       "CREATE TABLE [tb_cliente_ubicaciones_horarios] (\n" +
                           "[id] INTEGER  NOT NULL PRIMARY KEY,\n" +
                           "[id_cliente_ubicacion] INTEGER  NULL,\n" +
                           "[dia_desde] VARCHAR(10)  NULL,\n" +
                           "[hora_desde] TIME  NULL,\n" +
                           "[suma_dia] INTEGER  NULL,\n" +
                           "[dia_hasta] VARCHAR(10)  NULL,\n" +
                           "[hora_hasta] TIME  NULL\n" +
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_conductor];\n"+
                       "CREATE TABLE [tb_conductor] (\n" +
                           "[rut] VARCHAR(12) NULL,\n" +
                           "[nombre] VARCHAR(128)  NULL,\n" +
                           "[id_conductor_grupo] INTEGER NULL,\n"+
                           "[clave] INTEGER  NULL,\n"+
                           "[telefono] VARCHAR(64)  NULL,\n" +
                           "[email] VARCHAR(128)  NULL,\n" +
                           "[saldo] INTEGER  NULL,\n"+
                           "PRIMARY KEY ([rut],[id_conductor_grupo]) \n"+
                           ");\n" +
                       "DROP TABLE IF EXISTS [tb_conductor_grupo];\n"+
                       "CREATE TABLE [tb_conductor_grupo] (\n" +
                           "[id] INTEGER  NOT NULL PRIMARY KEY,\n"+
                           "[id_cliente] INTEGER  NULL,\n"+
                           "[descripcion] VARCHAR(128)  NULL,\n"+
                           "[envia_mail_ingreso] INTEGER  NULL,\n"+
                           "[envia_mail_retiro] INTEGER  NULL\n"+
                           ");\n" +
                       "DROP TABLE IF EXISTS [tb_conductor_grupo_ubicacion_descuento];\n"+
                       "CREATE TABLE [tb_conductor_grupo_ubicacion_descuento] (\n" +
                           "[id_conductor_grupo] INTEGER  NOT NULL,\n"+
                           "[id_cliente_ubicacion] INTEGER  NULL,\n"+
                           "[descuento] INTEGER  NULL,\n"+
                           "PRIMARY KEY ([id_conductor_grupo],[id_cliente_ubicacion]) \n"+
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_conductor_patentes];\n"+
                       "CREATE TABLE [tb_conductor_patentes] (\n" +
                           "[id] INTEGER  NOT NULL PRIMARY KEY,\n" +
                           "[rut_conductor] VARCHAR(12)  NULL,\n" +
                           "[patente] VARCHAR(10)  NULL\n" +
                           ");\n" +
                       "DROP TABLE IF EXISTS [tb_alertas];\n"+
                       "CREATE TABLE [tb_alertas] (\n" +
                           "[id] VARCHAR(36)  NULL PRIMARY KEY,\n" +
                           "[id_tipo_alerta] INTEGER  NULL,\n" +
                           "[id_cliente_ubicacion] INTEGER  NULL,\n" +
                           "[rut_usuario] VARCHAR(12)  NULL,\n" +
                           "[maquina] VARCHAR(32)  NULL,\n" +
                           "[comentario] VARCHAR(256)  NULL,\n" +
                           "[fecha_hora] TIMESTAMP  NULL,\n" +
                           "[enviado] INTEGER NULL\n"+
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_configuracion];\n"+
                       "CREATE TABLE [tb_configuracion] (\n" +
                           "[codigo] INTEGER  NULL,\n" +
                           "[seccion] VARCHAR(24)  NULL,\n" +
                           "[clave] VARCHAR(24)  NULL,\n" +
                           "[valor] VARCHAR(1024)  NULL\n" +
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_recaudacion_retiro];\n"+
                       "CREATE TABLE [tb_recaudacion_retiro] (\n" +
                           "[id] VARCHAR(36)  PRIMARY KEY NULL,\n" +
                           "[id_cliente_ubicacion] INTEGER  NULL,\n" +
                           "[rut_usuario_operador] VARCHAR(12)  NULL,\n" +
                           "[maquina] VARCHAR(32)  NULL,\n" +
                           "[rut_usuario_retiro] VARCHAR(12)  NULL,\n" +
                           "[fecha_recaudacion] DATE  NULL,\n" +
                           "[monto] INTEGER  NULL,\n" +
                           "[enviado] INTEGER  NULL\n" +
                           ");\n"+
                       "DROP TABLE IF EXISTS [tb_rol];\n"+
                       "CREATE TABLE [tb_rol] (\n" +
                           "[id] INTEGER  NOT NULL PRIMARY KEY,\n" +
                           "[nombre] VARCHAR(128)  NULL,\n" +
                           "[ing_web] INTEGER  NULL,\n" +
                           "[ing_web_pagina_inicio] VARCHAR(128)  NULL,\n" +
                           "[ing_mobile] INTEGER  NULL,\n" +
                           "[es_recaudador] INTEGER  NULL\n" +
                       ");\n"+
                       "DROP TABLE IF EXISTS [tb_etiquetas];\n"+
                       "CREATE TABLE [tb_etiquetas] (\n" +
                           "[num_etiqueta_actual] INTEGER  NOT NULL PRIMARY KEY\n" +
                           ");";


    public BDParkgo(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            String[] queries = sqlScript.split(";");
            for(String query : queries){
                db.execSQL(query);
            }
        }catch (Exception e) { e.getMessage(); }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            String[] queries = sqlScript.split(";");
            for(String query : queries){
                db.execSQL(query);
            }
        }catch (Exception e) { e.getMessage(); }
    }

}
