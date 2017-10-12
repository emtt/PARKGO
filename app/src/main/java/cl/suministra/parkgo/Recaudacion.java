package cl.suministra.parkgo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Recaudacion extends AppCompatActivity implements View.OnClickListener {

    private EditText EDT_FechaRecaudacion;
    private TextView TV_Operador;
    private TextView TV_Ubicacion;
    private TextView TV_Transacciones;
    private TextView TV_Minutos;
    private TextView TV_Prepago;
    private TextView TV_Efectivo;
    private TextView TV_Total;

    private DatePickerDialog fechaDatePickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recaudacion);
        this.setTitle("Recaudación");
        inicio();
        setDateTimeField();
        getRecaudacion();
    }

    private void inicio(){

        EDT_FechaRecaudacion = (EditText) findViewById(R.id.EDT_FechaRecaudacion);
        TV_Operador          = (TextView) findViewById(R.id.TV_Operador);
        TV_Ubicacion         = (TextView) findViewById(R.id.TV_Ubicacion);
        TV_Transacciones     = (TextView) findViewById(R.id.TV_Transacciones);
        TV_Minutos           = (TextView) findViewById(R.id.TV_Minutos);
        TV_Prepago           = (TextView) findViewById(R.id.TV_Prepago);
        TV_Efectivo          = (TextView) findViewById(R.id.TV_Efectivo);
        TV_Total             = (TextView) findViewById(R.id.TV_Total);

        EDT_FechaRecaudacion.setInputType(InputType.TYPE_NULL);
        EDT_FechaRecaudacion.requestFocus();
        EDT_FechaRecaudacion.setText(AppHelper.fechaFormat.format(new Date()));
    }

    private void setDateTimeField() {
        EDT_FechaRecaudacion.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        fechaDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                EDT_FechaRecaudacion.setText(AppHelper.fechaFormat.format(newDate.getTime()));
                getRecaudacion();
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

    }

    private void getRecaudacion(){

        TV_Operador.setText(AppHelper.getUsuario_nombre());
        TV_Ubicacion.setText(AppHelper.getUbicacion_nombre());

        String fecha_recaudacion = EDT_FechaRecaudacion.getText().toString();

        Cursor c;
        try {
            fecha_recaudacion = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd-MM-yyyy").parse(fecha_recaudacion));

            String[] args = new String[]{String.valueOf(AppHelper.getUbicacion_id()), String.valueOf(AppHelper.getUsuario_rut()), fecha_recaudacion };
            String qry = "SELECT  trp.id_cliente_ubicacion, "+
                                  "tcu.descripcion, "+
                                  "tcu.minutos_gratis, "+
                                  "tcu.valor_minuto, "+
                                  "trp.rut_usuario_out, "+
                                  "tu.nombre, "+
                                  "COUNT(*) AS transacciones_total, "+
                            "SUM(trp.minutos) AS recaudacion_minutos, "+
                            "SUM(trp.prepago) AS recaudacion_prepago, "+
                            "SUM(trp.efectivo) AS recaudacion_efectivo, "+
                            "SUM(trp.precio) AS recaudacion_total "+
                            "FROM tb_registro_patentes trp "+
                            "INNER JOIN tb_cliente_ubicaciones tcu ON tcu.id = trp.id_cliente_ubicacion "+
                            "INNER JOIN tb_usuario tu ON tu.rut = trp.rut_usuario_out "+
                            "WHERE trp.id_cliente_ubicacion=? AND tu.rut =? AND DATE(trp.fecha_hora_out) =? "+
                            "GROUP BY trp.id_cliente_ubicacion, rut_usuario_out ";

            c = AppHelper.getParkgoSQLite().rawQuery(qry, args);
            if (c.moveToFirst()) {

                String rs_transacciones = c.getString(6);
                int rs_minutos = c.getInt(7);
                int rs_prepago = c.getInt(8);
                int rs_efectivo= c.getInt(9);
                int rs_total   = c.getInt(10);

                TV_Transacciones.setText(rs_transacciones);
                TV_Minutos.setText(String.format("%,d", rs_minutos).replace(",","."));
                TV_Prepago.setText("$"+String.format("%,d", rs_prepago).replace(",","."));
                TV_Efectivo.setText("$"+String.format("%,d",rs_efectivo).replace(",","."));
                TV_Total.setText("$"+String.format("%,d", rs_total).replace(",","."));

            } else {

                TV_Transacciones.setText("0");
                TV_Minutos.setText("0");
                TV_Prepago.setText("$0");
                TV_Efectivo.setText("$0");
                TV_Total.setText("$0");

            }
            c.close();

        } catch (SQLException e) {
            Util.alertDialog(Recaudacion.this, "SQLException Recaudación", e.getMessage());
        } catch (ParseException e) {
            Util.alertDialog(Recaudacion.this, "SQLException ParseException", e.getMessage());
        }

    }

    @Override
    public void onClick(View v) {
        if(v == EDT_FechaRecaudacion) {
            fechaDatePickerDialog.show();
        }
    }
}
