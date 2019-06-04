package dev.android.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

public class LocationProvider  implements LocationListener {
    Context context;
    public LocationProvider(Context context){
        this.context=context;
    }
    @SuppressLint("MissingPermission")
    public Location getLocation() {
        //  Toast.makeText(RegisterActivity.this, "Enter", Toast.LENGTH_SHORT).show();
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Location service is off");  // GPS not found
                builder.setMessage("Enable Location to access device location"); // Want to enable?
                builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
                builder.setNegativeButton("cancel", null);
                builder.create().show();
                return null;
            } else {
                // Toast.makeText(RegisterActivity.this, "Enabled", Toast.LENGTH_SHORT).show();
                Location location = null;
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5, this);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                return location;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Location error", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
