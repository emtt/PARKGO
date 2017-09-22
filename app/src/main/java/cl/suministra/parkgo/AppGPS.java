package cl.suministra.parkgo;


import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

/**
 * Created by LENOVO on 14-09-2017.
 */

public class AppGPS implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private FusedLocationProviderApi locationProvider = LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient = new GoogleApiClient.Builder(App.context)
                                                                 .addApi(LocationServices.API)
                                                                 .addConnectionCallbacks(this)
                                                                 .addOnConnectionFailedListener(this)
                                                                 .build();
    private LocationRequest locationRequest = new LocationRequest()
                                                        .setInterval( 1 * 1000) // 1 seg
                                                        .setFastestInterval(5 * 1000) // 5 seg
                                                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    public void conectaGPS(){
        if (!googleApiClient.isConnected()){
            googleApiClient.connect();
        }
    }

    public void desconectaGPS(){
        if (googleApiClient.isConnected()){
            googleApiClient.disconnect();
        }
    }

    public void pausarGPS(){
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        requestLocationUpdates();
    }

    public void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        //locationChage(locationResult.getLastLocation());
                    }
                }, Looper.myLooper());
    }

    /* Entrega la ultima ubicación registrada mediante Google Play Services SDK (v11+) */
    public static void getLastLocation(final GPSCallback gpsCallback) {

        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(App.context);

        if (ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        gpsCallback.onResponseSuccess(location);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        gpsCallback.onResponseFailure(e);
                        Log.d(AppHelper.LOG_TAG, "AppGPS getLastLocation.onFailure "+e.getMessage());
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    public void locationChage(Location location){
        Toast.makeText(App.context, "Latitud "+Double.toString(location.getLatitude())+
                                    " Longitud "+Double.toString(location.getLongitude()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
    }


}
