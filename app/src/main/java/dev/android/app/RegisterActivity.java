package dev.android.app;

;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

import static android.app.PendingIntent.getActivity;

public class RegisterActivity extends AppCompatActivity implements LocationListener {
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    DatabaseReference accountsReference;
    DatabaseReference statisticsReference;
    DatabaseReference uidsReference;
    DatabaseReference ownersRefernce;
    LatLng latitudeLongitude;
    static public boolean generateAlertDialogAgain = true;

    static private String[] permissionsArray = new String[2];
    final private int LOCATION_PERMISSION_REQUEST = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        permissionsArray[0] = Manifest.permission.ACCESS_FINE_LOCATION;
        permissionsArray[1] = Manifest.permission.ACCESS_COARSE_LOCATION;

        findViewById(R.id.businessTitleTextView).setEnabled(false);
        findViewById(R.id.morningTimeTextView).setEnabled(false);
        findViewById(R.id.eveningTimeTextView).setEnabled(false);
        findViewById(R.id.getCurrentLocationButton).setEnabled(false);
        findViewById(R.id.locationTextView).setEnabled(false);

        accountsReference = FirebaseDatabase.getInstance().getReference("accounts");

        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterActivity.this.register();
            }
        });

        Switch registerAsOwnerSwitch = findViewById(R.id.registerAsOwnerSwitch);
        registerAsOwnerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    statisticsReference = FirebaseDatabase.getInstance().getReference("statistics");
                    uidsReference = FirebaseDatabase.getInstance().getReference("uids");
                    ownersRefernce = FirebaseDatabase.getInstance().getReference("owners");
                    findViewById(R.id.businessTitleTextView).setEnabled(true);
                    findViewById(R.id.morningTimeTextView).setEnabled(true);
                    findViewById(R.id.eveningTimeTextView).setEnabled(true);
                    findViewById(R.id.getCurrentLocationButton).setEnabled(true);
                    findViewById(R.id.locationTextView).setEnabled(false);
                } else {
                    findViewById(R.id.businessTitleTextView).setEnabled(false);
                    findViewById(R.id.morningTimeTextView).setEnabled(false);
                    findViewById(R.id.eveningTimeTextView).setEnabled(false);
                    findViewById(R.id.getCurrentLocationButton).setEnabled(false);
                    findViewById(R.id.locationTextView).setEnabled(false);
                }
            }
        });

        Button getCurrentLocation = findViewById(R.id.getCurrentLocationButton);
        getCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!weatherPermissionsGiven(permissionsArray)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(permissionsArray, LOCATION_PERMISSION_REQUEST);
                    } else {
                        final AlertDialog.Builder builder;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder = new AlertDialog.Builder(RegisterActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
                        } else {
                            RegisterActivity.this.setTheme(R.style.Theme_AppCompat_DayNight_Dialog);
                            builder = new AlertDialog.Builder(RegisterActivity.this);
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
                } else {
                    Geocoder geocoder = new Geocoder(RegisterActivity.this, Locale.getDefault());
                    Location location = getLocation();
                    if (location != null) {
                        RegisterActivity.this.latitudeLongitude = new LatLng(location.getLatitude(), location.getLongitude());
                        //Toast.makeText(RegisterActivity.this, "LAT : "+location.getLatitude()+" LONG : "+location.getLongitude(),Toast.LENGTH_LONG).show();
                        List<Address> addressList = null;
                        try {
                            List<Address> addresses = geocoder.getFromLocation(latitudeLongitude.latitude, latitudeLongitude.longitude, 1);
                            if (addresses.size() > 0) {
                                Address address = addresses.get(0);
                                String addressString = address.getAddressLine(0);
                                TextView locationTextView = findViewById(R.id.locationTextView);
                                locationTextView.setText(addressString);
                                findViewById(R.id.locationTextView).setEnabled(true);
                            } else {
                                Toast.makeText(RegisterActivity.this, "Unable to detect your location", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(RegisterActivity.this, "Location error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                         Toast.makeText(RegisterActivity.this, "Location service is turned off", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        //Do nothing
    }

    private void register() {
        Switch registerAsOwnerSwitch = findViewById(R.id.registerAsOwnerSwitch);
        if (registerAsOwnerSwitch.isChecked()) {
            TextView nameTextView = findViewById(R.id.subscribersNameTextView);
            final String name = nameTextView.getText().toString();
            TextView businessTitleTextView = findViewById(R.id.businessTitleTextView);
            String businessTitle = businessTitleTextView.getText().toString();
            TextView morningTimeTextView = findViewById(R.id.morningTimeTextView);
            String morningTime = morningTimeTextView.getText().toString();
            TextView eveningTimeTextView = findViewById(R.id.eveningTimeTextView);
            String eveningTime = eveningTimeTextView.getText().toString();
            TextView addressTextView = findViewById(R.id.locationTextView);
            String address = addressTextView.getText().toString();
            final ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
            progressDialog.setMessage("saving details...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            if (!name.isEmpty() && !businessTitle.isEmpty() && !morningTime.isEmpty() && !address.isEmpty() && !eveningTime.isEmpty() && !(latitudeLongitude == null)) {
                accountsReference.child(TimelineActivity.uid).child("name").setValue(name);
                accountsReference.child(TimelineActivity.uid).child("account_type").setValue("OWNER");
                accountsReference.child(TimelineActivity.uid).child("contact_number").setValue(firebaseAuth.getCurrentUser().getPhoneNumber());

                ownersRefernce.child(TimelineActivity.uid).child("business_title").setValue(businessTitle);
                ownersRefernce.child(TimelineActivity.uid).child("morning_time").setValue(morningTime);
                ownersRefernce.child(TimelineActivity.uid).child("evening_time").setValue(eveningTime);
                ownersRefernce.child(TimelineActivity.uid).child("contact").setValue(firebaseAuth.getCurrentUser().getPhoneNumber());
                ownersRefernce.child(TimelineActivity.uid).child("address").setValue(address);
                ownersRefernce.child(TimelineActivity.uid).child("location").child("latitude").setValue(latitudeLongitude.latitude);
                ownersRefernce.child(TimelineActivity.uid).child("location").child("longitude").setValue(latitudeLongitude.longitude);
                ownersRefernce.child(TimelineActivity.uid).child("latest_menu_url").setValue("");
                ownersRefernce.child(TimelineActivity.uid).child("menu_description_text").setValue("");

                statisticsReference.child("owner_count").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 0;
                        try {
                            count = Integer.parseInt(dataSnapshot.getValue().toString());
                        } catch (NullPointerException e) {
                            //count not yet set
                        }
                        uidsReference.child(String.valueOf(count)).setValue(TimelineActivity.uid);
                        count++;
                        statisticsReference.child("owner_count").setValue(count);
                        Toast.makeText(RegisterActivity.this, "Registered successfully", Toast.LENGTH_SHORT);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
                progressDialog.dismiss();
                Intent intentToStartTimelineActivity=new Intent(RegisterActivity.this,TimelineActivity.class);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentToStartTimelineActivity);
            } else {
                Toast.makeText(this, "All field are mandatory", Toast.LENGTH_SHORT).show();
            }
        } else {
            TextView nameTextView = findViewById(R.id.subscribersNameTextView);
            String name = nameTextView.getText().toString();
            final ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
            progressDialog.setMessage("storing details...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            if (!name.isEmpty()) {
                accountsReference.child(TimelineActivity.uid).child("name").setValue(name);
                accountsReference.child(TimelineActivity.uid).child("account_type").setValue("USER");
                accountsReference.child(TimelineActivity.uid).child("contact_number").setValue(firebaseAuth.getCurrentUser().getPhoneNumber());
                Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT);
                progressDialog.dismiss();
                Intent intentToStartTimelineActivity=new Intent(RegisterActivity.this,TimelineActivity.class);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentToStartTimelineActivity);
            } else {
                Toast.makeText(this, "Enter name to register", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        generateAlertDialogAgain = true;
    }



    private boolean weatherPermissionsGiven(String[] permissionsArray) {
        boolean allPermissionGiven = true;
        for (int i = 0; i < permissionsArray.length; i++) {
            if (RegisterActivity.this.checkCallingOrSelfPermission(permissionsArray[0]) == PackageManager.PERMISSION_DENIED)
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
        }
    }

    private void showPermissionAlert() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(RegisterActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
        } else {
            RegisterActivity.this.setTheme(R.style.Theme_AppCompat_DayNight_Dialog);
            builder = new AlertDialog.Builder(RegisterActivity.this);
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

    @SuppressLint("MissingPermission")
    public  Location getLocation() {
        //  Toast.makeText(RegisterActivity.this, "Enter", Toast.LENGTH_SHORT).show();
        try {
            LocationManager locationManager = (LocationManager) RegisterActivity.this.getSystemService(RegisterActivity.this.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                builder.setTitle("Location service is off");  // GPS not found
                builder.setMessage("Enable Location to access device location"); // Want to enable?
                builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        RegisterActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
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
            Toast.makeText(RegisterActivity.this, "Location error", Toast.LENGTH_LONG).show();
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
