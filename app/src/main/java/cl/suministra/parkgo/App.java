package cl.suministra.parkgo;

import android.app.Application;
import android.content.Context;

/**
 * Created by LENOVO on 08-09-2017.
 */

public class App extends Application {

    public static Context context;

    @Override public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

}
