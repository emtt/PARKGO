package cl.suministra.parkgo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vpos.apipackage.PosApiHelper;
import vpos.apipackage.PrintInitException;

/**
 * Created by LENOVO on 08-06-2018.
 */

public class Print_Thread extends Thread {

        final int PRINT_INGRESO = 0;
        final int PRINT_RETIRO  = 1;
        final int PRINT_ESTACIONADOS = 2;
        final int PRINT_RECAUDACION  = 3;

        private final static int ENABLE_RG = 10;
        private final static int DISABLE_RG = 11;

        int ret = -1;
        private boolean m_bThreadFinished = true;

        private int RESULT_CODE = 0;

        private int BatteryV;
        SharedPreferences sp;
        int IsWorking = 0;
        PosApiHelper posApiHelper = PosApiHelper.getInstance();

        String content = "1234567890";
        int type;

        //VARIABLES DE IMPRESIÓN ETIQUETAS.
        String patente;
        int espacios;
        String fecha_hora_in;
        String fecha_hora_out;
        int minutos;
        int minutos_gratis;
        int precio;
        int porcent_descuento;
        List<ListaPatente.PatentesPendiente> patentesList = new ArrayList<ListaPatente.PatentesPendiente>();
        String fecha_recaudacion;
        String rut_usuario_retiro;
        String nombre_usuario_retiro;
        int monto;

        //CONSTRUCTOR PARA IMPRIMIR TICKET DE INGRESO.
        public Print_Thread(int type, String patente, int espacios, String fecha_hora_in)
        {
            this.patente  = patente;
            this.espacios = espacios;
            this.fecha_hora_in = fecha_hora_in;
            this.type = type;
        }

        //CONSTRUCTOR PARA IMPRIMIR TICKET DE SALIDA O TICKET DE PAGO DEUDA.
        public Print_Thread(int type, String patente, int espacios, String fecha_hora_in,
                            String fecha_hora_out, int minutos, int minutos_gratis, int precio,
                            int porcent_descuento)
        {
            this.patente  = patente;
            this.espacios = espacios;
            this.fecha_hora_in = fecha_hora_in;
            this.fecha_hora_out = fecha_hora_out;
            this.minutos = minutos;
            this.minutos_gratis = minutos_gratis;
            this.precio  = precio;
            this.porcent_descuento = porcent_descuento;
            this.type = type;
        }


        //CONSTRUCTOR PARA IMPRIMIR RECAUDACION.
        public Print_Thread(int type, String fecha_recaudacion, String rut_usuario_retiro, String nombre_usuario_retiro, int monto)
        {
            this.fecha_recaudacion  = fecha_recaudacion;
            this.rut_usuario_retiro = rut_usuario_retiro;
            this.nombre_usuario_retiro = nombre_usuario_retiro;
            this.monto = monto;
            this.type = type;
        }


        //CONSTRUCTOR PARA IMPRIMIR LISTADO DE VEHICULOS ESTACIONADOS.
        public Print_Thread(int type, List<ListaPatente.PatentesPendiente> patentesList)
        {
            this.patentesList  = patentesList;
            this.type = type;
        }

        public int getRESULT_CODE() {
            return RESULT_CODE;
        }

        public boolean isThreadFinished() {
            return m_bThreadFinished;
        }

