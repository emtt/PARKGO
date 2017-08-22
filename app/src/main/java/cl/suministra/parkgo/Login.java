package cl.suministra.parkgo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Login extends AppCompatActivity {

    private EditText EDT_UsuarioCodigo;
    private EditText EDT_UsuarioClave;
    private Button BTN_Login;
    private String UsuarioCodigo;
    private String UsuarioClave;

    private ProgressDialog esperaDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppHelper.initParkgoDB(this);
        AppHelper.initSerialNum(this);
        init();
    }

    public void init(){

        EDT_UsuarioCodigo = (EditText) findViewById(R.id.EDT_UsuarioCodigo);
        EDT_UsuarioCodigo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioCodigo);
                    label.setText(EDT_UsuarioCodigo.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_UsuarioCodigo.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_UsuarioCodigo);
                label.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioCodigo);
                    label.setText("");
                }
            }
        });


        EDT_UsuarioClave = (EditText) findViewById(R.id.EDT_UsuarioClave);
        EDT_UsuarioClave.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioClave);
                    label.setText(EDT_UsuarioClave.getHint());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        EDT_UsuarioClave.setBackground(getDrawable(R.drawable.text_border_selector));
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.MSJ_UsuarioClave);
                label.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.LB_UsuarioClave);
                    label.setText("");
                }
            }
        });

        EDT_UsuarioCodigo.setText("admin");
        EDT_UsuarioClave.setText("admin");

        BTN_Login = (Button) findViewById(R.id.BTN_Login);
        BTN_Login.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick (View v){
                loginUsuario();
            }

        });

    }

    private void loginUsuario(){
        esperaDialog = ProgressDialog.show(this, "", "Autenticando...", true);
        esperaDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                procesoLoginUsuario();
                esperaDialog.dismiss();
            }
        }, 1000);
    }

    private void procesoLoginUsuario() {

        UsuarioCodigo = EDT_UsuarioCodigo.getText().toString();
        if (UsuarioCodigo == null || UsuarioCodigo.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_UsuarioCodigo);
            view.setText("Ingrese " + EDT_UsuarioCodigo.getHint());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_UsuarioCodigo.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return;
        }

        UsuarioClave = EDT_UsuarioClave.getText().toString();
        if (UsuarioClave == null || UsuarioClave.isEmpty()) {
            TextView view = (TextView) findViewById(R.id.MSJ_UsuarioClave);
            view.setText("Ingrese " + EDT_UsuarioClave.getHint());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EDT_UsuarioClave.setBackground(getDrawable(R.drawable.text_border_error));
            }
            return;
        }

        Cursor c;
        try {
            String[] args = new String[]{UsuarioCodigo, UsuarioClave};

            String qry = "SELECT tu.rut AS rut_usuario, tu.nombre AS nombre_usuario, tc.razon_social AS razon_social,\n" +
                                "tcu.descripcion AS ubicacion, tcu.direccion AS ubicacion_dir\n" +
                          "FROM tb_usuario tu\n" +
                          "LEFT JOIN tb_cliente_ubicaciones tcu ON tcu.id = tu.id_cliente_ubicacion\n" +
                          "LEFT JOIN tb_cliente tc ON tc.id = tcu.id_cliente\n" +
                          "WHERE tu.codigo =? AND tu.clave =? ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {
                String rs_usuario_rut          = c.getString(0);
                AppHelper.setUsuario_rut(rs_usuario_rut);
                String rs_usuario_nombre       = c.getString(1);
                String rs_cliente_razon_social = c.getString(2);
                String rs_usuario_ubicacion    = c.getString(3);
                String rs_usuario_ubicacion_dir= c.getString(4);

                Intent intent = new Intent(this, Menu.class);
                intent.putExtra("usuario_nombre", rs_usuario_nombre);
                intent.putExtra("cliente_razon_social", rs_cliente_razon_social);
                intent.putExtra("usuario_ubicacion", rs_usuario_ubicacion);
                intent.putExtra("usuario_ubicacion_dir", rs_usuario_ubicacion_dir);
                startActivity(intent);

            } else {
                Toast.makeText(getApplicationContext(), "Usuario y clave ingresados no existe, sincronize y verifique", Toast.LENGTH_LONG).show();
            }
            c.close();

        } catch (SQLException e) {Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show(); }
    }


}
