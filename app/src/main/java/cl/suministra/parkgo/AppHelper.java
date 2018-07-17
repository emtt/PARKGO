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
    private static int db_version    = 1;
    private static BDParkgo parkgoDB;
    private static SQLiteDatabase SQLiteParkgo;

    public static int timeout = 5000;

    private static String serial_no  = "";
    private static String usuario_rut= "";
    private static String usuario_nombre = "";
    private static String usuario_codigo = "";

    public static DateFormat fechaHoraFormat   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static DateFormat fechaHoraFormatID = new SimpleDateFormat("yyyyMMddHHmmss");
    public static DateFormat fechaFormat       = new SimpleDateFormat("dd-MM-yyyy",Locale.US);
    public static DateFormat horaFormat        = new SimpleDateFormat("HH:mm:ss",Locale.US);

    public static int cliente_id     = 0;
    public static int ubicacion_id   = 0;

    public static String ubicacion_nombre = "";
    public static int tipo_cobro     = 0;
    public static int valor_minuto   = 0;
    public static int valor_tramo    = 0;
    public static int minutos_tramo  = 0;
    public static int minutos_gratis = 0;
    public static String descripcion_tarifa = "";

    public static String voucher_ingreso = "";
    public static String voucher_salida  = "";
    public static String voucher_estacionados = "";
    public static String voucher_retiro_recaudacion  = "";

    public static int voucher_largo_linea = 32;
    public static int voucher_rollo_max  = 0;
    public static int voucher_rollo_alert= 0;

    public static String url_restful = "";
    public static String pagina_test = "";

    public static int minutos_diff   = 0; //diferencia máxima de minutos posible entre hora máquina y hora servidor. (se configura en tabla configuración);
    public static String LOG_TAG     = "parkgo_log";
    public static String LOG_PRINT     = "parkgo_printer";
    public static String LOG_TST     = "parkgo_tst";

    public static int imagen_calidad   = 0;
    public static double imagen_max_mb = 0;

    public static int print_densidad = 5;


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

    public static String getUsuario_codigo() {
        return usuario_codigo;
    }

    public static void setUsuario_codigo(String usuario_codigo) {
        AppHelper.usuario_codigo = usuario_codigo;
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

    public static int getTipo_cobro() {
        return tipo_cobro;
    }

    public static void setTipo_cobro(int tipo_cobro) {
        AppHelper.tipo_cobro = tipo_cobro;
    }

    public static int getValor_minuto() {
        return valor_minuto;
    }

    public static void setValor_minuto(int valor_minuto) {
        AppHelper.valor_minuto = valor_minuto;
    }

    public static int getValor_tramo() {
        return valor_tramo;
    }

    public static void setValor_tramo(int valor_tramo) {
        AppHelper.valor_tramo = valor_tramo;
    }

    public static int getMinutos_tramo() {
        return minutos_tramo;
    }

    public static void setMinutos_tramo(int minutos_tramo) {
        AppHelper.minutos_tramo = minutos_tramo;
    }

    public static int getMinutos_gratis() {
        return minutos_gratis;
    }

    public static void setMinutos_gratis(int minutos_gratis) {
        AppHelper.minutos_gratis = minutos_gratis;
    }

    public static String getDescripcion_tarifa() {
        return descripcion_tarifa;
    }

    public static void setDescripcion_tarifa(String descripcion_tarifa) {
        AppHelper.descripcion_tarifa = descripcion_tarifa;
    }

    public static String getVoucher_ingreso() {
        return voucher_ingreso;
    }

    public static void setVoucher_ingreso(String voucher_ingreso) {
        AppHelper.voucher_ingreso = voucher_ingreso;
    }

    public static String getVoucher_salida() {
        return voucher_salida;
    }

    public static void setVoucher_salida(String voucher_salida) {
        AppHelper.voucher_salida = voucher_salida;
    }

    public static String getVoucher_estacionados() {
        return voucher_estacionados;
    }

    public static void setVoucher_estacionados(String voucher_estacionados) {
        AppHelper.voucher_estacionados = voucher_estacionados;
    }

    public static String getVoucher_retiro_recaudacion() {
        return voucher_retiro_recaudacion;
    }

    public static void setVoucher_retiro_recaudacion(String voucher_retiro_recaudacion) {
        AppHelper.voucher_retiro_recaudacion = voucher_retiro_recaudacion;
    }

    public static int getVoucher_rollo_max() {
        return voucher_rollo_max;
    }

    public static void setVoucher_rollo_max(int voucher_rollo_max) {
        AppHelper.voucher_rollo_max = voucher_rollo_max;
    }

    public static int getVoucher_rollo_alert() {
        return voucher_rollo_alert;
    }

    public static void setVoucher_rollo_alert(int voucher_rollo_alert) {
        AppHelper.voucher_rollo_alert = voucher_rollo_alert;
    }

    public static int getVoucher_largo_linea() {
        return voucher_largo_linea;
    }

    public static void setVoucher_largo_linea(int voucher_largo_linea) {
        AppHelper.voucher_largo_linea = voucher_largo_linea;
    }

    public static void initSerialNum(Context context){
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial_no = (String) get.invoke(c, "ro.serialno");
            serial_no = serial_no.substring(0,8);
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

    public static int getMinutos_diff() {
        return minutos_diff;
    }

    public static void setMinutos_diff(int minutos_diff) {
        AppHelper.minutos_diff = minutos_diff;
    }

    public static int getImagen_calidad() {
        return imagen_calidad;
    }

    public static void setImagen_calidad(int imagen_calidad) {
        AppHelper.imagen_calidad = imagen_calidad;
    }

    public static double getImagen_max_mb() {
        return imagen_max_mb;
    }

    public static void setImagen_max_mb(double imagen_max_mb) {
        AppHelper.imagen_max_mb = imagen_max_mb;
    }

    //retorna directorio de almacenamiento para las imagenes de la camara.
    public static File getImageDir(Context context){
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        return storageDir;

    }


}