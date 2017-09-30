package cl.suministra.parkgo;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class Menu extends AppCompatActivity {

    private Button BTN_IngresoPatente;
    private Button BTN_RetiroPatente;
    private Button BTN_ListaPatente;

    private TextView TV_Num_Serie_Equipo;
    private TextView TV_Usuario_Nombre;
    private TextView TV_Cliente_Razon_Social;
    private TextView TV_Usuario_Ubicacion;
    private TextView TV_Usuario_Ubicacion_Dir;
    private TextView TV_Usuario_Ubicacion_Horario;

    private boolean horario_definido  = false;
    private int horario_suma_dia      = 0;
    private String nombre_dia_actual  = "";
    private String horario_dia_desde  = "";
    private String horario_hora_desde = "";
    private String horario_dia_hasta  = "";
    private String horario_hora_hasta = "";
    private Date   fechahora_actual   = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        inicio();
    }

    private void inicio(){

        Date fecha_actual = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.setTitle("Menú");

        TV_Num_Serie_Equipo       = (TextView) findViewById(R.id.TV_Num_Serie_Equipo);
        TV_Num_Serie_Equipo.setText(AppHelper.getSerialNum());
        TV_Usuario_Nombre       = (TextView) findViewById(R.id.TV_Usuario_Nombre);
        TV_Cliente_Razon_Social = (TextView) findViewById(R.id.TV_Cliente_Razon_Social);
        TV_Usuario_Ubicacion    = (TextView) findViewById(R.id.TV_Usuario_Ubicacion);
        TV_Usuario_Ubicacion_Dir= (TextView) findViewById(R.id.TV_Usuario_Ubicacion_Dir);
        TV_Usuario_Ubicacion_Horario= (TextView) findViewById(R.id.TV_Usuario_Ubicacion_Horario);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("usuario_nombre")) {
                TV_Usuario_Nombre.setText(extras.getString("usuario_nombre").toUpperCase());
            }
            if (extras.containsKey("cliente_razon_social")) {
                TV_Cliente_Razon_Social.setText(extras.getString("cliente_razon_social").toUpperCase());
            }
            if (extras.containsKey("usuario_ubicacion")) {
                TV_Usuario_Ubicacion.setText(extras.getString("usuario_ubicacion").toUpperCase());
            }
            if (extras.containsKey("usuario_ubicacion_dir")) {
                TV_Usuario_Ubicacion_Dir.setText(extras.getString("usuario_ubicacion_dir").toUpperCase());
            }
        }

        nombre_dia_actual = Util.nombreDiaSemana(AppHelper.fechaHoraFormat.format(fechahora_actual));

        String[] args = new String[] {String.valueOf(AppHelper.getUbicacion_id()),nombre_dia_actual};
        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT suma_dia, dia_desde, hora_desde, dia_hasta, hora_hasta " +
                "FROM tb_cliente_ubicaciones_horarios "+
                "WHERE id_cliente_ubicacion =? AND dia_desde =? ", args);
        if (c.moveToFirst()) {
            horario_definido   = true;
            horario_suma_dia   = c.getInt(0);
            horario_dia_desde  = c.getString(1);
            horario_hora_desde = c.getString(2);
            horario_dia_hasta  = c.getString(3);
            horario_hora_hasta = c.getString(4);
            TV_Usuario_Ubicacion_Horario.setText(horario_dia_desde+" desde las "+horario_hora_desde+ " hrs. hasta "+horario_dia_hasta+" a las "+horario_hora_hasta+" hrs.");
        }else{
            horario_definido   = false;
            TV_Usuario_Ubicacion_Horario.setText("Sin definir");
        }
        c.close();

        BTN_IngresoPatente = (Button) findViewById(R.id.BTN_IngresoPatente);
        BTN_IngresoPatente.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){

                if(horario_definido){
                    try {
                        Date fechahora_desde = AppHelper.fechaHoraFormat.parse(AppHelper.fechaHoraFormat.format(fechahora_actual).substring(0, 10) + " " + horario_hora_desde);
                        Date fechahora_hasta = AppHelper.fechaHoraFormat.parse(AppHelper.fechaHoraFormat.format(fechahora_actual).substring(0, 10) + " " + horario_hora_hasta);
                        fechahora_hasta = new Date(fechahora_hasta.getTime() + TimeUnit.DAYS.toMillis(horario_suma_dia));
                        //Si la fechahora_acual no se encuentra entre el horario fijado. Entonces no puede ingresar patentes.
                        if (fechahora_actual.before(fechahora_desde) || fechahora_actual.after(fechahora_hasta) ) {
                            Util.alertDialog(Menu.this, "Menu", "No puede ingresar vehículos. \nEl horario fijado para hoy "+horario_dia_desde+
                                    " es a partir de las "+horario_hora_desde+" hrs hasta el "+horario_dia_hasta+" a las "+horario_hora_hasta+" hrs.");
                        }else{
                            inicioIngresoPatente();
                        }
                    } catch (ParseException e) {
                        Util.alertDialog(Menu.this, "ParseException Menu", e.getMessage());
                    }
                }else{
                    Util.alertDialog(Menu.this, "Menu", "No puede ingresar vehículos. \nNo existe un horario definido para hoy "+nombre_dia_actual);
                }


            }

        });

        BTN_RetiroPatente = (Button) findViewById(R.id.BTN_RetiroPatente);
        BTN_RetiroPatente.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                inicioRetiroPatente();
            }

        });

        BTN_ListaPatente = (Button) findViewById(R.id.BTN_ListaPatente);
        BTN_ListaPatente.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                inicioListaPatente();
            }

        });

    }

    private void inicioIngresoPatente(){
        Intent intent = new Intent(this, IngresoPatente.class);
        startActivity(intent);
    }

    private void inicioRetiroPatente(){
        Intent intent = new Intent(this, RetiroPatente.class);
        startActivity(intent);
    }

    private void inicioListaPatente(){
        Intent intent = new Intent(this, ListaPatente.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

}
