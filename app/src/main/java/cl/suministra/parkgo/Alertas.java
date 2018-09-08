package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by LENOVO on 04-10-2017.
 */

public class Alertas extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private EditText EDT_Comentario;
    private Button BTN_AlertaSeguridad;
    private Button BTN_AlertaCarabineros;
    private Button BTN_AlertaBomberos;
    private Button BTN_AlertaAmbulancia;
    private ProgressDialog esperaDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_alertas);
        inicio();

    }

    private void inicio(){

        EDT_Comentario = (EditText) findViewById(R.id.EDT_Comentario);
        EDT_Comentario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_Comentario);
                    label.setText(EDT_Comentario.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_Comentario.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_Comentario);
                label.setText("");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_Comentario);
                    label.setText("");
                }
            }
        });

        BTN_AlertaSeguridad   = (Button) findViewById(R.id.BTN_AlertaSeguridad);
        BTN_AlertaCarabineros = (Button) findViewById(R.id.BTN_AlertaCarabineros);
        BTN_AlertaBomberos    = (Button) findViewById(R.id.BTN_AlertaBomberos);
        BTN_AlertaAmbulancia  = (Button) findViewById(R.id.BTN_AlertaAmbulancia);

        BTN_AlertaSeguridad.setOnClickListener(this);
        BTN_AlertaCarabineros.setOnClickListener(this);
        BTN_AlertaBomberos.setOnClickListener(this);
        BTN_AlertaAmbulancia.setOnClickListener(this);

        BTN_AlertaSeguridad.setOnLongClickListener(this);
        BTN_AlertaCarabineros.setOnLongClickListener(this);
        BTN_AlertaBomberos.setOnLongClickListener(this);
        BTN_AlertaAmbulancia.setOnLongClickListener(this);

    }

    @Override
    public void onClick(View v) {
        Toast.makeText(getApplicationContext(),"Mantenga presionado para enviar alerta...",Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onLongClick(View v){

        switch (v.getId()) {
            case R.id.BTN_AlertaSeguridad:
                procesoAlerta(2, String.valueOf(EDT_Comentario.getText()));
                break;
            case R.id.BTN_AlertaCarabineros:
                procesoAlerta(3, String.valueOf(EDT_Comentario.getText()));
                break;
            case R.id.BTN_AlertaAmbulancia:
                procesoAlerta(4, String.valueOf(EDT_Comentario.getText()));
                break;
            case R.id.BTN_AlertaBomberos:
                procesoAlerta(5, String.valueOf(EDT_Comentario.getText()));
                break;
        }
        return false;
    }

    private void procesoAlerta(final int id_tipo_alerta, final String comentario ){
        esperaDialog = ProgressDialog.show(Alertas.this, "", "Registrando alerta...", true);
        esperaDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                boolean resultado = AppCRUD.registrarAlerta(Alertas.this, id_tipo_alerta, comentario);
                if (resultado){
                    switch (id_tipo_alerta){
                        case 2:
                            Util.alertDialog(Alertas.this,"Registro Alerta","Alerta Seguridad registrada correctamente");
                            break;
                        case 3:
                            Util.alertDialog(Alertas.this,"Registro Alerta","Alerta Carabineros registrada correctamente");
                            break;
                        case 4:
                            Util.alertDialog(Alertas.this,"Registro Alerta","Alerta Ambulancia registrada correctamente");
                            break;
                        case 5:
                            Util.alertDialog(Alertas.this,"Registro Alerta","Alerta Bomberos registrada correctamente");
                            break;
                    }
                }
                esperaDialog.dismiss();
            }
        }, 2000);
        EDT_Comentario.setText("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Write your logic here
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
