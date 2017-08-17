package cl.suministra.parkgo;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.BaseMenuPresenter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ListaPatente extends AppCompatActivity {


    private ListView LST_Patente;
    private ImageView IMG_Vehiculo;
    private TextView TV_Patente;
    private TextView TV_Fecha_IN;
    private TextView TV_Espacios;
    private TextView TV_Minutos;

   List<PatentesPendiente> patentesList = new ArrayList<PatentesPendiente>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_patente);
        inicio();
    }

    private void inicio(){

        ConsultaPatentesPendiente();

        LST_Patente = (ListView) findViewById(R.id.LST_Patente);
        CustomAdapter customAdapter = new CustomAdapter();
        LST_Patente.setAdapter(customAdapter);


    }

    private void ConsultaPatentesPendiente(){

        String[] args = new String[]{"0"};
        Cursor c = AppHelper.getParkgoSQLite().rawQuery("SELECT\n" +
                                                        "id, patente, fecha_hora_in,\n" +
                                                        "datetime('now','localtime') as fecha_hora_out,\n" +
                                                        "espacios,\n" +
                                                        "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) As Integer) as dias,\n" +
                                                        "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) * 24 As Integer) as horas,\n" +
                                                        "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) * 24 * 60 As Integer) as minutos,\n" +
                                                        "CAST((JulianDay(datetime('now','localtime')) - JulianDay(fecha_hora_in)) * 24 * 60 * 60 As Integer) as segundos\n" +
                                                        "FROM tb_registro_patente\n" +
                                                        "WHERE finalizado =?", args);
        if (c.moveToFirst()) {
            do {
                String rs_patente = c.getString(1);
                String rs_fecha_hora_in = c.getString(2);
                int rs_espacios   = c.getInt(4);
                int rs_minutos    = c.getInt(7);
                PatentesPendiente patentePendiente = new PatentesPendiente(rs_patente,rs_fecha_hora_in, rs_espacios, rs_minutos);
                patentesList.add(patentePendiente);
            } while(c.moveToNext());
        }

    }

    class CustomAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return patentesList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.activity_lista_patente_custom, null);
            IMG_Vehiculo = (ImageView) convertView.findViewById(R.id.IMG_Vehiculo);
            TV_Patente   = (TextView)  convertView.findViewById(R.id.TV_Patente);
            TV_Fecha_IN  = (TextView)  convertView.findViewById(R.id.TV_Fecha_IN);
            TV_Espacios  = (TextView)  convertView.findViewById(R.id.TV_Espacios);
            TV_Minutos   = (TextView)  convertView.findViewById(R.id.TV_Minutos);

            TV_Patente.setText(String.valueOf(patentesList.get(position).Patente));
            TV_Fecha_IN.setText("Ingreso: "+String.valueOf(patentesList.get(position).Fecha_IN));
            TV_Espacios.setText("Espacios: "+String.valueOf(patentesList.get(position).Espacios));
            TV_Minutos.setText("Minutos: "+String.valueOf(patentesList.get(position).Minutos));

            return convertView;

        }
    }


    public class PatentesPendiente{

        String Patente;
        String Fecha_IN;
        int Espacios;
        int Minutos;

        public PatentesPendiente(String patente, String fecha_in, int espacios, int minutos){
            this.Patente  = patente;
            this.Fecha_IN = fecha_in;
            this.Espacios = espacios;
            this.Minutos  = minutos;
        }

    }

}
