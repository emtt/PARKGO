package cl.suministra.parkgo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Menu extends AppCompatActivity {

    private Button BTN_IngresoPatente;
    private Button BTN_RetiroPatente;
    private Button BTN_ListaPatente;


    private TextView TV_Num_Serie_Equipo;
    private TextView TV_Usuario_Nombre;
    private TextView TV_Cliente_Razon_Social;
    private TextView TV_Usuario_Ubicacion;
    private TextView TV_Usuario_Ubicacion_Dir;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        inicio();
    }

    private void inicio(){

        Date fecha_actual = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.setTitle("PARKGO "+dateFormat.format(fecha_actual));

        Toast.makeText(getApplicationContext(), AppHelper.getUsuario_rut(), Toast.LENGTH_LONG).show();


        TV_Num_Serie_Equipo       = (TextView) findViewById(R.id.TV_Num_Serie_Equipo);
        TV_Num_Serie_Equipo.setText("N° Serie:    "+AppHelper.getSerialNum());
        TV_Usuario_Nombre       = (TextView) findViewById(R.id.TV_Usuario_Nombre);
        TV_Cliente_Razon_Social = (TextView) findViewById(R.id.TV_Cliente_Razon_Social);
        TV_Usuario_Ubicacion    = (TextView) findViewById(R.id.TV_Usuario_Ubicacion);
        TV_Usuario_Ubicacion_Dir= (TextView) findViewById(R.id.TV_Usuario_Ubicacion_Dir);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("usuario_nombre")) {
                TV_Usuario_Nombre.setText("Usuario:     "+extras.getString("usuario_nombre").toUpperCase());
            }
            if (extras.containsKey("cliente_razon_social")) {
                TV_Cliente_Razon_Social.setText("Empresa:   "+extras.getString("cliente_razon_social").toUpperCase());
            }
            if (extras.containsKey("usuario_ubicacion")) {
                TV_Usuario_Ubicacion.setText("Ubicación: "+extras.getString("usuario_ubicacion").toUpperCase());
            }
            if (extras.containsKey("usuario_ubicacion_dir")) {
                TV_Usuario_Ubicacion_Dir.setText("Dirección:  "+extras.getString("usuario_ubicacion_dir").toUpperCase());
            }
        }

        BTN_IngresoPatente = (Button) findViewById(R.id.BTN_IngresoPatente);
        BTN_IngresoPatente.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                inicioIngresoPatente();
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


}