        public void run() {
            Log.d(AppHelper.LOG_PRINT, "Print_Thread[ run ] run() begin");
            Message msg = Message.obtain();
            Message msg1 = new Message();

            synchronized (this) {

                m_bThreadFinished = false;
                try {
                    ret = posApiHelper.PrintInit();
                } catch (PrintInitException e) {
                    e.printStackTrace();
                    int initRet = e.getExceptionCode();
                    Log.d(AppHelper.LOG_PRINT, "initRer : " + initRet);
                }

                Log.d(AppHelper.LOG_PRINT, "init code:" + ret);

                ret = AppHelper.print_densidad;
                Log.d(AppHelper.LOG_PRINT, "AppHelper.print_densidad: " + ret);

                posApiHelper.PrintSetGray(ret);

                //posApiHelper.PrintSetVoltage(BatteryV * 2 / 100);

                ret = posApiHelper.PrintCheckStatus();
                if (ret == -1) {
                    RESULT_CODE = -1;
                    Log.d(AppHelper.LOG_PRINT, "Lib_PrnCheckStatus fail, ret = " + ret);
                    SendMsg("Error, no hay papel verifique");
                    m_bThreadFinished = true;
                    return;
                } else if (ret == -2) {
                    RESULT_CODE = -1;
                    Log.d(AppHelper.LOG_PRINT, "Lib_PrnCheckStatus fail, ret = " + ret);
                    SendMsg("Error, la temperatura de la impresora es muy alta");
                    m_bThreadFinished = true;
                    return;
                } else if (ret == -3) {
                    RESULT_CODE = -1;
                    Log.d(AppHelper.LOG_PRINT, "voltage = " + (BatteryV * 2));
                    SendMsg("Queda poca batería :" + (BatteryV * 2));
                    m_bThreadFinished = true;
                    return;
                }/* else if (voltage_level < 5) {
                    RESULT_CODE = -1;
                    Log.e(tag, "voltage_level = " + voltage_level);
                    SendMsg("Battery capacity less : " + voltage_level);
                    m_bThreadFinished = true;
                    return;
                }*/ else {
                    RESULT_CODE = 0;
                }

                String lb_fechahora_actual  = Util.formateaLineaEtiqueta("Fecha hora: "+ AppHelper.fechaHoraFormat.format(new Date()));
                String lb_ubicacion         = Util.formateaLineaEtiqueta("Zona: "+AppHelper.getUbicacion_nombre());
                String lb_operador          = Util.formateaLineaEtiqueta("Operador: "+AppHelper.getUsuario_codigo()+" "+AppHelper.getUsuario_nombre());
                String lb_patente           = Util.formateaLineaEtiqueta("Patente: "+patente);
                String lb_espacios          = Util.formateaLineaEtiqueta("Espacios: "+espacios);
                String lb_fecha_hora_in     = Util.formateaLineaEtiqueta("Ingreso: "+fecha_hora_in);
                String lb_fecha_hora_out    = Util.formateaLineaEtiqueta("Retiro: "+fecha_hora_out);
                String lb_tiempo            = Util.formateaLineaEtiqueta("Tiempo: "+String.format("%,d", minutos).replace(",",".")+" min");
                String lb_gratis            = Util.formateaLineaEtiqueta("Gratis: "+String.format("%,d", minutos_gratis).replace(",",".")+" min");
                String lb_total             = Util.formateaLineaEtiqueta("Total: $"+String.format("%,d", precio).replace(",","."));
                String lb_descuento         = Util.formateaLineaEtiqueta("Descuento: "+String.format("%,d", porcent_descuento).replace(",",".")+"%");

                String lb_fecha_recaudacion = Util.formateaLineaEtiqueta("Fecha: "+fecha_recaudacion);
                String lb_maquina           = Util.formateaLineaEtiqueta("Maquina: "+AppHelper.getSerialNum());
                String lb_recaudador        = Util.formateaLineaEtiqueta("Recaudador:"+rut_usuario_retiro+" "+nombre_usuario_retiro);
                String lb_monto             = Util.formateaLineaEtiqueta("Monto: "+String.format("%,d", monto).replace(",","."));
                String lb_firma             = Util.formateaLineaEtiqueta("Firma:__________________________");

                switch (type) {

                    case PRINT_INGRESO:
                        //SendMsg("Imprimiendo...");
                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);

                        posApiHelper.PrintSetFont((byte) 24, (byte) 16, (byte) 0x00);
                        posApiHelper.PrintStr(AppHelper.getVoucher_ingreso()+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(AppHelper.getDescripcion_tarifa()+"\n\n");
                        posApiHelper.PrintStr(lb_ubicacion+"\n");
                        posApiHelper.PrintStr(lb_operador+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x33);
                        posApiHelper.PrintStr(lb_patente+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(lb_espacios+"\n");
                        posApiHelper.PrintStr(lb_fecha_hora_in+"\n");

                        //posApiHelper.PrintBarcode(patente, 180, 180, BarcodeFormat.QR_CODE);
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");

                        //SendMsg("Imprimiendo... ");
                        ret = posApiHelper.PrintStart();

                        msg1.what = ENABLE_RG;
                        handler.sendMessage(msg1);

                        Log.d(AppHelper.LOG_PRINT, "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("liuhao", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("Error, no hay papel verifique");
                            } else if (ret == -2) {
                                SendMsg("Error, la temperatura de la impresora es muy alta");
                            } else if (ret == -3) {
                                SendMsg("Queda poca batería");
                            } else {
                                SendMsg("Impresión fallida, intente nuevamente");
                            }
                        } else {
                            RESULT_CODE = 0;
                            //SendMsg("Impresión terminada");
                        }

                        break;

                    case PRINT_RETIRO:

                        //SendMsg("Imprimiendo...");
                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);

                        posApiHelper.PrintSetFont((byte) 24, (byte) 16, (byte) 0x00);
                        posApiHelper.PrintStr(AppHelper.getVoucher_salida()+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(AppHelper.getDescripcion_tarifa()+"\n\n");
                        posApiHelper.PrintStr(lb_ubicacion+"\n");
                        posApiHelper.PrintStr(lb_operador+"\n");
                        posApiHelper.PrintStr(lb_patente+"\n");
                        posApiHelper.PrintStr(lb_espacios+"\n");
                        posApiHelper.PrintStr(lb_fecha_hora_in+"\n");
                        posApiHelper.PrintStr(lb_fecha_hora_out+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x33);
                        posApiHelper.PrintStr(lb_tiempo+"\n");
                        posApiHelper.PrintStr(lb_total+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(lb_gratis+"\n");
                        posApiHelper.PrintStr(lb_descuento+"\n");

                        //posApiHelper.PrintBarcode(patente, 180, 180, BarcodeFormat.QR_CODE);
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");

                        //SendMsg("Imprimiendo... ");
                        ret = posApiHelper.PrintStart();

                        msg1.what = ENABLE_RG;
                        handler.sendMessage(msg1);

                        Log.d(AppHelper.LOG_PRINT, "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("liuhao", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("Error, no hay papel verifique");
                            } else if (ret == -2) {
                                SendMsg("Error, la temperatura de la impresora es muy alta");
                            } else if (ret == -3) {
                                SendMsg("Queda poca batería");
                            } else {
                                SendMsg("Impresión fallida, intente nuevamente");
                            }
                        } else {
                            RESULT_CODE = 0;
                            //SendMsg("Impresión terminada");
                        }

                        break;

                    case PRINT_ESTACIONADOS:

                        //SendMsg("Imprimiendo...");

                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);

                        posApiHelper.PrintSetFont((byte) 24, (byte) 16, (byte) 0x00);
                        posApiHelper.PrintStr(AppHelper.getVoucher_estacionados()+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(lb_fechahora_actual+"\n");
                        posApiHelper.PrintStr(lb_ubicacion+"\n");
                        posApiHelper.PrintStr(lb_operador+"\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 16, (byte) 0x00);
                        for (int i = 0; i < patentesList.size(); i++) {
                            posApiHelper.PrintStr(patentesList.get(i).patente +" "+ patentesList.get(i).fecha_in+"\n");
                        }
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");

                        //SendMsg("Imprimiendo... ");
                        ret = posApiHelper.PrintStart();

                        msg1.what = ENABLE_RG;
                        handler.sendMessage(msg1);

                        Log.d(AppHelper.LOG_PRINT, "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("liuhao", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("Error, no hay papel verifique");
                            } else if (ret == -2) {
                                SendMsg("Error, la temperatura de la impresora es muy alta");
                            } else if (ret == -3) {
                                SendMsg("Queda poca batería");
                            } else {
                                SendMsg("Impresión fallida, intente nuevamente");
                            }
                        } else {
                            RESULT_CODE = 0;
                            //SendMsg("Impresión terminada");
                        }

                        break;
                    case PRINT_RECAUDACION:

                        //SendMsg("Imprimiendo...");

                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);

                        posApiHelper.PrintSetFont((byte) 24, (byte) 16, (byte) 0x00);
                        posApiHelper.PrintStr(AppHelper.getVoucher_retiro_recaudacion()+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(lb_fecha_recaudacion+"\n");
                        posApiHelper.PrintStr(lb_ubicacion+"\n");
                        posApiHelper.PrintStr(lb_maquina+"\n");
                        posApiHelper.PrintStr(lb_recaudador+"\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x33);
                        posApiHelper.PrintStr(lb_monto+"\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                        posApiHelper.PrintStr(lb_firma+"\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");
                        posApiHelper.PrintStr("\n");

                        //SendMsg("Imprimiendo... ");
                        ret = posApiHelper.PrintStart();

                        msg1.what = ENABLE_RG;
                        handler.sendMessage(msg1);

                        Log.d(AppHelper.LOG_PRINT, "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("liuhao", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("Error, no hay papel verifique");
                            } else if (ret == -2) {
                                SendMsg("Error, la temperatura de la impresora es muy alta");
                            } else if (ret == -3) {
                                SendMsg("Queda poca batería");
                            } else {
                                SendMsg("Impresión fallida, intente nuevamente");
                            }
                        } else {
                            RESULT_CODE = 0;
                            //SendMsg("Impresión terminada");
                        }

                        break;

                    default:
                        break;
                }
                m_bThreadFinished = true;

                Log.d(AppHelper.LOG_PRINT, "goToSleep2...");
            }
        }

        public void SendMsg(String strInfo) {
            Message msg = new Message();
            Bundle b = new Bundle();
            b.putString("MSG", strInfo);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case DISABLE_RG:
                        IsWorking = 1;
                        break;

                    case ENABLE_RG:
                        IsWorking = 0;
                        break;
                    default:
                        Bundle b = msg.getData();
                        String strInfo = b.getString("MSG");
                        Toast.makeText(App.context, strInfo, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

}


