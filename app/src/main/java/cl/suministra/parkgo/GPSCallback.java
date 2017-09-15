package cl.suministra.parkgo;

import android.location.Location;

/**
 * Created by LENOVO on 14-09-2017.
 */

public interface GPSCallback {
    void onResponseSuccess(Location location);
    void onResponseFailure(Exception e);
}
