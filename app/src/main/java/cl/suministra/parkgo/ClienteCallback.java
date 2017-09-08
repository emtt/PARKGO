package cl.suministra.parkgo;

/**
 * Created by LENOVO on 07-09-2017.
 */

public interface ClienteCallback {
    void onResponse(int esError, int statusCode, String responseBody);
}
