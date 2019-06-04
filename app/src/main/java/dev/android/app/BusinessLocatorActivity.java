package dev.android.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

public class BusinessLocatorActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private static LatLng latLng;
    private Location location;
    private boolean generateAlertDialogAgain=true;
    private boolean permissionGiven=false;
    static private String[] permissionsArray = new String[2];
    final private int LOCATION_PERMISSION_REQUEST = 0;
    private boolean shouldMove=false;
    BusinessLocatorActivity(){
        permissionsArray[0] = android.Manifest.permission.ACCESS_FINE_LOCATION;
        permissionsArray[1] = Manifest.permission.ACCESS_COARSE_LOCATION;

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_locator);


        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        DatabaseReference ownersReference=FirebaseDatabase.getInstance().getReference("owners");
        DatabaseReference accountsReference=FirebaseDatabase.getInstance().getReference("accounts");

        ownersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String uid=getIntent().getStringExtra("uid");
              String  businessTitle=dataSnapshot.child(uid).child("business_title").getValue().toString();
                String contact=dataSnapshot.child(uid).child("contact").getValue().toString();
                String address=dataSnapshot.child(uid).child("address").getValue().toString();
                double latitude= Double.parseDouble(dataSnapshot.child(uid).child("location").child("latitude").getValue().toString());
                double longitude=Double.parseDouble(dataSnapshot.child(uid).child("location").child("longitude").getValue().toString());
                 BusinessLocatorActivity.latLng=new LatLng(latitude,longitude);
                ((TextView) findViewById(R.id.businessTitleTextView)).setText(businessTitle);
                ((TextView) findViewById(R.id.addressTextView)).setText(address);
                ((TextView) findViewById(R.id.contactTextView)).setText(contact);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        accountsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String uid=getIntent().getStringExtra("uid");
                String ownerName=dataSnapshot.child(uid).child("name").getValue().toString();
                ((TextView) findViewById(R.id.ownerNameTextView)).setText(ownerName);
                shouldMove=true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        progressDialog.dismiss();

        if (!weatherPermissionsGiven(permissionsArray)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissionsArray, LOCATION_PERMISSION_REQUEST);
            } else {
                final AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(BusinessLocatorActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
                } else {
                    BusinessLocatorActivity.this.setTheme(R.style.Theme_AppCompat_DayNight_Dialog);
                    builder = new AlertDialog.Builder(BusinessLocatorActivity.this);
                }
                builder.setTitle("Permission denied")
                        .setMessage("Go to apps settings and enable the location permission to " + getString(R.string.app_name))
                        .setPositiveButton("okay", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                generateAlertDialogAgain = false;
                                dialog.cancel();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (generateAlertDialogAgain) {
                                    builder.show();
                                }
                            }
                        })
                        .show();
            }
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        onMapReady(googleMap);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(shouldMove) {
            LatLng latLng=new LatLng(18.4669082,73.8347385);
            googleMap = googleMap;
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Business"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            Toast.makeText(BusinessLocatorActivity.this,"enter",Toast.LENGTH_SHORT).show();
        }
    }

    public void MoveMap(View V){
        onMapReady(googleMap);
    }

    private boolean weatherPermissionsGiven(String[] permissionsArray) {
        boolean allPermissionGiven = true;
        for (int i = 0; i < permissionsArray.length; i++) {
            if (BusinessLocatorActivity.this.checkCallingOrSelfPermission(permissionsArray[0]) == PackageManager.PERMISSION_DENIED)
                allPermissionGiven = false;
        }
        return allPermissionGiven;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean allPermissionsGiven = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allPermissionsGiven = false;
                }
            }
            if (!allPermissionsGiven) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean shouldShowRequestPermissionRational = false;
                    for (int i = 0; i < permissions.length; i++) {
                        shouldShowRequestPermissionRational = shouldShowRequestPermissionRational || shouldShowRequestPermissionRationale(permissions[i]);
                    }
                    if (shouldShowRequestPermissionRational) {
                        showPermissionAlert();
                    } else {
                        Toast.makeText(this, "Go to apps settings and enable the location permission to " + getString(R.string.app_name), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Go to apps settings and enable the location permission to " + getString(R.string.app_name), Toast.LENGTH_SHORT).show();
                }
            }
            permissionGiven=allPermissionsGiven;
        }
    }

    private void showPermissionAlert() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(BusinessLocatorActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
        } else {
            BusinessLocatorActivity.this.setTheme(R.style.Theme_AppCompat_DayNight_Dialog);
            builder = new AlertDialog.Builder(BusinessLocatorActivity.this);
        }
        builder.setTitle("Permission denied")
                .setMessage("Give location access to save your address")
                .setPositiveButton("sure", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        generateAlertDialogAgain = false;
                        dialog.cancel();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(permissionsArray, LOCATION_PERMISSION_REQUEST);
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (generateAlertDialogAgain) {
                            builder.show();
                        }
                    }
                })
                .show();
    }

}
